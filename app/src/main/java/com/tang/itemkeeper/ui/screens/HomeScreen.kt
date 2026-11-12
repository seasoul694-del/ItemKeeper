package com.tang.itemkeeper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tang.itemkeeper.model.Item
import com.tang.itemkeeper.ui.components.InlineItemCard
import com.tang.itemkeeper.ui.theme.Space

/** 首页直接展示物品列表；滚动进度上报给顶栏做连续收起，列表本身不参与形变 */
@Composable
fun HomeScreen(
    items: List<Item>,
    expandedItemId: String?,
    onToggleItem: (String) -> Unit,
    onEdit: (Item) -> Unit,
    onDelete: (Item) -> Unit,
    onScrollProgress: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState.value, scrollState.maxValue) {
        val max = scrollState.maxValue
        onScrollProgress(if (max <= 0) 0f else scrollState.value.toFloat() / max.toFloat())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = Space.x2, vertical = Space.x1)
    ) {
        Text(
            text = "共 ${items.size} 件物品",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = Space.x1)
        )
        if (items.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Text(
                    text = "清单还是空的，点底栏中间的「添加」记录第一件吧",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Space.x3)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Space.x1)) {
                items.sortedByDescending { it.purchaseDate }.forEach { item ->
                    InlineItemCard(
                        item = item,
                        expanded = expandedItemId == item.id,
                        onToggle = { onToggleItem(item.id) },
                        onEdit = { onEdit(item) },
                        onDelete = { onDelete(item) }
                    )
                }
            }
        }
        // 给悬浮玻璃底栏留出空间，避免最后一项被遮住
        Spacer(modifier = Modifier.height(120.dp))
    }
}
