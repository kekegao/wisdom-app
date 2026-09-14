package com.monkey.wisdom.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.data.model.IncomeExpenseItem
import com.monkey.wisdom.data.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 收支明细分页大小，与 Web 端 incomeExpenseList.vue 的 PAGE_SIZE 一致 */
const val INCOME_EXPENSE_PAGE_SIZE = 20

/** 收支明细页状态 */
data class IncomeExpenseUiState(
    /** 已加载的流水记录（翻页追加） */
    val records: List<IncomeExpenseItem> = emptyList(),
    /** 总记录数（服务端返回） */
    val total: Long = 0,
    /** 下一页页码，从 1 开始 */
    val nextPage: Int = 1,
    /** 首次加载 / 刷新中 */
    val loading: Boolean = false,
    /** 加载更多中 */
    val loadingMore: Boolean = false,
    /** 错误提示文案 */
    val errorMessage: String? = null,
) {
    /** 是否还有下一页（已加载条数小于总数） */
    val hasMore: Boolean get() = records.size < total
}

/**
 * 收支明细 ViewModel。
 *
 * 对应 Web 端 `/incomeExpenseList`：进入页面拉取第一页，滚动到底部「加载更多」按页追加，
 * 用户身份由后端从登录会话解析，客户端只传分页参数。
 */
class IncomeExpenseViewModel(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(IncomeExpenseUiState())
    val state: StateFlow<IncomeExpenseUiState> = _state.asStateFlow()

    /** 进入页面 / 点击刷新：重置为第一页 */
    fun refresh() = load(reset = true)

    /** 加载下一页（无更多数据或正在请求时忽略） */
    fun loadMore() {
        if (!_state.value.hasMore) return
        load(reset = false)
    }

    private fun load(reset: Boolean) {
        val current = _state.value
        if (current.loading || current.loadingMore) return
        if (!reset && !current.hasMore) return

        val pageNum = if (reset) 1 else current.nextPage
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = reset,
                    loadingMore = !reset,
                    errorMessage = null,
                    records = if (reset) emptyList() else it.records,
                    total = if (reset) 0 else it.total,
                    nextPage = if (reset) 1 else it.nextPage,
                )
            }
            when (val result = accountRepository.queryIncomeExpenseList(pageNum, INCOME_EXPENSE_PAGE_SIZE)) {
                is AppResult.Success -> {
                    val page = result.data
                    val loaded = page.records.orEmpty()
                    _state.update {
                        it.copy(
                            loading = false,
                            loadingMore = false,
                            errorMessage = null,
                            records = if (reset) loaded else it.records + loaded,
                            total = page.total ?: 0,
                            nextPage = (page.current ?: pageNum.toLong()).toInt() + 1,
                        )
                    }
                }

                is AppResult.Failure -> _state.update {
                    // 加载更多失败时保留已加载数据，仅提示错误
                    it.copy(
                        loading = false,
                        loadingMore = false,
                        errorMessage = result.message,
                        records = if (reset) emptyList() else it.records,
                    )
                }
            }
        }
    }
}
