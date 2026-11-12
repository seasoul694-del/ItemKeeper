package com.tang.itemkeeper.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
 * 五按钮液态玻璃底栏：胶囊玻璃底板 + 动画选中胶囊 + 中间蒂芙尼绿主按钮。
 * 支持点击与横滑切换；归总由上层以气泡呈现。
 */
@Composable
fun LiquidBottomBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
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
            targetValue = tabWidth * safeIndex,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
            label = "liquid-indicator"
        )
        var dragAccum by remember { mutableStateOf(0f) }
        val glassColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
        val highlight = Color.White.copy(alpha = 0.75f)

        // 1) 玻璃底板：半透明表面 + 高光描边 + 柔和外阴影
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(8.dp, RoundedCornerShape(999.dp))
                .clip(RoundedCornerShape(999.dp))
                .background(glassColor)
                .border(1.dp, highlight, RoundedCornerShape(999.dp))
        )

        // 2) 选中胶囊：随索引平滑位移
        Box(
            modifier = Modifier
                .padding(start = inset)
                .width(tabWidth)
                .height(56.dp)
                .align(Alignment.CenterStart)
                .offset(x = indicatorOffset)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (safeIndex == 2) Tiffany.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.55f),
                    RoundedCornerShape(999.dp)
                )
        )

        // 3) 五个按钮（含横滑切换）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = inset)
                .height(68.dp)
                .pointerInput(selectedIndex) {
                    detectHorizontalDragGestures(
                        onDragEnd = { dragAccum = 0f },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragAccum += amount
                            when {
                                dragAccum > 56f -> {
                                    onSelect((selectedIndex - 1).coerceIn(0, TAB_LABELS.lastIndex))
                                    dragAccum = 0f
                                }
                                dragAccum < -56f -> {
                                    onSelect((selectedIndex + 1).coerceIn(0, TAB_LABELS.lastIndex))
                                    dragAccum = 0f
                                }
                            }
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            TAB_LABELS.forEachIndexed { index, label ->
                val selected = index == safeIndex
                val tint = when {
                    index == 2 -> Color.White
                    selected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(68.dp)
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
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Tiffany),
                            contentAlignment = Alignment.Center
                        ) {
                            LineIcon(LineIconType.ADD, Color.White, Modifier.size(24.dp))
                        }
                    } else {
                        LineIcon(TAB_ICONS[index], tint, Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = if (selected && index != 2) tint
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                    )
                }
            }
        }
    }
}
