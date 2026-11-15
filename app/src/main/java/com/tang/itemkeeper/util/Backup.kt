package com.tang.itemkeeper.util

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.tang.itemkeeper.data.SortMode
import com.tang.itemkeeper.data.ThemeMode
import com.tang.itemkeeper.model.Item
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * 数据备份：单个 zip 文件打包「物品数据 + 图片 + 类别增删 + 界面设置」，
 * 卸载重装后可一键导入恢复。全程走 SAF 文件选择器，零权限、不联网。
 *
 * 包结构：
 *   manifest.json —— 结构化数据（App 识别与恢复的唯一依据，type + version 防呆）
 *   images/ 目录 —— 物品图与全局背景图的原始字节（不重编码，无 base64 膨胀）
 *   README.md —— 人能直接阅读的备份说明（清单表 + 恢复指引）
 *
 * 安全设计（导入绝不破坏现有数据）：
 *  - 识别不看扩展名/文件名，只读包内 manifest.json 校验 type 与 version
 *  - parse-first-then-commit：本文件只负责解析与图片落盘（只增不删），
 *    全部成功后才由 ViewModel 提交替换仓库与设置
 *  - 图片条目缺失只降级（对应物品/背景回退无图），不阻断导入
 *  - 条目名白名单 + 文件名安全校验，防 zip-slip
 */
object Backup {

    private const val MANIFEST_NAME = "manifest.json"
    private const val README_NAME = "README.md"
    private const val IMAGES_DIR = "images/"
    private const val BACKUP_TYPE = "itemkeeper-backup"
    private const val BACKUP_VERSION = 1

    /** 设置快照（与 SettingsRepository 的 5 个持久化 key 一一对应） */
    data class SettingsSnapshot(
        val themeMode: ThemeMode,
        val sortMode: SortMode,
        val backgroundImage: String,
        val customCategories: List<String>,
        val removedDefaultCategories: Set<String>
    )

    sealed class ImportResult {
        data class Success(val items: List<Item>, val settings: SettingsSnapshot) : ImportResult()
        data class Failure(val reason: String) : ImportResult()
    }

    // ---------- 导出 ----------

