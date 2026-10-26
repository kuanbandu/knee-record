package com.kuanbandu.kneerecord.model

/** 一次锻炼里做的单个动作，sets 可为空 */
data class ExerciseEntry(
    val name: String,
    val sets: String = "",
)

/** 一条锻炼记录 */
data class Record(
    val id: String,
    val date: String,
    val timestamp: Long,
    val feelingScore: Int,
    val durationMin: Int,
    val note: String,
    val exercises: List<ExerciseEntry>,
)

/** 个人资料，仅存档，不参与任何推荐 */
data class Assessment(
    val age: Int,
    val stage: String,
    val bendAngle: Int,
    val straightenOk: Boolean,
    val updatedAt: Long,
)

data class Settings(
    val reminderEnabled: Boolean,
    val reminderTime: String,
)

data class StageOption(
    val value: String,
    val label: String,
    val desc: String,
)

/** 阶段仅作为个人资料保存，不参与任何推荐 */
val STAGE_OPTIONS = listOf(
    StageOption("EARLY", "起步期", "刚开始，动作强度最低"),
    StageOption("MID", "进阶期", "已经练了一段时间"),
    StageOption("LATE", "强化期", "想加大强度"),
    StageOption("PREHAB", "日常保养", "让膝盖更稳一些"),
)

fun stageLabel(value: String): String =
    STAGE_OPTIONS.firstOrNull { it.value == value }?.label ?: ""
