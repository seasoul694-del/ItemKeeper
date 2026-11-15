package com.tang.itemkeeper.ui.theme

import androidx.compose.ui.graphics.Color
import com.tang.itemkeeper.model.Item

data class ChipStyle(val background: Color, val foreground: Color)

/** 七种物品状态各有独立色相；浅色 / 暗色各一套，保证文字对比度 */
private val LightStatusStyles = mapOf(
    "在用" to ChipStyle(Color(0xFFE8F5E9), Color(0xFF1B5E20)),
    "闲置" to ChipStyle(Color(0xFFE3F2FD), Color(0xFF0D47A1)),
    "送人" to ChipStyle(Color(0xFFF3E5F5), Color(0xFF6A1B9A)),
    "用完" to ChipStyle(Color(0xFFFFF3E0), Color(0xFFE65100)),
    "报废" to ChipStyle(Color(0xFFECEFF1), Color(0xFF455A64)),
    "售出" to ChipStyle(Color(0xFFE0F7FA), Color(0xFF00695C)),
    "丢失" to ChipStyle(Color(0xFFFFEBEE), Color(0xFFB71C1C))
)

private val DarkStatusStyles = mapOf(
    "在用" to ChipStyle(Color(0xFF1B5E20), Color(0xFFC8E6C9)),
    "闲置" to ChipStyle(Color(0xFF0D47A1), Color(0xFFBBDEFB)),
    "送人" to ChipStyle(Color(0xFF6A1B9A), Color(0xFFE1BEE7)),
    "用完" to ChipStyle(Color(0xFF7A3B00), Color(0xFFFFE0B2)),
    "报废" to ChipStyle(Color(0xFF37474F), Color(0xFFCFD8DC)),
    "售出" to ChipStyle(Color(0xFF00695C), Color(0xFFB2DFDB)),
    "丢失" to ChipStyle(Color(0xFF8E1616), Color(0xFFFFCDD2))
)

private val FallbackLight = ChipStyle(Color(0xFFEEEEEE), Color(0xFF424242))
private val FallbackDark = ChipStyle(Color(0xFF374151), Color(0xFFE5E7EB))

fun statusStyle(status: String, dark: Boolean): ChipStyle {
    val pool = if (dark) DarkStatusStyles else LightStatusStyles
    return pool[status] ?: if (dark) FallbackDark else FallbackLight
}

/** 价格与日均价格统一使用橙色 */
fun priceColor(dark: Boolean): Color = if (dark) Color(0xFFFFB74D) else Color(0xFFE65100)

/** 保存按钮固定红色 */
val SaveRed = Color(0xFFD32F2F)

val ALL_STATUSES: List<String> = Item.STATUSES
