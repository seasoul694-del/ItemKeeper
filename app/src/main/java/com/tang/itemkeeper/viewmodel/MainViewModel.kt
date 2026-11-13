package com.tang.itemkeeper.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tang.itemkeeper.data.ItemRepository
import com.tang.itemkeeper.data.SettingsRepository
import com.tang.itemkeeper.data.SortMode
import com.tang.itemkeeper.model.Item
import com.tang.itemkeeper.util.ItemImages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

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

    /**
     * 全局类别 = 预设/自定义 + 物品里实际出现过的类别。
     * 兜底：删除预设类别（如「衣服」）后，历史物品仍能被排序、归类、筛选覆盖。
     */
    val categories: StateFlow<List<String>> =
        combine(settings.allCategories, repository.items) { cats, items ->
            (cats + items.map { it.category }).distinct()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, settings.allCategories.value)

    val sortMode: StateFlow<SortMode> = settings.sortMode
    val backgroundImage: StateFlow<String> = settings.backgroundImage

    val currentTab = MutableStateFlow(Tabs.HOME)
    val previousTab = MutableStateFlow(Tabs.HOME)
    val summaryBubble = MutableStateFlow(false)
    val sortBubble = MutableStateFlow(false)

    val editingItem = MutableStateFlow<Item?>(null)
    val homeListExpanded = MutableStateFlow(false)
    val expandedItemId = MutableStateFlow<String?>(null)

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
    }

    fun closeBubble() {
        if (summaryBubble.value) {
            summaryBubble.value = false
            currentTab.value = previousTab.value
        }
    }

    /** 选择排序方式：写入持久化并关闭排序卡片 */
    fun setSortMode(mode: SortMode) {
        settings.setSortMode(mode)
        sortBubble.value = false
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
        currentTab.value = Tabs.HOME
    }

    fun deleteItem(id: String) {
        repository.delete(id)
        if (editingItem.value?.id == id) editingItem.value = null
        if (expandedItemId.value == id) expandedItemId.value = null
    }

    fun addCategory(name: String): Boolean = settings.addCustomCategory(name)

    /** 删除类别（预设/自定义均可），全局列表立即更新 */
    fun removeCategory(name: String): Boolean = settings.removeCategory(name)

    /**
     * 上传物品图片：压缩落盘后把文件名写进 Item。
     * 文件名带时间戳：重传时 StateFlow 值必然变化（触发 UI 重新加载），
     * 同时删除不再被引用的旧文件。
     */
    fun uploadItemImage(item: Item, uri: Uri): Boolean {
        val fileName = ItemImages.save(
            getApplication(),
            "${item.id}_${System.currentTimeMillis()}",
            uri
        ) ?: return false
        if (item.image.isNotBlank()) ItemImages.delete(getApplication(), item.image)
        val updated = item.copy(image = fileName)
        if (repository.items.value.any { it.id == item.id }) repository.update(updated)
        else repository.add(updated)
        return true
    }

    /** 上传全局背景图（长边 2048 保全屏清晰度）。时间戳命名 + 清理旧图，重传立即生效。 */
    fun uploadBackgroundImage(uri: Uri): Boolean {
        val fileName = ItemImages.save(
            getApplication(),
            "bg_${System.currentTimeMillis()}",
            uri,
            maxDimension = 2048
        ) ?: return false
        val old = settings.backgroundImage.value
        if (old.isNotBlank()) ItemImages.delete(getApplication(), old)
        settings.setBackgroundImage(fileName)
        return true
    }

    /** 恢复默认：清空背景图，回到主题底色 */
    fun resetBackgroundImage() {
        settings.setBackgroundImage("")
    }
}
