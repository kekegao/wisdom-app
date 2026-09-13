package com.monkey.wisdom.core.util

import com.monkey.wisdom.data.model.UserInfo
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 展示层统一格式化工具：金额、时间、手机号与卡号脱敏。
 *
 * 与 Web 端 `utils/account.ts`、`utils/bankCard.ts` 的行为保持一致，避免多端展示差异。
 */
object Formatters {

    private const val FALLBACK_AMOUNT_TEXT = "0.00"
    private const val FALLBACK_TIME_TEXT = "-"

    /** 字符串金额 → BigDecimal，非法或空值按 0 处理 */
    fun toAmount(value: String?): BigDecimal =
        value?.trim()?.takeIf { it.isNotEmpty() }?.toBigDecimalOrNull() ?: BigDecimal.ZERO

    /** 金额格式化：千分位 + 两位小数 */
    fun money(value: String?): String = money(toAmount(value))

    /** 金额格式化：千分位 + 两位小数 */
    fun money(value: BigDecimal): String = String.format(Locale.CHINA, "%,.2f", value)

    /** 金额格式化（元 → 展示文本），空值显示 - */
    fun moneyOrDash(value: String?): String =
        if (value.isNullOrBlank()) "-" else money(value)

    /** 重量格式化：去掉多余小数位（12.0 → 12，12.5 → 12.5） */
    fun weight(value: String?): String {
        val decimal = value?.trim()?.takeIf { it.isNotEmpty() }?.toBigDecimalOrNull()
            ?: return FALLBACK_AMOUNT_TEXT
        return decimal.stripTrailingZeros().toPlainString()
    }

    /** 时间文本：后端返回 "yyyy-MM-dd HH:mm:ss" 时截断到分钟，其它情况原样返回 */
    fun time(raw: String?): String {
        if (raw.isNullOrBlank()) return FALLBACK_TIME_TEXT
        return if (raw.length >= 16) raw.take(16) else raw
    }

    /** 当前时间文本（yyyy-MM-dd HH:mm） */
    fun nowText(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date())

    /** 手机号脱敏：138****8000 */
    fun maskMobile(mobile: String?): String {
        val value = mobile?.trim().orEmpty()
        return if (MOBILE_REGEX.matches(value)) "${value.take(3)}****${value.takeLast(4)}" else value
    }

    /** 姓名脱敏：仅保留姓氏，如「张**」 */
    fun maskRealName(name: String?): String {
        val value = name?.trim().orEmpty()
        return when {
            value.isEmpty() -> ""
            value.length == 1 -> value
            else -> "${value.first()}${"*".repeat(value.length - 1)}"
        }
    }

    /**
     * 欢迎语展示名：真实姓名 > 用户名 > 脱敏手机号 > 兜底文案。
     *
     * 与 Web 端首页一致：值为手机号时自动脱敏。
     */
    fun displayName(user: UserInfo?, fallback: String): String {
        val raw = user?.realName?.takeIf { it.isNotBlank() }
            ?: user?.userName?.takeIf { it.isNotBlank() }
            ?: user?.mobile?.takeIf { it.isNotBlank() }
            ?: return fallback
        return if (MOBILE_REGEX.matches(raw)) maskMobile(raw) else raw
    }

    /** 卡号掩码：**** **** **** 6821 */
    fun maskCardNo(cardNo: String): String = "**** **** **** ${cardTail(cardNo)}"

    /** 卡号后四位 */
    fun cardTail(cardNo: String): String = cardNo.trim().takeLast(4)

    private val MOBILE_REGEX = Regex("^1\\d{10}$")
}
