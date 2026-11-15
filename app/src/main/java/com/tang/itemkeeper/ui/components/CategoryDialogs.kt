package com.tang.itemkeeper.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import com.tang.itemkeeper.glass.utils.LiquidGestureState
import com.tang.itemkeeper.glass.utils.liquidGlassGestures
import com.tang.itemkeeper.ui.theme.LocalAppDark
import com.tang.itemkeeper.ui.theme.Motion
import com.tang.itemkeeper.ui.theme.SaveRed
import com.tang.itemkeeper.ui.theme.Space

/**
 * 添加 / 删除自定义类别的玻璃覆盖层（2026-11-15 用户要求与全 App 玻璃语言统一）。
 *
 * 重要：由 ItemKeeperApp 顶层承载（采样层的兄弟节点），backdrop 传主采样源——
 * 弹窗能像归总卡一样折射背景与页面内容。若留在页面内（采样树里）消费主 backdrop
 * 会渲染重入崩溃（铁律 1），所以通过「请求 + 回调闭包」从页面把交互提升上来。
 * 输入框保持实色（既有规则）。
 */
@Composable
fun AddCategoryDialog(
    backdrop: Backdrop,
    onAdd: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var newCategory by remember { mutableStateOf("") }
    var categoryError by remember { mutableStateOf("") }
    val dark = LocalAppDark.current
    val surfaceColor = if (dark) Color(0xFF15151C) else Color(0xFFFDFDFF)
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    val gestureScope = rememberCoroutineScope()
    val gesture = remember { LiquidGestureState(gestureScope) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (dark) 0.46f else 0.28f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = scaleIn(
                initialScale = 0.86f,
                animationSpec = spring(
                    dampingRatio = Motion.BOUNCY_DAMPING,
                    stiffness = Motion.BOUNCY_STIFFNESS
                )
            ) + fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(24f.dp) },
                        effects = {
                            val minDimension = size.minDimension
                            vibrancy()
                            blur(6f.dp.toPx())
                            lens(
                                refractionHeight = 0.24f * minDimension,
                                refractionAmount = 0.42f * minDimension,
                                depthEffect = true
                            )
                        },
                        layerBlock = gesture.layerBlock,
                        onDrawSurface = { drawRect(surfaceColor.copy(alpha = 0.42f)) }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .liquidGlassGestures(gesture)
                    .then(gesture.highlightModifier)
                    .padding(Space.x2)
            ) {
                OutlinedText(
                    text = "添加自定义类别",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                RoundedTextField(
                    value = newCategory,
                    onValueChange = { if (it.length <= 12) newCategory = it; categoryError = "" },
                    placeholder = "如：图书、运动装备"
                )
                if (categoryError.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(categoryError, color = SaveRed, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        OutlinedText(
                            text = "取消",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (onAdd(newCategory)) {
                            onDismiss()
                        } else {
                            categoryError = "类别不能为空、重复或超过 12 字"
                        }
                    }) {
                        OutlinedText(
                            text = "添加",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 13.sp,
                            strokeWidth = 0.3.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RemoveCategoryDialog(
    categories: List<String>,
    backdrop: Backdrop,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val dark = LocalAppDark.current
    val surfaceColor = if (dark) Color(0xFF15151C) else Color(0xFFFDFDFF)
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    val listScrollState = rememberScrollState()
    val gestureScope = rememberCoroutineScope()
    val gesture = remember { LiquidGestureState(gestureScope) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (dark) 0.46f else 0.28f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = scaleIn(
                initialScale = 0.86f,
                animationSpec = spring(
                    dampingRatio = Motion.BOUNCY_DAMPING,
                    stiffness = Motion.BOUNCY_STIFFNESS
                )
            ) + fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.72f).dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(24f.dp) },
                        effects = {
                            val minDimension = size.minDimension
                            vibrancy()
                            blur(6f.dp.toPx())
                            lens(
                                refractionHeight = 0.24f * minDimension,
                                refractionAmount = 0.42f * minDimension,
                                depthEffect = true
                            )
                        },
                        layerBlock = gesture.layerBlock,
                        onDrawSurface = { drawRect(surfaceColor.copy(alpha = 0.42f)) }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .liquidGlassGestures(gesture, listScrollState)
                    .then(gesture.highlightModifier)
                    .padding(Space.x2)
            ) {
                OutlinedText(
                    text = "删除类别",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (categories.isEmpty()) {
                    Text(
                        "没有可删除的类别",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "已删除的类别可随时重新添加；历史物品仍会被排序与筛选覆盖。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 类别列表：竖向可滚动；液态手势竖向过阈值让渡列表滚动（与排序卡一致）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(listScrollState)
                ) {
                    categories.forEach { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, fontSize = 14.sp)
                            TextButton(onClick = { onRemove(name) }) {
                                Text("删除", color = SaveRed, fontSize = 13.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        OutlinedText(
                            text = "完成",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
