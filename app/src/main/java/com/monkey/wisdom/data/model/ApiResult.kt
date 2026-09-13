package com.monkey.wisdom.data.model

import com.google.gson.annotations.SerializedName

/**
 * 后端统一响应结构。
 *
 * 兼容性设计（与 Web 端 login.vue 的判断口径一致）：
 * - 字段名兼容 `message / msg / detail / error`；
 * - 成功判定兼容 `code=200/0/0000` 与 `success=true` 两种结构。
 */
data class ApiResult<T>(
    @SerializedName(value = "code", alternate = ["status", "resultCode"])
    val code: String? = null,
    @SerializedName(value = "message", alternate = ["msg", "detail", "error"])
    val message: String? = null,
    @SerializedName("success")
    val success: Boolean? = null,
    @SerializedName(value = "data", alternate = ["result", "body"])
    val data: T? = null,
) {
    /** 是否业务成功 */
    val isSuccess: Boolean
        get() = success == true || SUCCESS_CODES.contains(code)

    /** 错误提示文案，优先取后端 message */
    val errorMessage: String?
        get() = message?.takeIf { it.isNotBlank() }

    private companion object {
        val SUCCESS_CODES = setOf("200", "0", "0000", "00000")
    }
}
