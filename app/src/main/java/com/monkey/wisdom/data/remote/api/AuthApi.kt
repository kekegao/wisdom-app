package com.monkey.wisdom.data.remote.api

import com.google.gson.JsonElement
import com.monkey.wisdom.data.model.ApiResult
import com.monkey.wisdom.data.model.request.LoginRequest
import com.monkey.wisdom.data.model.request.RegisterRequest
import com.monkey.wisdom.data.model.response.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 认证相关接口。
 *
 * baseUrl 为 `http://host:8083/`，与 Web 端 `/api/app/login` 经代理转发后的地址一致。
 */
interface AuthApi {

    /** 登录 */
    @POST("app/login")
    suspend fun login(@Body request: LoginRequest): ApiResult<LoginResponse>

    /** 注册（返回体只关心业务码与提示，故用 JsonElement 承接） */
    @POST("app/register")
    suspend fun register(@Body request: RegisterRequest): ApiResult<JsonElement>
}
