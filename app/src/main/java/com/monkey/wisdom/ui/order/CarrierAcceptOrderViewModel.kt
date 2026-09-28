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
    /** 上拉加载更多中（不影响首屏 loading 态） */
    val loadingMore: Boolean = false,
    val errorMessage: String? = null,
    /** 当前已加载的货源（分页累加结果） */
    val orders: List<OrderItem> = emptyList(),
    /** 当前页码（从 1 开始） */
    val pageNum: Int = 1,
    /** 每页条数 */
    val pageSize: Int = DEFAULT_PAGE_SIZE,
    /** 符合条件的货源总数（后端 count，不受已加载页数影响） */
    val total: Long = 0L,
    /** 可摘货源数量（status = 1，后端统计） */
    val openCount: Long = 0L,
    /** 是否还有下一页（App 据此决定是否继续上拉） */
    val hasMore: Boolean = false,
    /** 最近一次实际发出的查询条件，供失败重试 / 加载更多复用 */
    val appliedShipperKeyword: String = "",
    val appliedCarrierKeyword: String = "",
    /** 正在摘单的运单号 */
    val grabbingOrderId: String? = null,
    /** 摘单二次确认的运单 */
    val confirmOrder: OrderItem? = null,
    /** 详情查看的运单 */
    val detailOrder: OrderItem? = null,
    val successMessage: String? = null,
    /** 后端接口返回失败提示（弹窗展示） */
    val errorDialog: String? = null,
) {

    companion object {
        /** 每页条数，与后端默认分页大小一致 */
        const val DEFAULT_PAGE_SIZE = 10
    }

    /** 是否填写了检索关键字（用于空结果提示文案） */
    val hasKeyword: Boolean get() = shipperKeyword.isNotBlank() || carrierKeyword.isNotBlank()

    /** 列表计数文案：分页场景优先展示后端总数 */
    val countText: String
        get() = if (total > 0L) "共 $total 条货源" else "共 ${orders.size} 条货源"

    /** 搜索卡片底部提示 */
    val searchTipText: String
        get() = when {
            errorMessage != null -> "加载失败，可点「重置」或下方「重新加载」重试"
            searched -> "当前线路共 $total 条货源"
            else -> "输入发货地 / 收货地后点击搜索查询"
        }

    /** 空态标题 */
    val emptyTitle: String get() = if (hasKeyword) "没有找到匹配的货源" else "暂无货源信息"

    /** 空态说明 */
    val emptyTip: String
        get() = if (hasKeyword) "试试更换发货地 / 收货地关键词" else "当前暂无合适的货源，稍后再来看看"
}

/**
 * 货源大厅（承运方找货）ViewModel：线路搜索 + 分页加载 + 摘单。
 *
 * 分页策略：首次进入 / 搜索 / 重置拉取第 1 页并清空列表；
 * 上拉到底部时按 [loadMore] 追加下一页，通过后端的 hasMore 决定是否继续，
 * 避免一次拉全量导致列表过大、流量与内存浪费。
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

    /** 点击「搜索」：按输入框关键字从第 1 页重新查询 */
    fun search() {
        if (_state.value.loading || _state.value.loadingMore) return
        val current = _state.value
        _state.update { it.copy(searched = true) }
        query(current.shipperKeyword.trim(), current.carrierKeyword.trim())
    }

    /** 重置：清空输入并回到大厅全量第 1 页 */
    fun resetSearch() {
        if (_state.value.loading || _state.value.loadingMore) return
        _state.update { it.copy(shipperKeyword = "", carrierKeyword = "", searched = false) }
        query("", "")
    }

    /** 加载失败重试 / 刷新：沿用最近一次查询条件，回到第 1 页 */
    fun retry() {
        val current = _state.value
        query(current.appliedShipperKeyword, current.appliedCarrierKeyword)
    }

    /** 上拉加载更多：沿用当前查询条件追加下一页 */
    fun loadMore() {
        val current = _state.value
        if (!current.hasMore || current.loading || current.loadingMore) return
        query(current.appliedShipperKeyword, current.appliedCarrierKeyword, current.pageNum + 1, append = true)
    }

    /**
     * 分页查询货源大厅。
     *
     * @param shipperKeyword 发货地关键字，空即不限制
     * @param carrierKeyword 收货地关键字，空即不限制
     * @param pageNum        页码，从 1 开始
     * @param append         true 表示追加到现有列表（加载更多），false 表示替换（首次 / 搜索 / 重置）
     */
    private fun query(
        shipperKeyword: String,
        carrierKeyword: String,
        pageNum: Int = 1,
        append: Boolean = false,
    ) {
        if (_state.value.loading || _state.value.loadingMore) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = !append,
                    loadingMore = append,
                    errorMessage = null,
                    appliedShipperKeyword = shipperKeyword,
                    appliedCarrierKeyword = carrierKeyword,
                )
            }
            val query = SourceOrderQuery(
                shipperKeyword = shipperKeyword.ifBlank { null },
                carrierKeyword = carrierKeyword.ifBlank { null },
                pageNum = pageNum,
                pageSize = _state.value.pageSize,
            )
            when (val result = orderRepository.querySourceOrderPage(query)) {
                is AppResult.Success -> _state.update { state ->
                    val page = result.data
                    // 翻页期间可能有新货源发布导致重复，按运单号去重
                    val merged = if (append) {
                        (state.orders + page.records).distinctBy { it.orderId }
                    } else {
                        page.records
                    }
                    state.copy(
                        loading = false,
                        loadingMore = false,
                        errorMessage = null,
                        orders = merged,
                        pageNum = page.pageNum,
                        pageSize = page.pageSize,
                        total = page.total,
                        openCount = page.openCount,
                        hasMore = page.hasMore,
                    )
                }

                is AppResult.Failure -> _state.update {
                    it.copy(
                        loading = false,
                        loadingMore = false,
                        errorMessage = result.message,
                        // 加载更多失败保留已加载数据，首屏失败才清空
                        orders = if (append) it.orders else emptyList(),
                    )
                }
            }
        }
    }

    fun openDetail(order: OrderItem) = _state.update { it.copy(detailOrder = order) }

    fun closeDetail() = _state.update { it.copy(detailOrder = null) }

    fun openGrabConfirm(order: OrderItem) = _state.update { it.copy(confirmOrder = order) }

    fun dismissGrabConfirm() = _state.update { it.copy(confirmOrder = null) }

    fun consumeSuccess() = _state.update { it.copy(successMessage = null) }

    /** 关闭后端失败弹窗 */
    fun consumeErrorDialog() = _state.update { it.copy(errorDialog = null) }

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
                    state.copy(
                        grabbingOrderId = null,
                        orders = updated,
                        detailOrder = state.detailOrder?.let { detail ->
                            updated.firstOrNull { it.orderId == detail.orderId } ?: detail
                        },
                        successMessage = "摘单成功，请及时联系货主",
                    )
                }

                is AppResult.Failure -> _state.update {
                    it.copy(grabbingOrderId = null, errorDialog = result.message)
                }
            }
        }
    }
}
