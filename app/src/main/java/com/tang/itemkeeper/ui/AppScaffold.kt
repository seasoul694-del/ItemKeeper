package com.tang.itemkeeper.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tang.itemkeeper.model.Item
import com.tang.itemkeeper.ui.screens.AddScreen
import com.tang.itemkeeper.ui.screens.FilterScreen
import com.tang.itemkeeper.ui.screens.HomeScreen
import com.tang.itemkeeper.ui.screens.SettingsScreen
import com.tang.itemkeeper.ui.theme.ItemKeeperTheme
import com.tang.itemkeeper.ui.theme.SaveRed
import com.tang.itemkeeper.viewmodel.MainViewModel

private val TAB_LABELS = listOf("首页", "添加", "归类", "界面")
private val TAB_GLYPHS = listOf("▣", "＋", "▤", "◈")

/** 应用根：单 Activity + 四栏导航，编辑 / 删除直接生效，无权限门禁 */
@Composable
fun ItemKeeperApp(vm: MainViewModel = viewModel()) {
    val themeMode by vm.settings.themeMode.collectAsState()

    ItemKeeperTheme(mode = themeMode) {
        val tab by vm.currentTab.collectAsState()
        val items by vm.items.collectAsState()
        val categories by vm.categories.collectAsState()
        val editing by vm.editingItem.collectAsState()
        val listExpanded by vm.homeListExpanded.collectAsState()
        val statsExpanded by vm.homeStatsExpanded.collectAsState()

        var deleteTarget by remember { mutableStateOf<Item?>(null) }
        var snackbarMessage by remember { mutableStateOf("") }
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(snackbarMessage) {
            if (snackbarMessage.isNotBlank()) {
                snackbarHostState.showSnackbar(snackbarMessage)
                snackbarMessage = ""
            }
        }

        Scaffold(
            topBar = {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("物志", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = TAB_LABELS[tab],
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar {
                    TAB_LABELS.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = {
                                // 手动进入添加页时清除编辑态（从首页发起的编辑不受影响）
                                if (index == 1 && tab != 1) vm.clearEditing()
                                vm.setTab(index)
                            },
                            icon = { Text(TAB_GLYPHS[index], fontSize = 18.sp) },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (tab) {
                    0 -> HomeScreen(
                        items = items,
                        listExpanded = listExpanded,
                        statsExpanded = statsExpanded,
                        onToggleList = { vm.homeListExpanded.value = !listExpanded },
                        onToggleStats = { vm.homeStatsExpanded.value = !statsExpanded },
                        onEdit = { item -> vm.startEdit(item) },
                        onDelete = { item -> deleteTarget = item }
                    )

                    1 -> AddScreen(
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

                    2 -> FilterScreen(items = items, categories = categories)

                    3 -> SettingsScreen(
                        themeMode = themeMode,
                        onThemeChange = { vm.settings.setThemeMode(it) }
                    )
                }
            }
        }

        // 删除二次确认
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
