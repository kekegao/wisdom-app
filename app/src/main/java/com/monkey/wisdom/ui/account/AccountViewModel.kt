package com.monkey.wisdom.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.core.util.Formatters
import com.monkey.wisdom.data.model.AccountInfo
import com.monkey.wisdom.data.model.FrozenDetail
import com.monkey.wisdom.data.repository.AccountRepository
import com.monkey.wisdom.data.repository.LocalAccountStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

/** 冻结明细分页大小，与收支明细 INCOME_EXPENSE_PAGE_SIZE 及后端默认分页大小一致 */
const val FROZEN_PAGE_SIZE = 20

/** 智运宝账户页状态 */
data class AccountUiState(
    val loading: Boolean = false,
    val account: AccountInfo? = null,
    /** 余额加载失败提示（不影响冻结明细展示） */
    val errorMessage: String? = null,
    /** 已加载的服务端冻结明细（翻页追加） */
    val frozenRecords: List<FrozenDetail> = emptyList(),
    /** 本地提现冻结记录（提现为本地模拟，与服务端明细合并展示） */
    val localFrozenRecords: List<FrozenDetail> = emptyList(),
    /** 服务端冻结记录总数 */
    val frozenTotal: Long = 0,
    /** 冻结明细下一页页码，从 1 开始 */
    val frozenNextPage: Int = 1,
    /** 首次加载 / 刷新中 */
    val frozenLoading: Boolean = false,
    /** 加载更多中 */
    val frozenLoadingMore: Boolean = false,
    /** 冻结明细加载失败提示 */
    val frozenError: String? = null,
    /** 是否处于冻结明细视图 */
    val showFrozenDetail: Boolean = false,
) {
    /** 是否还有下一页（已加载服务端条数小于总数） */
    val frozenHasMore: Boolean get() = frozenRecords.size < frozenTotal

    /** 展示用冻结明细：本地提现记录 + 已加载的服务端记录 */
    val frozenDetails: List<FrozenDetail> get() = localFrozenRecords + frozenRecords

    /** 冻结记录总笔数（本地 + 服务端总数） */
    val frozenCount: Int get() = localFrozenRecords.size + frozenTotal.toInt()

    /** 已展示冻结明细的金额合计（元） */
    val frozenTotalAmount: String
        get() = frozenDetails.fold(BigDecimal.ZERO) { sum, item ->
            sum + Formatters.toAmount(item.amount)
        }.toPlainString()
}

/**
 * 智运宝账户 ViewModel：账户余额 + 冻结明细。
 *
 * 与 Web 端一致：
 * - 余额先用本地快照兜底展示，再以服务端账户快照覆盖并回写本地；
 * - 冻结明细默认展示本地提现冻结记录，进入明细页时按页查询后端冻结明细（分页口径与收支明细一致）；
 * - 任一请求失败均不阻塞另一块展示，保留本地兜底数据。
 */
class AccountViewModel(
    private val accountRepository: AccountRepository,
    private val localAccountStore: LocalAccountStore,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        // 本地兜底：避免首屏空白，同时保证后端不可用时仍有数据可看
        // 服务端查询由页面进入时触发（进入即刷新，可从充值 / 提现页返回后同步最新余额）
        _state.update {
            it.copy(
                account = localAccountStore.loadBalanceSnapshot(),
                localFrozenRecords = localAccountStore.loadWithdrawRecords(),
            )
        }
    }

    /** 查询账户余额：成功时以服务端快照覆盖展示并回写本地 */
    fun loadAccount() {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null) }
            when (val result = accountRepository.queryBalance()) {
                is AppResult.Success -> {
                    // 回写本地，供充值 / 提现页基于服务端真实余额计算
                    val account = result.data
                    localAccountStore.saveBalanceSnapshot(account)
                    _state.update {
                        it.copy(loading = false, errorMessage = null, account = account)
                    }
                }

                is AppResult.Failure -> _state.update {
                    // 后端不可用时保留本地兜底余额，仅提示错误
                    it.copy(loading = false, errorMessage = result.message)
                }
            }
        }
    }

    /** 刷新冻结明细：重置为第一页 */
    fun refreshFrozenDetails() = loadFrozenDetails(reset = true)

    /** 加载下一页冻结明细（无更多数据或正在请求时忽略） */
    fun loadMoreFrozenDetails() {
        if (!_state.value.frozenHasMore) return
        loadFrozenDetails(reset = false)
    }

    /**
     * 分页查询冻结明细：reset=true 取第一页覆盖，false 追加下一页。
     *
     * 展示列表始终合并本地提现冻结记录（提现为本地模拟，不合并会导致刚提交的提现记录消失）。
     */
    private fun loadFrozenDetails(reset: Boolean) {
        val current = _state.value
        if (current.frozenLoading || current.frozenLoadingMore) return
        if (!reset && !current.frozenHasMore) return

        val pageNum = if (reset) 1 else current.frozenNextPage
        viewModelScope.launch {
            _state.update {
                it.copy(
                    frozenLoading = reset,
                    frozenLoadingMore = !reset,
                    frozenError = null,
                    frozenRecords = if (reset) emptyList() else it.frozenRecords,
                    frozenTotal = if (reset) 0 else it.frozenTotal,
                    frozenNextPage = if (reset) 1 else it.frozenNextPage,
                )
            }
            when (val result = accountRepository.queryFrozenDetails(pageNum, FROZEN_PAGE_SIZE)) {
                is AppResult.Success -> {
                    val page = result.data
                    val loaded = page.records.orEmpty()
                    _state.update {
                        it.copy(
                            frozenLoading = false,
                            frozenLoadingMore = false,
                            frozenError = null,
                            frozenRecords = if (reset) loaded else it.frozenRecords + loaded,
                            frozenTotal = page.total ?: 0,
                            frozenNextPage = (page.current ?: pageNum.toLong()).toInt() + 1,
                        )
                    }
                }

                is AppResult.Failure -> _state.update {
                    // 加载更多失败时保留已加载数据，仅提示错误
                    it.copy(
                        frozenLoading = false,
                        frozenLoadingMore = false,
                        frozenError = result.message,
                        frozenRecords = if (reset) emptyList() else it.frozenRecords,
                    )
                }
            }
        }
    }

    /** 打开冻结明细：进入即刷新本地提现记录并重新查询第一页 */
    fun openFrozenDetail() {
        _state.update {
            it.copy(
                showFrozenDetail = true,
                localFrozenRecords = localAccountStore.loadWithdrawRecords(),
            )
        }
        refreshFrozenDetails()
    }

    fun closeFrozenDetail() = _state.update { it.copy(showFrozenDetail = false) }

    /** 关闭后端失败弹窗 */
    fun consumeError() = _state.update { it.copy(errorMessage = null, frozenError = null) }
}
