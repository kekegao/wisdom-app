package com.monkey.wisdom.ui.account

import androidx.lifecycle.ViewModel
import com.monkey.wisdom.data.model.BankCard
import com.monkey.wisdom.data.repository.BankCardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 单用户最多绑定的银行卡数量，与 Web 端 `bankList.vue` 的 MAX_CARDS 一致 */
const val BANK_CARD_MAX_COUNT = 6

/** 卡号长度限制（位） */
private const val CARD_NO_MIN_LENGTH = 15
private const val CARD_NO_MAX_LENGTH = 19

/** 卡号原始输入上限（位，含四位分组前的数字） */
private const val CARD_NO_INPUT_MAX = 23

/** 添加银行卡表单 */
data class BankCardFormState(
    val bank: String = "",
    /** 银行卡号（按四位分组展示） */
    val cardNo: String = "",
    /** 持卡人（选填，空值保存为「本人」） */
    val holder: String = "",
    val error: String = "",
)

/** 银行卡管理页状态 */
data class BankListUiState(
    val cards: List<BankCard> = emptyList(),
    /** 是否展示「添加银行卡」弹层 */
    val showAddSheet: Boolean = false,
    val form: BankCardFormState = BankCardFormState(),
    /** 待删除的银行卡，非空时展示二次确认 */
    val deleteTarget: BankCard? = null,
) {
    val cardsCount: Int get() = cards.size

    /** 是否已达绑定上限 */
    val reachLimit: Boolean get() = cards.size >= BANK_CARD_MAX_COUNT
}

/**
 * 银行卡管理 ViewModel。
 *
 * 对应 Web 端 `/bankList`：添加 / 解绑 / 列表均走本地存储
 * （后端当前未提供银行卡接口，与 Web 端 `utils/bankCard.ts` 行为一致）。
 */
class BankListViewModel(
    private val bankCardRepository: BankCardRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BankListUiState())
    val state: StateFlow<BankListUiState> = _state.asStateFlow()

    /** 重新读取银行卡列表（进入页面 / 增删后调用） */
    fun refresh() = _state.update { it.copy(cards = bankCardRepository.loadCards()) }

    /* ===== 添加银行卡 ===== */

    fun openAddSheet() = _state.update { it.copy(showAddSheet = true, form = BankCardFormState()) }

    fun closeAddSheet() = _state.update { it.copy(showAddSheet = false, form = BankCardFormState()) }

    fun selectBank(bank: String) = _state.update { it.copy(form = it.form.copy(bank = bank, error = "")) }

    /** 卡号输入：仅数字并自动四位分组 */
    fun onCardNoChanged(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(CARD_NO_INPUT_MAX)
        val grouped = digits.chunked(CARD_NO_GROUP_SIZE).joinToString(" ")
        _state.update { it.copy(form = it.form.copy(cardNo = grouped, error = "")) }
    }

    fun onHolderChanged(value: String) = _state.update { it.copy(form = it.form.copy(holder = value, error = "")) }

    /** 保存新增银行卡 */
    fun saveCard() {
        val form = _state.value.form
        val digits = form.cardNo.filter { it.isDigit() }
        if (form.bank.isBlank()) {
            _state.update { it.copy(form = it.form.copy(error = "请选择银行卡所属银行")) }
            return
        }
        if (digits.length < CARD_NO_MIN_LENGTH || digits.length > CARD_NO_MAX_LENGTH) {
            _state.update { it.copy(form = it.form.copy(error = "请输入正确的银行卡号（15~19 位数字）")) }
            return
        }
        bankCardRepository.addCard(bank = form.bank, cardNo = digits, holder = form.holder)
        _state.update { it.copy(cards = bankCardRepository.loadCards(), showAddSheet = false, form = BankCardFormState()) }
    }

    /* ===== 解绑银行卡 ===== */

    fun askDelete(card: BankCard) = _state.update { it.copy(deleteTarget = card) }

    fun cancelDelete() = _state.update { it.copy(deleteTarget = null) }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        val remaining = bankCardRepository.removeCard(target.id)
        _state.update { it.copy(cards = remaining, deleteTarget = null) }
    }

    private companion object {
        /** 卡号分组位数 */
        const val CARD_NO_GROUP_SIZE = 4
    }
}
