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

/** 承运端「我的运单」标签，与 Web 端一致 */
enum class CarrierOrderTab(val label: String) {
    ALL("全部"),
    RUNNING("进行中"),
    DONE("已完成"),
}

/** 承运端运单操作（确认发货 / 确认收货 / 对账） */
enum class CarrierConfirmAction(
    val title: String,
    val buttonText: String,
    val tip: String,
    val successMessage: String,
) {
    SHIP(
        title = "确认发货",
        buttonText = "确认发货",
        tip = "是否确认该运单已发货？确认后运单进入运输中，货主可实时查看运输进度。",
        successMessage = "已确认发货，运单进入运输中",
    ),
    RECEIPT(
        title = "确认收货",
        buttonText = "确认收货",
        tip = "是否确认该运单已送达并完成收货？确认后等待货主回单确认，运费按平台托管流程结算。",
        successMessage = "已确认收货，等待货主回单确认",
    ),
    RECONCILE(
        title = "对账确认",
        buttonText = "确认对账",
        tip = "是否确认该运单结算金额无误？确认后平台将按清算单把运费划入您的账户。",
        successMessage = "对账成功，运费已划入账户",
    ),
}

/** 承运端「我的运单」页状态 */
data class CarrierOrderListUiState(
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val allOrders: List<OrderItem> = emptyList(),
    val activeTabIndex: Int = 0,
    val confirmAction: CarrierConfirmAction? = null,
    val confirmOrder: OrderItem? = null,
    val submitting: Boolean = false,
    val detailOrder: OrderItem? = null,
    val toastMessage: String? = null,
) {

    val visibleOrders: List<OrderItem>
        get() = when (CarrierOrderTab.entries[activeTabIndex]) {
            CarrierOrderTab.ALL -> allOrders
            CarrierOrderTab.RUNNING -> allOrders.filter { it.status in RUNNING_STATUSES }
            CarrierOrderTab.DONE -> allOrders.filter { it.status !in RUNNING_STATUSES }
        }

    val total: Int get() = allOrders.size
    val runningCount: Int get() = allOrders.count { it.status in RUNNING_STATUSES }
    val doneCount: Int get() = allOrders.count { it.status !in RUNNING_STATUSES }
}

/**
 * 承运端「我的运单」ViewModel：状态筛选 + 确认发货 / 确认收货。
 */
class CarrierOrderListViewModel(
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CarrierOrderListUiState())
    val state: StateFlow<CarrierOrderListUiState> = _state.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null) }
            when (val result = orderRepository.queryCarrierOrders()) {
                is AppResult.Success -> _state.update { current ->
                    current.copy(
                        loading = false,
                        errorMessage = null,
                        allOrders = result.data,
                        detailOrder = current.detailOrder?.let { detail ->
                            result.data.firstOrNull { it.orderId == detail.orderId }
                        } ?: current.detailOrder,
                    )
                }

                is AppResult.Failure -> _state.update {
                    it.copy(loading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun selectTab(index: Int) = _state.update { it.copy(activeTabIndex = index) }

    fun openDetail(order: OrderItem) = _state.update { it.copy(detailOrder = order) }

    fun closeDetail() = _state.update { it.copy(detailOrder = null) }

    fun openConfirm(action: CarrierConfirmAction, order: OrderItem) =
        _state.update { it.copy(confirmAction = action, confirmOrder = order) }

    fun dismissConfirm() = _state.update { it.copy(confirmAction = null, confirmOrder = null) }

    fun consumeToast() = _state.update { it.copy(toastMessage = null) }

    /** 提交发货 / 收货确认，成功后重新拉取以保证状态与服务端一致 */
    fun submitConfirm() {
        val current = _state.value
        val action = current.confirmAction ?: return
        val order = current.confirmOrder ?: return
        if (current.submitting) return

        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            val result = when (action) {
                CarrierConfirmAction.SHIP -> orderRepository.shipOrder(order.orderId)
                CarrierConfirmAction.RECEIPT -> orderRepository.confirmReceipt(order.orderId)
                CarrierConfirmAction.RECONCILE -> orderRepository.reconcileOrder(order.orderId)
            }
            _state.update {
                it.copy(
                    submitting = false,
                    confirmAction = null,
                    confirmOrder = null,
                    toastMessage = when (result) {
                        is AppResult.Success -> action.successMessage
                        is AppResult.Failure -> result.message
                    },
                )
            }
            if (result is AppResult.Success) {
                _state.update { it.copy(loading = false) }
                loadOrders()
            }
        }
    }
}
