package com.monkey.wisdom.data.remote.api

import com.monkey.wisdom.data.model.AccountInfo
import com.monkey.wisdom.data.model.ApiResult
import com.monkey.wisdom.data.model.FrozenDetail
import com.monkey.wisdom.data.model.request.RechargeRequest
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 智运宝账户相关接口。当前登录用户由后端从登录会话中解析，客户端只需携带 token。
 */
interface AccountApi {

    /** 账户余额（返回体 data 为账户对象） */
    @POST("account/balance")
    suspend fun queryBalance(@Body body: HashMap<String, Any?>): ApiResult<AccountInfo>

    /** 充值（返回最新账户快照） */
    @POST("account/recharge")
    suspend fun recharge(@Body request: RechargeRequest): ApiResult<AccountInfo>

    /** 冻结中明细列表（返回体 data 为数组） */
    @POST("frozenDetail/list")
    suspend fun queryFrozenDetails(@Body body: HashMap<String, Any?>): ApiResult<List<FrozenDetail>>
}
