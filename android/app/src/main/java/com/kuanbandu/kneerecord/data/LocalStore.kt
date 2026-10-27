package com.kuanbandu.kneerecord.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.kuanbandu.kneerecord.model.Assessment
import com.kuanbandu.kneerecord.model.ExerciseEntry
import com.kuanbandu.kneerecord.model.MetronomeSound
import com.kuanbandu.kneerecord.model.Record
import com.kuanbandu.kneerecord.model.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 本地存储层（应用私有目录，三个 JSON 文件）。
 * 所有界面只允许通过这里读写数据。
 */
object LocalStore {

    private const val F_RECORDS = "knee_records.json"
    private const val F_ASSESSMENT = "knee_assessment.json"
    private const val F_SETTINGS = "knee_settings.json"
    private const val MAX_RECORDS = 200

    /** 节拍快慢的可选范围 */
    const val METRONOME_BPM_MIN = 40
    const val METRONOME_BPM_MAX = 180

    /** 记录版本号，界面用它触发刷新 */
    val recordsVersion = mutableStateOf(0)

    /** 个人资料版本号，训练清单据此重算 */
    val assessmentVersion = mutableStateOf(0)

    private lateinit var dir: File

    fun init(context: Context) {
        dir = context.filesDir
    }

    private fun file(name: String): File {
        if (!::dir.isInitialized) {
            throw IllegalStateException("LocalStore 未初始化")
        }
        return File(dir, name)
    }

    private fun readText(name: String): String? =
        runCatching { file(name).readText() }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun writeText(name: String, text: String) {
        file(name).writeText(text)
    }

    // -------------------------------------------------------------- 训练记录

