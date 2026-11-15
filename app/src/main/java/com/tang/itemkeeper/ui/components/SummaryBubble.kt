package com.tang.itemkeeper.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.RoundedRectangle
import com.tang.itemkeeper.glass.utils.LiquidGestureState
import com.tang.itemkeeper.glass.utils.liquidGlassGestures
import com.tang.itemkeeper.model.Item
import com.tang.itemkeeper.ui.theme.Brand
import com.tang.itemkeeper.ui.theme.LocalAppDark
import com.tang.itemkeeper.ui.theme.Motion
import com.tang.itemkeeper.ui.theme.Space
import com.tang.itemkeeper.ui.theme.Tiffany
import com.tang.itemkeeper.ui.theme.priceColor
import com.tang.itemkeeper.util.money
import kotlinx.coroutines.delay

/**
 * 柱状图转交区域观察者：按下落在区域内即置位标志（纯标记，不消费任何事件）。
 * 卡片级手势处理器在按下时读取该标志决定横向是否转交；区域外按下横向 = 纯胶黏。
 */
private fun Modifier.chartRegionTouchObserver(flag: MutableState<Boolean>): Modifier =
    pointerInput(flag) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            flag.value = true
            // 等所有手指抬起再清标志（awaitAllPointersUp 在本 compose 版本是 internal，手写等价循环）
            while (true) {
                val event = awaitPointerEvent()
                if (event.changes.none { it.pressed }) break
            }
            flag.value = false
        }
    }

@Composable
private fun CountNumber(
    target: Double,
    color: Color,
    prefix: String = "",
    suffix: String = "",
    started: Boolean
) {
    // WWDC23 弹簧体系（≈ .smooth：无回弹平滑弹簧）——数字滚动连续渐变、不过冲
    val value by animateFloatAsState(
        targetValue = if (started) target.toFloat() else 0f,
        animationSpec = spring(
            dampingRatio = Motion.SMOOTH_DAMPING,
            stiffness = Motion.SMOOTH_STIFFNESS,
            visibilityThreshold = 0.05f
        ),
        label = "count-up"
    )
    Text(
        text = "$prefix${money(value.toDouble())}$suffix",
        fontSize = 22.sp,
        fontWeight = FontWeight.Light,
        color = color
    )
}

/**
 * 归总玻璃气泡：真实背景折射（backdrop lens，无 blur 的清玻璃参数基线），
 * 覆盖所有状态的精炼统计 + 类别物价柱状图进入屏幕时从 0 生长。
 * 卡片限高 72% 屏高 + 内部滚动；点击卡片外弹回，底栏特效回到上一个按钮（由 ViewModel 处理）。
 */
