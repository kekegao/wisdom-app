package com.monkey.wisdom.data.model.response

import com.google.gson.annotations.SerializedName

/**
 * 登录响应体。
 *
 * 字段名做多别名兼容（token / accessToken / access_token ...），避免后端字段调整导致解析为空。
 */
data class LoginResponse(
    @SerializedName(value = "token", alternate = ["accessToken", "access_token", "tokenValue", "authToken", "jwt"])
    val token: String? = null,
    @SerializedName("userId")
    val userId: Long? = null,
    @SerializedName(value = "userType", alternate = ["user_type"])
    val userType: Int? = null,
    @SerializedName(value = "realName", alternate = ["real_name"])
    val realName: String? = null,
    @SerializedName(value = "userName", alternate = ["user_name", "nickName"])
    val userName: String? = null,
    @SerializedName("mobile")
    val mobile: String? = null,
)
