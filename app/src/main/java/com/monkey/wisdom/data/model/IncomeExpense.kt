package com.monkey.wisdom.data.model

/**
 * 账户收支流水模型，字段与后端 `IncomeExpenseDto` 对齐。
 *
 * 金额统一用 [String] 承接后按数值解析，兼容后端返回 number 或 string 两种情况
 * （与 [FrozenDetail]、[OrderItem] 的处理方式保持一致）。
 */
data class IncomeExpenseItem(
    /** 主键ID */
    val id: Long? = null,
    /** 收支流水号 */
    val flowNo: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    /** 账户类型：1用户账户 2平台公司对公账户 */
    val accountType: Int? = null,
    /** 记账方向：1收入 2支出 3冻结 4解冻 */
    val direction: Int? = null,
    /** 方向描述（后端冗余字段，优先展示） */
    val directionDesc: String? = null,
    /** 业务类型：1充值 2提现 3运费托管 4发货保证金 5货主清算扣划 6承运方清算划账 7人工调账 */
    val bizType: Int? = null,
    /** 业务类型名称 */
    val bizTypeName: String? = null,
    /** 收支科目：0不适用 1运费收入 2运费支出 3平台服务费 4充值 5提现 6保证金 7人工调账 8其他 */
    val incomeExpenseType: Int? = null,
    /** 收支科目名称 */
    val incomeExpenseTypeName: String? = null,
    /** 发生金额（元，恒为正数） */
    val amount: String? = null,
    /** 变动前 / 变动后账户余额 */
    val balanceBefore: String? = null,
    val balanceAfter: String? = null,
    /** 变动前 / 变动后可用余额 */
    val availableBefore: String? = null,
    val availableAfter: String? = null,
    /** 变动前 / 变动后冻结金额 */
    val frozenBefore: String? = null,
    val frozenAfter: String? = null,
    /** 对手方信息 */
    val counterpartyUserId: String? = null,
    val counterpartyName: String? = null,
    val counterpartyAccountType: Int? = null,
    /** 关联运单号 */
    val orderId: String? = null,
    /** 关联业务单号 */
    val bizNo: String? = null,
    /** 资金发生时间 */
    val flowTime: String? = null,
    /** 状态：1成功 2处理中 3失败 4已冲正 */
    val status: Int? = null,
    /** 状态描述 */
    val statusDesc: String? = null,
    /** 关联原流水号 */
    val relatedFlowNo: String? = null,
    val remark: String? = null,
    val createTime: String? = null,
    val updateTime: String? = null,
)

/** 收支流水分页结果，字段与后端 `IncomeExpensePageDto` 对齐 */
data class IncomeExpensePage(
    /** 当前页数据 */
    val records: List<IncomeExpenseItem>? = null,
    /** 总记录数 */
    val total: Long? = null,
    /** 总页数 */
    val pages: Long? = null,
    /** 当前页码 */
    val current: Long? = null,
    /** 每页大小 */
    val size: Long? = null,
)
