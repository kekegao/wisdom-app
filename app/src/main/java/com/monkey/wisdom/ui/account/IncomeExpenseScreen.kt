package com.monkey.wisdom.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monkey.wisdom.core.util.Formatters
import com.monkey.wisdom.data.model.IncomeExpenseItem
import com.monkey.wisdom.di.ServiceLocator
import com.monkey.wisdom.ui.components.AppScaffold
import com.monkey.wisdom.ui.components.EmptyBox
import com.monkey.wisdom.ui.components.SectionCard
import com.monkey.wisdom.ui.theme.BgCard
import com.monkey.wisdom.ui.theme.BorderLight
import com.monkey.wisdom.ui.theme.BrandDanger
import com.monkey.wisdom.ui.theme.BrandInfo
import com.monkey.wisdom.ui.theme.BrandSuccess
import com.monkey.wisdom.ui.theme.BrandWarning
import com.monkey.wisdom.ui.theme.TextMuted
import com.monkey.wisdom.ui.theme.TextPrimary
import com.monkey.wisdom.ui.theme.TextSecondary

/**
 * 收支明细页：资金流水列表（充值 / 提现 / 运费托管 / 清算划账等）。
 *
 * 对应 Web 端 `/incomeExpenseList`：进入页面即请求 `POST incomeExpense/list`，
 * 支持下拉到底「加载更多」按页追加与右上角手动刷新。
 */
@Composable
fun IncomeExpenseScreen(
    onBack: () -> Unit,
    viewModel: IncomeExpenseViewModel = viewModel {
        IncomeExpenseViewModel(ServiceLocator.accountRepository)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    AppScaffold(
        title = "收支明细",
        onBack = onBack,
        actions = {
            Text(
                text = "刷新",
                modifier = Modifier
                    .clickable(enabled = !state.loading, onClick = viewModel::refresh)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                color = if (state.loading) TextMuted else MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 汇总卡片
            item {
                SummaryCard(total = state.total, loading = state.loading)
            }

            if (state.records.isNotEmpty()) {
                item {
                    Text(
                        text = "共 ${state.total} 条记录",
                        modifier = Modifier.padding(horizontal = 4.dp),
                        color = TextMuted,
                        fontSize = 12.sp,
                    )
                }
            }

            state.errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        color = BrandDanger,
                        fontSize = 12.sp,
                    )
                }
            }

            if (state.records.isEmpty() && !state.loading) {
                item {
                    EmptyBox(
                        text = if (state.errorMessage == null) {
                            "暂无收支记录\n充值、提现、运费结算等资金变动将自动同步到这里"
                        } else {
                            "加载失败\n${state.errorMessage}"
                        },
                        actionText = "重新加载",
                        onAction = viewModel::refresh,
                        modifier = Modifier.height(220.dp),
                    )
                }
            }

            items(
                items = state.records,
                key = { item -> item.flowNo ?: item.id?.toString() ?: item.hashCode().toString() },
            ) { item: IncomeExpenseItem ->
                SectionCard {
                    IncomeExpenseItemRow(item = item)
                }
            }

            if (state.hasMore) {
                item {
                    LoadMoreButton(loading = state.loadingMore, onClick = viewModel::loadMore)
                }
            } else if (state.records.isNotEmpty()) {
                item {
                    Text(
                        text = "没有更多了",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** 累计记录汇总卡 */
@Composable
private fun SummaryCard(
    total: Long,
    loading: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "累计记录", color = Color(0xEBFFFFFF), fontSize = 13.sp)
        Text(
            text = if (loading && total == 0L) "…" else total.toString(),
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "资金变动实时同步自智运宝账户",
            color = Color(0xD9FFFFFF),
            fontSize = 11.sp,
        )
    }
}

/** 单条收支流水 */
@Composable
private fun IncomeExpenseItemRow(item: IncomeExpenseItem) {
    val meta = directionMeta(item.direction, item.directionDesc)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ==== 头部：业务类型 + 时间 + 金额 ====
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = item.bizTypeName?.takeIf { it.isNotBlank() } ?: "资金变动",
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = Formatters.time(item.flowTime),
                modifier = Modifier.weight(1f),
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
            )
            Text(
                text = "${meta.sign}¥ ${Formatters.money(item.amount)}",
                color = meta.color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
        }

        // ==== 主体：记账方向 + 收支科目 + 备注 + 关联单号 ====
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = meta.label,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(meta.color.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    color = meta.color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = item.incomeExpenseTypeName?.takeIf { it.isNotBlank() } ?: "-",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            item.remark?.takeIf { it.isNotBlank() }?.let { remark ->
                Text(text = remark, color = TextPrimary, fontSize = 13.sp, lineHeight = 19.sp)
            }

            val orderText = item.orderId?.takeIf { it.isNotBlank() }?.let { "运单号：$it" }
            val bizText = item.bizNo
                ?.takeIf { it.isNotBlank() && it != item.orderId }
                ?.let { "业务单号：$it" }
            if (orderText != null || bizText != null) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    orderText?.let { Text(text = it, color = TextMuted, fontSize = 11.sp) }
                    bizText?.let { Text(text = it, color = TextMuted, fontSize = 11.sp) }
                }
            }
        }

        // ==== 底部：流水号 + 变动后余额 ====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "流水号：${item.flowNo?.takeIf { it.isNotBlank() } ?: "-"}",
                modifier = Modifier.weight(1f),
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "余额 ¥ ${Formatters.money(item.balanceAfter)}",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/** 加载更多按钮 */
@Composable
private fun LoadMoreButton(
    loading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(BgCard)
            .border(width = 1.dp, color = BorderLight, shape = RoundedCornerShape(50))
            .clickable(enabled = !loading, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (loading) "加载中…" else "加载更多",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** 记账方向样式：颜色、金额符号、文案 */
private data class DirectionMeta(val color: Color, val sign: String, val label: String)

private fun directionMeta(direction: Int?, directionDesc: String?): DirectionMeta = when (direction) {
    1 -> DirectionMeta(BrandSuccess, "+", "收入")
    2 -> DirectionMeta(BrandDanger, "−", "支出")
    3 -> DirectionMeta(BrandWarning, "", "冻结")
    4 -> DirectionMeta(BrandInfo, "", "解冻")
    else -> DirectionMeta(BrandInfo, "", directionDesc?.takeIf { it.isNotBlank() } ?: "其他")
}
