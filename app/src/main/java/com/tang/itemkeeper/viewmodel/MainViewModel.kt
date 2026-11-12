package com.tang.itemkeeper.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tang.itemkeeper.data.ItemRepository
import com.tang.itemkeeper.data.SettingsRepository
import com.tang.itemkeeper.model.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 底栏索引：0 首页 / 1 归总(气泡) / 2 添加 / 3 归类 / 4 界面 */
object Tabs {
    const val HOME = 0
    const val SUMMARY = 1
    const val ADD = 2
    const val FILTER = 3
    const val SETTINGS = 4
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ItemRepository(application)
    val settings = SettingsRepository(application)

    val items: StateFlow<List<Item>> = repository.items
    val categories: StateFlow<List<String>> = settings.allCategories

    val currentTab = MutableStateFlow(Tabs.HOME)
    val previousTab = MutableStateFlow(Tabs.HOME)
    val summaryBubble = MutableStateFlow(false)

    val editingItem = MutableStateFlow<Item?>(null)
    val homeListExpanded = MutableStateFlow(false)
    val expandedItemId = MutableStateFlow<String?>(null)

    val searchQuery = MutableStateFlow("")
    val searchExpanded = MutableStateFlow(false)

    /** 首页滚动归一化进度 0..1，直接驱动顶栏形变，不另起动画 */
    val homeScrollProgress = MutableStateFlow(0f)

    fun selectTab(index: Int) {
        if (index == Tabs.SUMMARY) {
            if (!summaryBubble.value) {
                previousTab.value = currentTab.value
                summaryBubble.value = true
                currentTab.value = Tabs.SUMMARY
            }
            return
        }
        if (index == Tabs.ADD && currentTab.value != Tabs.ADD) editingItem.value = null
        summaryBubble.value = false
        currentTab.value = index
        if (index != Tabs.HOME) searchExpanded.value = false
    }

    fun closeBubble() {
        if (summaryBubble.value) {
            summaryBubble.value = false
            currentTab.value = previousTab.value
        }
    }

    fun setHomeScrollProgress(value: Float) {
        homeScrollProgress.value = value.coerceIn(0f, 1f)
    }

    fun startEdit(item: Item) {
        editingItem.value = item
        summaryBubble.value = false
        currentTab.value = Tabs.ADD
    }

    fun cancelEdit() {
        editingItem.value = null
        currentTab.value = Tabs.HOME
    }

    fun saveItem(item: Item) {
        if (editingItem.value == null) repository.add(item) else repository.update(item)
        editingItem.value = null
        homeListExpanded.value = true
        expandedItemId.value = null
        searchExpanded.value = false
        searchQuery.value = ""
        currentTab.value = Tabs.HOME
    }

    fun deleteItem(id: String) {
        repository.delete(id)
        if (editingItem.value?.id == id) editingItem.value = null
        if (expandedItemId.value == id) expandedItemId.value = null
    }

    fun addCategory(name: String): Boolean = settings.addCustomCategory(name)

    fun matchesSearch(item: Item, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return item.name.contains(q, ignoreCase = true) ||
            item.platform.contains(q, ignoreCase = true) ||
            item.category.contains(q, ignoreCase = true) ||
            item.status.contains(q, ignoreCase = true) ||
            item.note.contains(q, ignoreCase = true) ||
            item.purchaseDate.contains(q, ignoreCase = true)
    }
}
