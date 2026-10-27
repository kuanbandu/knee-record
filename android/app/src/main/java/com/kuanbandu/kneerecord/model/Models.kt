package com.kuanbandu.kneerecord.model

/** 伤情阶段。maxLevel 是该阶段允许的动作最高等级。 */
enum class Stage(val key: String, val label: String, val desc: String, val maxLevel: Int) {
    EARLY("EARLY", "术后早期", "术后 0-6 周，以消肿、唤醒肌肉为主", 1),
    MID("MID", "术后中期", "术后 6-12 周，开始逐步加负荷", 2),
    LATE("LATE", "术后后期", "术后 3 个月以上，准备回到运动", 3),
    PREVENT("PREVENT", "未手术（预防损伤）", "没有手术，目的是预防损伤", 4),
    ;

    companion object {
        fun of(key: String): Stage? = entries.firstOrNull { it.key == key }
    }
}

val STAGE_OPTIONS: List<Stage> get() = Stage.entries.toList()

/** 评估结果（我的情况） */
data class Assessment(
    val age: Int = 0,
    val stage: String = "",
    val painScore: Int = 0,
    val romFlexion: Int = 90,
    val extensionLimited: Boolean = false,
    val updatedAt: Long = 0L,
)

/** 已经完成的动作与组数 */
data class ExerciseEntry(val name: String = "", val sets: String = "")

/** 一次完整训练的记录 */
data class Record(
    val id: String = "",
    val date: String = "",
    val timestamp: Long = 0L,
    val painBefore: Int = 0,
    val painPeak: Int = 0,
    val rpe: Int = 0,
    val durationMin: Int = 0,
    val note: String = "",
    val exercises: List<ExerciseEntry> = emptyList(),
)

data class Settings(
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "19:00",
    val consentAt: Long = 0L,
)
