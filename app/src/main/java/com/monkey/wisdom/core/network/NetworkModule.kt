package com.monkey.wisdom.core.network

import com.monkey.wisdom.BuildConfig
import com.monkey.wisdom.core.common.Json
import com.monkey.wisdom.core.network.interceptor.AuthInterceptor
import com.monkey.wisdom.core.network.interceptor.HeaderInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 网络基础设施（OkHttp + Retrofit 单例）。
 *
 * 说明：工程未引入 DI 框架，统一由本模块 + ServiceLocator 提供实例，保证单例与可替换性。
 */
object NetworkModule {

    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 20L
    private const val WRITE_TIMEOUT_SECONDS = 20L

    /** 全局 OkHttpClient：公共头 + 鉴权头 + Debug 日志 */
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(HeaderInterceptor())
            .addInterceptor(AuthInterceptor())
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        },
                    )
                }
            }
            .build()
    }

    /** Retrofit 实例，baseUrl 由 BuildConfig 注入（模拟器：10.0.2.2 指向宿主机） */
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(Json.gson))
            .build()
    }

    /** 创建 API 接口代理 */
    inline fun <reified T : Any> createApi(): T = retrofit.create(T::class.java)
}
