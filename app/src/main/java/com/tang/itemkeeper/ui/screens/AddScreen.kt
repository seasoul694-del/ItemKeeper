package com.tang.itemkeeper.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import com.tang.itemkeeper.glass.utils.LiquidGestureState
import com.tang.itemkeeper.glass.utils.liquidGlassGestures
import com.tang.itemkeeper.model.Item
import com.tang.itemkeeper.ui.components.DatePickerDialog
import com.tang.itemkeeper.ui.components.EndDateDialog
import com.tang.itemkeeper.ui.components.OutlinedText
import com.tang.itemkeeper.ui.components.PageHeader
import com.tang.itemkeeper.ui.components.RoundedTextField
import com.tang.itemkeeper.ui.components.SelectableChip
import com.tang.itemkeeper.ui.theme.LocalAppDark
import com.tang.itemkeeper.ui.theme.Motion
import com.tang.itemkeeper.ui.theme.SaveRed
import com.tang.itemkeeper.ui.theme.Space
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

private val PRICE_PATTERN = Regex("^\\d{0,9}(\\.\\d{0,2})?$")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddScreen(
    editing: Item?,
    categories: List<String>,
    backdrop: Backdrop,
    onAddCategory: (String) -> Boolean,
    onRemoveCategory: (String) -> Boolean,
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
    // 终止状态（用完/报废/售出/丢失）的截止日期与售出价格
    var endDate by remember(editKey) { mutableStateOf(editing?.endDate ?: "") }
    var salePrice by remember(editKey) {
        mutableStateOf(
            editing?.salePrice?.takeIf { it > 0 }?.let { String.format(Locale.CHINA, "%.2f", it) } ?: ""
        )
    }
    var pendingEndStatus by remember { mutableStateOf<String?>(null) }
    var category by remember(editKey) {
        mutableStateOf(editing?.category ?: Item.DEFAULT_CATEGORIES.first())
    }
    var note by remember(editKey) { mutableStateOf(editing?.note ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showRemoveCategoryDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    fun submit() {
        val parsedPrice = priceText.toDoubleOrNull()
        if (name.isBlank()) {
            errorMessage = "请填写物品名称"
            return
        }
        if (parsedPrice == null) {
            errorMessage = "请填写有效价格（支持两位小数）"
            return
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
                note = note.trim(),
                image = editing?.image ?: "",
                endDate = if (status in Item.TERMINAL_STATUSES) endDate else "",
                salePrice = if (status == "售出") salePrice.toDoubleOrNull() ?: 0.0 else 0.0
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Space.x2),
        verticalArrangement = Arrangement.spacedBy(Space.x1)
    ) {
        PageHeader(title = "添加")
        if (editing != null) {
            Text(
                text = "正在编辑：${editing.name}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        FieldLabel("物品名称")
        RoundedTextField(
            value = name,
            onValueChange = { if (it.length <= Item.NAME_MAX) name = it },
            placeholder = "必填，最长 30 字符",
            supportingText = "${name.length}/${Item.NAME_MAX}"
        )

        FieldLabel("物品价格（元）")
        RoundedTextField(
            value = priceText,
            onValueChange = { if (PRICE_PATTERN.matches(it)) priceText = it },
            placeholder = "支持小数，精确到两位",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            supportingText = "价格与日均价格会在首页以橙色显示"
        )

        FieldLabel("购买日期")
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("$date    ·    年 / 月 / 日 滚动选择（2000–2050）", fontSize = 13.sp)
        }

        FieldLabel("购买平台")
        RoundedTextField(
            value = platform,
            onValueChange = { platform = it },
            placeholder = "可自由输入，或点选下方快捷项"
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.x1),
            verticalArrangement = Arrangement.spacedBy(Space.x1)
        ) {
            Item.PLATFORM_CANDIDATES.forEach { candidate ->
                SelectableChip(
                    text = candidate,
                    selected = platform == candidate,
                    onClick = { platform = candidate }
                )
            }
        }

        FieldLabel("物品状态")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.x1),
            verticalArrangement = Arrangement.spacedBy(Space.x1)
        ) {
            Item.STATUSES.forEach { option ->
                SelectableChip(
                    text = option,
                    selected = status == option,
                    onClick = {
                        if (option in Item.TERMINAL_STATUSES) {
                            // 终止状态：先弹玻璃卡填写截止信息，确认后才生效
                            pendingEndStatus = option
                        } else {
                            status = option
                            endDate = ""
                            salePrice = ""
                        }
                    }
                )
            }
        }

        FieldLabel("物品类别")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.x1),
            verticalArrangement = Arrangement.spacedBy(Space.x1)
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
            SelectableChip(
                text = "－ 删除类别",
                selected = false,
                onClick = { showRemoveCategoryDialog = true }
            )
        }

        FieldLabel("备注")
        RoundedTextField(
            value = note,
            onValueChange = { if (it.length <= Item.NOTE_MAX) note = it },
            placeholder = "选填，最长 50 字符",
            singleLine = false,
            minLines = 2,
            maxLines = 3,
            supportingText = "${note.length}/${Item.NOTE_MAX}"
        )

        Spacer(modifier = Modifier.height(Space.x1))
        if (errorMessage.isNotBlank()) {
            Text(text = errorMessage, color = SaveRed, fontSize = 13.sp)
        }
        Button(
            onClick = { submit() },
            colors = ButtonDefaults.buttonColors(
                containerColor = SaveRed,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
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
        Spacer(modifier = Modifier.height(120.dp))
    }

    pendingEndStatus?.let { pending ->
        EndDateDialog(
            status = pending,
            backdrop = backdrop,
            onDismiss = { pendingEndStatus = null },
            onConfirm = { end, sale ->
                status = pending
                endDate = end.toString()
                salePrice = if (sale > 0) String.format(Locale.CHINA, "%.2f", sale) else ""
                pendingEndStatus = null
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now()),
            onDismiss = { showDatePicker = false },
            onConfirm = { date = it.toString(); showDatePicker = false },
            backdrop = backdrop
        )
    }

    if (showRemoveCategoryDialog) {
        // 玻璃覆盖层（与日期弹窗同模式）：遮罩 + 玻璃卡 + 弹簧弹入 + 液态手势。
        // 遮罩在缩放动画之外原地淡入，卡片单独弹入（避免暗幕矩形边缘扫屏的灰影）
        val dialogDark = LocalAppDark.current
        val dialogSurfaceColor = if (dialogDark) Color(0xFF15151C) else Color(0xFFFDFDFF)
        val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
        val listScrollState = rememberScrollState()
        val gestureScope = rememberCoroutineScope()
        val gesture = remember { LiquidGestureState(gestureScope) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (dialogDark) 0.46f else 0.28f))
                .clickable(onClick = { showRemoveCategoryDialog = false }),
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
                        .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.72f).dp)
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
                            layerBlock = gesture.layerBlock,
                            onDrawSurface = { drawRect(dialogSurfaceColor.copy(alpha = 0.42f)) }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .liquidGlassGestures(gesture, listScrollState)
                        .then(gesture.highlightModifier)
                        .padding(Space.x2)
                ) {
                    OutlinedText(
                        text = "删除类别",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (categories.isEmpty()) {
                        Text(
                            "没有可删除的类别",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "已删除的类别可随时重新添加；历史物品仍会被排序与筛选覆盖。",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // 类别列表：竖向可滚动；液态手势竖向过阈值让渡列表滚动（与排序卡一致）
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(listScrollState)
                    ) {
                        categories.forEach { name ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, fontSize = 14.sp)
                                TextButton(onClick = {
                                    if (onRemoveCategory(name) && category == name) {
                                        // 删掉的若是当前选中类别，回落到剩余第一个
                                        category = categories.firstOrNull { it != name } ?: ""
                                    }
                                }) { Text("删除", color = SaveRed, fontSize = 13.sp) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showRemoveCategoryDialog = false }) {
                            OutlinedText(
                                text = "完成",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        // 玻璃覆盖层（与删除类别弹窗同模式）；输入框保持实色保证可读（既有规则）
        var newCategory by remember { mutableStateOf("") }
        var categoryError by remember { mutableStateOf("") }
        val dialogDark = LocalAppDark.current
        val dialogSurfaceColor = if (dialogDark) Color(0xFF15151C) else Color(0xFFFDFDFF)
        val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
        val gestureScope = rememberCoroutineScope()
        val gesture = remember { LiquidGestureState(gestureScope) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (dialogDark) 0.46f else 0.28f))
                .clickable(onClick = { showCategoryDialog = false }),
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
                            layerBlock = gesture.layerBlock,
                            onDrawSurface = { drawRect(dialogSurfaceColor.copy(alpha = 0.42f)) }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .liquidGlassGestures(gesture)
                        .then(gesture.highlightModifier)
                        .padding(Space.x2)
                ) {
                    OutlinedText(
                        text = "添加自定义类别",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RoundedTextField(
                        value = newCategory,
                        onValueChange = { if (it.length <= 12) newCategory = it; categoryError = "" },
                        placeholder = "如：图书、运动装备"
                    )
                    if (categoryError.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(categoryError, color = SaveRed, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showCategoryDialog = false }) {
                            OutlinedText(
                                text = "取消",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (onAddCategory(newCategory)) {
                                category = newCategory.trim()
                                showCategoryDialog = false
                            } else {
                                categoryError = "类别不能为空、重复或超过 12 字"
                            }
                        }) {
                            OutlinedText(
                                text = "添加",
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
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = Space.x1)
    )
}
