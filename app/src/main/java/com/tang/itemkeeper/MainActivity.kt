package com.tang.itemkeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tang.itemkeeper.ui.ItemKeeperApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 边到边：状态栏透明、导航栏（小白条）透明，
        // 全局背景图铺满整屏，时间/电量与小白条自然浮在图片之上
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ItemKeeperApp()
        }
    }
}
