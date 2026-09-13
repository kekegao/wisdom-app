package com.monkey.wisdom.data.model.request

/**
 * 注册请求体（POST /app/register）
 */
data class RegisterRequest(
    /** 用户类型：1 货主，2 司机 */
    val userType: Int,
    val realName: String,
    val mobile: String,
    val password: String,
)
