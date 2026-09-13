package com.monkey.wisdom.data.remote.api

import com.google.gson.JsonElement
import com.monkey.wisdom.data.model.ApiResult
import com.monkey.wisdom.data.model.OrderItem
import com.monkey.wisdom.data.model.request.OrderListQuery
import com.monkey.wisdom.data.model.request.OrderOperateRequest
import com.monkey.wisdom.data.model.request.PublishOrderRequest
import com.monkey.wisdom.data.model.request.SourceOrderQuery
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 运单相关接口（货主发布端 + 承运方接单端）。
 *
 * baseUrl 为 `http://host:8083/`，与 Web 端 `/api/xxx` 经代理转发后的地址一致。
 * 列表类接口后端返回的 `data` 为数组。
 */
interface OrderApi {

    // ==== 货主端 ====

    /** 发布运单 */
    @POST("publishOrder/publish")
    suspend fun publishOrder(@Body request: PublishOrderRequest): ApiResult<JsonElement>

    /** 我的发布运单列表 */
    @POST("publishOrder/list")
    suspend fun queryPublishedOrders(@Body query: OrderListQuery): ApiResult<List<OrderItem>>

    /** 货主确认成交：摘单(2) → 成交(3) */
    @POST("publishOrder/dealOrder")
    suspend fun dealOrder(@Body request: OrderOperateRequest): ApiResult<JsonElement>

    /** 货主取消承运方摘单：摘单(2) → 发布(1) */
    @POST("publishOrder/cancelAccept")
    suspend fun cancelAccept(@Body request: OrderOperateRequest): ApiResult<JsonElement>

    /** 货主回单确认：确认收货(5) → 回单确认(6) */
    @POST("publishOrder/receiptConfirm")
    suspend fun receiptConfirm(@Body request: OrderOperateRequest): ApiResult<JsonElement>

    /** 货主结算申请：回单确认(6) → 结算申请(7) */
    @POST("publishOrder/settleApply")
    suspend fun settleApply(@Body request: OrderOperateRequest): ApiResult<JsonElement>

    // ==== 承运端 ====

    /** 货源大厅 / 线路搜索 */
    @POST("accept/list")
    suspend fun querySourceOrders(@Body query: SourceOrderQuery): ApiResult<List<OrderItem>>

    /** 我的运单（承运方已摘运单） */
    @POST("accept/myOrders")
    suspend fun queryCarrierOrders(@Body query: OrderListQuery): ApiResult<List<OrderItem>>

    /** 摘单 */
    @POST("accept/acceptOrder")
    suspend fun acceptOrder(@Body request: OrderOperateRequest): ApiResult<JsonElement>

    /** 确认发货：成交(3) → 发货(4) */
    @POST("accept/shipOrder")
    suspend fun shipOrder(@Body request: OrderOperateRequest): ApiResult<JsonElement>

    /** 确认收货：发货(4) → 确认收货(5) */
    @POST("accept/confirmReceipt")
    suspend fun confirmReceipt(@Body request: OrderOperateRequest): ApiResult<JsonElement>
}
