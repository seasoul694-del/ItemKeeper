package com.tang.itemkeeper.glass.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.RuntimeShader
import com.kyant.backdrop.asComposeShader
import com.kyant.backdrop.isRuntimeShaderSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/**
 * 液态玻璃表面通用交互（2026-11-15 用户需求）：点按出触点光斑脉冲、按住拖动玻璃被牵拉
 * （跟手平移 + 方向性拉伸，官方 LiquidButton 配方），竖向拖动过阈值后让渡给页面滚动。
 *
 * 实现全部来自官方 Catalog 实证配方，不自造效果：
 *  - 形变：layerBlock 里「等比放大 + tanh 饱和平移 + atan2 方向性 scaleX/Y 拉伸」，
 *    backdrop 的 LayerBackdrop 会做逆变换采样 → 玻璃被拉走时透出的背景纹丝不动（液态视差）
 *  - 光斑：AGSL 径向渐变（Plus 混合）跟随手指，API < 33 降级为整体提亮
 *  - 性能红线：不调制 lens/effects（每帧重建 RenderEffect = 已知卡顿坑），形变只走
 *    graphicsLayer；highlightModifier 必须与 layerBlock 成对挂载——光斑 drawWithContent
 *    读取同一动画状态，是 backdrop 重采样（逆变换刷新）的失效来源，缺一个视差就错位
 *
 * 手势让渡规则（用户定稿）：
 *  - 竖向拖动：阈值（默认 32dp）内消费事件喂玻璃牵拉，页面不动；越过后玻璃弹回，
 *    后续位移经 pageScroll.dispatchRawDelta 直驱页面滚动，松手按手指速度 spline 惯性
 *  - 横向（含斜向主轴判定）：纯牵拉，无阈值不消费（页面滚动不抢横向事件）
 *  - 事件被子元素消费（滚轮/内部滚动/按键）→ 牵拉取消回弹；点按不在此处理，
 *    沿用既有 clickable（光斑脉冲即点按反馈），零回归
 */
data class LiquidGestureStyle(
    /** 按压时等比放大幅度（官方 LiquidButton 为 4dp） */
    val pressScale: Dp = 4.dp,
    /** 拖动方向性拉伸的最大幅度（官方为 4dp；本工程调大加强「胶黏感」） */
    val dragScale: Dp = 6.dp,
    /** 跟手平移的 tanh 初始导数（官方 0.05；本工程 0.08 = 玻璃初始跟手 8% 手指位移） */
    val translateDerivative: Float = 0.08f,
    /**
     * 横向平移饱和上限：玻璃边逼近屏幕边缘时渐近停住（胶压在墙上的手感），
     * 硬拉也不会越过屏幕被平切。默认 16dp ≈ 页边距。
     */
    val edgeRoom: Dp = 16.dp,
    /** 竖向平移饱和上限（物品卡的竖向裁剪余量与之对齐，默认 24dp） */
    val verticalRoom: Dp = 24.dp
)

