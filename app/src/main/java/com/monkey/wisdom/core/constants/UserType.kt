package com.monkey.wisdom.core.constants

/**
 * 用户类型（与后端约定：1 货主，2 司机）。
 */
object UserType {

    /** 货主 */
    const val SHIPPER = 1

    /** 司机 */
    const val CARRIER = 2

    /** 注册页可选类型 */
    val registerOptions: List<UserTypeOption> = listOf(
        UserTypeOption(SHIPPER, "货主"),
        UserTypeOption(CARRIER, "司机"),
    )

    /** 类型名称，未知类型兜底为货主 */
    fun labelOf(type: Int?): String =
        registerOptions.firstOrNull { it.value == type }?.label ?: "货主"
}

/** 用户类型选项 */
data class UserTypeOption(
    val value: Int,
    val label: String,
)
