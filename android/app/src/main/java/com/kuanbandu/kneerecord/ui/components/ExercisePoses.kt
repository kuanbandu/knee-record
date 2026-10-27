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
    hipY = 0.82f,
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

    "terminal-knee-extension" to listOf(
        AnimFrame(SUPINE_FREE.copy(hip = 22f, knee = 45f, prop = Prop.ROLL), moveMs = 900, holdMs = 300),
        AnimFrame(SUPINE_FREE.copy(hip = 22f, knee = 2f, prop = Prop.ROLL), moveMs = 900, holdMs = 700),
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

    "clam-shell" to listOf(
        AnimFrame(
            SIDE_FREE.copy(hip = 10f, knee = 90f, hip2 = 20f, knee2 = 25f),
            moveMs = 700, holdMs = 300,
        ),
        AnimFrame(
            SIDE_FREE.copy(hip = 50f, knee = 90f, hip2 = 20f, knee2 = 25f),
            moveMs = 700, holdMs = 500,
        ),
    ),

    "side-lying-leg-raise" to listOf(
        AnimFrame(
            SIDE_FREE.copy(hip = 0f, knee = 0f, hip2 = 15f, knee2 = 20f),
            moveMs = 900, holdMs = 300,
        ),
        AnimFrame(
            SIDE_FREE.copy(hip = 45f, knee = 0f, hip2 = 15f, knee2 = 20f),
            moveMs = 900, holdMs = 600,
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

    "wall-sit-60" to listOf(
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.70f, footX = 0.12f, arm = 40f, prop = Prop.WALL),
            moveMs = 800, holdMs = 700,
        ),
        AnimFrame(
            Pose(stance = Stance.STANDING, hipY = 0.708f, footX = 0.125f, arm = 42f, prop = Prop.WALL),
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

    "quad-stretch" to listOf(
        AnimFrame(
            Pose(stance = Stance.STANDING, legMode = LegMode.FREE, hip = 0f, knee = 6f, hip2 = -12f, knee2 = 100f, arm = 145f, prop = Prop.WALL),
            moveMs = 800, holdMs = 400,
        ),
        AnimFrame(
            Pose(stance = Stance.STANDING, legMode = LegMode.FREE, hip = 0f, knee = 6f, hip2 = -22f, knee2 = 128f, arm = 150f, prop = Prop.WALL),
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
