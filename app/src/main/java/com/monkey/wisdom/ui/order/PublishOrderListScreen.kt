package com.monkey.wisdom.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monkey.wisdom.core.util.Formatters
import com.monkey.wisdom.data.model.OrderItem
import com.monkey.wisdom.di.ServiceLocator
import com.monkey.wisdom.ui.components.AppScaffold
import com.monkey.wisdom.ui.components.ConfirmDialog
import com.monkey.wisdom.ui.components.EmptyBox
import com.monkey.wisdom.ui.components.ErrorBox
import com.monkey.wisdom.ui.components.InfoRow
import com.monkey.wisdom.ui.components.LoadingBox
import com.monkey.wisdom.ui.components.SectionCard
import com.monkey.wisdom.ui.components.showToast
import com.monkey.wisdom.ui.theme.BgCard
import com.monkey.wisdom.ui.theme.TextMuted
import com.monkey.wisdom.ui.theme.TextPrimary
import com.monkey.wisdom.ui.theme.TextSecondary

/**
 * 货主「我的订单」页：状态筛选、统计、运单操作与详情查看。
 *
 * 对应 Web 端 `/publishOrderList`。
 */
@Composable
fun PublishOrderListScreen(
    onBack: () -> Unit,
    onPublish: () -> Unit,
    viewModel: PublishOrderListViewModel = viewModel { PublishOrderListViewModel(ServiceLocator.orderRepository) },
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
            statusText = shipperStatusText(detailOrder.status, detailOrder.statusDesc),
            statusStyle = shipperStatusStyle(detailOrder.status),
            onBack = viewModel::closeDetail,
            actionBar = {
                ShipperDetailActions(
                    order = detailOrder,
                    submitting = state.submitting,
                    onAction = { action -> viewModel.openConfirm(action, detailOrder) },
                )
            },
        )
    } else {
        AppScaffold(
            title = "我的订单",
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
                        title = "已发布订单",
                        total = state.total,
                        stats = listOf(
                            "待接单" to state.waitingCount,
                            "进行中" to state.ongoingCount,
                            "已完成" to state.doneCount,
                        ),
                        actionText = "＋ 发布",
                        onAction = onPublish,
                    )
                }

                item {
                    FilterTabs(
                        labels = state.tabs.map { it.label },
                        selectedIndex = state.activeTabIndex,
                        onSelect = viewModel::selectTab,
                    )
                }

                when {
                    state.loading && orders.isEmpty() -> item {
                        LoadingBox(modifier = Modifier.height(220.dp), text = "正在从服务器查询您的发布订单")
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
                            text = "暂无相关订单\n切换到其他状态，或点击卡片上的「发布」按钮发布新订单",
                            actionText = "去发布运单",
                            onAction = onPublish,
                            modifier = Modifier.height(220.dp),
                        )
                    }

                    else -> {
                        item {
                            Text(
                                text = "共 ${orders.size} 条记录",
                                color = TextMuted,
                                fontSize = 12.sp,
                            )
                        }
                        items(items = orders, key = { it.orderId }) { order ->
                            ShipperOrderCard(
                                order = order,
                                onDetail = { viewModel.openDetail(order) },
                                onAction = { action -> viewModel.openConfirm(action, order) },
                            )
                        }
                    }
                }
            }
        }
    }

    // ==== 二次确认弹窗 ====
    val confirmAction = state.confirmAction
    val confirmOrder = state.confirmOrder
    if (confirmAction != null && confirmOrder != null) {
        ConfirmDialog(
            title = confirmAction.title,
            message = viewModel.confirmTip(confirmAction, confirmOrder),
            confirmText = if (state.submitting) "提交中…" else confirmAction.buttonText,
            dismissText = "再想想",
            confirmColor = if (confirmAction == OrderConfirmAction.CANCEL_ACCEPT) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
            onConfirm = viewModel::submitConfirm,
            onDismiss = viewModel::dismissConfirm,
        )
    }
}

