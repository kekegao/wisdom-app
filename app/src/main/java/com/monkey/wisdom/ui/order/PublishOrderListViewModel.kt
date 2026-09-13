package com.monkey.wisdom.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.data.model.OrderItem
import com.monkey.wisdom.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 货主「我的订单」筛选标签，与 Web 端 tabs 一致。
 */
enum class ShipperOrderTab(val label: String, val statuses: Set<Int>?) {
    ALL("全部", null),
    WAITING("待接单", setOf(1)),
    ACCEPTED("已接单", setOf(2, 3)),
    RUNNING("运输中", setOf(4)),
    DONE("已完成", (5..10).toSet()),
    CANCELED("已取消", setOf(-1)),
}

/**
 * 货主运单二次确认操作，文案与 Web 端 CONFIRM_TITLE / CONFIRM_BTN_TEXT / CONFIRM_FAIL_TEXT 一致。
 */
enum class OrderConfirmAction(
    val title: String,
    val buttonText: String,
    val failText: String,
) {
    DEAL("确认成交", "确认成交", "成交失败，请稍后重试"),
    CANCEL_ACCEPT("取消摘单", "确认取消", "取消摘单失败，请稍后重试"),
    RECEIPT_CONFIRM("回单确认", "确认回单", "回单确认失败，请稍后重试"),
    SETTLE_APPLY("结算申请", "确认申请", "结算申请失败，请稍后重试"),
}

/** 货主运单列表页状态 */
data class PublishOrderListUiState(
    val loading: Boolean = false,
    val errorMessage: String? = null,
    /** 全量订单，用于本地筛选与统计（与 Web 端 allOrders 口径一致） */
    val allOrders: List<OrderItem> = emptyList(),
    val activeTabIndex: Int = 0,
    /** 当前查看详情的运单，null 表示列表态 */
    val detailOrder: OrderItem? = null,
    val confirmAction: OrderConfirmAction? = null,
    val confirmOrder: OrderItem? = null,
    val submitting: Boolean = false,
    val toastMessage: String? = null,
) {

    val tabs: List<ShipperOrderTab> get() = ShipperOrderTab.entries

    /** 当前标签下的运单 */
    val visibleOrders: List<OrderItem>
        get() {
            val statuses = tabs[activeTabIndex].statuses ?: return allOrders
            return allOrders.filter { it.status in statuses }
        }

    /** 顶部统计：总数 / 待接单 / 进行中 / 已完成 */
    val total: Int get() = allOrders.size
    val waitingCount: Int get() = allOrders.count { it.status == 1 }
    val ongoingCount: Int get() = allOrders.count { it.status in 2..4 }
    val doneCount: Int get() = allOrders.count { it.status in 5..10 }
}

/**
 * 货主「我的订单」ViewModel：加载、筛选、成交 / 取消摘单 / 回单确认 / 结算申请。
 */
class PublishOrderListViewModel(
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PublishOrderListUiState())
    val state: StateFlow<PublishOrderListUiState> = _state.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null) }
            when (val result = orderRepository.queryPublishedOrders()) {
                is AppResult.Success -> _state.update { current ->
                    // 保留当前详情对象的最新数据，避免刷新后详情态丢失
                    val refreshedDetail = current.detailOrder?.let { detail ->
                        result.data.firstOrNull { it.orderId == detail.orderId }
                    }
                    current.copy(
                        loading = false,
                        errorMessage = null,
                        allOrders = result.data,
                        detailOrder = refreshedDetail ?: current.detailOrder,
                    )
                }

                is AppResult.Failure -> _state.update { current ->
                    current.copy(loading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun selectTab(index: Int) {
        _state.update { it.copy(activeTabIndex = index) }
    }

    fun openDetail(order: OrderItem) {
        _state.update { it.copy(detailOrder = order) }
    }

    fun closeDetail() {
        _state.update { it.copy(detailOrder = null) }
    }

    fun openConfirm(action: OrderConfirmAction, order: OrderItem) {
        _state.update { it.copy(confirmAction = action, confirmOrder = order) }
    }

    fun dismissConfirm() {
        _state.update { it.copy(confirmAction = null, confirmOrder = null) }
    }

    fun consumeToast() {
        _state.update { it.copy(toastMessage = null) }
    }

    /** 提交二次确认操作 */
    fun submitConfirm() {
        val current = _state.value
        val action = current.confirmAction ?: return
        val order = current.confirmOrder ?: return
        if (current.submitting) return

        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            val result = when (action) {
                OrderConfirmAction.DEAL -> orderRepository.dealOrder(order.orderId)
                OrderConfirmAction.CANCEL_ACCEPT -> orderRepository.cancelAccept(order.orderId)
                OrderConfirmAction.RECEIPT_CONFIRM -> orderRepository.receiptConfirm(order.orderId)
                OrderConfirmAction.SETTLE_APPLY -> orderRepository.settleApply(order.orderId)
            }

            when (result) {
                is AppResult.Success -> {
                    // 与 Web 端一致：成功后本地推进状态并提示
                    val (nextStatus, nextDesc, message) = when (action) {
                        OrderConfirmAction.DEAL -> Triple(3, "成交", "成交成功，订单进入履约阶段")
                        OrderConfirmAction.CANCEL_ACCEPT -> Triple(1, "发布", "已取消摘单，运单恢复为待接单")
                        OrderConfirmAction.RECEIPT_CONFIRM -> Triple(6, "回单确认", "回单确认成功，承运方发货保证金已解冻")
                        OrderConfirmAction.SETTLE_APPLY -> Triple(7, "结算申请", "结算申请已提交，平台将尽快办理运费结算")
                    }
                    _state.update { state ->
                        val updated = state.allOrders.map { item ->
                            if (item.orderId == order.orderId) {
                                item.copy(status = nextStatus, statusDesc = nextDesc)
                            } else {
                                item
                            }
                        }
                        state.copy(
                            submitting = false,
                            confirmAction = null,
                            confirmOrder = null,
                            allOrders = updated,
                            detailOrder = state.detailOrder?.let { detail ->
                                updated.firstOrNull { it.orderId == detail.orderId } ?: detail
                            },
                            toastMessage = message,
                        )
                    }
                }

                is AppResult.Failure -> _state.update {
                    it.copy(
                        submitting = false,
                        confirmAction = null,
                        confirmOrder = null,
                        toastMessage = result.message,
                    )
                }
            }
        }
    }

    /** 二次确认弹窗提示文案 */
    fun confirmTip(action: OrderConfirmAction, order: OrderItem): String {
        val carrier = order.carrierName?.takeIf { it.isNotBlank() }
            ?: order.carrierUserName?.takeIf { it.isNotBlank() }
            ?: "该承运方"
        return when (action) {
            OrderConfirmAction.DEAL ->
                "是否确认与「$carrier」成交该运单？成交后双方进入履约阶段，运费仍托管于平台。"
            OrderConfirmAction.CANCEL_ACCEPT ->
                "是否取消「$carrier」的摘单？取消后运单将恢复为待接单，重新进入货源大厅。"
            OrderConfirmAction.RECEIPT_CONFIRM ->
                "是否确认运单「${order.orderId}」的回单？确认后运单履约完成，承运方发货保证金将解冻退回。"
            OrderConfirmAction.SETTLE_APPLY ->
                "是否对运单「${order.orderId}」发起结算申请？提交后平台将按托管运费与承运方办理结算，运单进入结算流程。"
        }
    }
}
