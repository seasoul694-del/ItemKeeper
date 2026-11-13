package com.tang.itemkeeper.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
import com.tang.itemkeeper.ui.theme.LocalAppDark
import com.tang.itemkeeper.ui.theme.Motion
import com.tang.itemkeeper.ui.theme.Space
import java.time.LocalDate
import java.time.YearMonth

/**
 * 单列滚轮：固定行高 + 上下留白，松手后自动吸附到居中项。
 * 纯 Compose 实现，不依赖第三方 Picker，也不依赖实验性的 snapping API。
 */
@Composable
fun WheelColumn(
    values: List<String>,
    initialIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeight = 40.dp
    val itemPx = with(LocalDensity.current) { itemHeight.toPx() }
    val listState = rememberLazyListState()
    val initialized = remember { mutableStateOf(false) }
    val lastReported = remember { mutableStateOf(initialIndex.coerceIn(0, values.lastIndex)) }

    LaunchedEffect(Unit) {
        listState.scrollToItem(lastReported.value)
        initialized.value = true
    }

    // 滚动停止 -> 计算居中项 -> 吸附对齐 -> 回调上层；用 lastReported 防止无限循环
    LaunchedEffect(listState.isScrollInProgress) {
        if (!initialized.value || listState.isScrollInProgress) return@LaunchedEffect
        val raw = listState.firstVisibleItemIndex +
            if (listState.firstVisibleItemScrollOffset.toFloat() > itemPx / 2f) 1 else 0
        val target = raw.coerceIn(0, values.lastIndex)
        val offset = listState.firstVisibleItemScrollOffset
        if (target != lastReported.value) {
            lastReported.value = target
            onIndexChange(target)
        }
        if (offset != 0) listState.animateScrollToItem(target)
    }

    val centerIndex = (
        listState.firstVisibleItemIndex +
            if (listState.firstVisibleItemScrollOffset.toFloat() > itemPx / 2f) 1 else 0
        ).coerceIn(0, values.lastIndex)

    Box(modifier = modifier.height(itemHeight * 5)) {
        LazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = itemHeight * 2),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(values) { index, value ->
                val isCenter = index == centerIndex
                Box(
                    modifier = Modifier.height(itemHeight).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value,
                        fontSize = if (isCenter) 17.sp else 14.sp,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCenter) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        maxLines = 1
                    )
                }
            }
        }
        // 中心选中框（不带 clickable，不拦截滚动手势）
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(8.dp)
                )
        )
    }
}

/**
 * 年 / 月 / 日 三列滚动选择玻璃卡：与首页排序卡同材质
 * （透明折射全局背景 + 一点模糊，2026-11-13 用户定稿）。
 * 注意：不再使用系统 Dialog（独立窗口无法进入玻璃采样树），改为应用内覆盖层。
 */
@Composable
fun DatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    backdrop: Backdrop
) {
    var year by remember { mutableStateOf(initialDate.year.coerceIn(Item.MIN_YEAR, Item.MAX_YEAR)) }
    var month by remember { mutableStateOf(initialDate.monthValue) }
    var day by remember { mutableStateOf(initialDate.dayOfMonth) }

    val maxDay = YearMonth.of(year, month).lengthOfMonth()
    val safeDay = day.coerceIn(1, maxDay)

    val dark = LocalAppDark.current
    val surfaceColor = if (dark) Color(0xFF15151C) else Color(0xFFFDFDFF)

    // 挂载即弹入（WWDC 弹簧）
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (dark) 0.46f else 0.28f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = scaleIn(
                initialScale = 0.86f,
                animationSpec = spring(
                    dampingRatio = Motion.BOUNCY_DAMPING,
                    stiffness = Motion.BOUNCY_STIFFNESS
                )
            ) + fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(24f.dp) },
                        effects = {
                            val minDimension = size.minDimension
                            vibrancy()
                            blur(6f.dp.toPx())
                            lens(
                                refractionHeight = 0.24f * minDimension,
                                refractionAmount = 0.42f * minDimension,
                                depthEffect = true
                            )
                        },
                                                onDrawSurface = { drawRect(surfaceColor.copy(alpha = 0.42f)) }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(Space.x2)
            ) {
                OutlinedText(
                    text = "选择购买日期",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedText(
                    text = "$year 年 $month 月 $safeDay 日",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    WheelColumn(
                        values = (Item.MIN_YEAR..Item.MAX_YEAR).map { "${it}年" },
                        initialIndex = year - Item.MIN_YEAR,
                        onIndexChange = { year = Item.MIN_YEAR + it },
                        modifier = Modifier.weight(1.2f)
                    )
                    WheelColumn(
                        values = (1..12).map { "${it}月" },
                        initialIndex = month - 1,
                        onIndexChange = { month = it + 1 },
                        modifier = Modifier.weight(0.9f)
                    )
                    // 天数变化（如 1 月 31 日 -> 2 月）时强制重建该列，避免越界
                    key(maxDay) {
                        WheelColumn(
                            values = (1..maxDay).map { "${it}日" },
                            initialIndex = safeDay - 1,
                            onIndexChange = { day = it + 1 },
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        OutlinedText(
                            text = "取消",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onConfirm(LocalDate.of(year, month, safeDay)) }) {
                        OutlinedText(
                            text = "确定",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 13.sp,
                            strokeWidth = 0.3.dp
                        )
                    }
                }
            }
        }
    }
}
