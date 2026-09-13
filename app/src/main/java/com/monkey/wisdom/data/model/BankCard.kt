package com.monkey.wisdom.data.model

/**
 * 已绑定银行卡（当前后端未提供银行卡接口，与 Web 端保持一致采用本地存储）。
 */
data class BankCard(
    val id: Long,
    /** 银行名称 */
    val bank: String,
    /** 卡片类型：储蓄卡 / 信用卡 */
    val cardType: String,
    /** 卡号 */
    val cardNo: String,
    /** 持卡人 */
    val holder: String,
    /** 添加时间（yyyy-MM-dd HH:mm） */
    val addTime: String,
)
