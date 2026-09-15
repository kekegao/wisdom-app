package com.monkey.wisdom.core.constants

/**
 * 运单状态机（与后端约定）：
 * 1发布 → 2摘单 → 3成交 → 4发货 → 5确认收货 → 6回单确认 → 7结算申请 → 8结算 → 9对账 → 10发票
 */
enum class OrderStatus(val code: Int, val label: String) {

    /** 已发布，等待承运方摘单 */
    PUBLISHED(1, "发布"),

    /** 已被承运方摘单，等待货主确认成交 */
    ACCEPTED(2, "摘单"),

    /** 已成交，承运方可确认发货 */
    DEALED(3, "成交"),

    /** 已发货，等待确认收货 */
    SHIPPED(4, "发货"),

    /** 已确认收货，等待货主回单确认 */
    RECEIPT_CONFIRMED(5, "确认收货"),

    /** 已回单确认，等待货主发起结算申请 */
    RECEIPT_UPLOADED(6, "回单确认"),

    /** 已结算申请 */
    SETTLE_APPLIED(7, "结算申请"),

    /** 已结算 */
    SETTLED(8, "结算"),

    /** 已对账 */
    RECONCILED(9, "已对账"),

    /** 已开票 */
    INVOICED(10, "发票"),
    ;

    companion object {

        /** 编码 → 状态，未知返回 null */
        fun fromCode(code: Int?): OrderStatus? = entries.firstOrNull { it.code == code }

        /** 展示文案：优先使用后端冗余描述，缺失时按本地枚举兜底 */
        fun labelOf(code: Int?, desc: String? = null): String {
            if (!desc.isNullOrBlank()) return desc
            return fromCode(code)?.label ?: "未知状态"
        }
    }
}
