package com.monkey.wisdom.core.common

/**
 * 统一的业务调用结果，屏蔽网络异常与业务错误码差异，供 Repository 向上层返回。
 *
 * 约定：数据层不向上抛异常，UI 层只需处理 [Success] / [Failure] 两种分支。
 */
sealed interface AppResult<out T> {

    /** 调用成功 */
    data class Success<T>(val data: T) : AppResult<T>

    /**
     * 调用失败
     *
     * @param message 可直接展示给用户的提示文案
     * @param code    后端业务码（网络异常时为空）
     * @param cause   原始异常，便于日志排查
     */
    data class Failure(
        val message: String,
        val code: String? = null,
        val cause: Throwable? = null,
    ) : AppResult<Nothing>
}

/** 成功时返回数据，失败时返回 null */
fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data

/** 失败时的提示文案，成功时为 null */
fun <T> AppResult<T>.errorMessageOrNull(): String? = (this as? AppResult.Failure)?.message
