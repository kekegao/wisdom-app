package com.monkey.wisdom.core.common

import com.google.gson.JsonElement
import com.monkey.wisdom.core.exception.ApiBusinessException
import com.monkey.wisdom.core.network.safeApiCall
import com.monkey.wisdom.data.model.ApiResult

/**
 * 业务调用扩展：把后端 HTTP 200 但业务失败（success=false / code≠200）的情况转成失败结果。
 *
 * 各 Repository 统一复用，避免在每个方法里重复判断。
 */

/** 无返回数据的操作（如下单、摘单、发货），只关心是否成功 */
suspend fun safeUnitCall(
    fallbackMessage: String,
    block: suspend () -> ApiResult<JsonElement>,
): AppResult<Unit> = safeApiCall {
    val result = block()
    if (!result.isSuccess) {
        throw ApiBusinessException(result.errorMessage ?: fallbackMessage, result.code)
    }
}

/**
 * 无返回数据但需要展示后端文案的操作（如承运方确认发货）。
 *
 * 成功时把后端 `message` 原样透传给 UI，为空时由调用方用本地文案兜底。
 */
suspend fun safeMessageCall(
    fallbackMessage: String,
    block: suspend () -> ApiResult<JsonElement>,
): AppResult<String> = safeApiCall {
    val result = block()
    if (!result.isSuccess) {
        throw ApiBusinessException(result.errorMessage ?: fallbackMessage, result.code)
    }
    result.message.orEmpty()
}

/** 有返回数据的操作（如充值返回最新账户快照） */
suspend fun <T> safeDataCall(
    fallbackMessage: String,
    block: suspend () -> ApiResult<T>,
): AppResult<T> = safeApiCall {
    val result = block()
    if (!result.isSuccess) {
        throw ApiBusinessException(result.errorMessage ?: fallbackMessage, result.code)
    }
    val data = result.data ?: throw ApiBusinessException(fallbackMessage, result.code)
    data
}
