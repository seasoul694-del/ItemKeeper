package com.tang.itemkeeper.util

import com.tang.itemkeeper.data.SortMode
import com.tang.itemkeeper.model.Item

/**
 * 首页排序（2026-11-13 用户定稿的生命周期规则）：
 * 一级键 = 是否已终止使用（在用/闲置/送人 在前；用完/报废/售出/丢失 永远沉底），
 * 二级键 = 用户选择的排序方式 —— 终止物品内部同样受排序影响，只是不干扰正常使用物品。
 * 归组排序（类别/状态）保持稳定：先按购买日期从近到远排一遍，再按组键稳定归组，
 * 得到「同组靠一块、组内按日期新→旧」的效果。
 */
fun sortItems(
    items: List<Item>,
    mode: SortMode,
    categories: List<String>
): List<Item> {
    fun level(item: Item): Int = if (item.isEnded) 1 else 0

    val base: List<Item> = when (mode) {
        SortMode.DATE_DESC -> items.sortedWith(
            compareBy<Item> { level(it) }.thenByDescending { it.purchaseDate }
        )
        SortMode.DATE_ASC -> items.sortedWith(
            compareBy<Item> { level(it) }.thenBy { it.purchaseDate }
        )
        SortMode.PRICE_DESC -> items.sortedWith(
            compareBy<Item> { level(it) }.thenByDescending { it.price }
        )
        SortMode.PRICE_ASC -> items.sortedWith(
            compareBy<Item> { level(it) }.thenBy { it.price }
        )
        SortMode.DAILY_DESC -> items.sortedWith(
            compareBy<Item> { level(it) }.thenByDescending { it.dailyPrice() }
        )
        SortMode.DAILY_ASC -> items.sortedWith(
            compareBy<Item> { level(it) }.thenBy { it.dailyPrice() }
        )
        SortMode.CATEGORY -> items
            .sortedByDescending { it.purchaseDate }
            .sortedWith(
                compareBy<Item> { level(it) }.thenBy { categoryOrder(it.category, categories) }
            )
        SortMode.STATUS -> items
            .sortedByDescending { it.purchaseDate }
            .sortedWith(
                compareBy<Item> { level(it) }
                    .thenBy { Item.STATUSES.indexOf(it.status).coerceAtLeast(0) }
            )
    }
    return base
}

private fun categoryOrder(category: String, categories: List<String>): Int {
    val index = categories.indexOf(category)
    return if (index >= 0) index else Int.MAX_VALUE
}
