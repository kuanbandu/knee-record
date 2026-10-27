package com.kuanbandu.kneerecord.ui.components

import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GridColor = Color(0xFFE5E7EB)
private val LabelColor = Color(0xFF6B7280)
private val BeforeColor = Color(0xFF0A84FF)
private val PeakColor = Color(0xFFFF9F0A)
private val WarnColor = Color(0xFFFF3B30)

/** 疼痛趋势图：训练前疼痛 + 训练中峰值双线，虚线为 4 分警戒线 */
@Composable
fun PainChart(
    labels: List<String>,
    before: List<Int>,
    peak: List<Int>,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
    ) {
        val padL = 26.dp.toPx()
        val padR = 12.dp.toPx()
        val padT = 12.dp.toPx()
        val padB = 22.dp.toPx()
        val plotW = size.width - padL - padR
        val plotH = size.height - padT - padB
        if (labels.isEmpty() || plotW <= 0f || plotH <= 0f) return@Canvas

        fun xAt(i: Int): Float =
            padL + if (labels.size == 1) plotW / 2f else plotW * i / (labels.size - 1)

        fun yAt(v: Int): Float = padT + plotH * (1f - v.coerceIn(0, 10) / 10f)

        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val gridPaint = Paint().apply {
                color = GridColor.toArgb()
                strokeWidth = 1f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            for (v in 0..10 step 2) {
                native.drawLine(padL, yAt(v), size.width - padR, yAt(v), gridPaint)
            }

            val warnPaint = Paint().apply {
                color = WarnColor.toArgb()
                strokeWidth = 1.5.dp.toPx()
                style = Paint.Style.STROKE
                isAntiAlias = true
                pathEffect = DashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f)
            }
            native.drawLine(padL, yAt(4), size.width - padR, yAt(4), warnPaint)

            val yPaint = Paint().apply {
                color = LabelColor.toArgb()
                textSize = 10.sp.toPx()
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            for (v in 0..10 step 5) {
                native.drawText(v.toString(), padL - 6.dp.toPx(), yAt(v) + 4.dp.toPx(), yPaint)
            }

            val xPaint = Paint().apply {
                color = LabelColor.toArgb()
                textSize = 10.sp.toPx()
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val step = (labels.size / 6).coerceAtLeast(1)
            labels.forEachIndexed { i, label ->
                if (i % step == 0 || i == labels.size - 1) {
                    native.drawText(label, xAt(i), size.height - 6.dp.toPx(), xPaint)
                }
            }
        }

        fun series(values: List<Int>, color: Color) {
            if (values.isEmpty()) return
            val points = values.mapIndexed { i, v -> Offset(xAt(i), yAt(v)) }
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = color,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            points.forEach {
                drawCircle(color = color, radius = 3.dp.toPx(), center = it)
            }
        }

        series(before, BeforeColor)
        series(peak, PeakColor)
    }
}
