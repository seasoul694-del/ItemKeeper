package com.tang.itemkeeper.model

import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * 一件已购买的物品。所有字段都是展示或筛选所需的最小集合。
 * purchaseDate 统一用 yyyy-MM-dd 字符串存储，避免引入序列化反射库。
 */
data class Item(
    val id: String,
    val name: String,
    val price: Double,
    val purchaseDate: String,
    val platform: String,
    val status: String,
    val category: String,
    val note: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("price", price)
        put("purchaseDate", purchaseDate)
        put("platform", platform)
        put("status", status)
        put("category", category)
        put("note", note)
    }

    /** 持有天数：购买当天算 1 天；日期异常或未来日期同样按 1 天，保证日均价格不除零、不出现负数。 */
    fun heldDays(today: LocalDate = LocalDate.now()): Long {
        val date = runCatching { LocalDate.parse(purchaseDate) }.getOrNull() ?: return 1L
        return max(1L, ChronoUnit.DAYS.between(date, today) + 1)
    }

    /** 日均价格 = 价格 / 持有天数 */
    fun dailyPrice(today: LocalDate = LocalDate.now()): Double = price / heldDays(today)

    companion object {
        fun fromJson(json: JSONObject): Item = Item(
            id = json.optString("id"),
            name = json.optString("name"),
            price = json.optDouble("price", 0.0),
            purchaseDate = json.optString("purchaseDate", "2000-01-01"),
            platform = json.optString("platform", ""),
            status = json.optString("status", "在用"),
            category = json.optString("category", "数码"),
            note = json.optString("note", "")
        )

        val STATUSES = listOf("在用", "闲置", "送人", "用完", "报废", "售出", "丢失")

        val DEFAULT_CATEGORIES = listOf("数码", "衣服", "月度plan", "衣物")

        val PLATFORM_CANDIDATES = listOf(
            "淘宝", "京东", "拼多多", "抖音", "闲鱼", "转转", "得物", "唯品会", "实体店"
        )

        const val NAME_MAX = 30
        const val NOTE_MAX = 50
        const val MIN_YEAR = 2000
        const val MAX_YEAR = 2050
    }
}
