package com.tang.itemkeeper.data

import android.content.Context
import com.tang.itemkeeper.model.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** 首页排序方式（用户在排序卡片里选择，持久化保存） */
enum class SortMode {
    DATE_DESC,   // 购买日期 · 从近到远（默认）
    DATE_ASC,    // 购买日期 · 从远到近
    PRICE_DESC,  // 价格 · 从高到低
    PRICE_ASC,   // 价格 · 从低到高
    DAILY_DESC,  // 日均价 · 从高到低
    DAILY_ASC,   // 日均价 · 从低到高
    CATEGORY,    // 按类别归组
    STATUS       // 按状态归组
}

/** 设置持久化：主题模式、自定义类别、首页排序方式，纯本地、无鉴权逻辑。 */
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("itemkeeper_settings", Context.MODE_PRIVATE)

    // ---------- 主题 ----------
    private val _themeMode = MutableStateFlow(
        runCatching {
            ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    // ---------- 首页排序 ----------
    private val _sortMode = MutableStateFlow(
        runCatching {
            SortMode.valueOf(prefs.getString("sort_mode", SortMode.DATE_DESC.name) ?: SortMode.DATE_DESC.name)
        }.getOrDefault(SortMode.DATE_DESC)
    )
    val sortMode: StateFlow<SortMode> = _sortMode

    fun setSortMode(mode: SortMode) {
        prefs.edit().putString("sort_mode", mode.name).apply()
        _sortMode.value = mode
    }

    // ---------- 全局背景 ----------
    // 存 filesDir/background.jpg 的文件名；空串 = 未设置（使用主题底色）
    private val _backgroundImage = MutableStateFlow(prefs.getString("background_image", "") ?: "")
    val backgroundImage: StateFlow<String> = _backgroundImage

    fun setBackgroundImage(fileName: String) {
        prefs.edit().putString("background_image", fileName).apply()
        _backgroundImage.value = fileName
    }

    // ---------- 类别 ----------
    private val _customCategories = MutableStateFlow(readCustomCategories())
    // 已删除的预设类别（持久化：不记下来的话重启后预设会自动回来）
    private val _removedDefaults = MutableStateFlow(readRemovedDefaults())

    private fun computeAllCategories(): List<String> =
        Item.DEFAULT_CATEGORIES.filterNot { it in _removedDefaults.value } + _customCategories.value

    private val _allCategories = MutableStateFlow(computeAllCategories())
    val allCategories: StateFlow<List<String>> = _allCategories

    fun addCustomCategory(raw: String): Boolean {
        val name = raw.trim()
        if (name.isBlank() || name.length > 12 || _allCategories.value.contains(name)) return false
        val updated = _customCategories.value + name
        _customCategories.value = updated
        prefs.edit().putString("custom_categories", JSONArray(updated.toList()).toString()).apply()
        _allCategories.value = computeAllCategories()
        return true
    }

    /**
     * 删除类别：预设类别记入「已删除」持久化集合（可随时重加回），
     * 自定义类别直接移除。VM 层还有物品实际引用兜底，删除后历史物品仍可被排序/筛选。
     */
    fun removeCategory(name: String): Boolean {
        if (name.isBlank()) return false
        if (name in Item.DEFAULT_CATEGORIES) {
            if (name in _removedDefaults.value) return false
            val updated = _removedDefaults.value + name
            _removedDefaults.value = updated
            prefs.edit().putString("removed_default_categories", JSONArray(updated.toList()).toString()).apply()
        } else if (name in _customCategories.value) {
            val updated = _customCategories.value.filterNot { it == name }
            _customCategories.value = updated
            prefs.edit().putString("custom_categories", JSONArray(updated.toList()).toString()).apply()
        } else {
            return false
        }
        _allCategories.value = computeAllCategories()
        return true
    }

    private fun readCustomCategories(): List<String> = runCatching {
        val array = JSONArray(prefs.getString("custom_categories", "[]") ?: "[]")
        (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }
    }.getOrDefault(emptyList())

    private fun readRemovedDefaults(): Set<String> = runCatching {
        val array = JSONArray(prefs.getString("removed_default_categories", "[]") ?: "[]")
        (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }.toSet()
    }.getOrDefault(emptySet())
}
