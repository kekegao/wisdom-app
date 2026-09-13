package com.monkey.wisdom.core.common

import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * 全局 Gson 单例：统一序列化配置，避免多处 `Gson()` 造成行为不一致。
 *
 * - 关闭 HTML 转义，保证中文与 URL 原样输出（本地缓存可读性更好）；
 * - 解析失败不抛异常，返回 null 由调用方兜底。
 */
object Json {

    val gson: Gson = GsonBuilder()
        .disableHtmlEscaping()
        .create()

    /** 解析 JSON，失败返回 null */
    inline fun <reified T> fromJson(json: String): T? =
        runCatching { gson.fromJson(json, T::class.java) }.getOrNull()

    /** 序列化为 JSON */
    inline fun <reified T> toJson(value: T): String = gson.toJson(value)
}
