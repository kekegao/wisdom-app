package com.monkey.wisdom.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.data.model.OrderItem
import com.monkey.wisdom.data.model.request.SourceOrderQuery
import com.monkey.wisdom.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 货源大厅页状态 */
data class CarrierAcceptUiState(
    /** 发货地关键字（输入框） */
    val shipperKeyword: String = "",
    /** 收货地关键字（输入框） */
    val carrierKeyword: String = "",
    /** 是否已执行过检索（决定「重置」按钮与列表计数文案） */
    val searched: Boolean = false,
    val loading: Boolean = false,
    val errorMessage: String? = null,
    /** 当前展示列表：大厅全量或线路搜索结果 */
    val orders: List<OrderItem> = emptyList(),
    /** 大厅全量货源快照，仅用于顶部统计口径（线路搜索不改变） */
    val allOrders: List<OrderItem> = emptyList(),
    /** 最近一次实际发出的查询条件，供失败重试复用 */
    val appliedShipperKeyword: String = "",
    val appliedCarrierKeyword: String = "",
    /** 正在摘单的运单号 */
    val grabbingOrderId: String? = null,
    /** 摘单二次确认的运单 */
    val confirmOrder: OrderItem? = null,
    /** 详情查看的运单 */
    val detailOrder: OrderItem? = null,
    val toastMessage: String? = null,
) {
    /** 顶部统计口径以大厅全量货源为准 */
    val total: Int get() = allOrders.size

    /** 可摘单（status = 1） */
    val openCount: Int get() = allOrders.count { it.status == 1 }

    /** 已摘单（status != 1） */
    val acceptedCount: Int get() = allOrders.count { it.status != 1 }

    /** 是否填写了检索关键字（用于空结果提示文案） */
    val hasKeyword: Boolean get() = shipperKeyword.isNotBlank() || carrierKeyword.isNotBlank()

    /** 搜索卡片底部提示 */
    val searchTipText: String
        get() = when {
            errorMessage != null -> "加载失败，可点「重置」或下方「重新加载」重试"
            searched -> "当前线路共 ${orders.size} 条货源"
            else -> "输入发货地 / 收货地后点击搜索查询"
        }

    /** 空态标题 */
    val emptyTitle: String get() = if (hasKeyword) "没有找到匹配的货源" else "暂无货源信息"

    /** 空态说明 */
    val emptyTip: String
        get() = if (hasKeyword) "试试更换发货地 / 收货地关键词" else "当前暂无合适的货源，稍后再来看看"
}

/**
 * 货源大厅（承运方找货）ViewModel：线路搜索 + 摘单。
 *
 * 与 Web 端一致：顶部统计以「大厅全量货源」为准，线路搜索结果不改变统计口径；
 * 空关键字查询即大厅全量。
 */
class CarrierAcceptOrderViewModel(
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CarrierAcceptUiState())
    val state: StateFlow<CarrierAcceptUiState> = _state.asStateFlow()

    init {
        query("", "")
    }

    fun onShipperKeywordChange(value: String) {
        _state.update { it.copy(shipperKeyword = value) }
    }

    fun onCarrierKeywordChange(value: String) {
        _state.update { it.copy(carrierKeyword = value) }
    }

    /** 点击「搜索」：按输入框关键字查询 */
    fun search() {
        if (_state.value.loading) return
        _state.update { it.copy(searched = true) }
        val current = _state.value
        query(current.shipperKeyword.trim(), current.carrierKeyword.trim())
    }

    /** 重置：清空输入并回到大厅全量 */
    fun resetSearch() {
        if (_state.value.loading) return
        _state.update { it.copy(shipperKeyword = "", carrierKeyword = "", searched = false) }
        query("", "")
    }

    /** 加载失败重试 / 刷新：沿用最近一次查询条件 */
    fun retry() {
        val current = _state.value
        query(current.appliedShipperKeyword, current.appliedCarrierKeyword)
    }

    /**
     * 查询货源大厅。
     *
     * @param shipperKeyword 发货地关键字，空即不限制
     * @param carrierKeyword 收货地关键字，空即不限制
     * 两者都为空时视为大厅全量查询，会同步刷新统计快照。
     */
    private fun query(shipperKeyword: String, carrierKeyword: String) {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    errorMessage = null,
                    appliedShipperKeyword = shipperKeyword,
                    appliedCarrierKeyword = carrierKeyword,
                )
            }
            val query = SourceOrderQuery(
                shipperKeyword = shipperKeyword.ifBlank { null },
                carrierKeyword = carrierKeyword.ifBlank { null },
            )
            when (val result = orderRepository.querySourceOrders(query)) {
                is AppResult.Success -> _state.update { state ->
                    val rows = result.data
                    state.copy(
                        loading = false,
                        errorMessage = null,
                        orders = rows,
                        // 仅大厅全量结果刷新统计口径
                        allOrders = if (shipperKeyword.isBlank() && carrierKeyword.isBlank()) {
                            rows
                        } else {
                            state.allOrders
                        },
                    )
                }

                is AppResult.Failure -> _state.update {
                    it.copy(loading = false, errorMessage = result.message, orders = emptyList())
                }
            }
        }
    }

    fun openDetail(order: OrderItem) = _state.update { it.copy(detailOrder = order) }

    fun closeDetail() = _state.update { it.copy(detailOrder = null) }

    fun openGrabConfirm(order: OrderItem) = _state.update { it.copy(confirmOrder = order) }

    fun dismissGrabConfirm() = _state.update { it.copy(confirmOrder = null) }

    fun consumeToast() = _state.update { it.copy(toastMessage = null) }

    /** 确认摘单 */
    fun confirmGrab() {
        val order = _state.value.confirmOrder ?: return
        if (_state.value.grabbingOrderId != null) return

        viewModelScope.launch {
            _state.update { it.copy(grabbingOrderId = order.orderId, confirmOrder = null) }
            when (val result = orderRepository.acceptOrder(order.orderId)) {
                is AppResult.Success -> _state.update { state ->
                    // 与 Web 端一致：摘单成功后本地将该运单标记为「已摘单」
                    val updated = state.orders.map { item ->
                        if (item.orderId == order.orderId) {
                            item.copy(status = 2, statusDesc = sourceStatusText(2))
                        } else {
                            item
                        }
                    }
                    val updatedAll = state.allOrders.map { item ->
                        if (item.orderId == order.orderId) {
                            item.copy(status = 2, statusDesc = sourceStatusText(2))
                        } else {
                            item
                        }
                    }
                    state.copy(
                        grabbingOrderId = null,
                        orders = updated,
                        allOrders = updatedAll,
                        detailOrder = state.detailOrder?.let { detail ->
                            updated.firstOrNull { it.orderId == detail.orderId } ?: detail
                        },
                        toastMessage = "摘单成功，请及时联系货主",
                    )
                }

                is AppResult.Failure -> _state.update {
                    it.copy(grabbingOrderId = null, toastMessage = result.message)
                }
            }
        }
    }
}
