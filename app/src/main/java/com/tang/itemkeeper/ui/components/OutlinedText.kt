package com.tang.itemkeeper.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 描边文字：底层黑色细轮廓 + 上层原色实心，浮在任意明暗背景上都有清晰轮廓。
 *
 * 关键：两层必须共用同一份 TextStyle（fontSize/letterSpacing/lineHeight 完全一致，
 * 只有 drawStyle 不同）。否则实心层继承 Material 默认排版（letterSpacing 0.5sp）而
 * 描边层为裸样式（0）→ 每个字符累计错位，出现「鬼影」。
 * 描边为发丝级：小字 0.4dp、正文 0.5dp、大标题 0.6dp 左右，避免「重描边」观感。
 */
@Composable
fun OutlinedText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    strokeWidth: Dp = 0.3.dp,
    strokeColor: Color = Color.Black
) {
    val density = LocalDensity.current
    val strokePx = with(density) { strokeWidth.toPx() }
    // 两层共用的唯一排版来源：显式覆盖一切继承值，保证逐像素重叠
    val baseStyle = remember(fontSize, fontWeight, lineHeight) {
        TextStyle(
            fontSize = fontSize,
            fontWeight = fontWeight,
            lineHeight = lineHeight
        )
    }
    val strokeStyle = remember(baseStyle, strokePx) {
        baseStyle.copy(
            drawStyle = Stroke(
                width = strokePx,
                miter = 4f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
    Box(modifier = modifier) {
        Text(
            text = text,
            color = strokeColor,
            style = strokeStyle,
            maxLines = maxLines,
            overflow = overflow
        )
        Text(
            text = text,
            color = color,
            style = baseStyle,
            maxLines = maxLines,
            overflow = overflow
        )
    }
}
