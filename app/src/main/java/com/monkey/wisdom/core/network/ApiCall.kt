package com.monkey.wisdom.core.network

import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.core.exception.ApiBusinessException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * 安全调用网络请求：统一切换 IO 线程、捕获异常并转换为 [AppResult]。
 *
 * 注意：协程取消异常需重新抛出，避免破坏结构化并发。
 */
suspend fun <T> safeApiCall(block: suspend () -> T): AppResult<T> = withContext(Dispatchers.IO) {
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiBusinessException) {
        AppResult.Failure(e.message ?: "请求失败", e.code, e)
    } catch (e: Exception) {
        AppResult.Failure(ErrorMapper.toReadableMessage(e), cause = e)
    }
}
