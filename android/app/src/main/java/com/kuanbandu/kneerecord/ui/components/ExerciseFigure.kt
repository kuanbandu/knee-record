package com.kuanbandu.kneerecord.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** 体位：站立 / 仰卧 / 侧卧 */
enum class Stance { STANDING, SUPINE, SIDE_LYING }

/** 腿的求解方式：脚踩地面用 IK（自然），腿在空中用角度（可控） */
enum class LegMode { IK, FREE }

/** 示范道具。WALL 在身前（推墙/扶墙），WALL_BACK 在背后（靠墙静蹲） */
enum class Prop { NONE, WALL, WALL_BACK, STEP, PILLOW, ROLL, BAND, CHAIR, PATELLA }

/** 一个姿势。坐标是归一化 0..1，y 向下，地面在 GROUND_Y */
data class Pose(
    val stance: Stance = Stance.STANDING,
    val legMode: LegMode = LegMode.IK,
    /** 躯干倾斜：站立为前倾角；仰卧/侧卧时 trunkAuto 生效则无效 */
    val trunk: Float = 0f,
    /** 仰卧/侧卧时让肩背自动贴地 */
    val trunkAuto: Boolean = true,
    val hipY: Float = 0.58f,
    val footX: Float = 0.02f,
    /** 脚底所在高度，踏上台阶或腾空时用它 */
    val footY: Float = GROUND_Y,
    /** 副脚相对髋的偏移，null 表示只画一条腿 */
    val foot2X: Float? = null,
    val hip: Float = 0f,
    val knee: Float = 0f,
    val hip2: Float = 0f,
    val knee2: Float = 0f,
    /** 踝：正为背屈（勾脚），负为跖屈（绷脚） */
    val ankle: Float = 0f,
    val arm: Float = 0f,
    val prop: Prop = Prop.NONE,
    val propOffset: Float = 0f,
)

/** 一帧：摆到这个姿势用 moveMs，到位后停 holdMs */
data class AnimFrame(val pose: Pose, val moveMs: Int = 700, val holdMs: Int = 250)

const val GROUND_Y = 0.92f

private const val HIP_X = 0.46f
private const val HIP_X_SIDE = 0.42f
private const val L_THIGH = 0.17f
private const val L_SHANK = 0.17f
private const val L_TORSO = 0.25f
private const val L_ARM = 0.15f
private const val L_FOOT = 0.085f
private const val HEAD_GAP = 0.05f
private const val HEAD_R = 0.052f

private val BoneColor = Color(0xFF0A84FF)
private val Bone2Color = Color(0xFF9CC7F5)
private val GroundColor = Color(0xFFD1D5DB)
private val PropColor = Color(0xFFC7CBD1)
private val HeadColor = Color(0xFF0A84FF)

// ---------------------------------------------------------------- 几何

private fun dir(deg: Float): Offset {
    val r = Math.toRadians(deg.toDouble())
    return Offset(sin(r).toFloat(), cos(r).toFloat())
}

private fun norm(v: Offset): Offset {
    val d = sqrt(v.x * v.x + v.y * v.y).coerceAtLeast(1e-4f)
    return Offset(v.x / d, v.y / d)
}

/** 两连杆求膝：髋与脚固定，膝朝凸侧 */
private fun ik(hip: Offset, foot: Offset, l1: Float, l2: Float): Offset {
    val dx = foot.x - hip.x
    val dy = foot.y - hip.y
    val d0 = sqrt(dx * dx + dy * dy).coerceAtLeast(1e-4f)
    val ux = dx / d0
    val uy = dy / d0
    val d = d0.coerceIn(kotlin.math.abs(l1 - l2) + 1e-3f, l1 + l2 - 1e-3f)
    val a = (l1 * l1 - l2 * l2 + d * d) / (2 * d)
    val h = sqrt((l1 * l1 - a * a).coerceAtLeast(0f))
    return Offset(hip.x + a * ux + h * uy, hip.y + a * uy - h * ux)
}

private class Body(
    val hip: Offset,
    val shoulder: Offset,
    val head: Offset,
    val hand: Offset,
    val knee1: Offset,
    val ankle1: Offset,
    val toe1: Offset,
    val knee2: Offset?,
    val ankle2: Offset?,
    val toe2: Offset?,
) {
    fun points(): List<Offset> = listOf(hip, shoulder, head, hand, knee1, ankle1, toe1) +
        listOfNotNull(knee2, ankle2, toe2)

    fun maxY(): Float = points().maxOf { it.y }

    fun shifted(dy: Float): Body = Body(
        hip = Offset(hip.x, hip.y + dy),
        shoulder = Offset(shoulder.x, shoulder.y + dy),
        head = Offset(head.x, head.y + dy),
        hand = Offset(hand.x, hand.y + dy),
        knee1 = Offset(knee1.x, knee1.y + dy),
        ankle1 = Offset(ankle1.x, ankle1.y + dy),
        toe1 = Offset(toe1.x, toe1.y + dy),
        knee2 = knee2?.let { Offset(it.x, it.y + dy) },
        ankle2 = ankle2?.let { Offset(it.x, it.y + dy) },
        toe2 = toe2?.let { Offset(it.x, it.y + dy) },
    )
}

