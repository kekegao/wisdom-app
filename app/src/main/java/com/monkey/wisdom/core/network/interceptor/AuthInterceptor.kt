package com.monkey.wisdom.core.network.interceptor

import com.monkey.wisdom.core.storage.UserSession
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 鉴权拦截器：登录后自动为请求追加 `Authorization: Bearer {token}`，与 Web 端 axios 拦截器一致。
 */
class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = UserSession.token
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header(HEADER_AUTHORIZATION, "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }

    private companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
    }
}
