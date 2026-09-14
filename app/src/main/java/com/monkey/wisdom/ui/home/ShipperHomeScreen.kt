package com.monkey.wisdom.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.monkey.wisdom.data.model.UserInfo
import com.monkey.wisdom.ui.navigation.AppRoute

/**
 * 货主端首页：智运宝账户、我的运单、发布运单等入口。
 */
@Composable
fun ShipperHomeScreen(
    user: UserInfo,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
) {
    RoleHomeScreen(
        user = user,
        brandTitle = "智运宝 · 货主端",
        subtitle = "发布运单、托管运费，全程可视化跟踪",
        defaultGreeting = "尊敬的货主",
        quickCards = listOf(
            HomeQuickCard(
                badge = "宝",
                title = "智运宝",
                desc = "充值提现 · 运费托管",
                accent = Color(0xFF3B82F6),
                route = AppRoute.ACCOUNT,
            ),
            HomeQuickCard(
                badge = "单",
                title = "我的运单",
                desc = "发布记录 · 运输进度",
                accent = Color(0xFF059669),
                route = AppRoute.PUBLISH_ORDER_LIST,
            ),
        ),
        services = listOf(
            HomeServiceItem(badge = "卡", title = "银行卡管理", accent = Color(0xFF3B82F6), route = AppRoute.BANK_LIST),
            HomeServiceItem(badge = "发", title = "发布运单", accent = Color(0xFF059669), route = AppRoute.PUBLISH_ORDER),
            HomeServiceItem(
                badge = "收",
                title = "收支明细",
                accent = Color(0xFF8B5CF6),
                route = AppRoute.INCOME_EXPENSE_LIST,
            ),
            HomeServiceItem(badge = "信", title = "我的消息", accent = Color(0xFFF59E0B)),
            HomeServiceItem(badge = "更", title = "更多功能", accent = Color(0xFF6B7280)),
        ),
        onNavigate = onNavigate,
        onLogout = onLogout,
    )
}