    fun export(context: Context, uri: Uri, items: List<Item>, settings: SettingsSnapshot): Boolean =
        runCatching {
            val appContext = context.applicationContext
            val output = appContext.contentResolver.openOutputStream(uri) ?: return false
            ZipOutputStream(output.buffered()).use { zip ->
                // 1. manifest.json：App 恢复的唯一数据源
                zip.putNextEntry(ZipEntry(MANIFEST_NAME))
                zip.write(buildManifest(appContext, items, settings).toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 2. images/：只打包被引用的图（物品图 + 背景图），磁盘上已丢失的跳过
                val imageDir = ItemImages.dir(appContext)
                referencedImages(items, settings.backgroundImage).forEach { fileName ->
                    val file = File(imageDir, fileName)
                    if (file.exists()) {
                        zip.putNextEntry(ZipEntry(IMAGES_DIR + fileName))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }

                // 3. README.md：人能直接看的备份说明
                zip.putNextEntry(ZipEntry(README_NAME))
                zip.write(buildReadme(items, settings).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            true
        }.getOrDefault(false)

    private fun referencedImages(items: List<Item>, background: String): List<String> =
        buildList {
            items.forEach { if (it.image.isNotBlank()) add(it.image) }
            if (background.isNotBlank()) add(background)
        }.filter { isSafeFileName(it) }.distinct()

    private fun buildManifest(context: Context, items: List<Item>, settings: SettingsSnapshot): JSONObject =
        JSONObject().apply {
            put("type", BACKUP_TYPE)
            put("version", BACKUP_VERSION)
            put("exportTime", exportTime())
            put("appVersion", appVersion(context))
            put("items", JSONArray().apply { items.forEach { put(it.toJson()) } })
            put("settings", JSONObject().apply {
                put("themeMode", settings.themeMode.name)
                put("sortMode", settings.sortMode.name)
                put("backgroundImage", settings.backgroundImage)
                put("customCategories", JSONArray(settings.customCategories))
                put("removedDefaultCategories", JSONArray(settings.removedDefaultCategories.toList()))
            })
        }

    private fun buildReadme(items: List<Item>, settings: SettingsSnapshot): String {
        val sb = StringBuilder()
        val sorted = items.sortedWith(compareBy({ it.category }, { it.name }))
        val inUse = items.count { !it.isEnded }
        val dailySum = items.filter { !it.isEnded }.sumOf { it.dailyPrice() }

        sb.appendLine("# 物志 ItemKeeper 备份说明")
        sb.appendLine()
        sb.appendLine("> 本文件由 App 自动生成，供人阅读。恢复数据请打开 App：")
        sb.appendLine("> 「界面 → 数据备份 → 导入备份」选择本压缩包，")
        sb.appendLine("> App 会读取其中的 manifest.json 与 images/ 自动恢复全部内容。")
        sb.appendLine()
        sb.appendLine("## 概览")
        sb.appendLine()
        sb.appendLine("- 导出时间：${exportTime()}")
        sb.appendLine("- 物品总数：${items.size} 件（使用中 $inUse 件，已终止 ${items.size - inUse} 件）")
        sb.appendLine("- 物品总价值：¥${money(items.sumOf { it.price })}")
        sb.appendLine("- 使用中物品日均价总和：¥${money(dailySum)}（与 App 归总口径一致，仅计使用中物品）")
        if (settings.backgroundImage.isNotBlank()) {
            sb.appendLine("- 已设置全局背景图：images/${settings.backgroundImage}")
        }
        sb.appendLine()
        sb.appendLine("## 物品清单")
        sb.appendLine()
        if (sorted.isEmpty()) {
            sb.appendLine("（备份时物品清单为空）")
        } else {
            sb.appendLine("| 名称 | 类别 | 状态 | 价格 | 购买日期 | 日均价 |")
            sb.appendLine("|---|---|---|---|---|---|")
            sorted.forEach { item ->
                sb.appendLine(
                    "| ${mdCell(item.name)} | ${mdCell(item.category)} | ${mdCell(item.status)} " +
                        "| ¥${money(item.price)} | ${item.purchaseDate} | ¥${money(item.dailyPrice())}"
                )
            }
        }
        sb.appendLine()
        sb.appendLine("## 类别设置")
        sb.appendLine()
        sb.appendLine(
            "- 自定义类别：" + if (settings.customCategories.isEmpty()) "（无）"
            else settings.customCategories.joinToString("、")
        )
        sb.appendLine(
            "- 已删除的预设类别：" + if (settings.removedDefaultCategories.isEmpty()) "（无）"
            else settings.removedDefaultCategories.sorted().joinToString("、")
        )
        sb.appendLine()
        sb.appendLine("## 主题与排序")
        sb.appendLine()
        sb.appendLine("- App 风格：${themeLabel(settings.themeMode)}")
        sb.appendLine("- 首页排序：${sortLabel(settings.sortMode)}")
        return sb.toString()
    }

    // ---------- 导入 ----------

    /**
     * 解析备份文件。只读 zip + 向 images/ 写入新文件（只增不删），
     * 任何失败都在触碰仓库/设置之前返回 Failure，现有数据分毫不动。
     */
    fun import(context: Context, uri: Uri): ImportResult {
        val appContext = context.applicationContext
        val tempFile = File(appContext.cacheDir, "itemkeeper_import_${System.currentTimeMillis()}.zip")
        try {
            // SAF 流先落缓存临时文件：ZipFile 需要随机访问，也避免单遍流式读取的内存压力
            val copied = runCatching {
                appContext.contentResolver.openInputStream(uri)?.use { source ->
                    tempFile.outputStream().use { source.copyTo(it) }
                    true
                } ?: false
            }.getOrDefault(false)
            if (!copied) return ImportResult.Failure("无法读取所选文件")

            return runCatching {
                ZipFile(tempFile).use { zf -> parseBackup(zf, ItemImages.dir(appContext)) }
            }.getOrNull() ?: ImportResult.Failure("不是有效的备份文件")
        } catch (e: Exception) {
            return ImportResult.Failure("导入失败：${e.message ?: "未知错误"}")
        } finally {
            tempFile.delete()
        }
    }

    private fun parseBackup(zf: ZipFile, imageDir: File): ImportResult {
        val manifestEntry = zf.getEntry(MANIFEST_NAME)
            ?: return ImportResult.Failure("不是有效的备份文件")
        val manifest = runCatching {
            JSONObject(zf.getInputStream(manifestEntry).readBytes().toString(Charsets.UTF_8))
        }.getOrNull() ?: return ImportResult.Failure("不是有效的备份文件")

        if (manifest.optString("type") != BACKUP_TYPE) return ImportResult.Failure("不是有效的备份文件")
        val version = manifest.optInt("version", 0)
        if (version <= 0) return ImportResult.Failure("不是有效的备份文件")
        if (version > BACKUP_VERSION) return ImportResult.Failure("备份版本过新，请先升级 App 再导入")

        val items = runCatching {
            val array = manifest.getJSONArray("items")
            (0 until array.length()).map { Item.fromJson(array.getJSONObject(it)) }
        }.getOrNull()?.distinctBy { it.id } ?: return ImportResult.Failure("备份数据损坏，无法解析物品")

        val settingsJson = manifest.optJSONObject("settings") ?: JSONObject()
        val settings = SettingsSnapshot(
            themeMode = runCatching {
                ThemeMode.valueOf(settingsJson.optString("themeMode", ThemeMode.SYSTEM.name))
            }.getOrDefault(ThemeMode.SYSTEM),
            sortMode = runCatching {
                SortMode.valueOf(settingsJson.optString("sortMode", SortMode.DATE_DESC.name))
            }.getOrDefault(SortMode.DATE_DESC),
            backgroundImage = safeImageName(settingsJson.optString("backgroundImage")) ?: "",
            customCategories = readStringArray(settingsJson, "customCategories"),
            removedDefaultCategories = readStringArray(settingsJson, "removedDefaultCategories").toSet()
        )

        // 恢复被引用的图片（物品图 + 背景图）。条目缺失只降级为无图，不阻断导入。
        val finalItems = items.map { item ->
            val image = safeImageName(item.image)
            if (image != null && extractImage(zf, imageDir, image)) item.copy(image = image)
            else item.copy(image = "")
        }
        val finalSettings = if (settings.backgroundImage.isNotBlank() &&
            extractImage(zf, imageDir, settings.backgroundImage)
        ) settings else settings.copy(backgroundImage = "")

        return ImportResult.Success(finalItems, finalSettings)
    }

    /** 从 zip 提取 images/<fileName> 落盘（覆盖写）。条目缺失或 IO 失败返回 false。 */
    private fun extractImage(zf: ZipFile, imageDir: File, fileName: String): Boolean {
        if (!isSafeFileName(fileName)) return false
        val entry = zf.getEntry(IMAGES_DIR + fileName) ?: return false
        return runCatching {
            zf.getInputStream(entry).use { stream ->
                File(imageDir, fileName).outputStream().use { stream.copyTo(it) }
            }
            true
        }.getOrDefault(false)
    }

    // ---------- 工具 ----------

    /** 只接受 images/ 下的纯文件名（无路径分隔符、无 ..、必须 .jpg 结尾），防 zip-slip */
    private fun isSafeFileName(name: String): Boolean =
        name.isNotBlank() && name.endsWith(".jpg") &&
            name != "." && name != ".." &&
            !name.contains('/') && !name.contains('\\') &&
            File(name).name == name

    private fun safeImageName(raw: String): String? {
        val name = raw.trim()
        return if (isSafeFileName(name)) name else null
    }

    private fun readStringArray(json: JSONObject, key: String): List<String> = runCatching {
        val array = json.getJSONArray(key)
        (0 until array.length()).map { array.optString(it).trim() }.filter { it.isNotBlank() }
    }.getOrDefault(emptyList())

    private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)

    /** Markdown 表格单元格转义：竖线与换行会破坏表格结构 */
    private fun mdCell(text: String): String =
        text.replace("|", "\\|").replace("\n", " ")

    private fun exportTime(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

    /** BuildConfig 在 AGP 9 默认关闭，版本号从 PackageManager 读 */
    private fun appVersion(context: Context): String = runCatching {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, 0)
        }
        info.versionName ?: ""
    }.getOrDefault("")

    private fun themeLabel(mode: ThemeMode): String = when (mode) {
        ThemeMode.SYSTEM -> "跟随系统"
        ThemeMode.LIGHT -> "浅色"
        ThemeMode.DARK -> "暗色"
    }

    private fun sortLabel(mode: SortMode): String = when (mode) {
        SortMode.DATE_DESC -> "购买日期 · 从近到远"
        SortMode.DATE_ASC -> "购买日期 · 从远到近"
        SortMode.PRICE_DESC -> "价格 · 从高到低"
        SortMode.PRICE_ASC -> "价格 · 从低到高"
        SortMode.DAILY_DESC -> "日均价 · 从高到低"
        SortMode.DAILY_ASC -> "日均价 · 从低到高"
        SortMode.CATEGORY -> "按类别归组"
        SortMode.STATUS -> "按状态归组"
    }
}