private fun build(pose: Pose): Body {
    val body = compute(pose)
    // 兜底：骨架任何一点陷到地面以下就把整体上移，宁可浮空也不穿地板
    val penetration = body.maxY() - GROUND_Y
    return if (penetration > 0f) body.shifted(-penetration) else body
}

/** 这个姿势有多少陷进地面（0 表示正好站在/躺在地面上），启动自检用 */
fun groundPenetration(pose: Pose): Float = compute(pose).maxY() - GROUND_Y

/** 这个姿势有没有画出画面外，启动自检用 */
fun figureOutOfFrame(pose: Pose): Boolean = compute(pose).points()
    .any { it.x < 0.02f || it.x > 0.98f || it.y < 0.02f || it.y > 0.98f }

/** 一条腿：膝、踝、小腿方向角（脚掌要垂直于小腿） */
private data class Leg(val knee: Offset, val ankle: Offset, val shankDeg: Float)

private fun compute(pose: Pose): Body {
    val hip = when (pose.stance) {
        Stance.STANDING -> Offset(HIP_X, pose.hipY)
        Stance.SUPINE, Stance.SIDE_LYING -> Offset(HIP_X_SIDE, pose.hipY)
    }

    val shoulder = if (pose.trunkAuto && pose.stance != Stance.STANDING) {
        val targetY = GROUND_Y - 0.05f
        val dy = targetY - hip.y
        val dx = sqrt((L_TORSO * L_TORSO - dy * dy).coerceAtLeast(0.01f))
        Offset(hip.x - dx, targetY)
    } else {
        hip + dir(180f - pose.trunk) * L_TORSO
    }
    val torsoUp = norm(shoulder - hip)
    val head = shoulder + torsoUp * HEAD_GAP

    val armBase = if (pose.stance == Stance.STANDING) -pose.trunk else 90f
    val hand = shoulder + dir(armBase + pose.arm) * L_ARM

    val leg1 = solveLeg(pose, hip)
    val leg2 = if (pose.foot2X != null || pose.stance == Stance.SIDE_LYING) {
        solveLeg2(pose, hip)
    } else {
        null
    }

    return Body(
        hip = hip,
        shoulder = shoulder,
        head = head,
        hand = hand,
        knee1 = leg1.knee,
        ankle1 = leg1.ankle,
        toe1 = foot(leg1.ankle, leg1.shankDeg, pose),
        knee2 = leg2?.knee,
        ankle2 = leg2?.ankle,
        toe2 = leg2?.let { foot(it.ankle, it.shankDeg, pose) },
    )
}

private fun solveLeg(pose: Pose, hip: Offset): Leg {
    if (pose.legMode == LegMode.IK) {
        val ankle = Offset(hip.x + pose.footX, pose.footY)
        val dx = ankle.x - hip.x
        val dy = ankle.y - hip.y
        return Leg(
            knee = ik(hip, ankle, L_THIGH, L_SHANK),
            ankle = ankle,
            shankDeg = Math.toDegrees(kotlin.math.atan2(dx.toDouble(), dy.toDouble())).toFloat(),
        )
    }
    val base = if (pose.stance == Stance.STANDING) 0f else 90f
    val thighDeg = base + pose.hip
    val shankDeg = thighDeg - pose.knee
    val knee = hip + dir(thighDeg) * L_THIGH
    return Leg(knee, knee + dir(shankDeg) * L_SHANK, shankDeg)
}

private fun solveLeg2(pose: Pose, hip: Offset): Leg {
    if (pose.stance == Stance.SIDE_LYING) {
        // 侧卧：hip2 / knee2 是贴床那条腿
        return angledLeg(hip, 90f + pose.hip2, pose.knee2)
    }
    if (pose.legMode == LegMode.IK) {
        val x = pose.foot2X ?: return Leg(hip, hip, 0f)
        val ankle = Offset(hip.x + x, pose.footY)
        val dx = ankle.x - hip.x
        val dy = ankle.y - hip.y
        return Leg(
            knee = ik(hip, ankle, L_THIGH, L_SHANK),
            ankle = ankle,
            shankDeg = Math.toDegrees(kotlin.math.atan2(dx.toDouble(), dy.toDouble())).toFloat(),
        )
    }
    val base = if (pose.stance == Stance.STANDING) 0f else 90f
    return angledLeg(hip, base + pose.hip2, pose.knee2)
}

