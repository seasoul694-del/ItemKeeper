package com.tang.itemkeeper.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.RoundedRectangle
import com.tang.itemkeeper.data.SortMode
import com.tang.itemkeeper.ui.theme.LocalAppDark
import com.tang.itemkeeper.ui.theme.Motion
import com.tang.itemkeeper.ui.theme.Space
import com.tang.itemkeeper.ui.theme.Tiffany

private val sortOptions = listOf(
    SortMode.DATE_DESC to "购买日期 · 从近到远",
    SortMode.DATE_ASC to "购买日期 · 从远到近",
    SortMode.PRICE_DESC to "价格 · 从高到低",
    SortMode.PRICE_ASC to "价格 · 从低到高",
    SortMode.DAILY_DESC to "日均价 · 从高到低",
    SortMode.DAILY_ASC to "日均价 · 从低到高",
    SortMode.CATEGORY to "按类别归组",
    SortMode.STATUS to "按状态归组"
)

/**
 * 首页排序玻璃卡片：结构与玻璃参数与归总卡一致
 * （vibrancy + blur 8dp + lens 0.32/0.47 清玻璃 + Highlight.Plain + 28dp 圆角），
 * WWDC23 弹簧体系；点卡片外弹回。
 */
@Composable
fun SortBubble(
    visible: Boolean,
    current: SortMode,
    backdrop: Backdrop,
    onSelect: (SortMode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalAppDark.current
    val surfaceColor = if (dark) Color(0xFF15151C) else Color(0xFFFDFDFF)
    val maxCardHeight = (LocalConfiguration.current.screenHeightDp * 0.72f).dp

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.86f,
            animationSpec = spring(
                dampingRatio = Motion.BOUNCY_DAMPING,
                stiffness = Motion.BOUNCY_STIFFNESS
            )
        ) + fadeIn(),
        exit = fadeOut(animationSpec = tween(durationMillis = 120)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (dark) 0.46f else 0.28f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            // 玻璃卡：采样层（内容层）的兄弟节点，drawBackdrop 合法
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .heightIn(max = maxCardHeight)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(28f.dp) },
                        effects = {
                            val minDimension = size.minDimension
                            vibrancy()
                            blur(6f.dp.toPx())
                            lens(
                                refractionHeight = 0.24f * minDimension,
                                refractionAmount = 0.42f * minDimension,
                                depthEffect = true,
                                chromaticAberration = false
                            )
                        },
                                                onDrawSurface = { drawRect(surfaceColor.copy(alpha = 0.42f)) }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(horizontal = Space.x2, vertical = Space.x3)
            ) {
                Text(
                    "首页排序",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "选择清单的排列方式",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Space.x1))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    sortOptions.forEach { (mode, label) ->
                        val selected = mode == current
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(mode) }
                                .padding(horizontal = 6.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 选中态指示圆点
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) Tiffany
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) Tiffany
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Space.x1))
                Text(
                    "点击卡片外任意位置收起",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
