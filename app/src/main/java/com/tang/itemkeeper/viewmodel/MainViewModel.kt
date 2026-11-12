package com.tang.itemkeeper.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tang.itemkeeper.data.ItemRepository
import com.tang.itemkeeper.data.SettingsRepository
import com.tang.itemkeeper.model.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 单一状态源：四个页面都读这里，保证保存后首页 / 归类 / 统计实时刷新 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ItemRepository(application)
    val settings = SettingsRepository(application)

    val items: StateFlow<List<Item>> = repository.items
    val categories: StateFlow<List<String>> = settings.allCategories

    val currentTab = MutableStateFlow(0)
    val editingItem = MutableStateFlow<Item?>(null)

    // 首页两个板块的展开状态提升到 ViewModel：切页返回后保持，保存后可直接展开清单
    val homeListExpanded = MutableStateFlow(false)
    val homeStatsExpanded = MutableStateFlow(false)

    fun setTab(index: Int) {
        currentTab.value = index
    }

    fun clearEditing() {
        editingItem.value = null
    }

    fun startEdit(item: Item) {
        editingItem.value = item
        currentTab.value = 1
    }

    fun cancelEdit() {
        editingItem.value = null
        currentTab.value = 0
    }

    /** 新增或更新后自动回到首页并展开清单，用户立刻看到结果 */
    fun saveItem(item: Item) {
        if (editingItem.value == null) repository.add(item) else repository.update(item)
        editingItem.value = null
        homeListExpanded.value = true
        currentTab.value = 0
    }

    fun deleteItem(id: String) {
        repository.delete(id)
        if (editingItem.value?.id == id) editingItem.value = null
    }

    fun addCategory(name: String): Boolean = settings.addCustomCategory(name)
}
