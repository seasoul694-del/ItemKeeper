package com.tang.itemkeeper.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.tang.itemkeeper.glass.components.LiquidBottomTab
import com.tang.itemkeeper.glass.components.LiquidBottomTabs
import com.tang.itemkeeper.model.Item
import com.tang.itemkeeper.ui.components.LineIcon
import com.tang.itemkeeper.ui.components.LineIconType
import com.tang.itemkeeper.ui.components.OutlinedText
import com.tang.itemkeeper.ui.components.SortBubble
import com.tang.itemkeeper.ui.components.SummaryBubble
import com.tang.itemkeeper.ui.screens.AddScreen
import com.tang.itemkeeper.ui.screens.FilterScreen
import com.tang.itemkeeper.ui.screens.HomeScreen
import com.tang.itemkeeper.ui.screens.SettingsScreen
import com.tang.itemkeeper.ui.theme.ItemKeeperTheme
import com.tang.itemkeeper.ui.theme.LocalAppDark
import com.tang.itemkeeper.ui.theme.SaveRed
import com.tang.itemkeeper.ui.theme.Space
import com.tang.itemkeeper.ui.theme.Tiffany
import com.tang.itemkeeper.util.ItemImages
import com.tang.itemkeeper.util.sortItems
import com.tang.itemkeeper.viewmodel.MainViewModel
import com.tang.itemkeeper.viewmodel.Tabs
import kotlinx.coroutines.delay

private fun tabIcon(index: Int): LineIconType = when (index) {
    Tabs.HOME -> LineIconType.HOME
    Tabs.SUMMARY -> LineIconType.SUMMARY
    Tabs.ADD -> LineIconType.ADD
    Tabs.FILTER -> LineIconType.FILTER
    else -> LineIconType.SETTINGS
}

private fun tabLabel(index: Int): String = when (index) {
    Tabs.HOME -> "首页"
    Tabs.SUMMARY -> "归总"
    Tabs.ADD -> "添加"
    Tabs.FILTER -> "归类"
    else -> "界面"
}

