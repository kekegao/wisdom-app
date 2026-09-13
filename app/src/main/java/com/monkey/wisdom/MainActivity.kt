package com.monkey.wisdom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.monkey.wisdom.ui.WisdomApp
import com.monkey.wisdom.ui.theme.WisdomTheme

/**
 * 应用唯一宿主 Activity（单 Activity + Compose 架构）。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WisdomTheme {
                WisdomApp()
            }
        }
    }
}
