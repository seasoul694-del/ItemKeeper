package com.tang.itemkeeper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.tang.itemkeeper.ui.components.DatePickerDialog
import com.tang.itemkeeper.ui.components.GlassCard
import com.tang.itemkeeper.ui.components.PageHeader
import com.tang.itemkeeper.ui.components.OutlinedText
import com.tang.itemkeeper.ui.components.RoundedTextField
import com.tang.itemkeeper.ui.components.SelectableChip
import com.tang.itemkeeper.ui.theme.LocalAppDark
import com.tang.itemkeeper.ui.theme.Space
import com.tang.itemkeeper.ui.theme.priceColor
import com.tang.itemkeeper.util.money
import java.time.LocalDate

private val FILTER_PRICE_PATTERN = Regex("^\\d{0,9}(\\.\\d{0,2})?$")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterScreen(
    items: List<Item>,
    categories: List<String>,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var statusSelection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var categorySelection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    fun toggle(set: Set<String>, value: String): Set<String> =
        if (value in set) set - value else set + value

    val results = remember(
        items, minPrice, maxPrice, startDate, endDate, statusSelection, categorySelection
    ) {
        val min = minPrice.toDoubleOrNull()
        val max = maxPrice.toDoubleOrNull()
        items.filter { item ->
            val bought = runCatching { LocalDate.parse(item.purchaseDate) }.getOrNull()
            val priceOk = (min == null || item.price >= min) && (max == null || item.price <= max)
            val dateOk = (startDate == null || bought == null || !bought.isBefore(startDate)) &&
                (endDate == null || bought == null || !bought.isAfter(endDate))
            val statusOk = statusSelection.isEmpty() || item.status in statusSelection
            val categoryOk = categorySelection.isEmpty() || item.category in categorySelection
            priceOk && dateOk && statusOk && categoryOk
        }.sortedByDescending { it.purchaseDate }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.x2, vertical = Space.x1)
    ) {
        PageHeader(title = "归类")
        GlassCard(
            backdrop = backdrop,
            cornerRadius = 20.dp,
            contentPadding = PaddingValues(Space.x2)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedText(
                    text = "筛选条件",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                FilterLabel("价格区间", "（¥）")
                Row(horizontalArrangement = Arrangement.spacedBy(Space.x1)) {
                    RoundedTextField(
                        value = minPrice,
                        onValueChange = { if (FILTER_PRICE_PATTERN.matches(it)) minPrice = it },
                        placeholder = "最低价",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    RoundedTextField(
                        value = maxPrice,
                        onValueChange = { if (FILTER_PRICE_PATTERN.matches(it)) maxPrice = it },
                        placeholder = "最高价",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                FilterLabel("购买日期区间")
                Row(horizontalArrangement = Arrangement.spacedBy(Space.x1)) {
                    OutlinedButton(
                        onClick = { pickingStart = true },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        // 按钮透视化策略：微透背景 + 无边框（与「最低价」输入框同观感）
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
                        ),
                        border = null
                    ) {
                        OutlinedText(
                            text = "起",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    }
                    OutlinedButton(
                        onClick = { pickingEnd = true },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
                        ),
                        border = null
                    ) {
                        OutlinedText(
                            text = "止",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    }
                }

                FilterLabel("物品状态", "（可多选，不选 = 全部）")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.x1),
                    verticalArrangement = Arrangement.spacedBy(Space.x1)
                ) {
                    Item.STATUSES.forEach { option ->
                        SelectableChip(
                            text = option,
                            selected = option in statusSelection,
                            onClick = { statusSelection = toggle(statusSelection, option) }
                        )
                    }
                }

                FilterLabel("物品类别", "（可多选，不选 = 全部）")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.x1),
                    verticalArrangement = Arrangement.spacedBy(Space.x1)
                ) {
                    categories.forEach { option ->
                        SelectableChip(
                            text = option,
                            selected = option in categorySelection,
                            onClick = { categorySelection = toggle(categorySelection, option) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = {
                            minPrice = ""
                            maxPrice = ""
                            startDate = null
                            endDate = null
                            statusSelection = emptySet()
                            categorySelection = emptySet()
                        }
                    ) { Text("重置条件") }
                }
            }
        }

        Spacer(modifier = Modifier.height(Space.x2))
        OutlinedText(
            text = "符合条件 ${results.size} 件",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(Space.x1))
        if (results.isEmpty()) {
            OutlinedText(
                text = "没有匹配的物品，试试放宽筛选条件",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Space.x1)) {
                results.forEach { item -> ResultRow(item, backdrop) }
            }
        }
        Spacer(modifier = Modifier.height(120.dp))
    }

    if (pickingStart) {
        DatePickerDialog(
            initialDate = startDate ?: LocalDate.now().minusYears(1),
            onDismiss = { pickingStart = false },
            onConfirm = { startDate = it; pickingStart = false },
            backdrop = backdrop
        )
    }
    if (pickingEnd) {
        DatePickerDialog(
            initialDate = endDate ?: LocalDate.now(),
            onDismiss = { pickingEnd = false },
            onConfirm = { endDate = it; pickingEnd = false },
            backdrop = backdrop
        )
    }
}

@Composable
private fun ResultRow(item: Item, backdrop: Backdrop) {
    val dark = LocalAppDark.current
    val surfaceColor = if (dark) Color(0xFF15151C) else Color(0xFFFDFDFF)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(16f.dp) },
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
            modifier = Modifier.fillMaxWidth().padding(Space.x2),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                OutlinedText(
                    text = item.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                OutlinedText(
                    text = "${item.category} · ${item.status} · ${item.purchaseDate}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Text(
                "¥${money(item.price)}",
                color = priceColor(dark),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FilterLabel(main: String, hint: String? = null) {
    // 括号提示与主标题分离：小一号、更虚的灰色（含括号本身）
    Row(
        modifier = Modifier.padding(top = Space.x2, bottom = Space.x1),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = main,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        if (hint != null) {
            Text(
                text = "  $hint",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
        }
    }
}
