package com.tang.itemkeeper.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tang.itemkeeper.model.Item
import com.tang.itemkeeper.ui.theme.priceColor
import com.tang.itemkeeper.util.money

/**
 * 物品卡片：名称加粗、右侧橙色价格与日均价格、全部标签、状态专属颜色。
 * readOnly = true 用于归类结果（不提供修改入口）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemCard(
    item: Item,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var actionsVisible by remember(item.id) { mutableStateOf(false) }
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val orange = priceColor(dark)
    val subtle = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (readOnly) Modifier else Modifier.clickable { actionsVisible = !actionsVisible }),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "购于 ${item.purchaseDate} · 已持有 ${item.heldDays()} 天",
                        fontSize = 11.sp,
                        color = subtle
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "¥${money(item.price)}",
                        color = orange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "日均 ¥${money(item.dailyPrice())}/天",
                        color = orange,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusChip(item.status)
                TagChip("类别 · ${item.category}")
                TagChip("平台 · ${item.platform.ifBlank { "未填写" }}")
            }

            if (item.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "备注：${item.note}",
                    fontSize = 12.sp,
                    color = subtle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!readOnly) {
                AnimatedVisibility(
                    visible = actionsVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onEdit) { Text("编辑") }
                        TextButton(onClick = onDelete) {
                            Text("删除", color = Color(0xFFD32F2F))
                        }
                    }
                }
                if (!actionsVisible) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("点击卡片展开编辑 / 删除", fontSize = 10.sp, color = subtle)
                }
            }
        }
    }
}