    fun getRecords(): List<Record> {
        val arr = JSONArray(readText(F_RECORDS) ?: "[]")
        val out = mutableListOf<Record>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val exs = o.optJSONArray("exercises") ?: JSONArray()
            val exercises = mutableListOf<ExerciseEntry>()
            for (j in 0 until exs.length()) {
                val e = exs.optJSONObject(j) ?: continue
                exercises.add(ExerciseEntry(e.optString("name"), e.optString("sets")))
            }
            val ts = o.optLong("timestamp").takeIf { it > 0L } ?: System.currentTimeMillis()
            out.add(
                Record(
                    id = o.optString("id").ifBlank { "rec_$ts" },
                    date = o.optString("date").ifBlank { todayKey(ts) },
                    timestamp = ts,
                    painBefore = o.optInt("painBefore", o.optInt("feelingScore")),
                    painPeak = o.optInt("painPeak", o.optInt("feelingScore")),
                    rpe = o.optInt("rpe"),
                    durationMin = o.optInt("durationMin"),
                    note = o.optString("note"),
                    exercises = exercises,
                ),
            )
        }
        return out.sortedByDescending { it.timestamp }
    }

    fun addRecord(record: Record) {
        val list = getRecords().toMutableList()
        list.add(0, record)
        val arr = JSONArray()
        list.take(MAX_RECORDS).forEach { arr.put(recordToJson(it)) }
        writeText(F_RECORDS, arr.toString())
        recordsVersion.value++
    }

    fun deleteRecord(id: String) {
        val arr = JSONArray()
        getRecords().filter { it.id != id }.forEach { arr.put(recordToJson(it)) }
        writeText(F_RECORDS, arr.toString())
        recordsVersion.value++
    }

    private fun recordToJson(r: Record): JSONObject {
        val o = JSONObject()
        o.put("id", r.id)
        o.put("date", r.date)
        o.put("timestamp", r.timestamp)
        o.put("painBefore", r.painBefore)
        o.put("painPeak", r.painPeak)
        o.put("rpe", r.rpe)
        o.put("durationMin", r.durationMin)
        o.put("note", r.note)
        val exs = JSONArray()
        r.exercises.forEach { e ->
            val eo = JSONObject()
            eo.put("name", e.name)
            eo.put("sets", e.sets)
            exs.put(eo)
        }
        o.put("exercises", exs)
        return o
    }

    /** 今天是否已经有训练记录 */
    fun hasToday(): Boolean = getRecords().any { it.date == todayKey() }

    // -------------------------------------------------------------- 评估结果

    fun getAssessment(): Assessment? {
        val text = readText(F_ASSESSMENT) ?: return null
        val o = runCatching { JSONObject(text) }.getOrNull() ?: return null
        if (o.optString("stage", "").isBlank()) return null
        return Assessment(
            age = o.optInt("age"),
            stage = o.optString("stage"),
            painScore = o.optInt("painScore", o.optInt("feelingScore")),
            romFlexion = o.optInt("romFlexion", o.optInt("bendAngle", 90)),
            extensionLimited = o.optBoolean("extensionLimited", !o.optBoolean("straightenOk", true)),
            updatedAt = o.optLong("updatedAt"),
        )
    }

    fun saveAssessment(a: Assessment) {
        val o = JSONObject()
        o.put("age", a.age)
        o.put("stage", a.stage)
        o.put("painScore", a.painScore)
        o.put("romFlexion", a.romFlexion)
        o.put("extensionLimited", a.extensionLimited)
        o.put("updatedAt", a.updatedAt)
        writeText(F_ASSESSMENT, o.toString())
        assessmentVersion.value++
    }

    // -------------------------------------------------------------- 设置

    fun getSettings(): Settings {
        val o = runCatching { JSONObject(readText(F_SETTINGS) ?: "{}") }.getOrNull() ?: JSONObject()
        return Settings(
            reminderEnabled = o.optBoolean("reminderEnabled", false),
            reminderTime = o.optString("reminderTime", "").takeIf { it.isNotBlank() } ?: "19:00",
            consentAt = o.optLong("consentAt"),
            metronomeEnabled = o.optBoolean("metronomeEnabled", true),
            metronomeBpm = o.optInt("metronomeBpm", 60).coerceIn(METRONOME_BPM_MIN, METRONOME_BPM_MAX),
            metronomeSound = o.optString("metronomeSound", "")
                .takeIf { it.isNotBlank() } ?: MetronomeSound.WOOD.key,
            metronomeVolume = o.optInt("metronomeVolume", 70).coerceIn(0, 100),
            metronomeAccent = o.optInt("metronomeAccent", 4).coerceIn(0, 8),
            countAloud = o.optBoolean("countAloud", false),
            restCue = o.optBoolean("restCue", true),
            customSoundUri = o.optString("customSoundUri", ""),
        )
    }

    fun updateSettings(
        reminderEnabled: Boolean? = null,
        reminderTime: String? = null,
        consentAt: Long? = null,
        metronomeEnabled: Boolean? = null,
        metronomeBpm: Int? = null,
        metronomeSound: String? = null,
        metronomeVolume: Int? = null,
        metronomeAccent: Int? = null,
        countAloud: Boolean? = null,
        restCue: Boolean? = null,
        customSoundUri: String? = null,
    ): Settings {
        val current = getSettings()
        val next = Settings(
            reminderEnabled = reminderEnabled ?: current.reminderEnabled,
            reminderTime = reminderTime ?: current.reminderTime,
            consentAt = consentAt ?: current.consentAt,
            metronomeEnabled = metronomeEnabled ?: current.metronomeEnabled,
            metronomeBpm = metronomeBpm ?: current.metronomeBpm,
            metronomeSound = metronomeSound ?: current.metronomeSound,
            metronomeVolume = metronomeVolume ?: current.metronomeVolume,
            metronomeAccent = metronomeAccent ?: current.metronomeAccent,
            countAloud = countAloud ?: current.countAloud,
            restCue = restCue ?: current.restCue,
            customSoundUri = customSoundUri ?: current.customSoundUri,
        )
        val o = JSONObject()
        o.put("reminderEnabled", next.reminderEnabled)
        o.put("reminderTime", next.reminderTime)
        o.put("consentAt", next.consentAt)
        o.put("metronomeEnabled", next.metronomeEnabled)
        o.put("metronomeBpm", next.metronomeBpm)
        o.put("metronomeSound", next.metronomeSound)
        o.put("metronomeVolume", next.metronomeVolume)
        o.put("metronomeAccent", next.metronomeAccent)
        o.put("countAloud", next.countAloud)
        o.put("restCue", next.restCue)
        o.put("customSoundUri", next.customSoundUri)
        writeText(F_SETTINGS, o.toString())
        return next
    }

    fun clearAll() {
        runCatching {
            writeText(F_RECORDS, "[]")
            writeText(F_ASSESSMENT, "")
            writeText(F_SETTINGS, "")
            recordsVersion.value++
            assessmentVersion.value++
        }
    }

    // -------------------------------------------------------------- 备份导出 / 导入

    fun exportJson(): String {
        val root = JSONObject()
        root.put("app", "knee-record")
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        val arr = JSONArray()
        getRecords().forEach { arr.put(recordToJson(it)) }
        root.put("records", arr)

        val a = getAssessment()
        if (a == null) {
            root.put("assessment", JSONObject.NULL)
        } else {
            val ao = JSONObject()
            ao.put("age", a.age)
            ao.put("stage", a.stage)
            ao.put("painScore", a.painScore)
            ao.put("romFlexion", a.romFlexion)
            ao.put("extensionLimited", a.extensionLimited)
            ao.put("updatedAt", a.updatedAt)
            root.put("assessment", ao)
        }

        val s = getSettings()
        val so = JSONObject()
        so.put("reminderEnabled", s.reminderEnabled)
        so.put("reminderTime", s.reminderTime)
        so.put("metronomeEnabled", s.metronomeEnabled)
        so.put("metronomeBpm", s.metronomeBpm)
        so.put("metronomeSound", s.metronomeSound)
        so.put("metronomeVolume", s.metronomeVolume)
        so.put("metronomeAccent", s.metronomeAccent)
        so.put("countAloud", s.countAloud)
        so.put("restCue", s.restCue)
        so.put("customSoundUri", s.customSoundUri)
        root.put("settings", so)
        return root.toString(2)
    }

    fun exportCsv(): String {
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val time = SimpleDateFormat("HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append("日期,时间,训练前疼痛,疼痛峰值,自觉疲劳(RPE),时长(分钟),动作,备注\n")
        getRecords().forEach { r ->
            val exercises = r.exercises.joinToString(" ") {
                if (it.sets.isBlank()) it.name else "${it.name}(${it.sets}组)"
            }
            val cells = listOf(
                day.format(Date(r.timestamp)),
                time.format(Date(r.timestamp)),
                r.painBefore.toString(),
                r.painPeak.toString(),
                r.rpe.toString(),
                r.durationMin.toString(),
                exercises,
                r.note,
            )
            sb.append(cells.joinToString(",") { csvCell(it) }).append("\n")
        }
        return sb.toString()
    }

    private fun csvCell(value: String): String {
        val needQuote = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needQuote) "\"${value.replace("\"", "\"\"")}\"" else value
    }

    fun parseImportCount(text: String): Int {
        val root = JSONObject(text)
        val arr = root.optJSONArray("records") ?: throw IllegalArgumentException("文件里没有记录数据")
        return arr.length()
    }

    fun importFromJson(text: String, merge: Boolean): Int {
        val root = JSONObject(text)
        val arr = root.optJSONArray("records") ?: throw IllegalArgumentException("文件里没有记录数据")

        val incoming = mutableListOf<Record>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val exs = o.optJSONArray("exercises") ?: JSONArray()
            val exercises = mutableListOf<ExerciseEntry>()
            for (j in 0 until exs.length()) {
                val e = exs.optJSONObject(j) ?: continue
                exercises.add(ExerciseEntry(e.optString("name"), e.optString("sets")))
            }
            val id = o.optString("id").ifBlank { "rec_${System.currentTimeMillis()}_$i" }
            val ts = o.optLong("timestamp").takeIf { it > 0L } ?: System.currentTimeMillis()
            incoming.add(
                Record(
                    id = id,
                    date = o.optString("date").ifBlank { todayKey(ts) },
                    timestamp = ts,
                    painBefore = o.optInt("painBefore", o.optInt("feelingScore")),
                    painPeak = o.optInt("painPeak", o.optInt("feelingScore")),
                    rpe = o.optInt("rpe"),
                    durationMin = o.optInt("durationMin"),
                    note = o.optString("note"),
                    exercises = exercises,
                ),
            )
        }
        if (incoming.isEmpty()) throw IllegalArgumentException("文件里没有可导入的记录")

        val final = if (merge) {
            val merged = getRecords().toMutableList()
            val known = merged.map { it.id }.toHashSet()
            incoming.forEach { if (it.id !in known) merged.add(it) }
            merged.sortedByDescending { it.timestamp }
        } else {
            incoming.sortedByDescending { it.timestamp }
        }

        val out = JSONArray()
        final.take(MAX_RECORDS).forEach { out.put(recordToJson(it)) }
        writeText(F_RECORDS, out.toString())

        val ao = root.optJSONObject("assessment")
        if (ao != null) {
            val stage = ao.optString("stage", "")
            if (stage.isNotBlank()) {
                saveAssessment(
                    Assessment(
                        age = ao.optInt("age", 0),
                        stage = stage,
                        painScore = ao.optInt("painScore"),
                        romFlexion = ao.optInt("romFlexion", 90),
                        extensionLimited = ao.optBoolean("extensionLimited", false),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }

        val so = root.optJSONObject("settings")
        if (so != null) {
            updateSettings(
                reminderEnabled = if (so.has("reminderEnabled")) so.optBoolean("reminderEnabled") else null,
                reminderTime = so.optString("reminderTime", "").takeIf { it.isNotBlank() },
                metronomeEnabled = if (so.has("metronomeEnabled")) so.optBoolean("metronomeEnabled") else null,
                metronomeBpm = if (so.has("metronomeBpm")) so.optInt("metronomeBpm") else null,
                metronomeSound = so.optString("metronomeSound", "").takeIf { it.isNotBlank() },
                metronomeVolume = if (so.has("metronomeVolume")) so.optInt("metronomeVolume") else null,
                metronomeAccent = if (so.has("metronomeAccent")) so.optInt("metronomeAccent") else null,
                countAloud = if (so.has("countAloud")) so.optBoolean("countAloud") else null,
                restCue = if (so.has("restCue")) so.optBoolean("restCue") else null,
                customSoundUri = so.optString("customSoundUri", ""),
            )
        }

        recordsVersion.value++
        return incoming.size
    }

    // -------------------------------------------------------------- 工具

    /** YYYY-MM-DD */
    fun todayKey(ts: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(ts))

    fun formatDateTime(ts: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}
