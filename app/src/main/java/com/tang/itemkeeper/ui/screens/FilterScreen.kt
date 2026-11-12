package com.tang.itemkeeper.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tang.itemkeeper.model.Item
import com.tang.itemkeeper.ui.components.DatePickerDialog
import com.tang.itemkeeper.ui.components.RoundedTextField
import com.tang.itemkeeper.ui.components.SelectableChip
import com.tang.itemkeeper.ui.theme.Space
import java.time.LocalDate

private val FILTER_PRICE_PATTERN = Regex("^\\d{0,9}(\\.\\d{0,2})?$")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterScreen(
    items: List<Item>,
    categories: List<String>,
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
            .padding(Space.x2)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(Space.x2)) {
                Text("筛选条件", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(Space.x1))

                FilterLabel("价格区间（元）")
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
                        shape = RoundedCornerShape(28.dp)
                    ) { Text("起 ${startDate?.toString() ?: "不限"}", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = { pickingEnd = true },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) { Text("止 ${endDate?.toString() ?: "不限"}", fontSize = 12.sp) }
                }

                FilterLabel("物品状态（可多选，不选 = 全部）")
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

                FilterLabel("物品类别（可多选，不选 = 全部）")
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
        Text("符合条件 ${results.size} 件", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(Space.x1))
        Column(verticalArrangement = Arrangement.spacedBy(Space.x1)) {
            if (results.isEmpty()) {
                Text(
                    "没有匹配的物品，试试放宽筛选条件",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                results.forEach { item ->
                    ResultRow(item)
                }
            }
        }
        Spacer(modifier = Modifier.height(Space.x4))
    }

    if (pickingStart) {
        DatePickerDialog(
            initialDate = startDate ?: LocalDate.now().minusYears(1),
            onDismiss = { pickingStart = false },
            onConfirm = { startDate = it; pickingStart = false }
        )
    }
    if (pickingEnd) {
        DatePickerDialog(
            initialDate = endDate ?: LocalDate.now(),
            onDismiss = { pickingEnd = false },
            onConfirm = { endDate = it; pickingEnd = false }
        )
    }
}

@Composable
private fun ResultRow(item: Item) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Space.x2),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    "${item.category} · ${item.status} · ${item.purchaseDate}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "¥${com.tang.itemkeeper.util.money(item.price)}",
                color = com.tang.itemkeeper.ui.theme.priceColor(false),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FilterLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = Space.x2, bottom = Space.x1)
    )
}
