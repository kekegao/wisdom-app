package com.monkey.wisdom.core.constants

import androidx.compose.ui.graphics.Color

/**
 * 常用银行清单（含品牌渐变色），与 Web 端 `utils/bankCard.ts` 的 BANK_OPTIONS 保持一致。
 */
object BankOptions {

    val all: List<BankOption> = listOf(
        BankOption("招商银行", Color(0xFFE2574C), Color(0xFFA8322A)),
        BankOption("工商银行", Color(0xFFC9212F), Color(0xFF8E1520)),
        BankOption("建设银行", Color(0xFF2A6FD6), Color(0xFF164F9E)),
        BankOption("农业银行", Color(0xFF33A25D), Color(0xFF0F7A3D)),
        BankOption("中国银行", Color(0xFFC3272E), Color(0xFF8C1620)),
        BankOption("交通银行", Color(0xFF3F6FD8), Color(0xFF1F47A0)),
        BankOption("邮政储蓄银行", Color(0xFF3EA963), Color(0xFF1F7A41)),
        BankOption("中信银行", Color(0xFFD4513F), Color(0xFF9F3526)),
        BankOption("浦发银行", Color(0xFF2D63A8), Color(0xFF163D6B)),
    )

    private val fallback = BankOption(
        name = "其它银行",
        from = Color(0xFF64748B),
        to = Color(0xFF334155),
    )

    /** 按银行名称取品牌色，未收录时使用默认灰 */
    fun of(bank: String): BankOption = all.firstOrNull { it.name == bank } ?: fallback.copy(name = bank.ifBlank { fallback.name })
}

/** 银行品牌色选项 */
data class BankOption(
    val name: String,
    /** 渐变起始色 */
    val from: Color,
    /** 渐变结束色 */
    val to: Color,
)
