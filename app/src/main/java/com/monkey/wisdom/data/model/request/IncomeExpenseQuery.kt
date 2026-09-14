package com.monkey.wisdom.data.model.request

/**
 * 收支流水查询条件，字段与后端 `IncomeExpenseQueryRequest` 对齐。
 *
 * 用户身份由后端从登录会话回填，客户端只需传分页参数。
 */
data class IncomeExpenseQuery(
    /** 页码，从 1 开始 */
    val pageNum: Int,
    /** 每页条数，默认 20（后端上限 100） */
    val pageSize: Int,
)
