package com.monkey.wisdom.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monkey.wisdom.core.util.Formatters
import com.monkey.wisdom.data.model.UserInfo
import com.monkey.wisdom.ui.components.ConfirmDialog
import com.monkey.wisdom.ui.components.showToast
import com.monkey.wisdom.ui.theme.BgCard
import com.monkey.wisdom.ui.theme.BgPage
import com.monkey.wisdom.ui.theme.TextMuted
import com.monkey.wisdom.ui.theme.TextPrimary
import com.monkey.wisdom.ui.theme.TextSecondary

/** 首页快捷功能大卡 */
data class HomeQuickCard(
    /** 卡片角标文字（圆形色块内的单字） */
    val badge: String,
    val title: String,
    val desc: String,
    val accent: Color,
    /** 目标路由 */
    val route: String,
)

/** 首页「我的服务」宫格项 */
data class HomeServiceItem(
    val badge: String,
    val title: String,
    val accent: Color,
    /** 为空表示功能建设中 */
    val route: String? = null,
)

/**
 * 货主端 / 承运方端首页共用的页面实现，差异通过列表配置注入，避免重复代码。
 *
 * 布局与 Web 端一致：渐变欢迎区 + 两个功能大卡 + 「我的服务」宫格。
 */
@Composable
fun RoleHomeScreen(
    user: UserInfo,
    brandTitle: String,
    subtitle: String,
    defaultGreeting: String,
    quickCards: List<HomeQuickCard>,
    services: List<HomeServiceItem>,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    headerContent: @Composable (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeroHeader(
            brandTitle = brandTitle,
            greeting = "你好，${Formatters.displayName(user, defaultGreeting)}",
            subtitle = subtitle,
            onLogoutClick = { showLogoutConfirm = true },
        )

        headerContent?.invoke()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            quickCards.forEach { card ->
                QuickCard(
                    card = card,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(card.route) },
                )
            }
        }

        ServiceGrid(
            services = services,
            onItemClick = { item ->
                val route = item.route
                if (route != null) onNavigate(route) else showToast(context, "功能建设中，敬请期待")
            },
        )
    }

    if (showLogoutConfirm) {
        ConfirmDialog(
            title = "退出登录",
            message = "确认退出当前账号吗？",
            confirmText = "退出",
            onConfirm = {
                showLogoutConfirm = false
                onLogout()
            },
            onDismiss = { showLogoutConfirm = false },
        )
    }
}

/** 顶部渐变欢迎区 */
@Composable
private fun HeroHeader(
    brandTitle: String,
    greeting: String,
    subtitle: String,
    onLogoutClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6))))
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x29FFFFFF))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0x38FFFFFF)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "宝", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = brandTitle,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Text(
                    text = "退出登录",
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onLogoutClick)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    color = Color.White,
                    fontSize = 12.sp,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = greeting,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = Color(0xEBFFFFFF),
                fontSize = 13.sp,
            )
        }
    }
}

/** 快捷功能大卡 */
@Composable
private fun QuickCard(
    card: HomeQuickCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(card.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = card.badge, color = card.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = card.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(text = card.desc, color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

/** 「我的服务」宫格 */
@Composable
private fun ServiceGrid(
    services: List<HomeServiceItem>,
    onItemClick: (HomeServiceItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .padding(horizontal = 14.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "我的服务", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            services.chunked(SERVICE_COLUMNS).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowItems.forEach { item ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onItemClick(item) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(item.accent.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = item.badge,
                                    color = item.accent,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Text(text = item.title, color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    // 补齐占位，保证网格对齐
                    repeat(SERVICE_COLUMNS - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(1.dp))
    }
}

private const val SERVICE_COLUMNS = 4
