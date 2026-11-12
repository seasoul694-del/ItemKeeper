package com.tang.itemkeeper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tang.itemkeeper.ui.theme.SearchPurple
import com.tang.itemkeeper.ui.theme.Space

private fun lerpDp(start: Dp, end: Dp, fraction: Float): Dp = start + (end - start) * fraction

/**
 * Circle to Search Bar：紫色圆形 -> 搜索输入框。
 * 所有尺寸/圆角/颜色/文字透明度都是同一个 progress 的函数，全程只用一个组件，不切换。
 * progress 由「点击展开动画 × (1 - 滚动收起进度)」得到，见 AppScaffold。
 */
@Composable
fun CircleSearchBar(
    progress: Float,
    query: String,
    onQueryChange: (String) -> Unit,
    onToggle: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    // 设计稿 470，按实际屏宽自适应，避免窄屏溢出
    val expandedWidth = minOf(470.dp, screenWidth - Space.x4)
    val width = lerpDp(56.dp, expandedWidth, progress)
    val height = lerpDp(56.dp, 48.dp, progress)
    val corner = lerpDp(999.dp, 16.dp, progress)
    val background = lerp(SearchPurple, MaterialTheme.colorScheme.surfaceVariant, progress)
    val iconTint = lerp(Color.White, MaterialTheme.colorScheme.primary, progress)
    val hintColor = lerp(Color.White, MaterialTheme.colorScheme.onSurfaceVariant, progress)

    // 宽度落定后才淡入占位文字与光标
    val textAlpha = ((progress - 0.82f) / 0.18f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(background),
        contentAlignment = Alignment.CenterStart
    ) {
        // 图标相对框左边缘始终是 16dp：框向右展开时它随左边界同步左移到内边距处
        Box(
            modifier = Modifier
                .padding(start = Space.x2)
                .size(32.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggle
                ),
            contentAlignment = Alignment.Center
        ) {
            LineIcon(LineIconType.SEARCH, iconTint, Modifier.size(24.dp))
        }

        Box(
            modifier = Modifier
                .padding(start = 56.dp, end = Space.x2)
                .fillMaxWidth()
                .alpha(textAlpha)
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                enabled = progress > 0.95f,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            text = "搜索物品名称、类别、平台…",
                            fontSize = 14.sp,
                            maxLines = 1,
                            color = hintColor
                        )
                    }
                    inner()
                }
            )
        }
    }
}
