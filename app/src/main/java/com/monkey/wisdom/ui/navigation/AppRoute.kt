package com.monkey.wisdom.ui.navigation

/**
 * 全部页面路由（与 Web 端 vue-router 路径保持一致，便于两端对照排查）。
 */
object AppRoute {

    // ==== 认证 ====
    const val SHIPPER_HOME = "shipperHome"
    const val CARRIER_HOME = "carrierHome"

    // ==== 运单 ====
    const val PUBLISH_ORDER = "publishOrder"
    const val PUBLISH_ORDER_LIST = "publishOrderList"
    const val CARRIER_ACCEPT_ORDER = "carrierAcceptOrder"
    const val CARRIER_ORDER_LIST = "carrierOrderList"

    // ==== 账户 ====
    const val ACCOUNT = "account"
    const val RECHARGE = "recharge"
    const val WITHDRAW = "withdraw"
    const val BANK_LIST = "bankList"
}
