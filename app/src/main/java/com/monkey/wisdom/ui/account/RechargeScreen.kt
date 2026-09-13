package com.monkey.wisdom.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.monkey.wisdom.ui.theme.BorderLight
import com.monkey.wisdom.ui.theme.BrandDanger
import com.monkey.wisdom.ui.theme.TextMuted
import com.monkey.wisdom.ui.theme.TextPrimary
import com.monkey.wisdom.ui.theme.TextSecondary

/**
 * 充值页（智运宝账户）。
 *
 * 对应 Web 端 `/recharge`：金额输入 + 快捷档位 + 支付方式选择，
 * 提交调用账户充值接口，成功后以服务端账户快照回写本地。
 */
@Composable
fun RechargeScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: RechargeViewModel = viewModel {
        RechargeViewModel(ServiceLocator.accountRepository, ServiceLocator.localAccountStore)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppScaffold(title = "充值", onBack = onBack) { padding ->
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
                // ==== 充值金额 ====
                SectionCard {
                    Text(
                        text = "充值金额",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    MoneyInput(
                        value = state.amountText,
                        onValueChange = viewModel::onAmountChanged,
                        modifier = Modifier.padding(top = 10.dp),
                        placeholder = "请输入充值金额",
                    )
                    QuickAmountRow(
                        amounts = RECHARGE_QUICK_AMOUNTS,
                        currentAmount = state.amount.toDouble(),
                        onSelect = viewModel::selectQuickAmount,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        text = "单笔充值 ¥0.01 ~ ¥${Formatters.money(RECHARGE_MAX_AMOUNT)}，请确认金额后再付款",
                        modifier = Modifier.padding(top = 10.dp),
                        color = TextMuted,
                        fontSize = 12.sp,
                    )
                    if (state.errorTextOrSubmitError.isNotEmpty()) {
                        Text(
                            text = state.errorTextOrSubmitError,
                            modifier = Modifier.padding(top = 6.dp),
                            color = BrandDanger,
                            fontSize = 12.sp,
                        )
                    }
                }

                // ==== 支付方式 ====
                SectionCard {
                    Text(
                        text = "支付方式",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    PAY_CHANNELS.forEach { channel ->
                        PayChannelRow(
                            channel = channel,
                            selected = state.channel == channel.id,
                            onClick = { viewModel.selectChannel(channel.id) },
                        )
                    }
                }

                // ==== 说明 ====
                Text(
                    text = "所选支付渠道当前仅作展示。点击「立即充值」将发起智运宝账户充值，成功后金额实时到账可用余额，并同步账户页展示。",
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
                    Text(text = if (state.submitting) "充值中…" else "立即充值", fontSize = 16.sp)
                }
            }
        }
    }

    state.successAmount?.let { amount ->
        AccountSuccessDialog(
            title = "充值成功",
            amount = amount,
            tip = "已到账智运宝账户可用余额",
            onDone = onDone,
        )
    }
}

/** 支付方式行 */
@Composable
private fun PayChannelRow(
    channel: PayChannel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BgPage),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = channelBadgeText(channel.id),
                color = channelAccent(channel.id),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(text = channel.name, color = TextPrimary, fontSize = 14.sp)
        Text(text = channel.desc, color = TextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.weight(1f))
        RadioDot(selected = selected)
    }
}

/** 单选圆点 */
@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else BorderLight,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

private fun channelBadgeText(channelId: String): String = when (channelId) {
    "wechat" -> "微"
    "alipay" -> "支"
    else -> "银"
}

private fun channelAccent(channelId: String): Color = when (channelId) {
    "wechat" -> Color(0xFF22C55E)
    "alipay" -> Color(0xFF2563EB)
    else -> Color(0xFFF59E0B)
}
