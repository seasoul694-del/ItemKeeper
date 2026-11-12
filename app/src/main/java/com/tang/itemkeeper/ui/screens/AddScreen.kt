package com.tang.itemkeeper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tang.itemkeeper.model.Item
import com.tang.itemkeeper.ui.components.DatePickerDialog
import com.tang.itemkeeper.ui.components.SelectableChip
import com.tang.itemkeeper.ui.theme.SaveRed
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

/** 价格：最多 9 位整数 + 最多 2 位小数，允许 "1." 这类输入中间态 */
private val PRICE_PATTERN = Regex("^\\d{0,9}(\\.\\d{0,2})?$")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddScreen(
    editing: Item?,
    categories: List<String>,
    onAddCategory: (String) -> Boolean,
    onSave: (Item) -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editKey = editing?.id

    var name by remember(editKey) { mutableStateOf(editing?.name ?: "") }
    var priceText by remember(editKey) {
        mutableStateOf(
            editing?.price?.let { String.format(Locale.CHINA, "%.2f", it) } ?: ""
        )
    }
    var date by remember(editKey) {
        mutableStateOf(editing?.purchaseDate ?: LocalDate.now().toString())
    }
    var platform by remember(editKey) { mutableStateOf(editing?.platform ?: "") }
    var status by remember(editKey) { mutableStateOf(editing?.status ?: Item.STATUSES.first()) }
    var category by remember(editKey) {
        mutableStateOf(editing?.category ?: Item.DEFAULT_CATEGORIES.first())
    }
    var note by remember(editKey) { mutableStateOf(editing?.note ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    fun submit() {
        val parsedPrice = priceText.toDoubleOrNull()
        when {
            name.isBlank() -> {
                errorMessage = "请填写物品名称"
                return
            }
            parsedPrice == null -> {
                errorMessage = "请填写有效价格（支持两位小数）"
                return
            }
        }
        errorMessage = ""
        onSave(
            Item(
                id = editing?.id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                price = parsedPrice ?: 0.0,
                purchaseDate = date,
                platform = platform.trim(),
                status = status,
                category = category,
                note = note.trim()
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (editing != null) {
            Text(
                text = "正在编辑：${editing.name}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        FieldLabel("物品名称")
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= Item.NAME_MAX) name = it },
            singleLine = true,
            placeholder = { Text("必填，最长 30 字符") },
            supportingText = { Text("${name.length}/${Item.NAME_MAX}") },
            modifier = Modifier.fillMaxWidth()
        )

        FieldLabel("物品价格（元）")
        OutlinedTextField(
            value = priceText,
            onValueChange = { if (PRICE_PATTERN.matches(it)) priceText = it },
            singleLine = true,
            placeholder = { Text("支持小数，精确到两位") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        FieldLabel("购买日期")
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("$date    ·    年 / 月 / 日 滚动选择（2000–2050）")
        }

        FieldLabel("购买平台")
        OutlinedTextField(
            value = platform,
            onValueChange = { platform = it },
            singleLine = true,
            placeholder = { Text("可自由输入，或点选下方快捷项") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Item.PLATFORM_CANDIDATES.forEach { candidate ->
                SelectableChip(
                    text = candidate,
                    selected = platform == candidate,
                    onClick = { platform = candidate }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        FieldLabel("物品状态")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Item.STATUSES.forEach { option ->
                SelectableChip(
                    text = option,
                    selected = status == option,
                    onClick = { status = option }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        FieldLabel("物品类别")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { option ->
                SelectableChip(
                    text = option,
                    selected = category == option,
                    onClick = { category = option }
                )
            }
            SelectableChip(
                text = "＋ 自定义类别",
                selected = false,
                onClick = { showCategoryDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        FieldLabel("备注")
        OutlinedTextField(
            value = note,
            onValueChange = { if (it.length <= Item.NOTE_MAX) note = it },
            minLines = 2,
            maxLines = 3,
            placeholder = { Text("选填，最长 50 字符") },
            supportingText = { Text("${note.length}/${Item.NOTE_MAX}") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))
        if (errorMessage.isNotBlank()) {
            Text(text = errorMessage, color = SaveRed, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
        }

        // 红色保存按钮
        Button(
            onClick = { submit() },
            colors = ButtonDefaults.buttonColors(
                containerColor = SaveRed,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(
                text = if (editing == null) "保存" else "保存修改",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (editing != null) {
            TextButton(onClick = onCancelEdit, modifier = Modifier.fillMaxWidth()) {
                Text("取消编辑")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now()),
            onDismiss = { showDatePicker = false },
            onConfirm = {
                date = it.toString()
                showDatePicker = false
            }
        )
    }

    if (showCategoryDialog) {
        var newCategory by remember { mutableStateOf("") }
        var categoryError by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("添加自定义类别") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = {
                            if (it.length <= 12) newCategory = it
                            categoryError = ""
                        },
                        singleLine = true,
                        placeholder = { Text("如：图书、运动装备") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (categoryError.isNotBlank()) {
                        Text(categoryError, color = SaveRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (onAddCategory(newCategory)) {
                            category = newCategory.trim()
                            showCategoryDialog = false
                        } else {
                            categoryError = "类别不能为空、重复或超过 12 字"
                        }
                    }
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}
