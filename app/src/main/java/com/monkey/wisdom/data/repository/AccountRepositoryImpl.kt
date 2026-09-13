package com.monkey.wisdom.data.repository

import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.core.common.safeDataCall
import com.monkey.wisdom.data.model.AccountInfo
import com.monkey.wisdom.data.model.FrozenDetail
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

    override suspend fun queryFrozenDetails(): AppResult<List<FrozenDetail>> =
        safeDataCall(fallbackMessage = "查询冻结明细失败") { accountApi.queryFrozenDetails(hashMapOf()) }
}
