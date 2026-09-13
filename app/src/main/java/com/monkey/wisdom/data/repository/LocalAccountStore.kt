package com.monkey.wisdom.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.monkey.wisdom.core.common.Json
import com.monkey.wisdom.core.util.Formatters
import com.monkey.wisdom.data.model.AccountInfo
import com.monkey.wisdom.data.model.FrozenDetail
import java.math.BigDecimal

/**
 * 账户本地存储。
 *
 * 用途：
 * - 后端当前未提供提现接口，提现为本地模拟（与 Web 端 `utils/account.ts` 行为一致），
 *   提现产生的「提现冻结」记录落本地，在账户页与后端返回的冻结明细合并展示；
 * - 保留一份余额快照，作为后端不可用时账户页 / 充值页 / 提现页的兜底数据。
 */
interface LocalAccountStore {

    /** 本地提现冻结记录（最新在前） */
    fun loadWithdrawRecords(): List<FrozenDetail>

    /** 新增一条提现冻结记录 */
    fun addWithdrawRecord(orderNo: String, amount: String): FrozenDetail

    /** 本地余额快照（无本地数据时返回默认值） */
    fun loadBalanceSnapshot(): AccountInfo

    /** 以最新账户快照覆盖本地（余额查询 / 充值成功后调用） */
    fun saveBalanceSnapshot(account: AccountInfo)

    /** 提现：可用余额减少并转入冻结，返回更新后的快照 */
    fun applyWithdraw(amount: BigDecimal): AccountInfo
}

class LocalAccountStoreImpl(context: Context) : LocalAccountStore {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override fun loadWithdrawRecords(): List<FrozenDetail> {
        val raw = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        return runCatching {
            Json.gson.fromJson(raw, Array<FrozenDetail>::class.java)?.toList().orEmpty()
        }.getOrDefault(emptyList())
    }

    override fun addWithdrawRecord(orderNo: String, amount: String): FrozenDetail {
        val record = FrozenDetail(
            id = orderNo,
            bizType = BIZ_TYPE_WITHDRAW,
            bizTypeName = BIZ_TYPE_WITHDRAW_NAME,
            orderNo = orderNo,
            amount = amount,
            frozenTime = Formatters.nowText(),
            status = STATUS_FROZEN,
            statusDesc = STATUS_TEXT_FROZEN,
            remark = "提现申请已提交，预计 2 小时内到账",
        )
        val updated = listOf(record) + loadWithdrawRecords()
        prefs.edit().putString(KEY_RECORDS, Json.gson.toJson(updated)).apply()
        return record
    }

    override fun loadBalanceSnapshot(): AccountInfo {
        val raw = prefs.getString(KEY_BALANCE, null) ?: return DEFAULT_BALANCE
        return runCatching { Json.gson.fromJson(raw, AccountInfo::class.java) }
            .getOrNull()
            ?: DEFAULT_BALANCE
    }

    override fun saveBalanceSnapshot(account: AccountInfo) {
        prefs.edit().putString(KEY_BALANCE, Json.gson.toJson(account)).apply()
    }

    override fun applyWithdraw(amount: BigDecimal): AccountInfo {
        val current = loadBalanceSnapshot()
        val next = current.copy(
            availableAmount = (Formatters.toAmount(current.availableAmount) - amount).toPlainString(),
            frozenAmount = (Formatters.toAmount(current.frozenAmount) + amount).toPlainString(),
            updateTime = Formatters.nowText(),
        )
        saveBalanceSnapshot(next)
        return next
    }

    private companion object {
        const val PREF_NAME = "wisdom_local_account"
        const val KEY_RECORDS = "key_withdraw_records"
        const val KEY_BALANCE = "key_balance_snapshot"

        /** 冻结业务类型：2 提现冻结 */
        const val BIZ_TYPE_WITHDRAW = 2
        const val BIZ_TYPE_WITHDRAW_NAME = "提现冻结"
        const val STATUS_FROZEN = 1
        const val STATUS_TEXT_FROZEN = "处理中"

        /** 默认账户快照，与 Web 端 `utils/account.ts` 的 DEFAULT_BALANCE 保持一致 */
        val DEFAULT_BALANCE = AccountInfo(
            balance = "12850",
            availableAmount = "8749.5",
            frozenAmount = "4100.5",
            updateTime = "2026-09-04 10:32",
        )
    }
}