private fun angledLeg(hip: Offset, thighDeg: Float, kneeDeg: Float): Leg {
    val shankDeg = thighDeg - kneeDeg
    val knee = hip + dir(thighDeg) * L_THIGH
    return Leg(knee, knee + dir(shankDeg) * L_SHANK, shankDeg)
}

/** 脚掌：踩地时平放（背屈脚尖上抬），腿在空中时垂直于小腿 */
private fun foot(ankle: Offset, shankDeg: Float, pose: Pose): Offset {
    if (pose.legMode == LegMode.IK) {
        return Offset(
            ankle.x + L_FOOT * cos(Math.toRadians(pose.ankle.toDouble() * 0.7).toFloat()),
            ankle.y - 0.014f - pose.ankle * 0.0009f,
        )
    }
    return ankle + dir(shankDeg + 90f + pose.ankle) * L_FOOT
}

// ---------------------------------------------------------------- 渲染

@Composable
fun ExerciseFigure(pose: Pose, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
    ) {
        val scale = minOf(size.width, size.height)
        val ox = (size.width - scale) / 2f
        val oy = (size.height - scale) / 2f

        val body = build(pose)
        val thick = scale * 0.024f
        val thin = scale * 0.016f
        val dot = scale * 0.012f
        val px: (Offset) -> Offset = { Offset(ox + it.x * scale, oy + it.y * scale) }

        // 地面
        drawLine(
            color = GroundColor,
            start = px(Offset(0.02f, GROUND_Y)),
            end = px(Offset(0.98f, GROUND_Y)),
            strokeWidth = scale * 0.006f,
            cap = StrokeCap.Round,
        )

        drawProp(pose, body, px, scale)

        // 副腿（先画，作为后侧）
        if (body.knee2 != null && body.ankle2 != null && body.toe2 != null) {
            val joints = listOf(
                body.hip + Offset(-0.012f, 0.012f) to body.knee2,
                body.knee2 to body.ankle2,
                body.ankle2 to body.toe2,
            )
            joints.forEach { (a, b) ->
                drawLine(color = Bone2Color, start = px(a), end = px(b), strokeWidth = thin, cap = StrokeCap.Round)
            }
        }

        // 主腿
        listOf(body.hip to body.knee1, body.knee1 to body.ankle1, body.ankle1 to body.toe1)
            .forEach { (a, b) ->
                drawLine(color = BoneColor, start = px(a), end = px(b), strokeWidth = thick, cap = StrokeCap.Round)
            }

        // 躯干 + 手臂 + 头
        drawLine(color = BoneColor, start = px(body.hip), end = px(body.shoulder), strokeWidth = thick, cap = StrokeCap.Round)
        drawLine(color = BoneColor, start = px(body.shoulder), end = px(body.hand), strokeWidth = thin, cap = StrokeCap.Round)
        drawCircle(color = HeadColor, radius = scale * HEAD_R, center = px(body.head))
        drawCircle(color = Color.White, radius = dot, center = px(body.hip))
    }
}

