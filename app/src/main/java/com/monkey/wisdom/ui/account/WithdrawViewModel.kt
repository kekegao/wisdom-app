package com.monkey.wisdom.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.core.util.Formatters
import com.monkey.wisdom.data.model.BankCard
import com.monkey.wisdom.data.repository.AccountRepository
import com.monkey.wisdom.data.repository.BankCardRepository
import com.monkey.wisdom.data.repository.LocalAccountStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 单笔提现下限（元） */
val WITHDRAW_MIN_AMOUNT: BigDecimal = BigDecimal.ONE

/** 单笔提现上限（元） */
val WITHDRAW_MAX_AMOUNT: BigDecimal = BigDecimal("20000")

/** 手续费（元，模拟免费） */
val WITHDRAW_FEE: BigDecimal = BigDecimal.ZERO

/** 到账时效文案 */
const val WITHDRAW_ARRIVE_TEXT = "预计 2 小时内到账"

/**
 * 提现页状态。
 *
 * 校验规则与 Web 端 `withdraw.vue` 一致：金额 ≥ 1 元、不超过单笔上限、不超过可提现余额。
 */
data class WithdrawUiState(
    /** 余额加载中 */
    val loading: Boolean = false,
    /** 可提现余额（元）：优先服务端可用余额，失败时回落本地快照 */
    val available: BigDecimal = BigDecimal.ZERO,
    /** 提现金额（字符串便于输入控制） */
    val amountText: String = "",
    /** 到账账户：默认取首张已绑定银行卡 */
    val bankCard: BankCard? = null,
    val submitting: Boolean = false,
    /** 提现成功金额，非空时展示成功弹层 */
    val successAmount: BigDecimal? = null,
    /** 余额同步失败提示（后端不可用时保留本地兜底数据） */
    val balanceError: String? = null,
) {

    /** 解析后的提现金额 */
    val amount: BigDecimal get() = parseAmountInput(amountText)

    /** 实际到账金额（扣除手续费） */
    val actualAmount: BigDecimal get() = (amount - WITHDRAW_FEE).max(BigDecimal.ZERO)

    /** 输入校验提示 */
    val errorText: String
        get() = when {
            amountText.isEmpty() -> ""
            amount < WITHDRAW_MIN_AMOUNT -> "提现金额不能低于 ¥${WITHDRAW_MIN_AMOUNT.toPlainString()}"
            amount > WITHDRAW_MAX_AMOUNT -> "单笔提现金额不能超过 ¥${Formatters.money(WITHDRAW_MAX_AMOUNT)}"
            amount > available -> "提现金额超出可提现余额"
            else -> ""
        }

    /** 是否展示到账金额预览 */
    val showFeePreview: Boolean get() = amountText.isNotEmpty() && amount >= WITHDRAW_MIN_AMOUNT && errorText.isEmpty()

    /** 是否可提交：金额合法 + 已绑定到账银行卡 + 未在提交中 */
    val canSubmit: Boolean
        get() = !submitting && successAmount == null && bankCard != null &&
            errorText.isEmpty() && amount >= WITHDRAW_MIN_AMOUNT
}

/**
 * 提现 ViewModel。
 *
 * 对应 Web 端 `/withdraw`：可提现余额取「可用余额」，到账账户取首张已绑定银行卡。
 * 后端当前未提供提现接口，提交为本地模拟（可用余额转冻结 + 生成提现冻结记录），
 * 与 Web 端 `utils/account.ts` 的 withdraw / addFrozenRecord 行为一致。
 */
class WithdrawViewModel(
    private val accountRepository: AccountRepository,
    private val bankCardRepository: BankCardRepository,
    private val localAccountStore: LocalAccountStore,
) : ViewModel() {

    private val _state = MutableStateFlow(WithdrawUiState())
    val state: StateFlow<WithdrawUiState> = _state.asStateFlow()

    /** 进入页面：本地快照兜底 + 最新银行卡列表 + 服务端余额覆盖 */
    fun refresh() {
        _state.update {
            it.copy(
                available = Formatters.toAmount(localAccountStore.loadBalanceSnapshot().availableAmount),
                bankCard = bankCardRepository.loadCards().firstOrNull(),
            )
        }
        syncBalanceFromServer()
    }

    /** 查询服务端最新可用余额，失败时保留本地兜底数据 */
    fun syncBalanceFromServer() {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, balanceError = null) }
            when (val result = accountRepository.queryBalance()) {
                is AppResult.Success -> {
                    // 回写本地，供提现扣减与账户页兜底使用
                    val account = result.data
                    localAccountStore.saveBalanceSnapshot(account)
                    _state.update {
                        it.copy(
                            loading = false,
                            balanceError = null,
                            available = Formatters.toAmount(account.availableAmount),
                        )
                    }
                }

                is AppResult.Failure -> _state.update {
                    it.copy(loading = false, balanceError = result.message)
                }
            }
        }
    }

    fun onAmountChanged(text: String) = _state.update { it.copy(amountText = text) }

    /** 一键填入全部可用余额 */
    fun fillAllAvailable() = _state.update { it.copy(amountText = it.available.toInputText()) }

    /** 提交提现申请（本地模拟） */
    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            // 模拟请求耗时，与 Web 端保持一致
            delay(SUBMIT_DELAY_MILLIS)
            val amount = current.amount
            localAccountStore.applyWithdraw(amount)
            localAccountStore.addWithdrawRecord(genWithdrawOrderNo(), amount.toPlainString())
            _state.update { it.copy(submitting = false, successAmount = amount) }
        }
    }

    /** 生成模拟提现单号：TX + 时间数字 */
    private fun genWithdrawOrderNo(): String =
        "TX${SimpleDateFormat(ORDER_NO_PATTERN, Locale.CHINA).format(Date())}"

    private companion object {
        const val SUBMIT_DELAY_MILLIS = 800L
        const val ORDER_NO_PATTERN = "yyyyMMddHHmmss"
    }
}
