package com.monkey.wisdom.ui.home

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

/**
 * 承运方首页状态：仅展示今日货源统计。
 */
data class CarrierHomeUiState(
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val allOrders: List<OrderItem> = emptyList(),
) {
    /** 货源总量 */
    val total: Int get() = allOrders.size

    /** 可摘单（status = 1） */
    val openCount: Int get() = allOrders.count { it.status == 1 }

    /** 已摘单（status != 1） */
    val acceptedCount: Int get() = allOrders.count { it.status != 1 }
}

/**
 * 承运方首页 ViewModel：加载货源大厅全量数据用于首页统计卡。
 */
class CarrierHomeViewModel(
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CarrierHomeUiState())
    val state: StateFlow<CarrierHomeUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null) }
            when (val result = orderRepository.querySourceOrders(SourceOrderQuery())) {
                is AppResult.Success -> _state.update {
                    it.copy(loading = false, errorMessage = null, allOrders = result.data)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(loading = false, errorMessage = result.message)
                }
            }
        }
    }
}
