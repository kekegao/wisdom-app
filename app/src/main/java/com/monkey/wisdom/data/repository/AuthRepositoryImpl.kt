package com.monkey.wisdom.data.repository

import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.core.constants.UserType
import com.monkey.wisdom.core.exception.ApiBusinessException
import com.monkey.wisdom.core.network.safeApiCall
import com.monkey.wisdom.data.model.UserInfo
import com.monkey.wisdom.data.model.request.LoginRequest
import com.monkey.wisdom.data.model.request.RegisterRequest
import com.monkey.wisdom.data.model.response.LoginResponse
import com.monkey.wisdom.data.remote.api.AuthApi

/**
 * 认证数据仓库实现。
 */
class AuthRepositoryImpl(
    private val authApi: AuthApi,
) : AuthRepository {

    override suspend fun login(mobile: String, password: String): AppResult<UserInfo> = safeApiCall {
        val result = authApi.login(LoginRequest(mobile = mobile, password = password))
        if (!result.isSuccess) {
            // HTTP 200 也可能是业务失败（如 code=500），此处不进入登录成功流程
            throw ApiBusinessException(result.errorMessage ?: "登录失败，请稍后重试", result.code)
        }
        result.data.toUserInfo(fallbackMobile = mobile)
    }

    override suspend fun register(request: RegisterRequest): AppResult<Unit> = safeApiCall {
        val result = authApi.register(request)
        if (!result.isSuccess) {
            throw ApiBusinessException(result.errorMessage ?: "注册失败，请稍后重试", result.code)
        }
    }

    /** 响应体 → 会话用户信息，缺失字段用请求参数兜底 */
    private fun LoginResponse?.toUserInfo(fallbackMobile: String): UserInfo = UserInfo(
        token = this?.token,
        userId = this?.userId,
        userType = this?.userType ?: UserType.SHIPPER,
        realName = this?.realName,
        userName = this?.userName,
        mobile = this?.mobile ?: fallbackMobile,
    )
}
