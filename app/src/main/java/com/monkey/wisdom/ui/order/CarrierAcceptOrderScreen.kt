package com.monkey.wisdom.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.monkey.wisdom.ui.components.LoadingBox
import com.monkey.wisdom.ui.components.SectionCard
import com.monkey.wisdom.ui.components.showToast
import com.monkey.wisdom.ui.theme.BgCard
import com.monkey.wisdom.ui.theme.BorderLight
import com.monkey.wisdom.ui.theme.BrandSuccess
import com.monkey.wisdom.ui.theme.TextMuted
import com.monkey.wisdom.ui.theme.TextPrimary
import com.monkey.wisdom.ui.theme.TextSecondary

/**
 * 货源大厅 / 线路搜索页（承运方找货）。
 *
 * 对应 Web 端 `/carrierAcceptOrder`：发货地、收货地关键字检索 + 摘单。
 */
@Composable
fun CarrierAcceptOrderScreen(
    onBack: () -> Unit,
    onViewOrders: () -> Unit,
    viewModel: CarrierAcceptOrderViewModel = viewModel { CarrierAcceptOrderViewModel(ServiceLocator.orderRepository) },
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
            statusText = sourceStatusText(detailOrder.status),
            statusStyle = sourceStatusStyle(detailOrder.status),
            onBack = viewModel::closeDetail,
            actionBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgCard)
                        .padding(12.dp),
                ) {
                    if (detailOrder.status == 1) {
                        Button(
                            onClick = { viewModel.openGrabConfirm(detailOrder) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            enabled = state.grabbingOrderId != detailOrder.orderId,
                        ) {
                            Text(
                                text = if (state.grabbingOrderId == detailOrder.orderId) "摘单中…" else "立即摘单",
                                fontSize = 16.sp,
                            )
                        }
                    } else {
                        Text(
                            text = "已摘单，可联系货主安排运输",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            color = BrandSuccess,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            },
        )
    } else {
        AppScaffold(
            title = "货源大厅",
            onBack = onBack,
            actions = {
                Text(
                    text = "刷新",
                    modifier = Modifier
                        .clickable(enabled = !state.loading) { viewModel.retry() }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                )
            },
        ) { padding ->
            val orders = state.orders
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 线路检索：标签与输入框同行，搜索按钮靠右小尺寸
                item {
                    SectionCard(contentPadding = 12) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "发货地",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                modifier = Modifier.width(52.dp),
                            )
                            RegionInput(
                                value = state.shipperKeyword,
                                onValueChange = { viewModel.onShipperKeywordChange(it) },
                                placeholder = "省市区",
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "收货地",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                modifier = Modifier.width(52.dp),
                            )
                            RegionInput(
                                value = state.carrierKeyword,
                                onValueChange = { viewModel.onCarrierKeywordChange(it) },
                                placeholder = "省市区",
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (state.searched) {
                                Text(
                                    text = "重置",
                                    modifier = Modifier
                                        .clickable { viewModel.resetSearch() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                )
                            }
                            Button(
                                onClick = viewModel::search,
                                modifier = Modifier
                                    .height(34.dp)
                                    .widthIn(min = 80.dp),
                                enabled = !state.loading,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            ) {
                                Text(text = if (state.loading) "…" else "搜索", fontSize = 13.sp)
                            }
                        }
                    }
                }

                when {
                    state.loading && orders.isEmpty() -> item {
                        LoadingBox(modifier = Modifier.height(220.dp), text = "正在查询货源，请稍候")
                    }

                    state.errorMessage != null && orders.isEmpty() -> item {
                        ErrorBox(
                            message = state.errorMessage.orEmpty(),
                            onRetry = viewModel::retry,
                            modifier = Modifier.height(220.dp),
                        )
                    }

                    orders.isEmpty() -> item {
                        EmptyBox(
                            text = state.emptyTitle + "\n" + state.emptyTip,
                            modifier = Modifier.height(220.dp),
                        )
                    }

                    else -> {
                        item {
                            Text(
                                text = (if (state.searched) "线路匹配" else "共") + " ${orders.size} 条货源",
                                color = TextMuted,
                                fontSize = 12.sp,
                            )
                        }
                        items(items = orders, key = { it.orderId }) { order ->
                            SourceOrderCard(
                                order = order,
                                grabbing = state.grabbingOrderId == order.orderId,
                                onDetail = { viewModel.openDetail(order) },
                                onGrab = { viewModel.openGrabConfirm(order) },
                            )
                        }
                    }
                }
            }
        }
    }

    // 摘单二次确认
    val confirmOrder = state.confirmOrder
    if (confirmOrder != null) {
        ConfirmDialog(
            title = "确认摘单",
            message = "是否摘取运单「${confirmOrder.orderId}」？摘单后请及时与货主确认成交，运输期间运费由平台托管。",
            confirmText = "确认摘单",
            dismissText = "再想想",
            onConfirm = viewModel::confirmGrab,
            onDismiss = viewModel::dismissGrabConfirm,
        )
    }
}

/** 货源卡片 */
@Composable
private fun SourceOrderCard(
    order: OrderItem,
    grabbing: Boolean,
    onDetail: () -> Unit,
    onGrab: () -> Unit,
) {
    val statusStyle = sourceStatusStyle(order.status)
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = order.shipperName ?: "货主",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(text = "单号 ${order.orderId}", color = TextSecondary, fontSize = 12.sp)
            }
            StatusBadge(text = sourceStatusText(order.status), style = statusStyle)
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
            description = order.goodsDescription,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = Formatters.time(order.createTime), color = TextMuted, fontSize = 12.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardActionButton(text = "详情", onClick = onDetail)
                if (order.status == 1) {
                    CardActionButton(
                        text = if (grabbing) "摘单中…" else "摘单",
                        primary = true,
                        enabled = !grabbing,
                        onClick = onGrab,
                    )
                } else {
                    Text(
                        text = "✓ 已摘单",
                        color = BrandSuccess,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/**
 * 小尺寸省市区输入框：使用 BasicTextField + 自定义外框，避免 OutlinedTextField
 * 默认内边距导致固定高度后文字/下划线被截断的问题。
 */
@Composable
private fun RegionInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (isFocused) MaterialTheme.colorScheme.primary else BorderLight

    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(20)) },
        modifier = modifier,
        singleLine = true,
        textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                }
                innerTextField()
            }
        },
    )
}
