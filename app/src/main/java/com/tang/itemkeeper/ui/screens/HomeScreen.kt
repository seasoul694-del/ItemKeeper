package com.tang.itemkeeper.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tang.itemkeeper.model.Item
import com.tang.itemkeeper.ui.components.InlineItemCard
import com.tang.itemkeeper.ui.theme.Space

/**
 * 首页：一个占位框 -> spring 展开列表；列表项点击就地展开（形变在 InlineItemCard）。
 * 滚动进度直接上报给顶栏，用于搜索框连续收起；列表内容本身不参与形变。
 */
@Composable
fun HomeScreen(
    items: List<Item>,
    listExpanded: Boolean,
    expandedItemId: String?,
    onToggleList: () -> Unit,
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
            .padding(Space.x2)
    ) {
        SectionPlaceholder(
            title = "已添加物品列表",
            hint = "共 ${items.size} 件 · 点击展开清单，点条目看详情",
            expanded = listExpanded,
            onClick = onToggleList
        )

        AnimatedVisibility(
            visible = listExpanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.x1),
                verticalArrangement = Arrangement.spacedBy(Space.x1)
            ) {
                if (items.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
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
        }
        Spacer(modifier = Modifier.height(Space.x5))
    }
}

@Composable
private fun SectionPlaceholder(
    title: String,
    hint: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier.padding(Space.x2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = hint,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "▼",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}
