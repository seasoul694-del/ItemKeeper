package com.tang.itemkeeper.data

import android.content.Context
import com.tang.itemkeeper.model.Item
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File

/**
 * 本地 JSON 文件持久化。个人物品数量有限、筛选全在内存完成，
 * 因此不引入 Room / 注解处理器，构建更轻、更不易出错。
 * StateFlow 保证「添加 -> 首页 / 归类 / 统计」实时联动刷新。
 */
class ItemRepository(context: Context) {

    private val file = File(context.applicationContext.filesDir, "items.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _items = MutableStateFlow(readFromDisk())
    val items: StateFlow<List<Item>> = _items

    private fun readFromDisk(): List<Item> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            (0 until array.length()).map { Item.fromJson(array.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    /** 用快照写入 IO 线程，避免并发修改导致内容错乱 */
    private fun persist(snapshot: List<Item>) {
        scope.launch {
            runCatching {
                val array = JSONArray()
                snapshot.forEach { array.put(it.toJson()) }
                file.writeText(array.toString())
            }
        }
    }

    fun add(item: Item) {
        _items.value = _items.value + item
        persist(_items.value)
    }

    fun update(item: Item) {
        _items.value = _items.value.map { if (it.id == item.id) item else it }
        persist(_items.value)
    }

    fun delete(id: String) {
        _items.value = _items.value.filterNot { it.id == id }
        persist(_items.value)
    }
}
