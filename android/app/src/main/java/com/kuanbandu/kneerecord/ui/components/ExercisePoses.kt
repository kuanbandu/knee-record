package com.kuanbandu.kneerecord.ui.components

/**
 * 每个动作的示范动画帧。
 * 参数就是关节角度/髋高度，动画由这些关键帧插值出来，
 * 所以画面跟着动作的真实幅度走（比如全蹲 110° 就蹲到 110°）。
 */
private val SUPINE_FREE = Pose(
    stance = Stance.SUPINE,
    legMode = LegMode.FREE,
    hipY = 0.82f,
)

private val SIDE_FREE = Pose(
    stance = Stance.SIDE_LYING,
    legMode = LegMode.FREE,
    // 侧卧时髋离床面要留出骨盆的厚度，不然屈膝的腿会陷进地面
    hipY = 0.78f,
)

private val POSES: Map<String, List<AnimFrame>> = mapOf(

    // ---------------------------------------------------------- 热身

    "ankle-pump" to listOf(
        AnimFrame(SUPINE_FREE.copy(ankle = -35f, arm = 12f), moveMs = 700, holdMs = 300),
        AnimFrame(SUPINE_FREE.copy(ankle = 25f, arm = 12f), moveMs = 700, holdMs = 300),
    ),

    "heel-slide" to listOf(
        AnimFrame(SUPINE_FREE.copy(hip = 5f, knee = 10f, ankle = 10f), moveMs = 900, holdMs = 300),
        AnimFrame(SUPINE_FREE.copy(hip = 78f, knee = 82f, ankle = 15f), moveMs = 900, holdMs = 450),
    ),

    "quad-set" to listOf(
        AnimFrame(SUPINE_FREE.copy(hip = 0f, knee = 12f), moveMs = 900, holdMs = 400),
        AnimFrame(SUPINE_FREE.copy(hip = 0f, knee = 0f), moveMs = 900, holdMs = 700),
    ),

    "patella-mobilization" to listOf(
        AnimFrame(SUPINE_FREE.copy(prop = Prop.PATELLA, propOffset = 0f), moveMs = 500, holdMs = 280),
        AnimFrame(SUPINE_FREE.copy(prop = Prop.PATELLA, propOffset = 0.05f), moveMs = 500, holdMs = 280),
        AnimFrame(SUPINE_FREE.copy(prop = Prop.PATELLA, propOffset = -0.05f), moveMs = 500, holdMs = 280),
    ),

    // ---------------------------------------------------------- 主训练

    "straight-leg-raise" to listOf(
        AnimFrame(SUPINE_FREE.copy(hip = 0f, knee = 0f), moveMs = 900, holdMs = 300),
        AnimFrame(SUPINE_FREE.copy(hip = 45f, knee = 0f), moveMs = 900, holdMs = 600),
    ),

    "adductor-squeeze" to listOf(
        AnimFrame(
            SUPINE_FREE.copy(hip = 0f, knee = 0f, hip2 = 14f, knee2 = 0f, foot2X = 0f, prop = Prop.PILLOW),
            moveMs = 700, holdMs = 400,
        ),
        AnimFrame(
            SUPINE_FREE.copy(hip = 0f, knee = 0f, hip2 = 2f, knee2 = 0f, foot2X = 0f, prop = Prop.PILLOW),
            moveMs = 700, holdMs = 700,
        ),
    ),

    "heel-raise" to listOf(
        AnimFrame(Pose(stance = Stance.STANDING, hipY = 0.58f, footX = 0.02f, ankle = 0f), moveMs = 700, holdMs = 300),
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.575f, footX = 0.02f, footY = 0.905f, ankle = -35f, arm = 15f),
            moveMs = 700, holdMs = 600,
        ),
    ),

    // 终末伸膝：膝下垫卷，膝微屈，然后把膝完全伸直
    "terminal-knee-extension" to listOf(
        AnimFrame(SUPINE_FREE.copy(hip = -4f, knee = 30f, prop = Prop.ROLL), moveMs = 900, holdMs = 300),
        AnimFrame(SUPINE_FREE.copy(hip = -10f, knee = 0f, prop = Prop.ROLL), moveMs = 900, holdMs = 700),
    ),

    "glute-bridge" to listOf(
        AnimFrame(
            Pose(stance = Stance.SUPINE, hipY = 0.80f, footX = 0.14f, arm = 10f),
            moveMs = 800, holdMs = 300,
        ),
        AnimFrame(
            Pose(stance = Stance.SUPINE, hipY = 0.70f, footX = 0.18f, arm = 10f),
            moveMs = 800, holdMs = 700,
        ),
    ),

    // 蚌式开合：侧卧、双臂前伸、双脚并拢，上面那条腿的膝盖像蚌壳一样向上张开
    // hip2/knee2 是贴床的下腿（微屈），hip/knee 是上面那条腿（屈膝约 90°）
    "clam-shell" to listOf(
        AnimFrame(
            SIDE_FREE.copy(hip = 2f, knee = 70f, hip2 = -8f, knee2 = 30f, arm = 10f),
            moveMs = 800, holdMs = 250,
        ),
        AnimFrame(
            SIDE_FREE.copy(hip = 34f, knee = 100f, hip2 = -8f, knee2 = 30f, arm = 10f),
            moveMs = 800, holdMs = 900,
        ),
    ),

    "side-lying-leg-raise" to listOf(
        AnimFrame(
            SIDE_FREE.copy(hip = -20f, knee = 0f, hip2 = -8f, knee2 = 30f),
            moveMs = 900, holdMs = 300,
        ),
        AnimFrame(
            SIDE_FREE.copy(hip = 25f, knee = 0f, hip2 = -8f, knee2 = 30f),
            moveMs = 900, holdMs = 700,
        ),
    ),

    "single-leg-balance" to listOf(
        AnimFrame(
            Pose(stance = Stance.STANDING, legMode = LegMode.FREE, hip = 0f, knee = 5f, hip2 = 18f, knee2 = 35f, arm = 60f, prop = Prop.CHAIR),
            moveMs = 900, holdMs = 500,
        ),
        AnimFrame(
            Pose(stance = Stance.STANDING, legMode = LegMode.FREE, hip = 0f, knee = 5f, hip2 = 26f, knee2 = 48f, arm = 50f, prop = Prop.CHAIR),
            moveMs = 900, holdMs = 500,
        ),
    ),

    // 靠墙静蹲：墙在背后，躯干贴着墙竖直，脚向前伸
    "wall-sit-60" to listOf(
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.70f, footX = 0.12f, arm = 40f, prop = Prop.WALL_BACK),
            moveMs = 800, holdMs = 700,
        ),
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.708f, footX = 0.125f, arm = 42f, prop = Prop.WALL_BACK),
            moveMs = 800, holdMs = 700,
        ),
    ),

    "bodyweight-squat" to listOf(
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.58f, footX = 0.02f, trunk = 0f, arm = 20f),
            moveMs = 900, holdMs = 300,
        ),
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.72f, footX = 0.06f, trunk = 22f, arm = 75f),
            moveMs = 900, holdMs = 450,
        ),
    ),

    "step-up" to listOf(
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.58f, footX = 0.02f, prop = Prop.STEP),
            moveMs = 800, holdMs = 300,
        ),
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.46f, footX = 0.22f, footY = 0.72f, prop = Prop.STEP),
            moveMs = 800, holdMs = 700,
        ),
    ),

    "reverse-lunge" to listOf(
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.58f, footX = 0.02f, foot2X = -0.02f, arm = 25f),
            moveMs = 800, holdMs = 300,
        ),
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.74f, footX = 0.20f, foot2X = -0.22f, trunk = 8f, arm = 30f),
            moveMs = 800, holdMs = 550,
        ),
    ),

    "lateral-hop" to listOf(
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.60f, footX = -0.06f, foot2X = 0.10f, arm = 30f),
            moveMs = 500, holdMs = 300,
        ),
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.50f, footX = 0.0f, foot2X = 0.04f, footY = 0.80f, arm = 60f),
            moveMs = 450, holdMs = 180,
        ),
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.60f, footX = 0.10f, foot2X = -0.06f, arm = 30f),
            moveMs = 550, holdMs = 400,
        ),
    ),

    "squat-jump" to listOf(
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.73f, footX = 0.06f, trunk = 22f, arm = 80f),
            moveMs = 600, holdMs = 300,
        ),
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.44f, footX = 0.0f, footY = 0.76f, trunk = 5f, arm = 150f),
            moveMs = 450, holdMs = 200,
        ),
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.70f, footX = 0.05f, trunk = 18f, arm = 70f),
            moveMs = 600, holdMs = 300,
        ),
    ),

    "full-squat" to listOf(
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.58f, footX = 0.02f, trunk = 0f, arm = 15f),
            moveMs = 900, holdMs = 300,
        ),
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.80f, footX = 0.10f, trunk = 32f, arm = 85f),
            moveMs = 900, holdMs = 500,
        ),
    ),

    // ---------------------------------------------------------- 放松

    "hamstring-stretch" to listOf(
        AnimFrame(SUPINE_FREE.copy(hip = 0f, knee = 0f, arm = 40f, prop = Prop.BAND), moveMs = 1000, holdMs = 300),
        AnimFrame(SUPINE_FREE.copy(hip = 82f, knee = 0f, arm = 110f, prop = Prop.BAND), moveMs = 1000, holdMs = 900),
    ),

    // 股四头肌拉伸：单手扶墙，另一条腿屈膝把脚跟拉向臀部
    "quad-stretch" to listOf(
        AnimFrame(
            Pose(stance = Stance.STANDING, legMode = LegMode.FREE, hip = 0f, knee = 6f, hip2 = -12f, knee2 = 100f, arm = 85f, prop = Prop.WALL),
            moveMs = 800, holdMs = 400,
        ),
        AnimFrame(
            Pose(stance = Stance.STANDING, legMode = LegMode.FREE, hip = 0f, knee = 6f, hip2 = -22f, knee2 = 128f, arm = 90f, prop = Prop.WALL),
            moveMs = 800, holdMs = 900,
        ),
    ),

    "calf-stretch" to listOf(
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.60f, footX = 0.10f, foot2X = -0.10f, trunk = 8f, arm = 100f, prop = Prop.WALL),
            moveMs = 800, holdMs = 400,
        ),
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.62f, footX = 0.14f, foot2X = -0.15f, trunk = 22f, arm = 120f, prop = Prop.WALL),
            moveMs = 800, holdMs = 900,
        ),
    ),
)

