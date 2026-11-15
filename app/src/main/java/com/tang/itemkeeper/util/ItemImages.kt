package com.tang.itemkeeper.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * 物品图片的本地存取：纯应用私有目录（filesDir/images/），无权限、无联网。
 * 上传时用 inSampleSize 下采样到长边约 1024px、JPEG 85% 落盘——
 * 72dp 图片框 3x 屏也只需 ~216px，1024px 给未来可能的放大预览留足余量，
 * 单张 <300KB，几十件物品也毫无存储压力。
 */
object ItemImages {

    private const val JPEG_QUALITY = 85

    fun dir(context: Context): File =
        File(context.applicationContext.filesDir, "images").apply { mkdirs() }

    /**
     * 保存 Uri 指向的图片（按 name 命名，覆盖旧文件）。成功返回文件名，失败返回 null。
     * maxDimension：物品图 1024px 足够；全局背景走 2048px 保全屏清晰度。
     */
    fun save(context: Context, name: String, uri: Uri, maxDimension: Int = 1024): String? = runCatching {
        val resolver = context.applicationContext.contentResolver
        // 第一遍只读尺寸（inJustDecodeBounds 的 decodeStream 本来就返回 null，
        // 尺寸信息写在 options 里——之前误把 null 当失败导致上传永远不生效）
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        var w = bounds.outWidth
        var h = bounds.outHeight
        while (w / 2 >= maxDimension && h / 2 >= maxDimension) {
            sample *= 2
            w /= 2
            h /= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        val fileName = "$name.jpg"
        val target = File(dir(context), fileName)
        target.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        bitmap.recycle()
        fileName
    }.getOrNull()

    /** 读取物品图片；文件不存在或解码失败返回 null（上层回退到首字占位）。 */
    fun load(context: Context, fileName: String): Bitmap? = runCatching {
        if (fileName.isBlank()) return null
        val file = File(dir(context), fileName)
        if (!file.exists()) return null
        BitmapFactory.decodeFile(file.absolutePath)
    }.getOrNull()

    /** 删除旧图（重传场景：文件名带时间戳，旧文件不再被引用，直接清理） */
    fun delete(context: Context, fileName: String) {
        if (fileName.isBlank()) return
        runCatching { File(dir(context), fileName).delete() }
    }
}
