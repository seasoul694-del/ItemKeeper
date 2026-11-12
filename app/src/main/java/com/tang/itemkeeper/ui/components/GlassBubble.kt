package com.tang.itemkeeper.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tang.itemkeeper.model.Item
import com.tang.itemkeeper.ui.theme.Brand
import com.tang.itemkeeper.ui.theme.Space
import com.tang.itemkeeper.ui.theme.Tiffany
import com.tang.itemkeeper.ui.theme.priceColor
import com.tang.itemkeeper.util.money
import kotlinx.coroutines.delay

/** 数字滚动计数 */
@Composable
private fun CountNumber(
    target: Double,
    color: Color,
    prefix: String = "",
    suffix: String = "",
    started: Boolean
) {
    val value by animateFloatAsState(
        targetValue = if (started) target.toFloat() else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "count-up"
    )
    Text(
        text = "$prefix${money(value.toDouble())}$suffix",
        fontSize = 22.sp,
        fontWeight = FontWeight.Light,
        color = color
    )
}

/**
 * 「归总」玻璃气泡：点击底栏弹出，点卡片外自然弹回。
 * 数字滚动计数、柱子从 0 长到实际高度（进入屏幕时触发）。
 */
@Composable
fun SummaryBubble(
    visible: Boolean,
    items: List<Item>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(160)
            started = true
        } else {
            started = false
        }
    }

    val dark = MaterialTheme.colorScheme.surface.luminanceCompat() < 0.5f
    val orange = priceColor(dark)

    val totalPrice = items.sumOf { it.price }
    val totalDaily = items.sumOf { it.dailyPrice() }
    val byCategory = items
        .groupBy { it.category }
        .map { (name, list) -> name to list.sumOf { it.price } }
        .sortedByDescending { it.second }
        .take(6)
    val maxAmount = byCategory.maxOfOrNull { it.second } ?: 0.0

    val grow by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
        label = "bar-grow"
    )

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.86f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = scaleOut(
            targetScale = 0.86f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.18f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(28.dp),
                color = if (dark) Color(0xE6242430) else Color(0xECFFFFFF),
                border = BorderStroke(1.dp, Color.White.copy(alpha = if (dark) 0.12f else 0.85f)),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(Space.x3)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("物品归总", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "覆盖全部 ${items.size} 件物品（含所有状态）",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Space.x3))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("总物价", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            CountNumber(totalPrice, orange, prefix = "¥", started = started)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("物品数量", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            CountNumber(items.size.toDouble(), Brand, suffix = " 件", started = started)
                        }
                    }
                    Spacer(modifier = Modifier.height(Space.x2))
                    Text("日均价总和", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CountNumber(totalDaily, orange, prefix = "¥", started = started)

                    Spacer(modifier = Modifier.height(Space.x3))
                    Text("各类别物价分布", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(Space.x2))

                    if (byCategory.isEmpty()) {
                        Text(
                            "还没有物品，先去「添加」记录一件吧",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            horizontalArrangement = Arrangement.spacedBy(Space.x1),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            byCategory.forEachIndexed { index, (name, amount) ->
                                val ratio = if (maxAmount > 0) (amount / maxAmount).toFloat() else 0f
                                val barHeight = 120.dp * ratio * grow
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Text(
                                        text = if (grow > 0.6f) "¥${money(amount * grow.toDouble())}" else "",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .height(barHeight.coerceAtLeast(1.dp))
                                            .background(
                                                color = if (index == 0) Tiffany else Brand.copy(alpha = 0.55f),
                                                shape = RoundedCornerShape(
                                                    topStart = 8.dp,
                                                    topEnd = 8.dp
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = name,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Color.luminanceCompat(): Float = (red * 0.299f + green * 0.587f + blue * 0.114f)