class LiquidGestureState(
    val animationScope: CoroutineScope,
    val style: LiquidGestureStyle = LiquidGestureStyle()
) {

    private val pressProgressSpec = spring(0.5f, 300f, 0.001f)
    private val positionSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)

    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val positionAnimation =
        Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    private var startPosition = Offset.Zero

    val pressProgress: Float get() = pressProgressAnimation.value
    val offset: Offset get() = positionAnimation.value - startPosition

    /**
     * 手势进行中（按下 → 松手）。只在状态边界变化、不逐帧重组，
     * 供外层做「拉拽时不裁剪」等布局联动（如物品卡让玻璃凸出边界）。
     */
    var isActive: Boolean by mutableStateOf(false)
        private set

    /**
     * 裁剪余量是否放开（竖向凸出可见）。松手后不立即收起——玻璃回弹弹簧还要飞
     * 约 0.5s，余量若瞬间归零，还在外面的突出段会被突然裁出一条白线
     * （2026-11-15 用户反馈）。等弹回归位后再收，收的时候玻璃已在卡内，看不见裁切。
     */
    var slackActive: Boolean by mutableStateOf(false)
        private set

    /**
     * 横向拖拽进行中（主轴锁定为横向）。仅此时放开横向裁剪让玻璃凸出卡片边界
     * （胶黏凸出的舞台）；点按/展开/收起等其余时刻严格裁剪——任何越界渲染都会
     * 显形为灰框（2026-11-15 用户反馈根修）。
     */
    var stretchActive: Boolean by mutableStateOf(false)
        internal set

    /** 手指按下：光斑升起 + 定位到按下点 */
    fun press(position: Offset) {
        startPosition = position
        isActive = true
        slackActive = true
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressSpec) }
            launch { positionAnimation.snapTo(position) }
        }
    }

    /** 手指移动：光斑/牵拉实时跟手 */
    fun follow(position: Offset) {
        animationScope.launch { positionAnimation.snapTo(position) }
    }

    /** 恢复按压光斑但不重置牵拉锚点（滚动到边界后重新进入牵拉态，玻璃不跳位） */
    fun rePress() {
        animationScope.launch { pressProgressAnimation.animateTo(1f, pressProgressSpec) }
    }

    /**
     * 弹回动画（与官方 InteractiveHighlight 同参数，带回弹）。
     * 阈值让渡滚动时也调用——此时手势仍在进行中，isActive 不变。
     */
    fun release() {
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(0f, pressProgressSpec) }
            launch { positionAnimation.animateTo(startPosition, positionSpec) }
        }
    }

    /** 手势真正结束（松手/取消/被子级接管）：弹回并结束活跃态；余量延迟收起防白线 */
    fun finish() {
        isActive = false
        release()
        animationScope.launch {
            delay(SLACK_COLLAPSE_DELAY_MS)
            // 延迟期间若再次按下（isActive 回 true），本次不收，由新一轮 finish 接管；
            // stretchActive 同步延迟收——回弹弹簧（约 0.5s）期间凸出保持可见，归位后才收裁剪
            if (!isActive) {
                slackActive = false
                stretchActive = false
            }
        }
    }

    /**
     * 玻璃形变层（挂在 drawBackdrop 的 layerBlock 上）。
     * 官方 LiquidButton 公式（按压缩放 + tanh 饱和平移 + 方向性轴拉伸）+
     * 本工程调优：平移饱和上限收窄到「贴边余量」（玻璃压在屏幕边上而非越出屏幕被平切）、
     * 拉伸归一化位移也过 tanh（拉到头后玻璃「绷紧」，不再无限变长）。
     */
    val layerBlock: GraphicsLayerScope.() -> Unit = {
        val progress = pressProgress
        val scale = lerp(1f, 1f + style.pressScale.toPx() / size.height, progress)

        val roomX = style.edgeRoom.toPx()
        val roomY = style.verticalRoom.toPx()
        val currentOffset = offset
        translationX = roomX * tanh(style.translateDerivative * currentOffset.x / roomX)
        translationY = roomY * tanh(style.translateDerivative * currentOffset.y / roomY)

        val maxDragScale = style.dragScale.toPx() / size.height
        val offsetAngle = atan2(currentOffset.y, currentOffset.x)
        val satX = tanh(1.2f * currentOffset.x / size.maxDimension) / 1.2f
        val satY = tanh(1.2f * currentOffset.y / size.maxDimension) / 1.2f
        scaleX = scale +
                maxDragScale * abs(cos(offsetAngle) * satX) *
                (size.width / size.height).fastCoerceAtMost(1f)
        scaleY = scale +
                maxDragScale * abs(sin(offsetAngle) * satY) *
                (size.height / size.width).fastCoerceAtMost(1f)
    }

    /** 触点光斑（Plus 混合径向渐变，跟指）。与 layerBlock 成对挂载，见类注释。 */
    val highlightModifier: Modifier =
        Modifier.drawWithContent {
            val progress = pressProgressAnimation.value
            if (progress > 0f) {
                if (shader != null) {
                    drawRect(
                        Color.White.copy(0.08f * progress),
                        blendMode = BlendMode.Plus
                    )
                    shader.apply {
                        val position = positionAnimation.value
                        setFloatUniform("size", size.width, size.height)
                        setColorUniform("color", Color.White.copy(0.15f * progress))
                        setFloatUniform("radius", size.minDimension * 1.5f)
                        setFloatUniform(
                            "position",
                            position.x.fastCoerceIn(0f, size.width),
                            position.y.fastCoerceIn(0f, size.height)
                        )
                    }
                    drawRect(
                        ShaderBrush(shader.asComposeShader()),
                        blendMode = BlendMode.Plus
                    )
                } else {
                    drawRect(
                        Color.White.copy(0.25f * progress),
                        blendMode = BlendMode.Plus
                    )
                }
            }

            drawContent()
        }

    private val shader =
        if (isRuntimeShaderSupported()) {
            RuntimeShader(
                """
uniform float2 size;
layout(color) uniform half4 color;
uniform float radius;
uniform float2 position;

half4 main(float2 coord) {
    float dist = distance(coord, position);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}"""
            )
        } else {
            null
        }
}

