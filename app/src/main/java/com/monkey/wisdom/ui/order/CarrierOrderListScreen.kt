package com.monkey.wisdom.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monkey.wisdom.core.constants.OrderStatus
import com.monkey.wisdom.core.util.Formatters
import com.monkey.wisdom.data.model.OrderItem
import com.monkey.wisdom.di.ServiceLocator
import com.monkey.wisdom.ui.components.AppScaffold
import com.monkey.wisdom.ui.components.ConfirmDialog
import com.monkey.wisdom.ui.components.EmptyBox
import com.monkey.wisdom.ui.components.ErrorBox
import com.monkey.wisdom.ui.components.LoadingBox
import com.monkey.wisdom.ui.components.SectionCard
import com.monkey.wisdom.ui.components.showToast
import com.monkey.wisdom.ui.theme.BgCard
import com.monkey.wisdom.ui.theme.TextMuted
import com.monkey.wisdom.ui.theme.TextSecondary

/**
 * 承运方「我的运单」页：进度筛选、确认发货 / 确认收货。
 *
 * 对应 Web 端 `/carrierOrderList`。
 */
@Composable
fun CarrierOrderListScreen(
    onBack: () -> Unit,
    onFindSource: () -> Unit,
    viewModel: CarrierOrderListViewModel = viewModel { CarrierOrderListViewModel(ServiceLocator.orderRepository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { message ->
            showToast(context, message)
            viewModel.consumeToast()
        }
    }

    val detailOrder = state.detailOrder
    if (detailOrder != null) {
        OrderDetailScreen(
            order = detailOrder,
            statusText = carrierOrderStatusText(detailOrder.status, detailOrder.statusDesc),
            statusStyle = carrierOrderStatusStyle(detailOrder.status),
            onBack = viewModel::closeDetail,
            showCallButton = true,
            actionBar = {
                // 与后端流转一致：成交(3)→发货(4) 确认发货；发货(4)→确认收货(5) 确认收货；结算申请(7)→对账(9) 对账
                val action = when (detailOrder.status) {
                    OrderStatus.DEALED.code -> CarrierConfirmAction.SHIP
                    OrderStatus.SHIPPED.code -> CarrierConfirmAction.RECEIPT
                    OrderStatus.SETTLE_APPLIED.code -> CarrierConfirmAction.RECONCILE
                    else -> null
                }
                if (action != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BgCard)
                            .padding(12.dp),
                    ) {
                        Button(
                            onClick = { viewModel.openConfirm(action, detailOrder) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            colors = if (action == CarrierConfirmAction.RECONCILE) {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF97316),
                                    contentColor = Color.White,
                                )
                            } else {
                                ButtonDefaults.buttonColors()
                            },
                        ) {
                            Text(text = action.buttonText, fontSize = 16.sp)
                        }
                    }
                }
            },
        )
    } else {
        AppScaffold(
            title = "我的运单",
            onBack = onBack,
            actions = {
                Text(
                    text = "刷新",
                    modifier = Modifier
                        .clickable(enabled = !state.loading) { viewModel.loadOrders() }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                )
            },
        ) { padding ->
            val orders = state.visibleOrders
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    OrderStatCard(
                        title = "我的运单",
                        total = state.total,
                        stats = listOf("进行中" to state.runningCount, "已完成" to state.doneCount),
                        actionText = "去摘单",
                        onAction = onFindSource,
                    )
                }

                item {
                    FilterTabs(
                        labels = CarrierOrderTab.entries.map { it.label },
                        selectedIndex = state.activeTabIndex,
                        onSelect = viewModel::selectTab,
                    )
                }

                when {
                    state.loading && orders.isEmpty() -> item {
                        LoadingBox(modifier = Modifier.height(220.dp), text = "正在加载您的运单")
                    }

                    state.errorMessage != null && orders.isEmpty() -> item {
                        ErrorBox(
                            message = state.errorMessage.orEmpty(),
                            onRetry = viewModel::loadOrders,
                            modifier = Modifier.height(220.dp),
                        )
                    }

                    orders.isEmpty() -> item {
                        EmptyBox(
                            text = "暂无相关运单\n去「货源大厅」摘单后即可开始运输",
                            actionText = "去货源大厅",
                            onAction = onFindSource,
                            modifier = Modifier.height(220.dp),
                        )
                    }

                    else -> {
                        item {
                            Text(text = "共 ${orders.size} 条记录", color = TextMuted, fontSize = 12.sp)
                        }
                        items(items = orders, key = { it.orderId }) { order ->
                            CarrierOrderCard(
                                order = order,
                                submitting = state.submitting,
                                onDetail = { viewModel.openDetail(order) },
                                onConfirm = { action -> viewModel.openConfirm(action, order) },
                            )
                        }
                    }
                }
            }
        }
    }

    val confirmAction = state.confirmAction
    val confirmOrder = state.confirmOrder
    if (confirmAction != null && confirmOrder != null) {
        ConfirmDialog(
            title = confirmAction.title,
            message = "运单号：${confirmOrder.orderId}\n${confirmAction.tip}",
            confirmText = if (state.submitting) "提交中…" else confirmAction.buttonText,
            dismissText = "再想想",
            onConfirm = viewModel::submitConfirm,
            onDismiss = viewModel::dismissConfirm,
        )
    }
}

/** 承运端运单卡片 */
@Composable
private fun CarrierOrderCard(
    order: OrderItem,
    submitting: Boolean,
    onDetail: () -> Unit,
    onConfirm: (CarrierConfirmAction) -> Unit,
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "单号 ${order.orderId}", color = TextSecondary, fontSize = 12.sp)
                Text(
                    text = "货主 ${displayOrDash(order.shipperName ?: order.shipperUserName)} ${Formatters.maskMobile(order.shipperMobile)}",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
            }
            StatusBadge(
                text = carrierOrderStatusText(order.status, order.statusDesc),
                style = carrierOrderStatusStyle(order.status),
            )
        }

        OrderRouteBlock(
            modifier = Modifier.padding(top = 12.dp),
            shipperRegion = regionText(order.shipperProvince, order.shipperCity, order.shipperArea),
            shipperAddress = order.shipperAddress,
            carrierRegion = regionText(order.carrierProvince, order.carrierCity, order.carrierArea),
            carrierAddress = order.carrierAddress,
        )

        GoodsSummaryRow(
            modifier = Modifier.padding(top = 12.dp),
            goodsType = order.goodsType,
            weightText = orderWeightText(order.goodsWeight),
            feeText = orderFeeText(order.transportMoney),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = Formatters.time(order.updateTime ?: order.createTime), color = TextMuted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (order.status) {
                    // 成交(3)：等待承运方确认发货
                    OrderStatus.DEALED.code -> CardActionButton(
                        text = "确认发货",
                        primary = true,
                        enabled = !submitting,
                        onClick = { onConfirm(CarrierConfirmAction.SHIP) },
                    )

                    // 发货(4)：运输中，到货后由承运方确认收货
                    OrderStatus.SHIPPED.code -> CardActionButton(
                        text = "确认收货",
                        primary = true,
                        enabled = !submitting,
                        onClick = { onConfirm(CarrierConfirmAction.RECEIPT) },
                    )

                    OrderStatus.SETTLE_APPLIED.code -> CardActionButton(
                        text = "对账",
                        highlight = true,
                        enabled = !submitting,
                        onClick = { onConfirm(CarrierConfirmAction.RECONCILE) },
                    )
                }
                CardActionButton(text = "查看详情", onClick = onDetail)
            }
        }
    }
}
