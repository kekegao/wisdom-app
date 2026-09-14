package com.monkey.wisdom.data.repository

import com.google.gson.JsonElement
import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.core.common.safeDataCall
import com.monkey.wisdom.core.common.safeUnitCall
import com.monkey.wisdom.data.model.ApiResult
import com.monkey.wisdom.data.model.OrderItem
import com.monkey.wisdom.data.model.request.OrderListQuery
import com.monkey.wisdom.data.model.request.OrderOperateRequest
import com.monkey.wisdom.data.model.request.PublishOrderRequest
import com.monkey.wisdom.data.model.request.SourceOrderQuery
import com.monkey.wisdom.data.remote.api.OrderApi

/**
 * 运单数据仓库实现。
 */
class OrderRepositoryImpl(
    private val orderApi: OrderApi,
) : OrderRepository {

    override suspend fun publishOrder(request: PublishOrderRequest): AppResult<Unit> =
        safeUnitCall(fallbackMessage = "发布失败，请稍后重试") { orderApi.publishOrder(request) }

    override suspend fun queryPublishedOrders(query: OrderListQuery): AppResult<List<OrderItem>> =
        safeDataCall(fallbackMessage = "加载运单列表失败") { orderApi.queryPublishedOrders(query) }

    override suspend fun querySourceOrders(query: SourceOrderQuery): AppResult<List<OrderItem>> =
        safeDataCall(fallbackMessage = "加载货源大厅失败") { orderApi.querySourceOrders(query) }

    override suspend fun queryCarrierOrders(query: OrderListQuery): AppResult<List<OrderItem>> =
        safeDataCall(fallbackMessage = "加载我的运单失败") { orderApi.queryCarrierOrders(query) }

    override suspend fun dealOrder(orderId: String): AppResult<Unit> =
        operate(orderId, "确认成交失败") { orderApi.dealOrder(it) }

    override suspend fun cancelAccept(orderId: String): AppResult<Unit> =
        operate(orderId, "取消摘单失败") { orderApi.cancelAccept(it) }

    override suspend fun receiptConfirm(orderId: String): AppResult<Unit> =
        operate(orderId, "回单确认失败") { orderApi.receiptConfirm(it) }

    override suspend fun settleApply(orderId: String): AppResult<Unit> =
        operate(orderId, "结算申请失败") { orderApi.settleApply(it) }

    override suspend fun acceptOrder(orderId: String): AppResult<Unit> =
        operate(orderId, "摘单失败，请稍后重试") { orderApi.acceptOrder(it) }

    override suspend fun shipOrder(orderId: String): AppResult<Unit> =
        operate(orderId, "确认发货失败") { orderApi.shipOrder(it) }

    override suspend fun confirmReceipt(orderId: String): AppResult<Unit> =
        operate(orderId, "确认收货失败") { orderApi.confirmReceipt(it) }

    override suspend fun reconcileOrder(orderId: String): AppResult<Unit> =
        operate(orderId, "对账失败，请稍后重试") { orderApi.reconcileOrder(it) }

    /** 运单操作统一入口：不同动作只有接口与兜底文案不同 */
    private suspend fun operate(
        orderId: String,
        fallbackMessage: String,
        block: suspend (OrderOperateRequest) -> ApiResult<JsonElement>,
    ): AppResult<Unit> = safeUnitCall(fallbackMessage) { block(OrderOperateRequest(orderId)) }
}
