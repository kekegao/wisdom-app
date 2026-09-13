package com.monkey.wisdom.data.model

import com.monkey.wisdom.core.constants.UserType

/**
 * 本地登录用户信息（会话内使用）。
 */
data class UserInfo(
    val token: String? = null,
    val userId: Long? = null,
    /** 1 货主，2 司机 */
    val userType: Int = UserType.SHIPPER,
    val realName: String? = null,
    val userName: String? = null,
    val mobile: String? = null,
) {
    /** 展示名：真实姓名 > 用户名 > 手机号 */
    val displayName: String
        get() = realName?.takeIf { it.isNotBlank() }
            ?: userName?.takeIf { it.isNotBlank() }
            ?: mobile?.takeIf { it.isNotBlank() }
            ?: "用户"
}
