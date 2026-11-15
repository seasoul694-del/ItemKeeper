package com.tang.itemkeeper.glass.utils

import androidx.compose.runtime.withFrameNanos

// 纯 Android 工程的 awaitFrame 实现（原 Catalog 为 KMP expect/actual）。
suspend fun awaitFrame() {
    withFrameNanos { }
}
