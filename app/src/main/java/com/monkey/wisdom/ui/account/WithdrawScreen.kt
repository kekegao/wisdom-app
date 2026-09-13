package com.monkey.wisdom.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monkey.wisdom.core.util.Formatters
import com.monkey.wisdom.di.ServiceLocator
import com.monkey.wisdom.ui.components.AppScaffold
import com.monkey.wisdom.ui.components.SectionCard
import com.monkey.wisdom.ui.theme.BgCard
import com.monkey.wisdom.ui.theme.BgPage
import com.monkey.wisdom.ui.theme.BrandDanger
import com.monkey.wisdom.ui.theme.BrandSuccess
import com.monkey.wisdom.ui.theme.TextMuted
import com.monkey.wisdom.ui.theme.TextPrimary
import com.monkey.wisdom.ui.theme.TextSecondary

/**
 * 提现页（智运宝账户）。
 *
 * 对应 Web 端 `/withdraw`：可提现余额取账户可用余额，到账账户取首张已绑定银行卡；
 * 后端暂未提供提现接口，提交为本地模拟（可用余额转冻结 + 提现冻结记录）。
 */
@Composable
fun WithdrawScreen(
    onBack: () -> Unit,
    onManageBank: () -> Unit,
    onDone: () -> Unit,
    viewModel: WithdrawViewModel = viewModel {
        WithdrawViewModel(
            accountRepository = ServiceLocator.accountRepository,
            bankCardRepository = ServiceLocator.bankCardRepository,
            localAccountStore = ServiceLocator.localAccountStore,
        )
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 进入页面 / 从银行卡管理返回时刷新余额与到账账户
    LaunchedEffect(Unit) { viewModel.refresh() }

    AppScaffold(title = "提现", onBack = onBack) { padding ->
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
                // ==== 可提现余额 ====
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF15803D))))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "可提现余额（元）", color = Color(0xEBFFFFFF), fontSize = 13.sp)
                    Text(
                        text = Formatters.money(state.available),
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (state.loading) {
                        Text(text = "正在同步账户余额…", color = Color(0xB3FFFFFF), fontSize = 11.sp)
                    }
                }

                // ==== 提现金额 ====
                SectionCard {
                    MoneyInput(
                        value = state.amountText,
                        onValueChange = viewModel::onAmountChanged,
                        placeholder = "请输入提现金额",
                        trailing = {
                            Text(
                                text = "全部提现",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(onClick = viewModel::fillAllAvailable)
                                    .background(BgPage)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        },
                    )
                    Text(
                        text = "单笔提现 ¥${WITHDRAW_MIN_AMOUNT.toPlainString()} ~ ¥${Formatters.money(WITHDRAW_MAX_AMOUNT)}",
                        modifier = Modifier.padding(top = 10.dp),
                        color = TextMuted,
                        fontSize = 12.sp,
                    )
                    if (state.errorText.isNotEmpty()) {
                        Text(
                            text = state.errorText,
                            modifier = Modifier.padding(top = 6.dp),
                            color = BrandDanger,
                            fontSize = 12.sp,
                        )
                    }
                    state.balanceError?.let { message ->
                        Text(
                            text = message,
                            modifier = Modifier.padding(top = 6.dp),
                            color = TextMuted,
                            fontSize = 12.sp,
                        )
                    }
                    if (state.showFeePreview) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BgPage)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "到账金额（手续费 ¥${Formatters.money(WITHDRAW_FEE)}）",
                                color = TextSecondary,
                                fontSize = 13.sp,
                            )
                            Text(
                                text = "¥ ${Formatters.money(state.actualAmount)}",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // ==== 到账账户 ====
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "到账账户",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "管理",
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onManageBank)
                                .background(BgPage)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            color = TextSecondary,
                            fontSize = 12.sp,
                        )
                    }

                    val card = state.bankCard
                    if (card != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            BankBadge(bank = card.bank, size = 42.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${card.bank}（${card.cardType}）",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "尾号 ${Formatters.cardTail(card.cardNo)}",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                )
                            }
                            Text(
                                text = "已绑定",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BrandSuccess.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                color = BrandSuccess,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BgPage)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "暂未绑定到账银行卡，请先完成绑卡",
                                color = TextSecondary,
                                fontSize = 13.sp,
                            )
                            Button(
                                onClick = onManageBank,
                                modifier = Modifier.height(36.dp),
                            ) {
                                Text(text = "去银行卡管理绑定", fontSize = 13.sp)
                            }
                        }
                    }

                    Text(
                        text = "$WITHDRAW_ARRIVE_TEXT · 申请提交后资金将暂时冻结",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        color = TextMuted,
                        fontSize = 12.sp,
                    )
                }

                // ==== 规则说明 ====
                Text(
                    text = "模拟提现场景：提交申请后，对应金额计入「冻结金额」，可在账户页冻结明细中查看；提现成功后自动回到账户页。",
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }

            // ==== 底部操作栏 ====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCard)
                    .padding(12.dp),
            ) {
                Button(
                    onClick = viewModel::submit,
                    enabled = state.canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(text = if (state.submitting) "提交中…" else "确认提现", fontSize = 16.sp)
                }
            }
        }
    }

    state.successAmount?.let { amount ->
        val card = state.bankCard
        val tip = if (card != null) {
            "款项已冻结，${WITHDRAW_ARRIVE_TEXT}至${card.bank}（尾号 ${Formatters.cardTail(card.cardNo)}）"
        } else {
            "款项已冻结，$WITHDRAW_ARRIVE_TEXT"
        }
        AccountSuccessDialog(
            title = "提现申请已提交",
            amount = amount,
            tip = tip,
            onDone = onDone,
        )
    }
}
