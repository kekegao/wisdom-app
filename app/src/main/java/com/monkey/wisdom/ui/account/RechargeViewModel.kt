package com.monkey.wisdom.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.core.util.Formatters
import com.monkey.wisdom.data.repository.AccountRepository
import com.monkey.wisdom.data.repository.LocalAccountStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

/** 支付渠道（模拟，仅作展示） */
data class PayChannel(
    val id: String,
    val name: String,
    val desc: String,
)

/** 可选的支付渠道，与 Web 端 `recharge.vue` 的 payChannels 一致 */
val PAY_CHANNELS: List<PayChannel> = listOf(
    PayChannel("wechat", "微信支付", "微信安全支付"),
    PayChannel("alipay", "支付宝", "推荐有礼"),
    PayChannel("bank", "银行卡", "快捷支付"),
)

/** 快捷充值金额档位 */
val RECHARGE_QUICK_AMOUNTS: List<Int> = listOf(100, 200, 500, 1000, 2000)

/** 单笔充值上限（元） */
val RECHARGE_MAX_AMOUNT: BigDecimal = BigDecimal("50000")

/**
 * 充值页状态。
 *
 * 校验规则与 Web 端 `recharge.vue` 一致：金额 > 0 且不超过单笔上限。
 */
data class RechargeUiState(
    /** 充值金额（字符串便于输入控制） */
    val amountText: String = "",
    /** 当前选中的支付渠道 */
    val channel: String = PAY_CHANNELS.first().id,
    val submitting: Boolean = false,
    /** 充值成功金额，非空时展示成功弹层 */
    val successAmount: BigDecimal? = null,
    /** 接口提交失败提示（与输入校验共用同一错误区） */
    val submitError: String? = null,
) {

    /** 解析后的充值金额（两位小数） */
    val amount: BigDecimal get() = parseAmountInput(amountText)

    /** 输入校验提示 */
    val errorText: String
        get() = when {
            amountText.isEmpty() -> ""
            amount <= BigDecimal.ZERO -> "请输入大于 0 的充值金额"
            amount > RECHARGE_MAX_AMOUNT -> "单笔充值金额不能超过 ¥${Formatters.money(RECHARGE_MAX_AMOUNT)}"
            else -> ""
        }

    /** 错误区文案：优先展示输入校验，其次展示接口失败提示 */
    val errorTextOrSubmitError: String get() = errorText.ifEmpty { submitError.orEmpty() }

    /** 是否命中快捷金额 */
    fun isQuick(quick: Int): Boolean = amount.compareTo(BigDecimal(quick)) == 0

    val canSubmit: Boolean
        get() = !submitting && successAmount == null && errorText.isEmpty() && amount > BigDecimal.ZERO
}

/**
 * 充值 ViewModel。
 *
 * 对应 Web 端 `/recharge`：调用智运宝账户充值接口（POST account/recharge），
 * 成功后以服务端返回的最新账户快照回写本地，账户页 / 提现页即时可见。
 */
class RechargeViewModel(
    private val accountRepository: AccountRepository,
    private val localAccountStore: LocalAccountStore,
) : ViewModel() {

    private val _state = MutableStateFlow(RechargeUiState())
    val state: StateFlow<RechargeUiState> = _state.asStateFlow()

    fun onAmountChanged(text: String) = _state.update { it.copy(amountText = text, submitError = null) }

    /** 点击快捷金额 */
    fun selectQuickAmount(amount: Int) =
        _state.update { it.copy(amountText = amount.toString(), submitError = null) }

    fun selectChannel(channelId: String) = _state.update { it.copy(channel = channelId) }

    /** 提交充值 */
    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, submitError = null) }
            when (val result = accountRepository.recharge(current.amount.toDouble())) {
                is AppResult.Success -> {
                    // 服务端返回最新账户快照，直接覆盖本地余额
                    localAccountStore.saveBalanceSnapshot(result.data)
                    _state.update { it.copy(submitting = false, successAmount = current.amount) }
                }

                is AppResult.Failure -> _state.update {
                    it.copy(submitting = false, submitError = result.message)
                }
            }
        }
    }
}
