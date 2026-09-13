package com.monkey.wisdom.data.model

/**
 * 智运宝账户快照，字段与后端 AccountDto 对齐。
 *
 * 金额字段后端可能返回 number 或 string，统一用 [String] 承接，展示与计算时再转 BigDecimal，
 * 避免浮点误差（资金相关字段不做 Double 运算）。
 */
data class AccountInfo(
    val id: Long? = null,
    val userId: String? = null,
    val userName: String? = null,
    val realName: String? = null,
    val mobile: String? = null,
    /** 总余额（元） */
    val balance: String? = null,
    /** 冻结金额（元） */
    val frozenAmount: String? = null,
    /** 可用金额（元） */
    val availableAmount: String? = null,
    /** 状态：1 正常 */
    val status: Int? = null,
    val updateTime: String? = null,
)
