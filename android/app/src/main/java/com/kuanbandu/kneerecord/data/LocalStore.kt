package com.kuanbandu.kneerecord.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.kuanbandu.kneerecord.model.Assessment
import com.kuanbandu.kneerecord.model.ExerciseEntry
import com.kuanbandu.kneerecord.model.Record
import com.kuanbandu.kneerecord.model.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 本地存储层：三个 JSON 文件放在应用私有目录，无后端、无上传。
 * 界面只允许通过这里读写数据。
 */
object LocalStore {

    private const val F_RECORDS = "knee_records.json"
    private const val F_ASSESSMENT = "knee_assessment.json"
    private const val F_SETTINGS = "knee_settings.json"

    /** 记录条数上限 */
    private const val MAX_RECORDS = 200

    /** 数据版本号：任何写入后自增，界面靠它决定是否重新读取 */
    val recordsVersion = mutableStateOf(0)

    private lateinit var dir: File

    fun init(context: Context) {
        dir = context.filesDir
    }

    // -------------------------------------------------------------- 读写工具

    private fun file(name: String) = File(dir, name)

    private fun readArray(name: String): JSONArray {
        val f = file(name)
        if (!f.exists()) return JSONArray()
        return try {
            JSONArray(f.readText())
        } catch (ignored: Exception) {
            JSONArray()
        }
    }

    private fun readObject(name: String): JSONObject {
        val f = file(name)
        if (!f.exists()) return JSONObject()
        return try {
            JSONObject(f.readText())
        } catch (ignored: Exception) {
            JSONObject()
        }
    }

    private fun write(name: String, text: String) {
        try {
            file(name).writeText(text)
        } catch (ignored: Exception) {
            // 写入失败不阻断记录流程
        }
    }

    // -------------------------------------------------------------- 锻炼记录