/** 松手惯性衰减率（每秒）：速度按 exp(-rate·t) 衰减，约 1 秒归零，接近系统 spline 手感 */
private const val DECAY_RATE = 3.2f

/** 松手后裁剪余量保持的时长（ms）：需盖过回弹弹簧的归位时间，防止白线闪现 */
private const val SLACK_COLLAPSE_DELAY_MS = 600L

/** 松手惯性：滚动 mutex 内逐帧指数摩擦衰减（竖向页面与横向列表共用） */
private suspend fun decayFling(scrollState: ScrollableState, velocity: Float, minVelocity: Float) {
    scrollState.scroll {
        var velocity = velocity
        var lastNanos = 0L
        while (abs(velocity) > minVelocity) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = ((now - lastNanos) / 1e9f).coerceAtMost(0.05f)
                    scrollBy(velocity * dt)
                    velocity *= exp(-DECAY_RATE * dt)
                }
                lastNanos = now
            }
        }
    }
}

/**
 * 玻璃表面的手势处理器：点按光斑 + 拖动牵拉 + 竖向阈值让渡页面滚动。
 *
 * 手势归属规则（2026-11-15 用户定稿「由起始点决定，全程连贯」）：
 *  - down 在 Main pass 判定：按钮等可交互子元素会先消费 down（叶子节点先处理），
 *    落在它们上面的按下不触发玻璃反馈（「非按键处才有反馈」）
 *  - 认领后的手势全程在【Initial pass】消费位移——父级先于所有子级看到事件，
 *    内层滚动/滚轮/文字区都抢不走手势（快速滑动/斜向漂移也不再断，2026-11-15 根修）
 *  - 竖向阈值（默认 32dp）后先探测滚动余量：有余量才让渡滚动（玻璃弹回）；
 *    没余量（内容不满一屏/已滚到边界）就留在牵拉态；滚动到边界后继续拉
 *    自动回到牵拉态（橡皮筋式回胶）
 *  - 横向锁定的手势同样全程受保护；horizontalScroll 非空且 horizontalHandoff()
 *    为真（按下起点在转交区域内）时，横向位移转交给该滚动容器（如归总卡柱状图），
 *    到边界自动变纯胶黏，松手带惯性；否则横向 = 纯胶黏牵拉
 *
 * 挂载位置约定（同节点同时有「拦截点击」clickable 时）：必须挂在 clickable 的
 * 【内侧】（modifier 链中更靠后）——本处理器不消费 down/up，点按链路原样保留。
 *
 * @param pageScroll 竖向阈值让渡目标（页面/卡内滚动容器）。null = 竖向纯牵拉（如弹窗）
 * @param horizontalScroll 横向位移转交目标（卡内横向列表）。null = 横向纯牵拉
 * @param horizontalHandoff 横向转交门控（按下时求值一次）。用于把转交限定在
 *        卡内某个子区域——由子区域的观察者写标志位，起始点在区域内才转交
 */
