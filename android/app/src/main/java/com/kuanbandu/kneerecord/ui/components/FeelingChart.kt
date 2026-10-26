package com.kuanbandu.kneerecord.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 身体感受趋势折线图：Y 轴 0-10，只用 Canvas 画，不引第三方图表库。
 */
@Composable
fun FeelingChart(
    points: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    val lineColor = Color(0xFF0A84FF)

    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas

        val padLeft = 32.dp.toPx()
        val padRight = 16.dp.toPx()
        val padTop = 16.dp.toPx()
        val padBottom = 26.dp.toPx()
        val plotW = size.width - padLeft - padRight
        val plotH = size.height - padTop - padBottom

        val xOf: (Int) -> Float = { i ->
            if (points.size == 1) padLeft + plotW / 2 else padLeft + plotW * i / (points.size - 1)
        }
        val yOf: (Float) -> Float = { v -> padTop + plotH - (v / 10f) * plotH }

        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas

            val gridPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#E8EAED")
                strokeWidth = 1f
                isAntiAlias = true
            }
            val textPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#9AA0A6")
                textSize = 10.sp.toPx()
                isAntiAlias = true
            }
            val datePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#9AA0A6")
                textSize = 10.sp.toPx()
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            for (v in 0..10 step 2) {
                val y = yOf(v.toFloat())
                native.drawLine(padLeft, y, padLeft + plotW, y, gridPaint)
                native.drawText(v.toString(), 6f, y + 4f, textPaint)
            }

            labels.forEachIndexed { i, label ->
                if (i < points.size) {
                    native.drawText(label, xOf(i), size.height - 6f, datePaint)
                }
            }
        }

        for (i in 1 until points.size) {
            drawLine(
                color = lineColor,
                start = Offset(xOf(i - 1), yOf(points[i - 1].toFloat())),
                end = Offset(xOf(i), yOf(points[i].toFloat())),
                strokeWidth = 2.dp.toPx(),
            )
        }
        points.forEachIndexed { i, v ->
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(xOf(i), yOf(v.toFloat())))
        }
    }
}
