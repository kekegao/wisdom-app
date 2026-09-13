package com.monkey.wisdom.core.network.interceptor

import com.monkey.wisdom.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 公共请求头拦截器：统一声明客户端类型与版本，便于后端做灰度与埋点统计。
 */
class HeaderInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(HEADER_ACCEPT, "application/json")
            .header(HEADER_CLIENT_TYPE, "android")
            .header(HEADER_CLIENT_VERSION, BuildConfig.VERSION_NAME)
            .build()
        return chain.proceed(request)
    }

    private companion object {
        const val HEADER_ACCEPT = "Accept"
        const val HEADER_CLIENT_TYPE = "X-Client-Type"
        const val HEADER_CLIENT_VERSION = "X-Client-Version"
    }
}
