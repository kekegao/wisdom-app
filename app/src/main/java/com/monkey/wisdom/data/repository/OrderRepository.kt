package com.monkey.wisdom.data.repository

import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.data.model.OrderItem
import com.monkey.wisdom.data.model.request.OrderListQuery
import com.monkey.wisdom.data.model.request.PublishOrderRequest
import com.monkey.wisdom.data.model.request.SourceOrderQuery

/**
 * 运单数据仓库：屏蔽接口细节，向上层返回 [AppResult]。
 */
interface OrderRepository {

    /** 发布运单 */
    suspend fun publishOrder(request: PublishOrderRequest): AppResult<Unit>

    /** 我的发布运单（货主视角） */
    suspend fun queryPublishedOrders(query: OrderListQuery = OrderListQuery()): AppResult<List<OrderItem>>

    /** 货源大厅 / 线路搜索（承运方视角） */
    suspend fun querySourceOrders(query: SourceOrderQuery = SourceOrderQuery()): AppResult<List<OrderItem>>

    /** 我的运单（承运方已摘运单） */
    suspend fun queryCarrierOrders(query: OrderListQuery = OrderListQuery()): AppResult<List<OrderItem>>

    /** 货主确认成交 */
    suspend fun dealOrder(orderId: String): AppResult<Unit>

    /** 货主取消承运方摘单 */
    suspend fun cancelAccept(orderId: String): AppResult<Unit>

    /** 货主回单确认 */
    suspend fun receiptConfirm(orderId: String): AppResult<Unit>

    /** 货主结算申请 */
    suspend fun settleApply(orderId: String): AppResult<Unit>

    /** 承运方摘单 */
    suspend fun acceptOrder(orderId: String): AppResult<Unit>

    /** 承运方确认发货 */
    suspend fun shipOrder(orderId: String): AppResult<Unit>

    /** 承运方确认收货 */
    suspend fun confirmReceipt(orderId: String): AppResult<Unit>
}
