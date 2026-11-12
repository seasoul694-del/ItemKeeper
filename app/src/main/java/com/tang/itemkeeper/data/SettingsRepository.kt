package com.tang.itemkeeper.data

import android.content.Context
import com.tang.itemkeeper.model.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** 设置持久化：只有主题模式与自定义类别，纯本地、无鉴权逻辑。 */
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

    // ---------- 类别 ----------
    private val _customCategories = MutableStateFlow(readCustomCategories())
    private val _allCategories = MutableStateFlow(Item.DEFAULT_CATEGORIES + _customCategories.value)
    val allCategories: StateFlow<List<String>> = _allCategories

    fun addCustomCategory(raw: String): Boolean {
        val name = raw.trim()
        if (name.isBlank() || name.length > 12 || _allCategories.value.contains(name)) return false
        val updated = _customCategories.value + name
        _customCategories.value = updated
        prefs.edit().putString("custom_categories", JSONArray(updated).toString()).apply()
        _allCategories.value = Item.DEFAULT_CATEGORIES + updated
        return true
    }

    private fun readCustomCategories(): List<String> = runCatching {
        val array = JSONArray(prefs.getString("custom_categories", "[]") ?: "[]")
        (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }
    }.getOrDefault(emptyList())
}
