package com.tang.itemkeeper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tang.itemkeeper.model.Item
import java.time.LocalDate
import java.time.YearMonth

/**
 * 单列滚轮：固定行高 + 上下留白，松手后自动吸附到居中项。
 * 纯 Compose 实现，不依赖第三方 Picker，也不依赖实验性的 snapping API。
 */
@Composable
private fun WheelColumn(
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

/** 年 / 月 / 日 三列滚动选择器，年份 2000–2050，日数随年月自动变化 */
@Composable
fun DatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    var year by remember { mutableStateOf(initialDate.year.coerceIn(Item.MIN_YEAR, Item.MAX_YEAR)) }
    var month by remember { mutableStateOf(initialDate.monthValue) }
    var day by remember { mutableStateOf(initialDate.dayOfMonth) }

    val maxDay = YearMonth.of(year, month).lengthOfMonth()
    val safeDay = day.coerceIn(1, maxDay)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "选择购买日期",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$year 年 $month 月 $safeDay 日",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
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
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = { onConfirm(LocalDate.of(year, month, safeDay)) }) {
                        Text("确定")
                    }
                }
            }
        }
    }
}
