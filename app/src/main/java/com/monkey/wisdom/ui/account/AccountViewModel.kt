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

/** 智运宝账户页状态 */
data class AccountUiState(
    val loading: Boolean = false,
    val account: AccountInfo? = null,
    /** 余额加载失败提示（不影响冻结明细展示） */
    val errorMessage: String? = null,
    val frozenDetails: List<FrozenDetail> = emptyList(),
    val frozenLoading: Boolean = false,
    /** 冻结明细加载失败提示 */
    val frozenError: String? = null,
    /** 是否处于冻结明细视图 */
    val showFrozenDetail: Boolean = false,
) {
    val frozenTotal: String
        get() = frozenDetails.fold(BigDecimal.ZERO) { sum, item ->
            sum + Formatters.toAmount(item.amount)
        }.toPlainString()

    val frozenCount: Int get() = frozenDetails.size
}

/**
 * 智运宝账户 ViewModel：账户余额 + 冻结明细。
 *
 * 与 Web 端一致：
 * - 余额先用本地快照兜底展示，再以服务端账户快照覆盖并回写本地；
 * - 冻结明细默认展示本地提现冻结记录，进入明细页时才实时查询后端「冻结中」明细；
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
                frozenDetails = localAccountStore.loadWithdrawRecords(),
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

    /**
     * 查询「冻结中」明细。
     *
     * 合并本地提现冻结记录：提现为本地模拟，不合并会导致刚提交的提现记录在服务端返回后消失。
     */
    fun loadFrozenDetails() {
        if (_state.value.frozenLoading) return
        viewModelScope.launch {
            _state.update { it.copy(frozenLoading = true, frozenError = null) }
            when (val result = accountRepository.queryFrozenDetails()) {
                is AppResult.Success -> _state.update {
                    it.copy(
                        frozenLoading = false,
                        frozenError = null,
                        frozenDetails = localAccountStore.loadWithdrawRecords() + result.data,
                    )
                }

                is AppResult.Failure -> _state.update {
                    it.copy(
                        frozenLoading = false,
                        frozenError = result.message,
                        frozenDetails = localAccountStore.loadWithdrawRecords(),
                    )
                }
            }
        }
    }

    /** 打开冻结明细：进入即实时查询后端「冻结中」明细 */
    fun openFrozenDetail() {
        _state.update { it.copy(showFrozenDetail = true) }
        loadFrozenDetails()
    }

    fun closeFrozenDetail() = _state.update { it.copy(showFrozenDetail = false) }
}
