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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monkey.wisdom.core.util.Formatters
import com.monkey.wisdom.data.model.AccountInfo
import com.monkey.wisdom.data.model.FrozenDetail
import com.monkey.wisdom.di.ServiceLocator
import com.monkey.wisdom.ui.components.AppScaffold
import com.monkey.wisdom.ui.components.EmptyBox
import com.monkey.wisdom.ui.components.SectionCard
import com.monkey.wisdom.ui.theme.BgCard
import com.monkey.wisdom.ui.theme.BrandDanger
import com.monkey.wisdom.ui.theme.BrandWarning
import com.monkey.wisdom.ui.theme.TextMuted
import com.monkey.wisdom.ui.theme.TextPrimary
import com.monkey.wisdom.ui.theme.TextSecondary

/**
 * 智运宝账户页：余额总览 + 资金入口 + 冻结明细。
 *
 * 对应 Web 端 `/account`：余额优先取后端快照（失败时回落本地兜底），
 * 冻结明细默认展示本地提现冻结记录，进入明细页时实时查询后端。
 */
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onRecharge: () -> Unit,
    onWithdraw: () -> Unit,
    onBankList: () -> Unit,
    onMyOrder: () -> Unit,
    viewModel: AccountViewModel = viewModel {
        AccountViewModel(ServiceLocator.accountRepository, ServiceLocator.localAccountStore)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 进入账户页 / 从充值、提现、银行卡页返回时同步最新余额
    LaunchedEffect(Unit) { viewModel.loadAccount() }

    if (state.showFrozenDetail) {
        FrozenDetailScreen(
            state = state,
            onBack = viewModel::closeFrozenDetail,
            onRefresh = viewModel::loadFrozenDetails,
        )
        return
    }

    AppScaffold(title = "智运宝", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ==== 余额主卡 ====
                BalanceCard(
                    account = state.account,
                    loading = state.loading,
                    onRecharge = onRecharge,
                    onWithdraw = onWithdraw,
                )

                state.errorMessage?.let { message ->
                    Text(text = message, color = BrandDanger, fontSize = 12.sp)
                }

                // ==== 余额明细 ====
                DetailCard(account = state.account, onFrozenDetail = viewModel::openFrozenDetail)

                // ==== 银行卡管理入口 ====
                EntryCard(
                    badgeText = "卡",
                    badgeColor = BrandWarning,
                    title = "银行卡管理",
                    subtitle = "管理提现到账的银行卡",
                    onClick = onBankList,
                )

                // ==== 我的运单入口 ====
                EntryCard(
                    badgeText = "单",
                    badgeColor = MaterialTheme.colorScheme.primary,
                    title = "我的运单",
                    subtitle = "查看全部运单与运输进度",
                    onClick = onMyOrder,
                )
            }

        }
    }
}

/** 余额主卡 */
@Composable
private fun BalanceCard(
    account: AccountInfo?,
    loading: Boolean,
    onRecharge: () -> Unit,
    onWithdraw: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "资金已托管",
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x2EFFFFFF))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BalanceActionButton(text = "充值", solid = true, onClick = onRecharge)
                BalanceActionButton(text = "提现", solid = false, onClick = onWithdraw)
            }
        }

        Text(
            text = "账户余额（元）",
            modifier = Modifier.padding(top = 4.dp),
            color = Color(0xEBFFFFFF),
            fontSize = 13.sp,
        )
        Text(
            text = "¥ ${Formatters.money(account?.balance)}",
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (loading) {
                "正在同步账户余额…"
            } else {
                "更新于 ${Formatters.time(account?.updateTime)}"
            },
            color = Color(0xCCFFFFFF),
            fontSize = 11.sp,
        )
    }
}

/** 余额卡内的资金操作按钮（充值实心 / 提现描边） */
@Composable
private fun BalanceActionButton(
    text: String,
    solid: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (solid) Color.White else Color(0x24FFFFFF))
            .border(
                width = 1.dp,
                color = if (solid) Color.Transparent else Color(0xCCFFFFFF),
                shape = RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = if (solid) Color(0xFF3B82F6) else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** 余额明细卡：可用余额 + 可点击的冻结金额 */
@Composable
private fun DetailCard(
    account: AccountInfo?,
    onFrozenDetail: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "可用余额", color = TextSecondary, fontSize = 14.sp)
            Text(
                text = "¥ ${Formatters.money(account?.availableAmount)}",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onFrozenDetail),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "冻结金额", color = TextSecondary, fontSize = 14.sp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "¥ ${Formatters.money(account?.frozenAmount)}",
                    color = BrandWarning,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = "›", color = TextMuted, fontSize = 18.sp)
            }
        }

        Text(
            text = "冻结金额为进行中运单的运费托管及提现处理中的款项，点击可查看明细",
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
    }
}

/** 功能入口卡 */
@Composable
private fun EntryCard(
    badgeText: String,
    badgeColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(badgeColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = badgeText,
                color = badgeColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = TextMuted, fontSize = 12.sp)
        }
        Text(text = "›", color = TextMuted, fontSize = 18.sp)
    }
}

/** 冻结明细页 */
@Composable
private fun FrozenDetailScreen(
    state: AccountUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    AppScaffold(
        title = "冻结明细",
        onBack = onBack,
        actions = {
            Text(
                text = "刷新",
                modifier = Modifier
                    .clickable(enabled = !state.frozenLoading, onClick = onRefresh)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                color = if (state.frozenLoading) TextMuted else MaterialTheme.colorScheme.primary,
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
            // 冻结总额
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "当前冻结总额（元）", color = Color(0xEBFFFFFF), fontSize = 13.sp)
                    Text(
                        text = "¥ ${Formatters.money(state.frozenTotal)}",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "运费托管及提现处理中的款项，解冻后自动转入可用余额",
                        color = Color(0xD9FFFFFF),
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                    )
                }
            }

            item {
                when {
                    state.frozenLoading -> Text(
                        text = "正在同步最新冻结明细…",
                        modifier = Modifier.padding(horizontal = 4.dp),
                        color = TextSecondary,
                        fontSize = 12.sp,
                    )

                    state.frozenDetails.isNotEmpty() -> Text(
                        text = "共 ${state.frozenCount} 笔冻结记录",
                        modifier = Modifier.padding(horizontal = 4.dp),
                        color = TextMuted,
                        fontSize = 12.sp,
                    )
                }
            }

            state.frozenError?.let { message ->
                item {
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        color = BrandDanger,
                        fontSize = 12.sp,
                    )
                }
            }

            if (state.frozenDetails.isEmpty()) {
                item {
                    EmptyBox(
                        text = "暂无冻结明细\n进行中的运单运费托管或提现将显示在这里",
                        actionText = "重新加载",
                        onAction = onRefresh,
                        modifier = Modifier.height(220.dp),
                    )
                }
            } else {
                items(
                    items = state.frozenDetails,
                    key = { it.id ?: (it.orderNo ?: "") + (it.amount ?: "") },
                ) { detail: FrozenDetail ->
                    SectionCard {
                        FrozenDetailItemRow(
                            bizTypeName = detail.bizTypeName ?: "运费托管",
                            orderNo = detail.orderNo ?: "-",
                            amount = detail.amount ?: "0",
                            frozenTime = Formatters.time(detail.frozenTime),
                            statusText = detail.statusDesc ?: "处理中",
                        )
                    }
                }
            }

            item {
                Text(
                    text = "如对冻结款项有疑问，请联系平台客服处理",
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
