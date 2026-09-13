package com.monkey.wisdom.core.exception

/**
 * 业务异常：后端返回 HTTP 200 但业务码非成功（如 code=500）时抛出。
 */
class ApiBusinessException(
    message: String,
    val code: String? = null,
) : RuntimeException(message)