private fun DrawScope.drawProp(
    pose: Pose,
    body: Body,
    px: (Offset) -> Offset,
    scale: Float,
) {
    val w = scale * 0.007f
    when (pose.prop) {
        Prop.NONE -> Unit
        Prop.WALL -> {
            // 墙在身前：推墙、扶墙
            val x = body.hip.x + 0.26f
            drawLine(
                color = PropColor,
                start = px(Offset(x, 0.16f)),
                end = px(Offset(x, GROUND_Y)),
                strokeWidth = w * 2,
            )
            for (i in 0..3) {
                val y = 0.30f + i * 0.16f
                drawLine(
                    color = PropColor,
                    start = px(Offset(x, y)),
                    end = px(Offset(x - 0.06f, y + 0.05f)),
                    strokeWidth = w,
                )
            }
        }
        Prop.WALL_BACK -> {
            // 墙在背后：靠墙静蹲，背贴着墙
            val x = body.hip.x - 0.08f
            drawLine(
                color = PropColor,
                start = px(Offset(x, 0.16f)),
                end = px(Offset(x, GROUND_Y)),
                strokeWidth = w * 2,
            )
            for (i in 0..3) {
                val y = 0.30f + i * 0.16f
                drawLine(
                    color = PropColor,
                    start = px(Offset(x, y)),
                    end = px(Offset(x + 0.06f, y + 0.05f)),
                    strokeWidth = w,
                )
            }
        }
        Prop.STEP -> {
            drawRect(
                color = Color(0xFFE5E7EB),
                topLeft = px(Offset(0.60f, 0.72f)),
                size = Size(0.36f * scale, (GROUND_Y - 0.72f) * scale),
            )
            drawLine(
                color = PropColor,
                start = px(Offset(0.60f, 0.72f)),
                end = px(Offset(0.96f, 0.72f)),
                strokeWidth = w,
            )
        }
        Prop.PILLOW -> {
            drawOval(
                color = Color(0xFFE5E7EB),
                topLeft = px(Offset(0.50f, body.knee1.y - 0.035f + pose.propOffset)),
                size = Size(0.12f * scale, 0.07f * scale),
            )
        }
        Prop.ROLL -> {
            // 膝下垫的卷：贴着地面，正好托住膝盖
            drawCircle(
                color = Color(0xFFE5E7EB),
                radius = scale * 0.035f,
                center = px(Offset(body.knee1.x, GROUND_Y - 0.035f)),
            )
        }
        Prop.BAND -> {
            drawLine(
                color = Color(0xFFFF9F0A),
                start = px(body.ankle1),
                end = px(body.hand),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
        }
        Prop.CHAIR -> {
            // 椅子跟着人走，伸手够得着
            val x = body.hip.x + 0.24f
            drawLine(
                color = PropColor,
                start = px(Offset(x, 0.40f)),
                end = px(Offset(x, 0.56f)),
                strokeWidth = w,
            )
            drawLine(
                color = PropColor,
                start = px(Offset(x - 0.12f, 0.56f)),
                end = px(Offset(x + 0.10f, 0.56f)),
                strokeWidth = w * 1.5f,
            )
            drawLine(
                color = PropColor,
                start = px(Offset(x, 0.56f)),
                end = px(Offset(x, GROUND_Y)),
                strokeWidth = w,
            )
        }
        Prop.PATELLA -> {
            drawCircle(
                color = Color(0xFFFF9F0A),
                radius = scale * 0.016f,
                center = px(Offset(body.knee1.x, body.knee1.y - 0.03f + pose.propOffset)),
            )
        }
    }
}

// ---------------------------------------------------------------- 动画

private fun lerp(a: Float, b: Float, k: Float) = a + (b - a) * k

fun lerpPose(a: Pose, b: Pose, k: Float) = Pose(
    stance = a.stance,
    legMode = a.legMode,
    trunkAuto = a.trunkAuto,
    prop = a.prop,
    trunk = lerp(a.trunk, b.trunk, k),
    hipY = lerp(a.hipY, b.hipY, k),
    footX = lerp(a.footX, b.footX, k),
    footY = lerp(a.footY, b.footY, k),
    foot2X = if (a.foot2X != null && b.foot2X != null) lerp(a.foot2X, b.foot2X, k) else a.foot2X,
    hip = lerp(a.hip, b.hip, k),
    knee = lerp(a.knee, b.knee, k),
    hip2 = lerp(a.hip2, b.hip2, k),
    knee2 = lerp(a.knee2, b.knee2, k),
    ankle = lerp(a.ankle, b.ankle, k),
    arm = lerp(a.arm, b.arm, k),
    propOffset = lerp(a.propOffset, b.propOffset, k),
)

/** 按时间取姿势：先停在 hold，再过渡到下一帧 */
private fun poseAt(frames: List<AnimFrame>, tMs: Float): Pose {
    if (frames.isEmpty()) return Pose()
    if (frames.size == 1) return frames[0].pose
    var acc = 0f
    for (i in frames.indices) {
        val f = frames[i]
        if (tMs < acc + f.holdMs) return f.pose
        acc += f.holdMs
        val next = frames[(i + 1) % frames.size]
        if (tMs < acc + f.moveMs) {
            val raw = (tMs - acc) / f.moveMs.toFloat().coerceAtLeast(1f)
            val k = raw * raw * (3 - 2 * raw) // 缓入缓出，别像机器人
            return lerpPose(f.pose, next.pose, k)
        }
        acc += f.moveMs
    }
    return frames.last().pose
}

/** 循环播放的动作示范。playing = false 时停在当前姿势 */
@Composable
fun ExerciseAnimation(
    frames: List<AnimFrame>,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    speed: Float = 1f,
) {
    val total = remember(frames) { frames.sumOf { it.moveMs + it.holdMs } }
    var t by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(frames, playing, speed, total) {
        if (!playing || total <= 0) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = (now - last) / 1_000_000f
            last = now
            t = (t + dt * speed) % total.toFloat()
        }
    }

    ExerciseFigure(pose = poseAt(frames, t), modifier = modifier)
}
