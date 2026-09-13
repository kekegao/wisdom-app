package com.monkey.wisdom.core.network

import com.google.gson.JsonParseException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 异常 → 用户可读文案的统一映射，避免把技术细节暴露给用户。
 */
object ErrorMapper {

    fun toReadableMessage(throwable: Throwable): String = when (throwable) {
        is SocketTimeoutException -> "请求超时，请稍后重试"
        is UnknownHostException, is ConnectException ->
            "无法连接服务器，请确认后端服务已启动（模拟器访问宿主机需使用 10.0.2.2）"

        is HttpException -> "服务器开小差了（HTTP ${throwable.code()}），请稍后重试"
        is JsonParseException -> "数据解析失败，请稍后重试"
        is IOException -> "网络异常，请检查网络连接"
        else -> throwable.message?.takeIf { it.isNotBlank() } ?: "请求失败，请稍后重试"
    }
}
