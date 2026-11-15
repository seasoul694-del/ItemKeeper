package com.tang.itemkeeper.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 页面顶栏：放在每个页面滚动内容的第一个元素位置——
 * 内容上滑时顶栏随之滚出屏幕，下滑到内容顶端才重新出现
 * （2026-11-13 用户定稿：顶栏属于内容，而非固定在整个 App 上方）。
 */
@Composable
fun PageHeader(
    title: String,
    showSort: Boolean = false,
    onSortClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(72.dp)
    ) {
        OutlinedText(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            strokeWidth = 0.3.dp,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        if (showSort && onSortClick != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSortClick),
                contentAlignment = Alignment.Center
            ) {
                LineIcon(
                    type = LineIconType.SORT,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
