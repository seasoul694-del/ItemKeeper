package com.tang.itemkeeper.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tang.itemkeeper.model.Item
import com.tang.itemkeeper.ui.theme.Brand
import com.tang.itemkeeper.ui.theme.MotionCurve
import com.tang.itemkeeper.ui.theme.SaveRed
import com.tang.itemkeeper.ui.theme.Space
import com.tang.itemkeeper.ui.theme.priceColor
import com.tang.itemkeeper.util.money

/**
 * 列表项就地展开：88 -> 268dp；详情三层依次淡入。
 * 详情内容严格控制在 180dp 内，编辑/删除按钮始终可见。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InlineItemCard(
    item: Item,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = MotionCurve),
        label = "inline-expand"
    )
    val cardHeight = 88.dp + 180.dp * p
    val imageAlpha = ((p - 0.08f) / 0.26f).coerceIn(0f, 1f)
    val textAlpha = ((p - 0.30f) / 0.26f).coerceIn(0f, 1f)
    val tagAlpha = ((p - 0.52f) / 0.30f).coerceIn(0f, 1f)

    val orange = priceColor(MaterialTheme.colorScheme.surface.luminance() < 0.5f)
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .padding(horizontal = Space.x2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                StatusChip(item.status)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "¥${money(item.price)}",
                    color = orange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = "日均 ¥${money(item.dailyPrice())}/天",
                    color = orange,
                    fontSize = 11.sp
                )
            }
        }

        // 详情：88dp 之下 180dp，内容紧凑排布，按钮不被裁切
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 88.dp, start = Space.x2, end = Space.x2)
                .height(180.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .alpha(imageAlpha)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brand.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.name.take(1),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Light,
                    color = Brand
                )
            }
            Spacer(modifier = Modifier.width(Space.x2))
            Column(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.alpha(textAlpha)) {
                    Text("购买日期  ${item.purchaseDate}", fontSize = 12.sp, lineHeight = 18.sp, color = subtle)
                    Text(
                        "购买平台  ${item.platform.ifBlank { "未填写" }}",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = subtle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("已持有    ${item.heldDays()} 天", fontSize = 12.sp, lineHeight = 18.sp, color = subtle)
                    if (item.note.isNotBlank()) {
                        Text(
                            "备注      ${item.note}",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = subtle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.alpha(tagAlpha),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    maxItemsInEachRow = 2
                ) {
                    TagChip("类别 · ${item.category}")
                    TagChip("平台 · ${item.platform.ifBlank { "未填写" }}")
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(tagAlpha),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onEdit) { Text("编辑", fontSize = 12.sp) }
                    TextButton(onClick = onDelete) {
                        Text("删除", color = SaveRed, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