/** 货主运单卡片 */
@Composable
private fun ShipperOrderCard(
    order: OrderItem,
    onDetail: () -> Unit,
    onAction: (OrderConfirmAction) -> Unit,
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "单号 ${order.orderId}", color = TextSecondary, fontSize = 12.sp)
            StatusBadge(
                text = shipperStatusText(order.status, order.statusDesc),
                style = shipperStatusStyle(order.status),
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
            Text(text = Formatters.time(order.createTime), color = TextMuted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (order.status) {
                    2 -> {
                        CardActionButton(text = "成交", primary = true, onClick = { onAction(OrderConfirmAction.DEAL) })
                        CardActionButton(text = "取消", danger = true, onClick = { onAction(OrderConfirmAction.CANCEL_ACCEPT) })
                    }

                    5 -> CardActionButton(text = "回单确认", primary = true, onClick = { onAction(OrderConfirmAction.RECEIPT_CONFIRM) })
                    6 -> CardActionButton(text = "结算申请", primary = true, onClick = { onAction(OrderConfirmAction.SETTLE_APPLY) })
                }
                CardActionButton(text = "查看详情", onClick = onDetail)
            }
        }
    }
}

/** 详情页底部操作栏（货主视角） */
@Composable
private fun ShipperDetailActions(
    order: OrderItem,
    submitting: Boolean,
    onAction: (OrderConfirmAction) -> Unit,
) {
    val actions = when (order.status) {
        2 -> listOf(
            OrderConfirmAction.DEAL to true,
            OrderConfirmAction.CANCEL_ACCEPT to false,
        )

        5 -> listOf(OrderConfirmAction.RECEIPT_CONFIRM to true)
        6 -> listOf(OrderConfirmAction.SETTLE_APPLY to true)
        else -> emptyList()
    }
    if (actions.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        actions.forEach { (action, primary) ->
            if (primary) {
                androidx.compose.material3.Button(
                    onClick = { onAction(action) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    enabled = !submitting,
                ) {
                    Text(text = action.buttonText.removePrefix("确认"), fontSize = 15.sp)
                }
            } else {
                androidx.compose.material3.OutlinedButton(
                    onClick = { onAction(action) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    enabled = !submitting,
                ) {
                    Text(text = "取消摘单", fontSize = 15.sp, color = Color(0xFFDC2626))
                }
            }
        }
    }
}

/**
 * 运单详情页（货主 / 承运方共用布局）。
 */
@Composable
fun OrderDetailScreen(
    order: OrderItem,
    statusText: String,
    statusStyle: StatusStyle,
    onBack: () -> Unit,
    actionBar: @Composable () -> Unit = {},
    showShipperContact: Boolean = true,
) {
    AppScaffold(title = "订单详情", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 状态 + 线路概览
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgCard)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusBadge(text = statusText, style = statusStyle)
                        Text(
                            text = "${Formatters.time(order.createTime)} 发布",
                            color = TextMuted,
                            fontSize = 12.sp,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = displayOrDash(order.shipperCity),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "  →  ",
                            color = TextMuted,
                            fontSize = 16.sp,
                        )
                        Text(
                            text = displayOrDash(order.carrierCity),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    GoodsSummaryRow(
                        goodsType = order.goodsType,
                        weightText = orderWeightText(order.goodsWeight),
                        feeText = orderFeeText(order.transportMoney),
                    )
                }

                // 运输路线
                SectionCard {
                    Text(text = "运输路线", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    OrderRouteBlock(
                        modifier = Modifier.padding(top = 12.dp),
                        shipperRegion = regionText(order.shipperProvince, order.shipperCity, order.shipperArea),
                        shipperAddress = order.shipperAddress,
                        carrierRegion = regionText(order.carrierProvince, order.carrierCity, order.carrierArea),
                        carrierAddress = order.carrierAddress,
                    )
                    if (showShipperContact && !order.shipperName.isNullOrBlank()) {
                        Text(
                            text = "联系人：${order.shipperName} ${Formatters.maskMobile(order.shipperMobile)}",
                            modifier = Modifier.padding(top = 12.dp),
                            color = TextSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }

                // 订单信息
                SectionCard {
                    Text(text = "订单信息", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        InfoRow(label = "运单号", value = displayOrDash(order.orderId))
                        InfoRow(label = "物品类型", value = displayOrDash(order.goodsType))
                        InfoRow(label = "物品重量", value = orderWeightText(order.goodsWeight))
                        InfoRow(label = "运费", value = orderFeeText(order.transportMoney))
                        InfoRow(label = "物品描述", value = displayOrDash(order.goodsDescription), valueBold = false)
                        InfoRow(label = "承运方", value = displayOrDash(order.carrierName ?: order.carrierUserName))
                        InfoRow(label = "更新时间", value = Formatters.time(order.updateTime))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(12.dp),
                ) {
                    Text(
                        text = "如对订单有疑问，请及时联系平台客服处理",
                        color = Color(0xFF92400E),
                        fontSize = 12.sp,
                    )
                }
            }

            actionBar()
        }
    }
}
