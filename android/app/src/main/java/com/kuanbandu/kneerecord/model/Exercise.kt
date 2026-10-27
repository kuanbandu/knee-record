package com.kuanbandu.kneerecord.model

/** 训练环节 */
enum class Slot(val label: String) {
    WARMUP("热身"),
    MAIN("主训练"),
    COOLDOWN("放松"),
}

/** 动作类别。疼痛门禁按类别拦截。 */
enum class ExerciseCategory(val label: String) {
    MOBILITY("活动度"),
    ISOMETRIC("等长收缩"),
    HIP("髋部"),
    SQUAT("深蹲"),
    LUNGE("弓步"),
    BALANCE("平衡"),
    JUMP("跳跃"),
    STRETCH("拉伸"),
}

/** 单次动作的计量方式 */
enum class DoseMode(val label: String) {
    HOLD("保持计时"),
    COUNT_UP("正计时"),
    REPS("次数"),
}

data class Dosage(
    val mode: DoseMode = DoseMode.REPS,
    /** HOLD/COUNT_UP：秒数；HOLD 为单次保持时长 */
    val seconds: Int = 0,
    /** HOLD/REPS：每组次数 */
    val reps: Int = 0,
    val sets: Int = 3,
    /** 组间休息秒数 */
    val restSec: Int = 45,
) {
    fun describe(): String = when (mode) {
        DoseMode.HOLD -> "每组保持 $seconds 秒 × $reps 次，共 $sets 组"
        DoseMode.COUNT_UP -> "每组持续 $seconds 秒，共 $sets 组"
        DoseMode.REPS -> "每组 $reps 次，共 $sets 组"
    }
}

data class Exercise(
    val id: String,
    val name: String,
    /** 难度等级 1-4 */
    val level: Int,
    val category: ExerciseCategory,
    val slot: Slot,
    val dosage: Dosage,
    /** 膝盖口令 */
    val kneeCue: String,
    /** 主要作用 */
    val target: String,
    val safety: List<String>,
    val mistakes: List<String>,
    /** 完成该动作至少需要多少度屈膝活动度，0 表示无要求 */
    val needsFlexion: Int = 0,
)
