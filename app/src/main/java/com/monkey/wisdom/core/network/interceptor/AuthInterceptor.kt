package com.monkey.wisdom.core.network.interceptor

import com.monkey.wisdom.core.exception.SessionExpiredException
import com.monkey.wisdom.core.storage.SessionExpiredBus
import com.monkey.wisdom.core.storage.UserSession
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * 鉴权拦截器。
 *
 * 职责（与 Web 端 axios 拦截器对齐）：
 * - 登录后自动为请求追加 `Authorization: Bearer {token}`；
 * - 接口返回 401 时判定为登录态失效：清空本地会话、广播过期事件（UI 回退登录页），
 *   并抛出 [SessionExpiredException] 终止本次调用，避免上层把 401 当普通业务数据处理。
 *
 * 跳过判定的情况：未携带 token、标记了 [SkipSessionCheck] 的请求、登录 / 注册接口
 * （这两类的 401 属于账号密码错误，不应踢出登录页）。
 */
class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = UserSession.token
        val request = chain.request()
        val authorizedRequest = if (token.isNullOrBlank()) {
            request
        } else {
            request.newBuilder()
                .header(HEADER_AUTHORIZATION, "Bearer $token")
                .build()
        }

        val response = chain.proceed(authorizedRequest)
        if (shouldSkipSessionCheck(request, token)) return response

        if (response.code == HTTP_UNAUTHORIZED) {
            response.close()
            SessionExpiredBus.notifyExpired()
            throw SessionExpiredException()
        }
        return response
    }

    /** 无需判定会话过期的请求 */
    private fun shouldSkipSessionCheck(request: Request, token: String?): Boolean =
        token.isNullOrBlank() ||
            request.tag(SkipSessionCheck::class.java) != null ||
            request.header(HEADER_SKIP_SESSION_CHECK) != null ||
            isAuthEndpoint(request)

    /** 登录 / 注册接口：失败属于账号密码问题，不是会话过期 */
    private fun isAuthEndpoint(request: Request): Boolean {
        val path = request.url.encodedPath
        return AUTH_ENDPOINTS.any { path.endsWith(it) }
    }

    private companion object {
        const val HEADER_AUTHORIZATION = "Authorization"

        /** 公开接口标记：打了该头的请求不做会话过期判定 */
        const val HEADER_SKIP_SESSION_CHECK = "X-Skip-Session-Check"

        const val HTTP_UNAUTHORIZED = 401
        val AUTH_ENDPOINTS = listOf("/app/login", "/app/register")
    }
}