@Composable
fun ItemKeeperApp(vm: MainViewModel = viewModel()) {
    val themeMode by vm.settings.themeMode.collectAsState()

    ItemKeeperTheme(mode = themeMode) {
        val appDark = LocalAppDark.current
        val currentTab by vm.currentTab.collectAsState()
        val previousTab by vm.previousTab.collectAsState()
        val bubbleVisible by vm.summaryBubble.collectAsState()
        val sortBubbleVisible by vm.sortBubble.collectAsState()
        val sortMode by vm.sortMode.collectAsState()
        val items by vm.items.collectAsState()
        val categories by vm.categories.collectAsState()
        val editing by vm.editingItem.collectAsState()
        val expandedItemId by vm.expandedItemId.collectAsState()

        // 底栏胶囊位置：气泡打开时统一指「归总」，关闭时回当前 tab
        // （closeBubble 会把 currentTab 弹回上一个按钮，视觉上即「弹回」）。
        // derivedStateOf 捕获的都是稳定 State 委托，保证跨重组读到最新值。
        val bottomTab by remember {
            derivedStateOf { if (bubbleVisible) Tabs.SUMMARY else currentTab }
        }
        val displayTab = if (bubbleVisible) previousTab else currentTab

        var deleteTarget by remember { mutableStateOf<Item?>(null) }
        var uploadTarget by remember { mutableStateOf<Item?>(null) }
        var snackbarMessage by remember { mutableStateOf("") }
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(snackbarMessage) {
            if (snackbarMessage.isNotBlank()) {
                snackbarHostState.showSnackbar(snackbarMessage)
                snackbarMessage = ""
            }
        }

        // 系统相册选择器（Photo Picker，无需任何存储权限）；回传后压缩落盘并写入物品
        val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            val target = uploadTarget
            uploadTarget = null
            if (uri != null && target != null) {
                if (!vm.uploadItemImage(target, uri)) {
                    snackbarMessage = "图片处理失败，请换一张试试"
                }
            }
        }

        // 全局背景图（「界面」页上传）：长边 2048，铺满整屏，
        // 状态栏时间/电量与底部小白条透明浮在图片上
        val pickBackground = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null && !vm.uploadBackgroundImage(uri)) {
                snackbarMessage = "图片处理失败，请换一张试试"
            }
        }
        val bgImageName by vm.backgroundImage.collectAsState()
        val appContext = androidx.compose.ui.platform.LocalContext.current
        val bgBitmap = remember(bgImageName) {
            bgImageName.takeIf { it.isNotBlank() }?.let { ItemImages.load(appContext, it) }
        }

        // 首页排序（WWDC 弹簧体系下的功能面）：归组/数值/日期统一在 util 里完成
        val sortedItems = remember(items, sortMode, categories) {
            sortItems(items, sortMode, categories)
        }

        // 玻璃结构铁律：layerBackdrop 只挂内容层，玻璃组件是采样源的兄弟节点。
        // 双采样源设计：
        //   backdrop(主) = 背景镜像 + 页面内容 —— 底栏 / 归总卡 / 排序卡折射「整个世界」
        //   bgBackdrop   = 仅背景图（独立层，页面卡片不在其子树内）—— 页面内的框折射「纯背景」
        val backdrop = rememberLayerBackdrop()
        val bgBackdrop = rememberLayerBackdrop()

        Box(
            modifier = Modifier
                .fillMaxSize()
                // 页面背景必须跟随 App 主题（v1 由 Scaffold 提供，重写时一度丢失，
                // 导致手动暗色下页面背景仍用窗口白色背景，出现明暗混搭）
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // ---------- 第 1 层：全局背景图（全屏，含状态栏与小白条区域）----------
            // 暗色模式下叠加淡灰遮罩压低整体亮度，避免浅色背景图在暗色界面里刺眼
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layerBackdrop(bgBackdrop)
            ) {
                if (bgBitmap != null) {
                    Image(
                        bitmap = bgBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (appDark) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.35f))
                        )
                    }
                }
            }

            // ---------- 第 2 层：采样层 = 背景镜像 + 页面内容 ----------
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layerBackdrop(backdrop)
            ) {
                // 背景镜像（同一 bitmap）：玻璃底栏折射出背景图的关键
                if (bgBitmap != null) {
                    Image(
                        bitmap = bgBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (appDark) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.35f))
                        )
                    }
                }
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (displayTab) {
                            Tabs.HOME -> HomeScreen(
                                items = sortedItems,
                                expandedItemId = expandedItemId,
                                onToggleItem = { id ->
                                    vm.expandedItemId.value = if (expandedItemId == id) null else id
                                },
                                onEdit = { vm.startEdit(it) },
                                onDelete = { deleteTarget = it },
                                onUploadImage = { item ->
                                    uploadTarget = item
                                    pickImage.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onSortClick = { vm.sortBubble.value = true },
                                backdrop = bgBackdrop
                            )

                            Tabs.ADD -> AddScreen(
                                editing = editing,
                                categories = categories,
                                backdrop = bgBackdrop,
                                onAddCategory = { vm.addCategory(it) },
                                onRemoveCategory = { vm.removeCategory(it) },
                                onSave = { item ->
                                    val wasEditing = editing != null
                                    vm.saveItem(item)
                                    snackbarMessage = if (wasEditing) "修改已保存，首页清单已更新"
                                    else "已保存，首页清单已更新"
                                },
                                onCancelEdit = { vm.cancelEdit() }
                            )

                            Tabs.FILTER -> FilterScreen(
                                items = items,
                                categories = categories,
                                backdrop = bgBackdrop
                            )

                            Tabs.SETTINGS -> SettingsScreen(
                                themeMode = themeMode,
                                onThemeChange = { vm.settings.setThemeMode(it) },
                                backdrop = bgBackdrop,
                                hasBackground = bgImageName.isNotBlank(),
                                onUploadBackground = {
                                    pickBackground.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onResetBackground = { vm.resetBackgroundImage() }
                            )
                        }

                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }

            // ---------- 液态玻璃底栏（采样层的兄弟节点）----------
            LiquidBottomTabs(
                selectedTabIndex = { bottomTab },
                onTabSelected = { vm.selectTab(it) },
                backdrop = backdrop,
                tabsCount = 5,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 22.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 6.dp)
            ) {
                repeat(5) { index ->
                    LiquidBottomTab(onClick = { vm.selectTab(index) }) {
                        val selected = index == bottomTab
                        val contentColor = when {
                            index == Tabs.ADD -> Tiffany // 「添加」Tiffany 绿高亮，只有图标无文字
                            selected -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                        }
                        LineIcon(
                            type = tabIcon(index),
                            color = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        if (index != Tabs.ADD) {
                            Text(
                                text = tabLabel(index),
                                fontSize = 10.sp,
                                lineHeight = 12.sp, // 收紧行高：上下留白 > 图标文字间距
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = contentColor
                            )
                        }
                    }
                }
            }

            // ---------- 归总玻璃气泡（采样层的兄弟节点，覆盖层）----------
            SummaryBubble(
                visible = bubbleVisible,
                items = items,
                backdrop = backdrop,
                onDismiss = { vm.closeBubble() },
                modifier = Modifier.fillMaxSize()
            )

            // ---------- 首页排序玻璃卡（与归总卡同参数）----------
            SortBubble(
                visible = sortBubbleVisible,
                current = sortMode,
                backdrop = backdrop,
                onSelect = { vm.setSortMode(it) },
                onDismiss = { vm.sortBubble.value = false },
                modifier = Modifier.fillMaxSize()
            )

            deleteTarget?.let { target ->
                AlertDialog(
                    onDismissRequest = { deleteTarget = null },
                    title = { Text("删除物品") },
                    text = { Text("确定删除「${target.name}」？此操作不可撤销。") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                vm.deleteItem(target.id)
                                deleteTarget = null
                                snackbarMessage = "已删除，首页清单已更新"
                            }
                        ) { Text("删除", color = SaveRed) }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteTarget = null }) { Text("取消") }
                    }
                )
            }
        }
    }
}
