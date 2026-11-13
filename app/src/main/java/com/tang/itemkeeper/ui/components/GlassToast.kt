package com.tang.itemkeeper.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import com.tang.itemkeeper.ui.theme.LocalAppDark
import com.tang.itemkeeper.ui.theme.Motion

/**
 * 左上角液态玻璃提示（替代 material3 Snackbar 黑框）：
 * 从左上角原点弹性放大弹出，停留后弹性缩回（2026-11-13 用户定稿）。
 * 显示时长由外部控制（调用方 LaunchedEffect 延迟清空 message）。
 */
@Composable
fun GlassToast(
    message: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val dark = LocalAppDark.current
    val surfaceColor = if (dark) Color(0xFF15151C) else Color(0xFFFDFDFF)

    // message 非空即弹出；外部清空后弹性缩回
    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = message.isNotBlank()

    AnimatedVisibility(
        visibleState = visibleState,
        enter = scaleIn(
            initialScale = 0.55f,
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f), // 从左上角原点长出来
            animationSpec = spring(
                dampingRatio = Motion.BOUNCY_DAMPING,
                stiffness = Motion.BOUNCY_STIFFNESS
            )
        ) + fadeIn(),
        exit = scaleOut(
            targetScale = 0.55f,
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f),
            animationSpec = spring(
                dampingRatio = 1f,
                stiffness = Motion.SNAPPY_STIFFNESS
            )
        ) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
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
                    onDrawSurface = { drawRect(surfaceColor.copy(alpha = 0.42f)) }
                )
                .padding(horizontal = 18.dp, vertical = 11.dp)
        ) {
            OutlinedText(
                text = message,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                strokeWidth = 0.3.dp
            )
        }
    }
}