package com.monkey.wisdom.data.repository

import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.data.model.OrderItem
import com.monkey.wisdom.data.model.PageResult
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

    /** 货源大厅 / 线路搜索（承运方视角，全量） */
    suspend fun querySourceOrders(query: SourceOrderQuery = SourceOrderQuery()): AppResult<List<OrderItem>>

    /**
     * 货源大厅分页查询（承运方视角，上拉加载更多）
     *
     * @return 分页结果：当前页记录 + 总数 + 可摘数量 + 是否还有下一页
     */
    suspend fun querySourceOrderPage(query: SourceOrderQuery = SourceOrderQuery()): AppResult<PageResult<OrderItem>>

    /** 我的运单（承运方已摘运单） */
    suspend fun queryCarrierOrders(query: OrderListQuery = OrderListQuery()): AppResult<List<OrderItem>>

    /** 货主确认成交 */
    suspend fun dealOrder(orderId: String): AppResult<Unit>

    /**
     * 货主修改已发布运单运费（仅「已发布待摘单」运单可改价）
     *
     * @return 成功后携带后端返回的提示文案，供 UI 弹框展示（为空时 UI 用本地文案兜底）
     */
    suspend fun changePrice(orderId: String, transportMoney: Double): AppResult<String>

    /** 货主取消承运方摘单 */
    suspend fun cancelAccept(orderId: String): AppResult<Unit>

    /** 货主确认收货 */
    suspend fun receiptConfirm(orderId: String): AppResult<Unit>

    /** 货主运费结算 */
    suspend fun settleApply(orderId: String): AppResult<Unit>

    /** 承运方摘单 */
    suspend fun acceptOrder(orderId: String): AppResult<Unit>

    /**
     * 承运方确认发货
     *
     * @return 成功后携带后端返回的提示文案，供 UI 弹框展示（为空时 UI 用本地文案兜底）
     */
    suspend fun shipOrder(orderId: String): AppResult<String>

    /** 承运方卸货 */
    suspend fun confirmReceipt(orderId: String): AppResult<Unit>

    /** 承运方对账：确认清算金额并触发资金结算 */
    suspend fun reconcileOrder(orderId: String): AppResult<Unit>
}
