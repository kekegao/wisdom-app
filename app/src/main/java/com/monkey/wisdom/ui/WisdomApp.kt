package com.monkey.wisdom.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.monkey.wisdom.core.constants.UserType
import com.monkey.wisdom.core.storage.UserSession
import com.monkey.wisdom.data.model.UserInfo
import com.monkey.wisdom.ui.account.AccountScreen
import com.monkey.wisdom.ui.account.BankListScreen
import com.monkey.wisdom.ui.account.IncomeExpenseScreen
import com.monkey.wisdom.ui.account.RechargeScreen
import com.monkey.wisdom.ui.account.WithdrawScreen
import com.monkey.wisdom.ui.home.CarrierHomeScreen
import com.monkey.wisdom.ui.home.ShipperHomeScreen
import com.monkey.wisdom.ui.login.LoginScreen
import com.monkey.wisdom.ui.navigation.AppRoute
import com.monkey.wisdom.ui.order.CarrierAcceptOrderScreen
import com.monkey.wisdom.ui.order.CarrierOrderListScreen
import com.monkey.wisdom.ui.order.PublishOrderListScreen
import com.monkey.wisdom.ui.order.PublishOrderScreen

/**
 * 应用组合根。
 *
 * 未登录展示登录页；登录后按 userType 进入货主端 / 承运方端首页，
 * 并通过 Navigation Compose 管理业务流程内跳转。
 */
@Composable
fun WisdomApp() {
    var currentUser by remember { mutableStateOf(UserSession.currentUser) }
    val user = currentUser

    if (user == null) {
        LoginScreen(onLoginSuccess = { loggedInUser -> currentUser = loggedInUser })
    } else {
        MainNavHost(
            user = user,
            onLogout = {
                UserSession.clear()
                currentUser = null
            },
        )
    }
}

@Composable
private fun MainNavHost(
    user: UserInfo,
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()
    val startRoute = if (user.userType == UserType.CARRIER) AppRoute.CARRIER_HOME else AppRoute.SHIPPER_HOME
    val navigate: (String) -> Unit = { route -> navController.navigate(route) }
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = startRoute) {
        composable(AppRoute.SHIPPER_HOME) {
            ShipperHomeScreen(user = user, onNavigate = navigate, onLogout = onLogout)
        }
        composable(AppRoute.CARRIER_HOME) {
            CarrierHomeScreen(user = user, onNavigate = navigate, onLogout = onLogout)
        }

        // ==== 运单 ====
        composable(AppRoute.PUBLISH_ORDER) {
            PublishOrderScreen(
                onBack = back,
                onViewOrders = { navigate(AppRoute.PUBLISH_ORDER_LIST) },
            )
        }
        composable(AppRoute.PUBLISH_ORDER_LIST) {
            PublishOrderListScreen(
                onBack = back,
                onPublish = { navigate(AppRoute.PUBLISH_ORDER) },
            )
        }
        composable(AppRoute.CARRIER_ACCEPT_ORDER) {
            CarrierAcceptOrderScreen(
                onBack = back,
                onViewOrders = { navigate(AppRoute.CARRIER_ORDER_LIST) },
            )
        }
        composable(AppRoute.CARRIER_ORDER_LIST) {
            CarrierOrderListScreen(
                onBack = back,
                onFindSource = { navigate(AppRoute.CARRIER_ACCEPT_ORDER) },
            )
        }

        // ==== 账户 ====
        composable(AppRoute.ACCOUNT) {
            AccountScreen(
                onBack = back,
                onRecharge = { navigate(AppRoute.RECHARGE) },
                onWithdraw = { navigate(AppRoute.WITHDRAW) },
                onBankList = { navigate(AppRoute.BANK_LIST) },
                // 我的运单：按角色进入对应运单列表（货主看发布单，承运方看接单）
                onMyOrder = {
                    navigate(
                        if (user.userType == UserType.CARRIER) {
                            AppRoute.CARRIER_ORDER_LIST
                        } else {
                            AppRoute.PUBLISH_ORDER_LIST
                        },
                    )
                },
            )
        }
        composable(AppRoute.RECHARGE) {
            RechargeScreen(onBack = back, onDone = back)
        }
        composable(AppRoute.WITHDRAW) {
            WithdrawScreen(
                onBack = back,
                onManageBank = { navigate(AppRoute.BANK_LIST) },
                onDone = back,
            )
        }
        composable(AppRoute.BANK_LIST) {
            BankListScreen(onBack = back)
        }
        composable(AppRoute.INCOME_EXPENSE_LIST) {
            IncomeExpenseScreen(onBack = back)
        }
    }
}
