package com.tang.itemkeeper.util

import com.tang.itemkeeper.data.SortMode
import com.tang.itemkeeper.model.Item

/**
 * 首页排序。归组排序（类别/状态）保持稳定：
 * 先按购买日期从近到远排一遍，再按组键稳定归组，
 * 得到「同组靠一块、组内按日期新→旧」的效果。
 * 组间顺序：状态用固定顺序表（在用/闲置/…），类别用类别列表的出现顺序。
 */
fun sortItems(
    items: List<Item>,
    mode: SortMode,
    categories: List<String>
): List<Item> = when (mode) {
    SortMode.DATE_DESC -> items.sortedByDescending { it.purchaseDate }
    SortMode.DATE_ASC -> items.sortedBy { it.purchaseDate }
    SortMode.PRICE_DESC -> items.sortedByDescending { it.price }
    SortMode.PRICE_ASC -> items.sortedBy { it.price }
    SortMode.DAILY_DESC -> items.sortedByDescending { it.dailyPrice() }
    SortMode.DAILY_ASC -> items.sortedBy { it.dailyPrice() }
    SortMode.CATEGORY -> items
        .sortedByDescending { it.purchaseDate }
        .sortedBy { categoryOrder(it.category, categories) }
    SortMode.STATUS -> items
        .sortedByDescending { it.purchaseDate }
        .sortedBy { Item.STATUSES.indexOf(it.status).coerceAtLeast(0) }
}

private fun categoryOrder(category: String, categories: List<String>): Int {
    val index = categories.indexOf(category)
    return if (index >= 0) index else Int.MAX_VALUE
}
