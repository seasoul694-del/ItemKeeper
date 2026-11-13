package com.tang.itemkeeper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.RoundedRectangle
import com.tang.itemkeeper.ui.theme.LocalAppDark
import com.tang.itemkeeper.ui.theme.Space

/**
 * 页面内玻璃卡片的统一实现 —— 「所有框」规则（2026-11-13 用户定稿）：
 * 卡片透明、能透过它看到全局背景本身，框内带一点模糊（blur 8dp）保证文字可读。
 * 折射源是「仅背景图」的 bgBackdrop（背景图层是独立采样源，卡片不在其子树内，
 * 结构合法），因此卡片透出的永远是背景图本身，列表滚动不影响框内画面。
 * 折射量按卡片宽度计算而非较小边，卡片高矮不影响模糊观感（跨页面一致）。
 */
@Composable
fun GlassCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    surfaceAlpha: Float = 0.30f,
    contentPadding: PaddingValues = PaddingValues(Space.x2),
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = LocalAppDark.current
    val surfaceColor = if (dark) Color(0xFF15151C) else Color(0xFFFDFDFF)
    Column(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(cornerRadius) },
                effects = {
                    vibrancy()
                    blur(6f.dp.toPx())
                    lens(
                        refractionHeight = 0.24f * size.minDimension,
                        refractionAmount = 0.42f * size.minDimension,
                        depthEffect = true,
                        chromaticAberration = false
                    )
                },
                                onDrawSurface = { drawRect(surfaceColor.copy(alpha = surfaceAlpha)) }
            )
            .padding(contentPadding),
        content = content
    )
}
