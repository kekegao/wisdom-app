package com.monkey.wisdom.data.repository

import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.data.model.AccountInfo
import com.monkey.wisdom.data.model.FrozenDetail
import com.monkey.wisdom.data.model.IncomeExpensePage

/**
 * 智运宝账户数据仓库。
 */
interface AccountRepository {

    /** 查询账户余额 */
    suspend fun queryBalance(): AppResult<AccountInfo>

    /** 充值，成功时返回最新账户快照 */
    suspend fun recharge(amount: Double): AppResult<AccountInfo>

    /** 查询「冻结中」的冻结明细 */
    suspend fun queryFrozenDetails(): AppResult<List<FrozenDetail>>

    /** 分页查询当前登录用户的收支流水 */
    suspend fun queryIncomeExpenseList(pageNum: Int, pageSize: Int): AppResult<IncomeExpensePage>
}