private val FALLBACK = listOf(
    AnimFrame(Pose(stance = Stance.STANDING, hipY = 0.58f, footX = 0.02f), moveMs = 800, holdMs = 800),
)

/** 取某个动作的示范动画；没有定义就给一个站立待机的画面 */
fun framesFor(exerciseId: String): List<AnimFrame> = POSES[exerciseId] ?: FALLBACK

/** 哪些动作还没配示范动画，供启动自检用 */
fun missingAnimations(ids: List<String>): List<String> = ids.filter { !POSES.containsKey(it) }

/**
 * 启动自检：逐个动作把关键帧和中间姿势都算一遍，
 * 只要有一帧陷进地面或者画出画面外就报出来。
 * 这类问题在手机上很难一眼看出，交给机器查。
 */
fun animationIssues(): List<String> {
    val issues = mutableListOf<String>()
    POSES.forEach { (id, frames) ->
        frames.forEachIndexed { i, frame ->
            val next = frames[(i + 1) % frames.size]
            for (step in 0..4) {
                val pose = if (step == 0) frame.pose else lerpPose(frame.pose, next.pose, step / 4f)
                val label = "$id 第 ${i + 1} 帧（中间 $step/4）"
                val penetration = groundPenetration(pose)
                if (penetration > 0.015f) {
                    issues.add("$label 陷进地面 ${(penetration * 100).toInt()}%")
                }
                if (figureOutOfFrame(pose)) {
                    issues.add("$label 画出画面外")
                }
            }
        }
    }
    return issues
}
