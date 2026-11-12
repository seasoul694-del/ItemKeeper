package com.tang.itemkeeper.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tang.itemkeeper.ui.theme.Space
import com.tang.itemkeeper.ui.theme.Tiffany

private val TAB_LABELS = listOf("首页", "归总", "添加", "归类", "界面")
private val TAB_ICONS = listOf(
    LineIconType.HOME,
    LineIconType.SUMMARY,
    LineIconType.ADD,
    LineIconType.FILTER,
    LineIconType.SETTINGS
)

/**
 * 五按钮玻璃底栏（原生降级实现）：半透明底板 + 顶部高光 + 内阴影 + 可膨胀的选中气泡。
 * 选中气泡与图标文字严格同高 58dp 且垂直居中，避免出现「内容不在气泡中心」的错觉。
 */
@Composable
fun LiquidBottomBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = with(MaterialTheme.colorScheme.surface) {
        red * 0.299f + green * 0.587f + blue * 0.114f < 0.5f
    }
    val baseColor = if (dark) Color(0xFF1C1C26).copy(alpha = 0.86f)
    else Color.White.copy(alpha = 0.66f)
    val borderColor = if (dark) Color.White.copy(alpha = 0.26f)
    else Color.White.copy(alpha = 0.95f)
    val bubbleColor = if (dark) Color.White.copy(alpha = 0.13f)
    else Color.White.copy(alpha = 0.72f)
    val bubbleBorder = if (dark) Color.White.copy(alpha = 0.30f)
    else Color.White.copy(alpha = 0.98f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.x2, vertical = Space.x1)
            .height(72.dp)
    ) {
        val inset = Space.x1
        val tabWidth = (maxWidth - inset * 2) / TAB_LABELS.size
        val safeIndex = selectedIndex.coerceIn(0, TAB_LABELS.lastIndex)

        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * safeIndex + tabWidth * 0.06f,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
            label = "liquid-indicator"
        )
        // 切换时气泡先收缩再弹回，做出「膨胀」的体积感
        val pulse = remember { Animatable(1f) }
        LaunchedEffect(safeIndex) {
            pulse.snapTo(0.84f)
            pulse.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        var dragAccum by remember { mutableStateOf(0f) }

        // 1) 玻璃底板
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(12.dp, RoundedCornerShape(999.dp))
                .clip(RoundedCornerShape(999.dp))
                .background(baseColor)
                .border(1.dp, borderColor, RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (dark) 0.12f else 0.55f),
                                Color.Transparent,
                                Color.Black.copy(alpha = if (dark) 0.18f else 0.05f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (dark) 0.45f else 0.95f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // 2) 选中气泡（可膨胀）
        Box(
            modifier = Modifier
                .padding(start = inset)
                .width(tabWidth * 0.88f)
                .height(58.dp)
                .align(Alignment.CenterStart)
                .offset(x = indicatorOffset)
                .graphicsLayer {
                    scaleX = pulse.value
                    scaleY = pulse.value
                }
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    if (safeIndex == 2) Tiffany.copy(alpha = if (dark) 0.22f else 0.20f)
                    else bubbleColor
                )
                .border(1.2.dp, bubbleBorder, CircleShape)
        )

        // 3) 五个按钮：与气泡同为 58dp、垂直居中
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = inset)
                .height(68.dp)
                .pointerInput(safeIndex) {
                    detectHorizontalDragGestures(
                        onDragEnd = { dragAccum = 0f },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragAccum += amount
                            when {
                                dragAccum > 56f -> {
                                    onSelect((safeIndex - 1).coerceIn(0, TAB_LABELS.lastIndex))
                                    dragAccum = 0f
                                }
                                dragAccum < -56f -> {
                                    onSelect((safeIndex + 1).coerceIn(0, TAB_LABELS.lastIndex))
                                    dragAccum = 0f
                                }
                            }
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            TAB_LABELS.forEachIndexed { index, label ->
                val selected = index == safeIndex
                val labelColor = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                val iconColor = if (selected) MaterialTheme.colorScheme.primary else labelColor
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(index) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (index == 2) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Tiffany),
                            contentAlignment = Alignment.Center
                        ) {
                            LineIcon(LineIconType.ADD, Color.White, Modifier.size(22.dp))
                        }
                    } else {
                        LineIcon(TAB_ICONS[index], iconColor, Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = label, fontSize = 10.sp, color = labelColor)
                }
            }
        }
    }
}
