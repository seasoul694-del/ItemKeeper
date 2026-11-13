package com.tang.itemkeeper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.tang.itemkeeper.model.Item
import com.tang.itemkeeper.ui.components.GlassCard
import com.tang.itemkeeper.ui.components.InlineItemCard
import com.tang.itemkeeper.ui.components.PageHeader
import com.tang.itemkeeper.ui.components.OutlinedText
import com.tang.itemkeeper.ui.theme.LocalAppDark
import com.tang.itemkeeper.ui.theme.Space

/** 首页直接展示物品列表；滚动进度上报给顶栏做连续收起，列表本身不参与形变 */
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
    val scrollState = rememberScrollState()

    val dark = LocalAppDark.current
    val surfaceColor = if (dark) Color(0xFF15151C) else Color(0xFFFDFDFF)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = Space.x2, vertical = Space.x1)
    ) {
        // 顶栏属于滚动内容：上滑随内容滚出，下滑到内容顶端重新出现
        PageHeader(title = "首页", showSort = true, onSortClick = onSortClick)
        // 计数玻璃小条：浮在背景图上时保证可读
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = Space.x1)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                    },
                    highlight = { Highlight.Plain },
                    onDrawSurface = { drawRect(surfaceColor.copy(alpha = 0.35f)) }
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            OutlinedText(
                text = "共 ${items.size} 件物品",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                strokeWidth = 0.1.dp
            )
        }
        if (items.isEmpty()) {
            GlassCard(
                backdrop = backdrop,
                cornerRadius = 20.dp,
                contentPadding = PaddingValues(Space.x3)
            ) {
                Text(
                    text = "清单还是空的，点底栏中间的「添加」记录第一件吧",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Space.x1)) {
                // 顺序由上层（排序卡片选择）决定，这里不再自行排序
                items.forEach { item ->
                    InlineItemCard(
                        item = item,
                        expanded = expandedItemId == item.id,
                        onToggle = { onToggleItem(item.id) },
                        onEdit = { onEdit(item) },
                        onDelete = { onDelete(item) },
                        onUploadImage = { onUploadImage(item) },
                        backdrop = backdrop
                    )
                }
            }
        }
        // 给悬浮玻璃底栏留出空间，避免最后一项被遮住
        Spacer(modifier = Modifier.height(120.dp))
    }
}
