package com.tang.itemkeeper.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.tang.itemkeeper.data.ThemeMode
import com.tang.itemkeeper.glass.utils.LiquidGestureStyle
import com.tang.itemkeeper.ui.components.GlassCard
import com.tang.itemkeeper.ui.components.PageHeader
import com.tang.itemkeeper.ui.components.OutlinedText
import com.tang.itemkeeper.ui.theme.Space

/** 界面（设置）：App 风格 + 更改全局背景，卡片均为透明玻璃（透过可见背景图） */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    backdrop: Backdrop,
    hasBackground: Boolean,
    onUploadBackground: () -> Unit,
    onResetBackground: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    backupBusy: Boolean,
    modifier: Modifier = Modifier
) {
    // 页面滚动状态：传给玻璃卡做竖向拖动的阈值让渡（拖卡先玻璃牵拉，越阈值页面滚动）
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            // 横向页边距放在 verticalScroll 内侧：视口裁剪边=屏幕边缘，玻璃可凸出（2026-11-15）
            .padding(vertical = 16.dp)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
    ) {
        PageHeader(title = "ItemKeeper")
        GlassCard(
            backdrop = backdrop,
            cornerRadius = 14.dp,
            liquidStyle = LiquidGestureStyle(),
            pageScroll = scrollState
        ) {
            OutlinedText(
                text = "App 风格",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            RadioRow(
                text = "跟随系统（默认）",
                selected = themeMode == ThemeMode.SYSTEM,
                onClick = { onThemeChange(ThemeMode.SYSTEM) }
            )
            RadioRow(
                text = "浅色",
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { onThemeChange(ThemeMode.LIGHT) }
            )
            RadioRow(
                text = "暗色",
                selected = themeMode == ThemeMode.DARK,
                onClick = { onThemeChange(ThemeMode.DARK) }
            )
        }

        Spacer(modifier = Modifier.height(Space.x2))

        GlassCard(
            backdrop = backdrop,
            cornerRadius = 14.dp,
            liquidStyle = LiquidGestureStyle(),
            pageScroll = scrollState
        ) {
            OutlinedText(
                text = "更改全局背景",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedText(
                text = if (hasBackground)
                    "已上传自定义背景，底栏与玻璃卡片会实时折射它。重新上传即覆盖。"
                else
                    "上传一张图片作为全屏背景：状态栏与小白条会透明浮在图片上，所有玻璃元素实时折射。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(Space.x1))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onUploadBackground) {
                    OutlinedText(
                        text = if (hasBackground) "重新选择图片" else "选择图片",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(Space.x1))
                TextButton(
                    onClick = onResetBackground,
                    enabled = hasBackground
                ) {
                    OutlinedText(
                        text = "恢复默认",
                        color = if (hasBackground) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(Space.x2))

        // 数据备份：单个 zip 打包物品/图片/类别/设置，卸载重装后一键导入恢复
        GlassCard(
            backdrop = backdrop,
            cornerRadius = 14.dp,
            liquidStyle = LiquidGestureStyle(),
            pageScroll = scrollState
        ) {
            OutlinedText(
                text = "数据备份",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedText(
                text = "把物品、图片、类别与设置打包成一个 zip 文件，保存到手机任意位置；重装 App 后可一键导入恢复。全程本地，不联网、不要权限。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(Space.x1))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onExportBackup, enabled = !backupBusy) {
                    OutlinedText(
                        text = "导出备份",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(Space.x1))
                TextButton(
                    onClick = onImportBackup,
                    enabled = !backupBusy
                ) {
                    OutlinedText(
                        text = "导入备份",
                        color = if (!backupBusy) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
        // 底部留出 120dp 给悬浮玻璃底栏（与首页一致）：最后一张卡能完整滚动到
        // 底栏上方，整卡可触摸触发液态效果，不会被底栏遮住大半（2026-11-15 用户反馈）
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(6.dp))
        OutlinedText(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp
        )
    }
}
