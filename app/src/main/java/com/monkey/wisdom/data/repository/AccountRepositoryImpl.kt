package com.monkey.wisdom.data.repository

import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.core.common.safeDataCall
import com.monkey.wisdom.data.model.AccountInfo
import com.monkey.wisdom.data.model.FrozenDetailPage
import com.monkey.wisdom.data.model.IncomeExpensePage
import com.monkey.wisdom.data.model.request.FrozenDetailQuery
import com.monkey.wisdom.data.model.request.IncomeExpenseQuery
import com.monkey.wisdom.data.model.request.RechargeRequest
import com.monkey.wisdom.data.remote.api.AccountApi

/**
 * 智运宝账户数据仓库实现。
 */
class AccountRepositoryImpl(
    private val accountApi: AccountApi,
) : AccountRepository {

    override suspend fun queryBalance(): AppResult<AccountInfo> =
        safeDataCall(fallbackMessage = "查询账户余额失败") { accountApi.queryBalance(hashMapOf()) }

    override suspend fun recharge(amount: Double): AppResult<AccountInfo> =
        safeDataCall(fallbackMessage = "充值失败，请稍后重试") { accountApi.recharge(RechargeRequest(amount)) }

    override suspend fun queryFrozenDetails(pageNum: Int, pageSize: Int): AppResult<FrozenDetailPage> =
        safeDataCall(fallbackMessage = "查询冻结明细失败") {
            accountApi.queryFrozenDetails(FrozenDetailQuery(pageNum, pageSize))
        }

    override suspend fun queryIncomeExpenseList(pageNum: Int, pageSize: Int): AppResult<IncomeExpensePage> =
        safeDataCall(fallbackMessage = "查询收支明细失败") {
            accountApi.queryIncomeExpenseList(IncomeExpenseQuery(pageNum, pageSize))
        }
}
