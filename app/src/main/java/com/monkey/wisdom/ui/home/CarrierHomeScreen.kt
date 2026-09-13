package com.monkey.wisdom.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monkey.wisdom.data.model.UserInfo
import com.monkey.wisdom.di.ServiceLocator
import com.monkey.wisdom.ui.navigation.AppRoute
import com.monkey.wisdom.ui.order.OrderStatCard

/**
 * 承运方端首页：货源大厅摘单、我的运单、银行卡等入口。
 */
@Composable
fun CarrierHomeScreen(
    user: UserInfo,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: CarrierHomeViewModel = viewModel { CarrierHomeViewModel(ServiceLocator.orderRepository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    RoleHomeScreen(
        user = user,
        brandTitle = "智运宝 · 承运方端",
        subtitle = "海量货源在线摘单，承接运输全程可视",
        defaultGreeting = "尊敬的承运方",
        quickCards = listOf(
            HomeQuickCard(
                badge = "宝",
                title = "智运宝",
                desc = "充值提现 · 运费结算",
                accent = Color(0xFF3B82F6),
                route = AppRoute.ACCOUNT,
            ),
            HomeQuickCard(
                badge = "摘",
                title = "我去摘单",
                desc = "货源大厅 · 抢单接单",
                accent = Color(0xFF059669),
                route = AppRoute.CARRIER_ACCEPT_ORDER,
            ),
        ),
        services = listOf(
            HomeServiceItem(badge = "卡", title = "银行卡管理", accent = Color(0xFF3B82F6), route = AppRoute.BANK_LIST),
            HomeServiceItem(badge = "单", title = "我的运单", accent = Color(0xFF059669), route = AppRoute.CARRIER_ORDER_LIST),
            HomeServiceItem(badge = "信", title = "我的消息", accent = Color(0xFFF59E0B)),
            HomeServiceItem(badge = "更", title = "更多功能", accent = Color(0xFF6B7280)),
        ),
        onNavigate = onNavigate,
        onLogout = onLogout,
        headerContent = {
            OrderStatCard(
                title = "今日货源",
                total = state.total,
                stats = listOf("可摘单" to state.openCount, "已摘单" to state.acceptedCount),
                actionText = "我的运单",
                onAction = { onNavigate(AppRoute.CARRIER_ORDER_LIST) },
            )
        },
    )
}