fun Modifier.liquidGlassGestures(
    state: LiquidGestureState,
    pageScroll: ScrollableState? = null,
    horizontalScroll: ScrollableState? = null,
    verticalThreshold: Dp = 32.dp,
    horizontalHandoff: () -> Boolean = { true }
): Modifier = pointerInput(state, pageScroll, horizontalScroll, verticalThreshold) {
    val thresholdPx = verticalThreshold.toPx()
    val touchSlop = viewConfiguration.touchSlop
    // compose ui 的 ViewConfiguration 不暴露 fling 速度阈值，用 50dp/s（Android 默认值）
    val minFlingVelocity = 50.dp.toPx()

    awaitEachGesture {
        // down 用 Main pass 且要求未被消费：按钮/输入框/选项行的按下已被子元素消费，跳过
        val down = awaitFirstDown(requireUnconsumed = true)
        val downId = down.id
        // 转交门控在按下时求值一次（子区域观察者此刻已处理完同一个 down——叶子先处理）
        val handoffAllowed = horizontalScroll != null && horizontalHandoff()
        state.press(down.position)

        // 0 = 未定主轴，1 = 横向（牵拉/转交横向滚动），2 = 竖向（阈值让渡）
        var mode = 0
        var totalX = 0f
        var totalY = 0f
        var scrolling = false
        var hScrolling = false
        val velocityTracker = VelocityTracker()

        try {
            while (true) {
                // Initial pass：先于所有子元素看到本事件——认领后的手势在此消费位移
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.fastFirstOrNull { it.id == downId } ?: break
                var weConsumed = false

                if (change.changedToUpIgnoreConsumed()) {
                    // 松手惯性：手指速度取反即滚动方向；指数摩擦衰减逐帧驱动
                    // （滚动 mutex 全程持有，与系统列表互斥安全）
                    val fingerVelocity = velocityTracker.calculateVelocity()
                    if (scrolling && pageScroll != null && abs(-fingerVelocity.y) > minFlingVelocity) {
                        state.animationScope.launch {
                            decayFling(pageScroll, -fingerVelocity.y, minFlingVelocity)
                        }
                    }
                    if (hScrolling && horizontalScroll != null && abs(-fingerVelocity.x) > minFlingVelocity) {
                        state.animationScope.launch {
                            decayFling(horizontalScroll, -fingerVelocity.x, minFlingVelocity)
                        }
                    }
                    break
                }

                val delta = change.positionChange()
                if (delta != Offset.Zero) {
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    totalX += delta.x
                    totalY += delta.y

                    if (mode == 0 && (abs(totalX) > touchSlop || abs(totalY) > touchSlop)) {
                        // 斜向按主轴判定：横向占优 → 纯牵拉；竖向占优 → 阈值让渡
                        mode = if (abs(totalX) > abs(totalY)) 1 else 2
                        if (mode == 1) state.stretchActive = true
                    }

                    if (mode == 2 && pageScroll != null) {
                        if (!scrolling && abs(totalY) < thresholdPx) {
                            // 阈值前：消费竖向位移喂玻璃牵拉，页面纹丝不动
                            state.follow(change.position)
                            change.consume()
                            weConsumed = true
                        } else {
                            // 阈值后：探测滚动余量——有余量才让渡滚动；
                            // 没余量（到边界/内容不满一屏）保持玻璃牵拉（竖向胶黏感）
                            val consumed = pageScroll.dispatchRawDelta(-delta.y)
                            if (abs(consumed) > 0.5f) {
                                if (!scrolling) {
                                    scrolling = true
                                    state.release()
                                }
                            } else {
                                if (scrolling) {
                                    // 滚动到边界后再拉：回到牵拉态（橡皮筋式回胶）
                                    scrolling = false
                                    state.rePress()
                                }
                                state.follow(change.position)
                            }
                            change.consume()
                            weConsumed = true
                        }
                    } else if (mode == 1) {
                        // 锁定横向：消费全部位移保护手势（斜向漂移不再被纵向滚动抢走）。
                        // 转交判定只看真实横向位移——竖向漂移/零横向位移事件不触发
                        // 「滚动↔回胶」逐帧翻转（转圈/斜拖时的闪烁根因，2026-11-15）；
                        // 柱状图滚动中玻璃不跟手（与竖向让位一致），边界回胶后才恢复跟手
                        if (handoffAllowed && abs(delta.x) > 0.5f) {
                            val consumed = horizontalScroll.dispatchRawDelta(-delta.x)
                            if (abs(consumed) > 0.5f) {
                                if (!hScrolling) {
                                    hScrolling = true
                                    state.release()
                                }
                            } else if (hScrolling) {
                                // 横向滚到边界后再拉：回到牵拉态（与竖向回胶一致）
                                hScrolling = false
                                state.rePress()
                            }
                        }
                        if (!hScrolling) {
                            state.follow(change.position)
                        }
                        change.consume()
                        weConsumed = true
                    } else {
                        // 竖向但无滚动目标（弹窗卡面）：纯牵拉跟手不消费，滚轮子级照常接管
                        state.follow(change.position)
                    }
                }

                // Final pass：仅当「我们没消费、却被子元素抢走」时取消玻璃反馈
                // （横向滑动撞上柱状图 LazyRow、弹窗里撞上滚轮），让子手势干净接管
                if (!weConsumed) {
                    val finalChange = awaitPointerEvent(PointerEventPass.Final)
                        .changes.fastFirstOrNull { it.id == downId } ?: break
                    if (!finalChange.changedToUpIgnoreConsumed() && finalChange.isConsumed) {
                        break
                    }
                }
            }
        } finally {
            state.finish()
        }
    }
}
