package com.tang.itemkeeper.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tang.itemkeeper.data.ThemeMode
import kotlin.math.abs

/** 8pt 间距网格，全 App 统一使用 */
object Space {
    val x1 = 8.dp
    val x2 = 16.dp
    val x3 = 24.dp
    val x4 = 32.dp
    val x5 = 40.dp
}

val Brand = Color(0xFF4F46E5)
val SearchPurple = Color(0xFF7C3AED)
val Tiffany = Color(0xFF0ABAB5)
val TiffanySoft = Color(0xFFD9F7F5)

/**
 * 与设计稿一致的 CSS cubic-bezier(1.2, .9, .22, 1)。
 * Compose 内置 CubicBezierEasing 对控制点范围有限制，这里用牛顿迭代自建，
 * 允许 x1 > 1，形变与收起共用同一条曲线。
 */
class CssBezierEasing(
    private val x1: Float,
    private val y1: Float,
    private val x2: Float,
    private val y2: Float
) : Easing {
    override fun transform(fraction: Float): Float {
        if (fraction <= 0f) return 0f
        if (fraction >= 1f) return 1f
        var t = fraction
        repeat(8) {
            val dx = curve(t, x1, x2) - fraction
            val slope = slope(t, x1, x2)
            if (abs(slope) < 1e-6f) return@repeat
            t = (t - dx / slope).coerceIn(0f, 1f)
        }
        return curve(t, y1, y2)
    }

    private fun curve(t: Float, p1: Float, p2: Float): Float {
        val u = 1f - t
        return 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t
    }

    private fun slope(t: Float, p1: Float, p2: Float): Float {
        val u = 1f - t
        return 3f * u * u * p1 + 6f * u * t * (p2 - p1) + 3f * t * t * (1f - p2)
    }
}

val MotionCurve: Easing = CssBezierEasing(1.2f, 0.9f, 0.22f, 1f)

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0F766E),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2F1F7),
    onSurfaceVariant = Color(0xFF5B5A6B),
    outlineVariant = Color(0xFFE7E6EF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC7D2FE),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF5EEAD4),
    surface = Color(0xFF121216),
    surfaceVariant = Color(0xFF26262E),
    onSurfaceVariant = Color(0xFFB9B8C6),
    outlineVariant = Color(0xFF373742)
)

@Composable
fun isAppInDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun ItemKeeperTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (isAppInDark(mode)) DarkColors else LightColors,
        content = content
    )
}
