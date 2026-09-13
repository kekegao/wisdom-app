package com.monkey.wisdom.data.model

/**
 * 冻结明细，字段与后端 FrozenDetailDto 对齐（资金托管 / 提现冻结）。
 */
data class FrozenDetail(
    val id: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    /** 冻结业务类型：1运费托管 2提现冻结 */
    val bizType: Int? = null,
    /** 业务类型名称（冗余）：运费托管 / 提现冻结 */
    val bizTypeName: String? = null,
    /** 关联业务ID（运单ID / 提现申请ID） */
    val refId: String? = null,
    /** 关联单号（YD 运单号 / TX 提现单号） */
    val orderNo: String? = null,
    /** 冻结金额（元） */
    val amount: String? = null,
    val frozenTime: String? = null,
    /** 状态：1冻结中 2已解冻 3已打款 */
    val status: Int? = null,
    /** 状态描述（冗余）：冻结中 / 已解冻 / 已打款 */
    val statusDesc: String? = null,
    val finishTime: String? = null,
    val remark: String? = null,
    val createTime: String? = null,
    val updateTime: String? = null,
)
