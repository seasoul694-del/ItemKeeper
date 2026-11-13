package com.tang.itemkeeper.model

import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * 一件已购买的物品。所有字段都是展示或筛选所需的最小集合。
 * purchaseDate 统一用 yyyy-MM-dd 字符串存储，避免引入序列化反射库。
 * image 存 filesDir/images/ 下的文件名；空串 = 未上传，显示名称首字占位。
 *
 * 生命周期（2026-11-13 用户定稿）：
 *  - 「在用 / 闲置 / 送人」= 使用中，日均价格持续计算（购买日 → 今天）
 *  - 「用完 / 报废 / 售出 / 丢失」= 已终止，用户在弹窗里填写截止日期后
 *    日均冻结为「购买日 → 截止日」的均值，此后不再增长
 *  - 「售出」额外记录售出价格：净成本 = 原价 − 售出价，日均按净成本计算
 */
data class Item(
    val id: String,
    val name: String,
    val price: Double,
    val purchaseDate: String,
    val platform: String,
    val status: String,
    val category: String,
    val note: String = "",
    val image: String = "",
    val endDate: String = "",      // 终止日期（用完/报废/售出/丢失），yyyy-MM-dd；空 = 未终止
    val salePrice: Double = 0.0    // 售出价格（仅「售出」状态使用）
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
        put("image", image)
        put("endDate", endDate)
        put("salePrice", salePrice)
    }

    /** 是否已终止使用（日均冻结、首页排序沉底、不计入归总日均总和） */
    val isEnded: Boolean
        get() = status in TERMINAL_STATUSES

    /** 持有天数：购买当天算 1 天；已终止物品按「购买日 → 截止日」冻结；日期异常兜底 1 天。 */
    fun heldDays(today: LocalDate = LocalDate.now()): Long {
        val start = runCatching { LocalDate.parse(purchaseDate) }.getOrNull() ?: return 1L
        val end = if (isEnded) {
            runCatching { LocalDate.parse(endDate) }.getOrNull() ?: today
        } else {
            today
        }
        // 截止日不会晚于今天（未来日期兜底），保证天数不倒扣
        val effectiveEnd = if (end.isAfter(today)) today else end
        return max(1L, ChronoUnit.DAYS.between(start, effectiveEnd) + 1)
    }

    /** 日均价格：使用中 = 价格 / 持有天数；售出 = 净成本（原价 − 售出价）/ 持有天数；其余终止状态冻结。 */
    fun dailyPrice(today: LocalDate = LocalDate.now()): Double {
        val cost = if (status == "售出" && salePrice > 0.0) price - salePrice else price
        return cost / heldDays(today)
    }

    companion object {
        fun fromJson(json: JSONObject): Item = Item(
            id = json.optString("id"),
            name = json.optString("name"),
            price = json.optDouble("price", 0.0),
            purchaseDate = json.optString("purchaseDate", "2000-01-01"),
            platform = json.optString("platform", ""),
            status = json.optString("status", "在用"),
            category = json.optString("category", "数码"),
            note = json.optString("note", ""),
            image = json.optString("image", ""),
            endDate = json.optString("endDate", ""),
            salePrice = json.optDouble("salePrice", 0.0)
        )

        val STATUSES = listOf("在用", "闲置", "送人", "用完", "报废", "售出", "丢失")

        /** 已终止使用的状态：日均冻结、需填写截止日期 */
        val TERMINAL_STATUSES = listOf("用完", "报废", "售出", "丢失")

        /** 终止状态的截止日期字段标题 */
        fun endLabel(status: String): String = when (status) {
            "用完" -> "用完时间"
            "报废" -> "报废时间"
            "丢失" -> "丢失时间"
            "售出" -> "售出时间"
            else -> "截止时间"
        }

        // 2026-11-13 用户定稿：删除「衣服」，「衣物」提前到第二位
        val DEFAULT_CATEGORIES = listOf("数码", "衣物", "月度plan")

        val PLATFORM_CANDIDATES = listOf(
            "淘宝", "京东", "拼多多", "抖音", "闲鱼", "转转", "得物", "唯品会", "实体店"
        )

        const val NAME_MAX = 30
        const val NOTE_MAX = 50
        const val MIN_YEAR = 2000
        const val MAX_YEAR = 2050
    }
}