@Composable
fun SummaryBubble(
    visible: Boolean,
    items: List<Item>,
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(160)
            started = true
        } else {
            started = false
        }
    }

    val dark = LocalAppDark.current
    val orange = priceColor(dark)
    val surfaceColor = if (dark) Color(0xFF15151C) else Color(0xFFFDFDFF)

    val totalPrice = items.sumOf { it.price }
    // 日均价总和只统计使用中的物品（在用/闲置/送人）；
    // 用完/报废/售出/丢失的物品日均已冻结在截止日，不再计入
    val totalDaily = items.filter { !it.isEnded }.sumOf { it.dailyPrice() }
    // 不设 take 上限：类别再多也不吞数据，柱状图用滑动承载
    val byCategory = items
        .groupBy { it.category }
        .map { (name, list) -> name to list.sumOf { it.price } }
        .sortedByDescending { it.second }
    val maxAmount = byCategory.maxOfOrNull { it.second } ?: 0.0

    val grow by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Motion.BOUNCY_DAMPING,
            stiffness = Motion.BOUNCY_STIFFNESS,
            visibilityThreshold = 0.002f
        ),
        label = "bar-grow"
    )

    val maxCardHeight = (LocalConfiguration.current.screenHeightDp * 0.72f).dp

    // 液态手势：卡面点按光斑 + 拖动牵拉；竖向过阈值让渡卡内滚动；横向位移转交
    // 柱状图列表（到边界自动变纯胶黏）——斜向/快速滑动全程连贯（2026-11-15）
    val contentScrollState = rememberScrollState()
    val barListState = rememberLazyListState()
    val gestureScope = rememberCoroutineScope()
    val gesture = remember { LiquidGestureState(gestureScope) }
    // 柱状图转交门控：按下起点落在「分布标题 → 底部提示」区域内才转交横向滚动，
    // 区域外横向 = 纯胶黏，不再带动柱状图（2026-11-15 用户定稿：起始点决定）
    val chartRegionTouched = remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // 暗幕只淡入淡出、不缩放：全屏暗幕若跟随 scaleIn，其矩形边缘会以
        // 「一大片灰影同时扩大」的形式扫过屏幕（2026-11-15 用户反馈去除）
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(durationMillis = 120)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (dark) 0.46f else 0.28f))
                    .clickable(onClick = onDismiss)
            )
        }

        // 玻璃卡：缩放入场只作用于卡片本体（本组件是采样层的兄弟节点，drawBackdrop 合法）
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                initialScale = 0.86f,
                animationSpec = spring(
                    dampingRatio = Motion.BOUNCY_DAMPING,
                    stiffness = Motion.BOUNCY_STIFFNESS
                )
            ) + fadeIn(),
            // 退出只做快速淡出，不再缩放：scaleOut 的 layer 变换会让 drawBackdrop 的
            // shader 采样与 surface 绘制错位，退出瞬间闪现一大块灰色表面（真机已复现）
            exit = fadeOut(animationSpec = tween(durationMillis = 120)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // 液态手势挂在拦截 clickable 的内侧：Main pass 由内向外派发，
                // 本处理器先看到事件（不消费 down/up，点按拦截链路原样保留）
                Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = maxCardHeight)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(28f.dp) },
                        effects = {
                            val minDimension = size.minDimension
                            vibrancy()
                            blur(6f.dp.toPx())
                            lens(
                                refractionHeight = 0.24f * minDimension,
                                refractionAmount = 0.42f * minDimension,
                                depthEffect = true,
                                chromaticAberration = false
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
                    .liquidGlassGestures(
                        gesture,
                        contentScrollState,
                        barListState,
                        horizontalHandoff = { chartRegionTouched.value }
                    )
                    .then(gesture.highlightModifier)
                    .padding(Space.x3)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(contentScrollState)
                ) {
                    Text("物品归总", fontSize = 20.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "覆盖全部 ${items.size} 件物品（含所有状态）",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Space.x2))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("总物价", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            CountNumber(totalPrice, orange, prefix = "¥", started = started)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("物品数量", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            CountNumber(items.size.toDouble(), Brand, suffix = " 件", started = started)
                        }
                    }
                    Spacer(modifier = Modifier.height(Space.x1))
                    Text("日均价总和", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CountNumber(totalDaily, orange, prefix = "¥", started = started)
                    Text(
                        "仅统计使用中的物品（在用 / 闲置 / 送人），已用完 / 报废 / 售出 / 丢失的物品日均冻结在截止日",
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )

                    Spacer(modifier = Modifier.height(Space.x2))
                    Text(
                        "各类别物价分布",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.chartRegionTouchObserver(chartRegionTouched)
                    )
                    Spacer(modifier = Modifier.height(Space.x1))

                    if (byCategory.isEmpty()) {
                        Text(
                            "还没有物品，先去「添加」记录一件吧",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // 恒定间距柱状图：类别少时按宽度均分挤满；
                        // 类别多时柱槽恒定 72dp、左右滑动，滑到边界即停（2026-11-13 用户定稿：不循环）
                        // （barListState 已提升到组件顶部：液态手势的横向位移转交给它）
                        val bubbleInnerWidth =
                            (LocalConfiguration.current.screenWidthDp.dp * 0.9f) - Space.x3 * 2
                        val barSlotWidth = 72.dp
                        val needsScroll = (barSlotWidth + Space.x1) * byCategory.size > bubbleInnerWidth
                        val itemWidth = if (needsScroll) barSlotWidth
                        else (bubbleInnerWidth - Space.x1 * (byCategory.size - 1)) / byCategory.size

                        LazyRow(
                            state = barListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp)
                                .chartRegionTouchObserver(chartRegionTouched),
                            horizontalArrangement = Arrangement.spacedBy(Space.x1),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            itemsIndexed(byCategory) { index, (name, amount) ->
                                val ratio =
                                    if (maxAmount > 0) (amount / maxAmount).toFloat() else 0f
                                val barHeight = 112.dp * ratio * grow
                                Column(
                                    modifier = Modifier.width(itemWidth).fillMaxHeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // 金额标签：固定高度槽，出现/消失不引起任何位移
                                    Box(
                                        modifier = Modifier.height(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (grow > 0.5f) "¥${money(amount * grow.toDouble())}" else "",
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // 柱子生长区：固定槽 + 底部锚定——柱底像素恒定，
                                    // 弹簧动画只让柱顶向上生长/回弹
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(28.dp)
                                                .height(barHeight.coerceAtLeast(1.dp))
                                                .background(
                                                    color = if (index == 0) Tiffany
                                                    else Brand.copy(alpha = 0.55f),
                                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                                        topStart = 8.dp,
                                                        topEnd = 8.dp
                                                    )
                                                )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = name,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Space.x2))
                    Text(
                        "点击卡片外任意位置收起",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.chartRegionTouchObserver(chartRegionTouched)
                    )
                }
                }
            }
        }
    }
}
