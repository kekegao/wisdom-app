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
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.monkey.wisdom.ui.components.ErrorDialog
import com.monkey.wisdom.ui.components.InfoRow
import com.monkey.wisdom.ui.components.LoadingBox
import com.monkey.wisdom.ui.components.SectionCard
import com.monkey.wisdom.ui.components.SuccessDialog
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

    // 后端接口返回成功：弹窗提示，用户点「确定」后关闭
    SuccessDialog(message = state.successMessage, onDismiss = viewModel::consumeSuccess)

    // 进入页面或从发布页返回时刷新，保证刚发布的运单立即可见
    LaunchedEffect(Unit) { viewModel.loadOrders() }

    // 运单操作（成交 / 取消摘单 / 回单确认 / 结算申请）失败：弹窗提示
    ErrorDialog(message = state.errorDialog, onDismiss = viewModel::consumeErrorDialog)

    val detailOrder = state.detailOrder
    if (detailOrder != null) {
        OrderDetailScreen(
            order = detailOrder,
            statusText = shipperStatusText(detailOrder.status, detailOrder.statusDesc),
            statusStyle = shipperStatusStyle(detailOrder.status),
            onBack = viewModel::closeDetail,
            showCarrierMobile = true,
            actionBar = {
                ShipperDetailActions(
                    order = detailOrder,
                    submitting = state.submitting,
                    onAction = { action -> viewModel.openConfirm(action, detailOrder) },
                    onChangePrice = { viewModel.openChangePrice(detailOrder) },
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
                                onChangePrice = { viewModel.openChangePrice(order) },
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

    // ==== 改价弹窗（仅已发布待摘单运单） ====
    val changePriceOrder = state.changePriceOrder
    if (changePriceOrder != null) {
        ChangePriceDialog(
            order = changePriceOrder,
            input = state.changePriceInput,
            submitting = state.submitting,
            error = state.changePriceError,
            onInputChange = viewModel::onChangePriceInputChange,
            onConfirm = viewModel::submitChangePrice,
            onDismiss = viewModel::dismissChangePrice,
        )
    }
}

/**
 * 改价弹窗：输入新运费，后端同步调整运费托管冻结金额。
 */
@Composable
private fun ChangePriceDialog(
    order: OrderItem,
    input: String,
    submitting: Boolean,
    error: String?,
    onInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "修改运费",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "运单号：${order.orderId}", color = TextSecondary, fontSize = 13.sp)
                Text(
                    text = "当前运费：${orderFeeText(order.transportMoney)}",
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(text = "请输入新的运费金额", fontSize = 14.sp) },
                    suffix = { Text(text = "元", fontSize = 14.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                )
                Text(
                    text = "改价后平台将按新运费同步冻结金额（涨价补冻结、降价释放差额）",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
                if (error != null) {
                    Text(text = error, color = Color(0xFFDC2626), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !submitting) {
                Text(text = if (submitting) "提交中…" else "确定", fontSize = 15.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消", fontSize = 15.sp, color = TextSecondary)
            }
        },
        containerColor = BgCard,
    )
}

/** 货主运单卡片 */
@Composable
private fun ShipperOrderCard(
    order: OrderItem,
    onDetail: () -> Unit,
    onAction: (OrderConfirmAction) -> Unit,
    onChangePrice: () -> Unit,
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
                    // 已发布待摘单：尚未被摘单，允许修改运费
                    1 -> CardActionButton(text = "改价", primary = true, onClick = onChangePrice)

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
    onChangePrice: () -> Unit,
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
    // 已发布待摘单：尚未被摘单，允许修改运费
    val canChangePrice = order.status == 1
    if (actions.isEmpty() && !canChangePrice) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (canChangePrice) {
            androidx.compose.material3.OutlinedButton(
                onClick = onChangePrice,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                enabled = !submitting,
            ) {
                Text(text = "修改价格", fontSize = 15.sp)
            }
        }
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
    showCallButton: Boolean = false,
    showCarrierMobile: Boolean = false,
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
                    val contactName = order.shipperName ?: order.shipperUserName
                    if (showShipperContact && (!contactName.isNullOrBlank() || !order.shipperMobile.isNullOrBlank())) {
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "联系人：${displayOrDash(contactName)} ${if (showCallButton) order.shipperMobile.orEmpty() else Formatters.maskMobile(order.shipperMobile)}",
                                modifier = Modifier.weight(1f),
                                color = TextSecondary,
                                fontSize = 13.sp,
                            )
                            if (showCallButton && !order.shipperMobile.isNullOrBlank()) {
                                val context = LocalContext.current
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.shipperMobile}"))
                                        context.startActivity(intent)
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "拨打电话",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
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
                        if (showCarrierMobile && !order.carrierMobile.isNullOrBlank()) {
                            InfoRow(label = "承运方电话", value = order.carrierMobile)
                        }
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
