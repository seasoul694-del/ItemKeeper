package com.tang.itemkeeper.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** 统一 1.6dp 描边的线性图标，替代系统 emoji / 字符图标，跨机型渲染一致 */
enum class LineIconType { HOME, SUMMARY, ADD, FILTER, SETTINGS, SEARCH, SORT }

@Composable
fun LineIcon(
    type: LineIconType,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 1.6.dp
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val w = strokeWidth.toPx()
        val stroke = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)

        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(
            color = color,
            start = Offset(x1 * s, y1 * s),
            end = Offset(x2 * s, y2 * s),
            strokeWidth = w,
            cap = StrokeCap.Round
        )

        when (type) {
            LineIconType.HOME -> {
                val path = Path().apply {
                    moveTo(0.14f * s, 0.47f * s)
                    lineTo(0.50f * s, 0.16f * s)
                    lineTo(0.86f * s, 0.47f * s)
                    moveTo(0.26f * s, 0.43f * s)
                    lineTo(0.26f * s, 0.85f * s)
                    lineTo(0.74f * s, 0.85f * s)
                    lineTo(0.74f * s, 0.43f * s)
                }
                drawPath(path, color, style = stroke)
            }
            LineIconType.SUMMARY -> {
                line(0.16f, 0.84f, 0.84f, 0.84f)
                line(0.28f, 0.84f, 0.28f, 0.54f)
                line(0.50f, 0.84f, 0.50f, 0.22f)
                line(0.72f, 0.84f, 0.72f, 0.42f)
            }
            LineIconType.ADD -> {
                line(0.50f, 0.22f, 0.50f, 0.78f)
                line(0.22f, 0.50f, 0.78f, 0.50f)
            }
            LineIconType.FILTER -> {
                line(0.16f, 0.32f, 0.84f, 0.32f)
                line(0.16f, 0.50f, 0.84f, 0.50f)
                line(0.16f, 0.68f, 0.84f, 0.68f)
                drawCircle(color, radius = 0.055f * s, center = Offset(0.36f * s, 0.32f * s), style = stroke)
                drawCircle(color, radius = 0.055f * s, center = Offset(0.66f * s, 0.50f * s), style = stroke)
                drawCircle(color, radius = 0.055f * s, center = Offset(0.44f * s, 0.68f * s), style = stroke)
            }
            LineIconType.SETTINGS -> {
                // 线性齿轮：外圈圆 + 六根径向齿 + 中心点（与「归类」滑杆明确区分）
                val cx = 0.5f * s
                val cy = 0.5f * s
                val r1 = 0.26f * s
                val r2 = 0.40f * s
                drawCircle(color, radius = r1, center = Offset(cx, cy), style = stroke)
                drawCircle(color, radius = 0.11f * s, center = Offset(cx, cy), style = stroke)
                for (i in 0..5) {
                    val angle = Math.toRadians(i * 60.0 + 90.0)
                    drawLine(
                        color,
                        start = Offset(
                            cx + (r1 * cos(angle)).toFloat(),
                            cy - (r1 * sin(angle)).toFloat()
                        ),
                        end = Offset(
                            cx + (r2 * cos(angle)).toFloat(),
                            cy - (r2 * sin(angle)).toFloat()
                        ),
                        strokeWidth = w,
                        cap = StrokeCap.Round
                    )
                }
            }
            LineIconType.SEARCH -> {
                drawCircle(
                    color,
                    radius = 0.24f * s,
                    center = Offset(0.44f * s, 0.44f * s),
                    style = stroke
                )
                line(0.62f, 0.62f, 0.84f, 0.84f)
            }
            LineIconType.SORT -> {
                // 左侧向上箭头 + 右侧向下箭头（排序）
                line(0.30f, 0.80f, 0.30f, 0.20f)
                line(0.15f, 0.35f, 0.30f, 0.20f)
                line(0.45f, 0.35f, 0.30f, 0.20f)
                line(0.70f, 0.20f, 0.70f, 0.80f)
                line(0.55f, 0.65f, 0.70f, 0.80f)
                line(0.85f, 0.65f, 0.70f, 0.80f)
            }
        }
    }
}
