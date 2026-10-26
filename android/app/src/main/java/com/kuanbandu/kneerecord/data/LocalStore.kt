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
