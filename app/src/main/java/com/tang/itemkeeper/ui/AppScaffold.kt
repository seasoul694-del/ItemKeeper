package com.tang.itemkeeper.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tang.itemkeeper.model.Item
import com.tang.itemkeeper.ui.components.CircleSearchBar
import com.tang.itemkeeper.ui.components.LiquidBottomBar
import com.tang.itemkeeper.ui.components.SummaryBubble
import com.tang.itemkeeper.ui.screens.AddScreen
import com.tang.itemkeeper.ui.screens.FilterScreen
import com.tang.itemkeeper.ui.screens.HomeScreen
import com.tang.itemkeeper.ui.screens.SettingsScreen
import com.tang.itemkeeper.ui.theme.ItemKeeperTheme
import com.tang.itemkeeper.ui.theme.MotionCurve
import com.tang.itemkeeper.ui.theme.SaveRed
import com.tang.itemkeeper.ui.theme.Space
import com.tang.itemkeeper.viewmodel.MainViewModel
import com.tang.itemkeeper.viewmodel.Tabs
import kotlinx.coroutines.delay

@Composable
fun ItemKeeperApp(vm: MainViewModel = viewModel()) {
    val themeMode by vm.settings.themeMode.collectAsState()

    ItemKeeperTheme(mode = themeMode) {
        val currentTab by vm.currentTab.collectAsState()
        val previousTab by vm.previousTab.collectAsState()
        val bubbleVisible by vm.summaryBubble.collectAsState()
        val items by vm.items.collectAsState()
        val categories by vm.categories.collectAsState()
        val editing by vm.editingItem.collectAsState()
        val listExpanded by vm.homeListExpanded.collectAsState()
        val expandedItemId by vm.expandedItemId.collectAsState()
        val query by vm.searchQuery.collectAsState()
        val searchExpanded by vm.searchExpanded.collectAsState()
        val scrollProgress by vm.homeScrollProgress.collectAsState()

        // 气泡弹出时内容保持在上一页，底栏特效停在「归总」
        val displayTab = if (bubbleVisible) previousTab else currentTab
        val showSearch = displayTab == Tabs.HOME

        // 点击展开：360ms 同一条曲线；滚动收起：直接取滚动进度，不另起动画
        val expand by animateFloatAsState(
            targetValue = if (searchExpanded && showSearch) 1f else 0f,
            animationSpec = tween(durationMillis = 360, easing = MotionCurve),
            label = "circle-to-search"
        )
        val collapse = if (showSearch) scrollProgress else 0f
        val searchProgress = (expand * (1f - collapse)).coerceIn(0f, 1f)
        val titleSize = (20f - 5f * collapse).sp
        val titleAlpha = 1f - searchProgress * 0.92f

        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(searchExpanded) {
            if (searchExpanded) {
                delay(360)
                focusRequester.requestFocus()
            }
        }

        var deleteTarget by remember { mutableStateOf<Item?>(null) }
        var snackbarMessage by remember { mutableStateOf("") }
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(snackbarMessage) {
            if (snackbarMessage.isNotBlank()) {
                snackbarHostState.showSnackbar(snackbarMessage)
                snackbarMessage = ""
            }
        }

        val visibleItems = remember(items, query) {
            items.filter { vm.matchesSearch(it, query) }
        }

        fun toggleSearch() {
            val next = !searchExpanded
            vm.searchExpanded.value = next
            if (!next) vm.searchQuery.value = ""
        }

        val title = when (displayTab) {
            Tabs.HOME -> "首页"
            Tabs.ADD -> "添加"
            Tabs.FILTER -> "归类"
            Tabs.SETTINGS -> "ItemKeeper"
            else -> "首页"
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.surface,
                topBar = {
                    // 无阴影、无分隔线，顶栏与内容同为纯色背景
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .height(72.dp)
                                .padding(horizontal = Space.x2)
                        ) {
                            Text(
                                text = title,
                                fontSize = titleSize,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .alpha(titleAlpha)
                            )
                            if (showSearch) {
                                CircleSearchBar(
                                    progress = searchProgress,
                                    query = query,
                                    onQueryChange = { vm.searchQuery.value = it },
                                    onToggle = { toggleSearch() },
                                    focusRequester = focusRequester,
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                )
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    LiquidBottomBar(
                        selectedIndex = currentTab,
                        onSelect = { vm.selectTab(it) }
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (displayTab) {
                        Tabs.HOME -> HomeScreen(
                            items = visibleItems,
                            listExpanded = listExpanded,
                            expandedItemId = expandedItemId,
                            onToggleList = { vm.homeListExpanded.value = !listExpanded },
                            onToggleItem = { id ->
                                vm.expandedItemId.value = if (expandedItemId == id) null else id
                            },
                            onEdit = { vm.startEdit(it) },
                            onDelete = { deleteTarget = it },
                            onScrollProgress = { vm.setHomeScrollProgress(it) }
                        )

                        Tabs.ADD -> AddScreen(
                            editing = editing,
                            categories = categories,
                            onAddCategory = { vm.addCategory(it) },
                            onSave = { item ->
                                val wasEditing = editing != null
                                vm.saveItem(item)
                                snackbarMessage = if (wasEditing) "修改已保存，首页清单已更新"
                                else "已保存，首页清单已更新"
                            },
                            onCancelEdit = { vm.cancelEdit() }
                        )

                        Tabs.FILTER -> FilterScreen(items = items, categories = categories)

                        Tabs.SETTINGS -> SettingsScreen(
                            themeMode = themeMode,
                            onThemeChange = { vm.settings.setThemeMode(it) }
                        )
                    }
                }
            }

            // 归总玻璃气泡：点卡片外自然弹回，底栏特效回到上一个按钮
            SummaryBubble(
                visible = bubbleVisible,
                items = items,
                onDismiss = { vm.closeBubble() },
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