    fun getRecords(): List<Record> {
        val arr = readArray(F_RECORDS)
        val out = mutableListOf<Record>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val exs = o.optJSONArray("exercises") ?: JSONArray()
            val exercises = mutableListOf<ExerciseEntry>()
            for (j in 0 until exs.length()) {
                val e = exs.optJSONObject(j) ?: continue
                exercises.add(ExerciseEntry(e.optString("name"), e.optString("sets")))
            }
            out.add(
                Record(
                    id = o.optString("id"),
                    date = o.optString("date"),
                    timestamp = o.optLong("timestamp"),
                    feelingScore = o.optInt("feelingScore"),
                    durationMin = o.optInt("durationMin"),
                    note = o.optString("note"),
                    exercises = exercises,
                ),
            )
        }
        return out
    }

    fun addRecord(record: Record) {
        val arr = readArray(F_RECORDS)
        val out = JSONArray()
        out.put(recordToJson(record))
        for (i in 0 until arr.length()) {
            if (out.length() >= MAX_RECORDS) break
            out.put(arr.optJSONObject(i) ?: continue)
        }
        write(F_RECORDS, out.toString())
        recordsVersion.value++
    }

    fun removeRecord(id: String) {
        val arr = readArray(F_RECORDS)
        val out = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optString("id") != id) out.put(o)
        }
        write(F_RECORDS, out.toString())
        recordsVersion.value++
    }

    private fun recordToJson(r: Record): JSONObject {
        val exs = JSONArray()
        r.exercises.forEach { e ->
            val o = JSONObject()
            o.put("name", e.name)
            o.put("sets", e.sets)
            exs.put(o)
        }
        val o = JSONObject()
        o.put("id", r.id)
        o.put("date", r.date)
        o.put("timestamp", r.timestamp)
        o.put("feelingScore", r.feelingScore)
        o.put("durationMin", r.durationMin)
        o.put("note", r.note)
        o.put("exercises", exs)
        return o
    }

    // -------------------------------------------------------------- 个人资料

    fun getAssessment(): Assessment? {
        val o = readObject(F_ASSESSMENT)
        val stage = o.optString("stage", "")
        if (stage.isBlank()) return null
        return Assessment(
            age = o.optInt("age", 0),
            stage = stage,
            bendAngle = o.optInt("bendAngle", 90),
            straightenOk = o.optBoolean("straightenOk", false),
            updatedAt = o.optLong("updatedAt", 0L),
        )
    }

    fun saveAssessment(a: Assessment) {
        val o = JSONObject()
        o.put("age", a.age)
        o.put("stage", a.stage)
        o.put("bendAngle", a.bendAngle)
        o.put("straightenOk", a.straightenOk)
        o.put("updatedAt", System.currentTimeMillis())
        write(F_ASSESSMENT, o.toString())
    }

    // -------------------------------------------------------------- 设置

    fun getSettings(): Settings {
        val o = readObject(F_SETTINGS)
        val time = o.optString("reminderTime", "")
        return Settings(
            reminderEnabled = o.optBoolean("reminderEnabled", false),
            reminderTime = if (time.isBlank()) "19:00" else time,
        )
    }

    /** 分次保存时与已有设置合并，避免互相覆盖 */
    fun updateSettings(reminderEnabled: Boolean? = null, reminderTime: String? = null) {
        val cur = getSettings()
        val o = JSONObject()
        o.put("reminderEnabled", reminderEnabled ?: cur.reminderEnabled)
        o.put("reminderTime", reminderTime ?: cur.reminderTime)
        write(F_SETTINGS, o.toString())
    }

    fun clearAll() {
        write(F_RECORDS, JSONArray().toString())
        file(F_ASSESSMENT).delete()
        updateSettings(reminderEnabled = false, reminderTime = "19:00")
        recordsVersion.value++
    }

    // -------------------------------------------------------------- 备份导出 / 导入

    /** 完整备份：记录 + 个人资料 + 设置 */
    fun exportJson(): String {
        val root = JSONObject()
        root.put("app", "knee-record")
        root.put("version", 1)
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
            ao.put("bendAngle", a.bendAngle)
            ao.put("straightenOk", a.straightenOk)
            ao.put("updatedAt", a.updatedAt)
            root.put("assessment", ao)
        }

        val s = getSettings()
        val so = JSONObject()
        so.put("reminderEnabled", s.reminderEnabled)
        so.put("reminderTime", s.reminderTime)
        root.put("settings", so)
        return root.toString(2)
    }

    /** 表格导出：一条记录一行，带 UTF-8 BOM，Excel 打开中文不乱码 */
    fun exportCsv(): String {
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val time = SimpleDateFormat("HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append("日期,时间,身体感受,时长(分钟),动作,备注\n")
        getRecords().forEach { r ->
            val exercises = r.exercises.joinToString(" ") {
                if (it.sets.isBlank()) it.name else "${it.name}(${it.sets}组)"
            }
            val cells = listOf(
                day.format(Date(r.timestamp)),
                time.format(Date(r.timestamp)),
                r.feelingScore.toString(),
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

    /** 导入前先看文件里有多少条，失败会抛异常由界面提示 */
    fun parseImportCount(text: String): Int {
        val root = JSONObject(text)
        val arr = root.optJSONArray("records") ?: throw IllegalArgumentException("文件里没有记录数据")
        return arr.length()
    }

    /**
     * 导入备份。merge = true 按 id 去重后补充到现有记录；false 先清空再导入。
     * 返回文件里的记录条数。
     */
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
                    feelingScore = o.optInt("feelingScore"),
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
        write(F_RECORDS, out.toString())

        // 个人资料：文件里有就覆盖，没有则保持本机不变
        val ao = root.optJSONObject("assessment")
        if (ao != null) {
            val stage = ao.optString("stage", "")
            if (stage.isNotBlank()) {
                saveAssessment(
                    Assessment(
                        age = ao.optInt("age", 0),
                        stage = stage,
                        bendAngle = ao.optInt("bendAngle", 90),
                        straightenOk = ao.optBoolean("straightenOk", false),
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
            )
        }

        recordsVersion.value++
        return incoming.size
    }

    // -------------------------------------------------------------- 工具

    /** YYYY-MM-DD */
    fun todayKey(ts: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(ts))

    /** MM-DD HH:mm */
    fun formatDateTime(ts: Long): String =
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))

    /** 打开应用时若已过提醒时间且今天还没记录，返回一行提示文案 */
    fun reminderTip(): String {
        val s = getSettings()
        if (!s.reminderEnabled) return ""
        val parts = s.reminderTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return ""
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val now = Calendar.getInstance()
        val current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val today = todayKey()
        if (current < hour * 60 + minute) return ""
        if (getRecords().any { it.date == today }) return ""
        return "提醒时间是 ${s.reminderTime}，今天还没有记录。"
    }
}
