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
 * 列表项就地展开：高度 88 -> 268 连续过渡，后续项由 Column 自动跟随位移；
 * 详情的图 / 文字 / 标签按同一个高度进度分三层依次淡入。
 * 展开与收起共用一条曲线，不跳页、不弹层。
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
        // 收起态始终可见的基础信息
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.x2, vertical = Space.x2),
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
                Spacer(modifier = Modifier.height(Space.x1))
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

        // 展开态详情：从 88dp 之下开始，随高度一起被裁切/显露
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 88.dp, start = Space.x2, end = Space.x2)
                .height(180.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .alpha(imageAlpha)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brand.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.name.take(1),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    color = Brand
                )
            }
            Spacer(modifier = Modifier.width(Space.x2))
            Column(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.alpha(textAlpha)) {
                    Text("购买日期  ${item.purchaseDate}", fontSize = 12.sp, color = subtle)
                    Text("购买平台  ${item.platform.ifBlank { "未填写" }}", fontSize = 12.sp, color = subtle)
                    Text("已持有    ${item.heldDays()} 天", fontSize = 12.sp, color = subtle)
                    if (item.note.isNotBlank()) {
                        Text(
                            "备注      ${item.note}",
                            fontSize = 12.sp,
                            color = subtle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Space.x1))
                FlowRow(
                    modifier = Modifier.alpha(tagAlpha),
                    horizontalArrangement = Arrangement.spacedBy(Space.x1),
                    verticalArrangement = Arrangement.spacedBy(Space.x1)
                ) {
                    StatusChip(item.status)
                    TagChip("类别 · ${item.category}")
                    TagChip("平台 · ${item.platform.ifBlank { "未填写" }}")
                }
                Spacer(modifier = Modifier.height(Space.x1))
                Row(
                    modifier = Modifier.alpha(tagAlpha),
                    horizontalArrangement = Arrangement.End
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onEdit) { Text("编辑", fontSize = 12.sp) }
                    TextButton(onClick = onDelete) {
                        Text("删除", color = SaveRed, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
