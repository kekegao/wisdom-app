package com.monkey.wisdom.data.model.request

/**
 * 发布运单请求体，字段与 Web 端 `PublishOrderParams` 一致。
 *
 * 说明：Gson 默认不序列化 null 字段，与 axios 忽略 undefined 的行为一致，
 * 因此货主/承运方等由后端从登录态补全的字段保持 null 即可。
 */
data class PublishOrderRequest(
    val shipperUserId: String? = null,
    val shipperUserName: String? = null,
    val shipperName: String? = null,
    val shipperMobile: String? = null,
    val carrierUserId: String? = null,
    val carrierUserName: String? = null,
    val carrierName: String? = null,
    val carrierMobile: String? = null,

    /** 物品类型 */
    val goodsType: String,
    /** 物品描述 */
    val goodsDescription: String,
    /** 物品重量（吨） */
    val goodsWeight: Double,
    /** 运费（元），null 表示面议 */
    val transportMoney: Double? = null,

    // ==== 发货地 ====
    val shipperProvince: String,
    val shipperCity: String,
    val shipperArea: String,
    val shipperAddress: String,

    // ==== 收货地 ====
    val carrierProvince: String,
    val carrierCity: String,
    val carrierArea: String,
    val carrierAddress: String,
)

/** 订单列表查询条件（我的发布 / 我的运单共用） */
data class OrderListQuery(
    /** 单值精确匹配 */
    val status: Int? = null,
    /** IN 查询，优先级高于 status */
    val statusList: List<Int>? = null,
)

/** 承运端货源大厅查询条件 */
data class SourceOrderQuery(
    /** 发货地关键字 */
    val shipperKeyword: String? = null,
    /** 收货地关键字 */
    val carrierKeyword: String? = null,
)

/** 运单操作请求体（摘单 / 发货 / 确认收货 / 成交 / 取消摘单 / 回单确认 / 结算申请共用） */
data class OrderOperateRequest(
    val orderId: String,
)

/** 充值请求体 */
data class RechargeRequest(
    /** 充值金额（元） */
    val amount: Double,
)
