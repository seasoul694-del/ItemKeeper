package com.tang.itemkeeper.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.RoundedRectangle
import com.tang.itemkeeper.model.Item
import com.tang.itemkeeper.ui.theme.Brand
import com.tang.itemkeeper.ui.theme.LocalAppDark
import com.tang.itemkeeper.ui.theme.Motion
import com.tang.itemkeeper.ui.theme.SaveRed
import com.tang.itemkeeper.ui.theme.Space
import com.tang.itemkeeper.ui.theme.priceColor
import com.tang.itemkeeper.util.ItemImages
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
    onUploadImage: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    // WWDC23 弹簧体系（≈ .snappy：轻快小回弹）——就地展开/收起同一弹簧，速度连续
    val p by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Motion.SNAPPY_DAMPING,
            stiffness = Motion.SNAPPY_STIFFNESS,
            visibilityThreshold = 0.002f
        ),
        label = "inline-expand"
    )
    val cardHeight = 88.dp + 144.dp * p   // 展开总高 232dp：紧凑排布，不再大片留白
    val imageAlpha = ((p - 0.08f) / 0.26f).coerceIn(0f, 1f)
    val textAlpha = ((p - 0.30f) / 0.26f).coerceIn(0f, 1f)
    val tagAlpha = ((p - 0.52f) / 0.30f).coerceIn(0f, 1f)

    val orange = priceColor(MaterialTheme.colorScheme.surface.luminance() < 0.5f)
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant
    val dark = LocalAppDark.current
    val surfaceColor = if (dark) Color(0xFF15151C) else Color(0xFFFDFDFF)

    // 展开动画流畅度关键：外层 Box 只做「高度动画 + 裁剪窗口」（布局级，便宜），
    // 玻璃面（drawBackdrop）固定 232dp 挂在内层——shader 尺寸恒定，
    // 不会在高度动画期间每帧重建 lens 采样，卡顿即源于此。
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onToggle)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(232.dp)
                // 物品卡玻璃化：折射纯背景图（bgBackdrop），透明表面 + 一点模糊保证可读
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(20f.dp) },
                    effects = {
                        vibrancy()
                        blur(6f.dp.toPx())
                        lens(
                            refractionHeight = 0.24f * size.minDimension,
                            refractionAmount = 0.42f * size.minDimension,
                            depthEffect = true
                        )
                    },
                    onDrawSurface = { drawRect(surfaceColor.copy(alpha = 0.30f)) }
                )
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .padding(horizontal = Space.x2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                OutlinedText(
                    text = item.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                StatusChip(item.status)
            }
            Column(horizontalAlignment = Alignment.End) {
                OutlinedText(
                    text = "¥${money(item.price)}",
                    color = orange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                OutlinedText(
                    text = "日均 ¥${money(item.dailyPrice())}/天",
                    color = orange,
                    fontSize = 11.sp
                )
            }
        }

        // 详情：88dp 之下 180dp；收起完成后整层从组合中移除，
        // 杜绝「未展开却点到下方编辑/删除」的幽灵点击（alpha=0 的层仍会接收触摸）
        if (p > 0.02f) {
            val context = LocalContext.current
            // 上传后的物品图片（remember 按 image 文件名缓存，覆盖上传后自动刷新）
            val itemBitmap = remember(item.image) {
                item.image.takeIf { it.isNotBlank() }?.let { ItemImages.load(context, it) }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 88.dp, start = Space.x2, end = Space.x2)
                    .height(144.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .alpha(imageAlpha)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brand.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (itemBitmap != null) {
                        Image(
                            bitmap = itemBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(72.dp)
                        )
                    } else {
                        OutlinedText(
                            text = item.name.take(1),
                            color = Brand,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Light,
                            strokeWidth = 0.3.dp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(Space.x2))
                Column(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.alpha(textAlpha)) {
                        OutlinedText(
                            text = "购买日期  ${item.purchaseDate}",
                            color = subtle,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        // 「购买平台」与下方「平台 · 闲鱼」标签重复，按用户要求移除
                        OutlinedText(
                            text = "已持有    ${item.heldDays()} 天",
                            color = subtle,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        if (item.note.isNotBlank()) {
                            OutlinedText(
                                text = "备注      ${item.note}",
                                color = subtle,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.alpha(tagAlpha),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        maxItemsInEachRow = 2
                    ) {
                        TagChip("类别 · ${item.category}")
                        TagChip("平台 · ${item.platform.ifBlank { "未填写" }}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(tagAlpha),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onUploadImage) {
                            OutlinedText(
                                text = "上传图片",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = onEdit) {
                            OutlinedText(
                                text = "编辑",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }
                        TextButton(onClick = onDelete) {
                            OutlinedText(
                                text = "删除",
                                color = SaveRed,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
        }
    }
}
