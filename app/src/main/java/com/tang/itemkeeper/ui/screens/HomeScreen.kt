package com.tang.itemkeeper.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import com.tang.itemkeeper.model.Item
import com.tang.itemkeeper.ui.components.ItemCard
import com.tang.itemkeeper.ui.theme.priceColor
import com.tang.itemkeeper.util.money

/** 首页：两个占位框点击后以弹簧动画自然展开，统计只给三个核心数字 */
@Composable
fun HomeScreen(
    items: List<Item>,
    listExpanded: Boolean,
    statsExpanded: Boolean,
    onToggleList: () -> Unit,
    onToggleStats: () -> Unit,
    onEdit: (Item) -> Unit,
    onDelete: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SectionPlaceholder(
            title = "已添加物品列表",
            hint = "共 ${items.size} 件 · 点击展开清单、价格与日均价格",
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
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (items.isEmpty()) {
                    EmptyHint("清单还是空的，去「添加」页记录第一件物品吧")
                } else {
                    items.sortedByDescending { it.purchaseDate }.forEach { item ->
                        ItemCard(
                            item = item,
                            onEdit = { onEdit(item) },
                            onDelete = { onDelete(item) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionPlaceholder(
            title = "物品归总统计",
            hint = "总物价 · 物品数量 · 日均价总和",
            expanded = statsExpanded,
            onClick = onToggleStats
        )

        AnimatedVisibility(
            visible = statsExpanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            StatsBlock(items = items)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionPlaceholder(
    title: String,
    hint: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f
    )
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = hint,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Text(
                text = "▼",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.rotate(arrowRotation)
            )
        }
    }
}

/** 只呈现三项聚合指标，不逐条罗列明细 */
@Composable
private fun StatsBlock(items: List<Item>) {
    val totalPrice = items.sumOf { it.price }
    val totalDaily = items.sumOf { it.dailyPrice() }
    val orange = priceColor(MaterialTheme.colorScheme.surface.luminance() < 0.5f)

    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard("总物价", "¥${money(totalPrice)}", orange, Modifier.weight(1f))
            StatCard("物品数量", "${items.size} 件", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            StatCard("日均价总和", "¥${money(totalDaily)}", orange, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "覆盖全部 ${items.size} 件物品（含所有状态）",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(20.dp)
        )
    }
}
