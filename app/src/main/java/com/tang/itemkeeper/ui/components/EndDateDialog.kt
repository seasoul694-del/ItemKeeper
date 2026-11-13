package com.tang.itemkeeper.ui.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import com.tang.itemkeeper.model.Item
import com.tang.itemkeeper.ui.theme.LocalAppDark
import com.tang.itemkeeper.ui.theme.Motion
import com.tang.itemkeeper.ui.theme.Space
import java.time.LocalDate
import java.time.YearMonth

/**
 * 终止信息玻璃卡：物品状态改为「用完 / 报废 / 售出 / 丢失」时弹出，
 * 填写截止时间（「售出」还需填写售出价格）。材质与排序卡/日期卡一致
 * （透明折射全局背景 + 一点模糊），应用内覆盖层而非系统 Dialog。
 */
@Composable
fun EndDateDialog(
    status: String,
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, Double) -> Unit
) {
    var year by remember { mutableStateOf(LocalDate.now().year.coerceIn(Item.MIN_YEAR, Item.MAX_YEAR)) }
    var month by remember { mutableStateOf(LocalDate.now().monthValue) }
    var day by remember { mutableStateOf(LocalDate.now().dayOfMonth) }
    var priceText by remember { mutableStateOf("") }

    val maxDay = YearMonth.of(year, month).lengthOfMonth()
    val safeDay = day.coerceIn(1, maxDay)
    val isSale = status == "售出"

    val dark = LocalAppDark.current
    val surfaceColor = if (dark) Color(0xFF15151C) else Color(0xFFFDFDFF)

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
                    text = Item.endLabel(status),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "「$status」的物品从购买日到该日期计算日均，之后冻结。",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    key(maxDay) {
                        WheelColumn(
                            values = (1..maxDay).map { "${it}日" },
                            initialIndex = safeDay - 1,
                            onIndexChange = { day = it + 1 },
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }
                if (isSale) {
                    Spacer(modifier = Modifier.height(8.dp))
                    RoundedTextField(
                        value = priceText,
                        onValueChange = { if (Regex("^\\d{0,9}(\\.\\d{0,2})?$").matches(it)) priceText = it },
                        placeholder = "售出价格（元），未售出可留空",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        )
                    )
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
                    Button(onClick = {
                        val date = LocalDate.of(year, month, safeDay)
                        val salePrice = if (isSale) priceText.toDoubleOrNull() ?: 0.0 else 0.0
                        onConfirm(date, salePrice)
                    }) {
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
