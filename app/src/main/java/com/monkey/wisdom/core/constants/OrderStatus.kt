package com.monkey.wisdom.core.constants

/**
 * 运单状态机（与后端约定）：
 * 1发布 → 2摘单 → 3成交 → 4发货 → 5卸货 → 6确认收货 → 7货主运费结算 → 8承运方运费结算（完结）→ 9对账 → 10发票
 */
enum class OrderStatus(val code: Int, val label: String) {

    /** 已发布，等待承运方摘单 */
    PUBLISHED(1, "发布"),

    /** 已被承运方摘单，等待货主确认成交 */
    ACCEPTED(2, "摘单"),

    /** 已成交，承运方可确认发货 */
    DEALED(3, "成交"),

    /** 已发货，等待卸货 */
    SHIPPED(4, "发货"),

    /** 已卸货，等待货主确认收货 */
    RECEIPT_CONFIRMED(5, "卸货"),

    /** 已确认收货，等待货主发起运费结算 */
    RECEIPT_UPLOADED(6, "确认收货"),

    /** 已发起货主运费结算，等待平台办理结算 */
    SETTLE_APPLIED(7, "货主运费结算"),

    /** 承运方运费结算已完成，运单履约流程完结 */
    SETTLED(8, "承运方运费结算"),

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
