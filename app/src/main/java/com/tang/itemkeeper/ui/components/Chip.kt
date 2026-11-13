package com.tang.itemkeeper.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tang.itemkeeper.ui.theme.statusStyle

/**
 * 2026-11-13 用户定稿：全 App 的标签/按钮类元素统一「微透背景」策略——
 * 保持语义色相但半透明，浮在全局背景图上不再出现突兀的实色块。
 */
private val ChipSurfaceAlpha = 0.62f

/** 表单/筛选用的可点击标签：选中态用主色填充 */
@Composable
fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val foreground = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = background.copy(alpha = if (selected) 0.78f else ChipSurfaceAlpha),
        shape = RoundedCornerShape(50),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        OutlinedText(
            text = text,
            color = foreground,
            fontSize = 12.sp,
            strokeWidth = 0.3.dp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

/** 只读信息标签（类别、平台等） */
@Composable
fun TagChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = ChipSurfaceAlpha),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        OutlinedText(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            strokeWidth = 0.3.dp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/** 物品状态标签：七种状态七种颜色，随明暗主题自动切换 */
@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val style = statusStyle(status, dark)
    Surface(
        color = style.background.copy(alpha = ChipSurfaceAlpha),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        OutlinedText(
            text = status,
            color = style.foreground,
            fontSize = 11.sp,
            strokeWidth = 0.3.dp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
