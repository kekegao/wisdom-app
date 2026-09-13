package com.monkey.wisdom.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monkey.wisdom.ui.theme.BgCard
import com.monkey.wisdom.ui.theme.BgPage
import com.monkey.wisdom.ui.theme.TextPrimary

/**
 * 业务页统一骨架：白底顶栏 + 返回按钮 + 右侧操作区 + 灰底内容区。
 *
 * 统一封装可保证各页面状态栏内边距、返回行为与配色一致。
 * 返回按钮使用文本箭头，避免为少量图标引入 material-icons 依赖。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        Text(
                            text = "←",
                            modifier = Modifier
                                .clickable(onClick = onBack)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            fontSize = 22.sp,
                            color = TextPrimary,
                        )
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgCard),
            )
        },
        containerColor = BgPage,
        content = content,
    )
}
