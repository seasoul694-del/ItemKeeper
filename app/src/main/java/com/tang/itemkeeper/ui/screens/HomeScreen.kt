package com.tang.itemkeeper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.Capsule
import com.tang.itemkeeper.glass.utils.LiquidGestureState
import com.tang.itemkeeper.glass.utils.LiquidGestureStyle
import com.tang.itemkeeper.glass.utils.liquidGlassGestures
import com.tang.itemkeeper.model.Item
import com.tang.itemkeeper.ui.components.GlassCard
import com.tang.itemkeeper.ui.components.InlineItemCard
import com.tang.itemkeeper.ui.components.OutlinedText
import com.tang.itemkeeper.ui.components.PageHeader
import com.tang.itemkeeper.ui.theme.LocalAppDark
import com.tang.itemkeeper.ui.theme.Space

/**
 * 首页 = Lazy scroll container（官方 LazyScrollContainerContent 配方，2026-11-15）：
 * 玻璃卡放进滚动容器，折射随滚动实时跟随（backdrop 坐标联动，无需接线）；
 * 物品卡/计数条/空态卡挂液态手势（点按光斑 + 拖动牵拉），竖向拖动过阈值让渡页面滚动。
 * 顺序由上层（排序卡片选择）决定，这里不再自行排序。
 */
@Composable
fun HomeScreen(
    items: List<Item>,
    expandedItemId: String?,
    onToggleItem: (String) -> Unit,
    onEdit: (Item) -> Unit,
    onDelete: (Item) -> Unit,
    onUploadImage: (Item) -> Unit,
    onSortClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val dark = LocalAppDark.current
    val surfaceColor = if (dark) Color(0xFF15151C) else Color(0xFFFDFDFF)

    // 计数玻璃小条的液态手势（竖向让渡目标 = 页面列表）
    val gestureScope = rememberCoroutineScope()
    val countGesture = remember { LiquidGestureState(gestureScope) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        // 边距走 contentPadding 而非 modifier padding：LazyColumn 视口裁剪边
        // = 控件全宽（到屏幕边缘），玻璃牵拉凸出时不会被 16dp 页边距裁掉
        contentPadding = PaddingValues(horizontal = Space.x2, vertical = Space.x1),
        verticalArrangement = Arrangement.spacedBy(Space.x1)
    ) {
        // 顶栏属于滚动内容：上滑随内容滚出，下滑到内容顶端重新出现
        item(key = "header") {
            PageHeader(title = "首页", showSort = true, onSortClick = onSortClick)
        }

        // 计数玻璃小条：浮在背景图上时保证可读
        item(key = "count") {
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .liquidGlassGestures(countGesture, listState)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                        },
                        highlight = { Highlight.Plain },
                        layerBlock = countGesture.layerBlock,
                        onDrawSurface = { drawRect(surfaceColor.copy(alpha = 0.35f)) }
                    )
                    .then(countGesture.highlightModifier)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                OutlinedText(
                    text = "共 ${items.size} 件物品",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    strokeWidth = 0.1.dp
                )
            }
        }

        if (items.isEmpty()) {
            item(key = "empty") {
                GlassCard(
                    backdrop = backdrop,
                    cornerRadius = 20.dp,
                    contentPadding = PaddingValues(Space.x3),
                    liquidStyle = LiquidGestureStyle(),
                    pageScroll = listState
                ) {
                    Text(
                        text = "清单还是空的，点底栏中间的「添加」记录第一件吧",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(items, key = { it.id }) { item ->
                InlineItemCard(
                    item = item,
                    expanded = expandedItemId == item.id,
                    onToggle = { onToggleItem(item.id) },
                    onEdit = { onEdit(item) },
                    onDelete = { onDelete(item) },
                    onUploadImage = { onUploadImage(item) },
                    backdrop = backdrop,
                    pageScroll = listState
                )
            }
        }

        // 给悬浮玻璃底栏留出空间，避免最后一项被遮住
        item(key = "bottom-spacer") {
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}
