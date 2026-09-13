package com.monkey.wisdom.data.model.request

/**
 * 登录请求体（POST /app/login）
 */
data class LoginRequest(
    val mobile: String,
    val password: String,
)
