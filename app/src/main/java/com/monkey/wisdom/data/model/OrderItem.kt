package com.monkey.wisdom.data.model

/**
 * 运单模型（货主发布端 / 承运方接单端共用），字段与后端 OrderDto 对齐。
 *
 * 金额与重量统一用 [String] 承接后按数值解析，兼容后端返回 number 或 string 两种情况。
 */
data class OrderItem(
    val id: Long? = null,
    /** 运单号 */
    val orderId: String = "",
    /**
     * 订单状态：1发布 2摘单 3成交 4发货 5确认收货 6回单确认 7结算申请 8结算 9对账 10发票
     */
    val status: Int = 0,
    /** 订单状态描述（后端冗余字段，优先展示） */
    val statusDesc: String? = null,

    // ==== 货主信息 ====
    val shipperUserId: String? = null,
    val shipperUserName: String? = null,
    val shipperName: String? = null,
    val shipperMobile: String? = null,

    // ==== 承运方信息 ====
    val carrierUserId: String? = null,
    val carrierUserName: String? = null,
    val carrierName: String? = null,
    val carrierMobile: String? = null,

    // ==== 货物信息 ====
    /** 物品类型，例如建材、钢铁、煤炭 */
    val goodsType: String? = null,
    /** 物品描述 */
    val goodsDescription: String? = null,
    /** 物品重量（吨） */
    val goodsWeight: String? = null,
    /** 运费（元），空表示面议 */
    val transportMoney: String? = null,

    // ==== 发货地 ====
    val shipperProvince: String? = null,
    val shipperCity: String? = null,
    val shipperArea: String? = null,
    val shipperAddress: String? = null,

    // ==== 收货地 ====
    val carrierProvince: String? = null,
    val carrierCity: String? = null,
    val carrierArea: String? = null,
    val carrierAddress: String? = null,

    val deleteFlag: Int? = null,
    val createTime: String? = null,
    val updateTime: String? = null,
) {

    /** 发货地：省市区 + 详细地址 */
    val shipperFullAddress: String
        get() = joinAddress(shipperProvince, shipperCity, shipperArea, shipperAddress)

    /** 收货地：省市区 + 详细地址 */
    val carrierFullAddress: String
        get() = joinAddress(carrierProvince, carrierCity, carrierArea, carrierAddress)

    private fun joinAddress(vararg parts: String?): String =
        parts.filterNotNull().filter { it.isNotBlank() }.joinToString("")

    companion object {
        /** 便于筛选「全部」等场景 */
        const val STATUS_ALL = -1
    }
}
