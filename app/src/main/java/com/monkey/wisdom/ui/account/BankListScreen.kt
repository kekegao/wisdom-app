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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monkey.wisdom.core.constants.BankOptions
import com.monkey.wisdom.core.util.Formatters
import com.monkey.wisdom.data.model.BankCard
import com.monkey.wisdom.di.ServiceLocator
import com.monkey.wisdom.ui.components.AppScaffold
import com.monkey.wisdom.ui.components.AppTextField
import com.monkey.wisdom.ui.components.ConfirmDialog
import com.monkey.wisdom.ui.components.EmptyBox
import com.monkey.wisdom.ui.theme.BgCard
import com.monkey.wisdom.ui.theme.BgPage
import com.monkey.wisdom.ui.theme.BorderLight
import com.monkey.wisdom.ui.theme.BrandDanger
import com.monkey.wisdom.ui.theme.TextMuted
import com.monkey.wisdom.ui.theme.TextPrimary
import com.monkey.wisdom.ui.theme.TextSecondary

/**
 * 银行卡管理页。
 *
 * 对应 Web 端 `/bankList`：列表 / 添加 / 解绑均走本地存储，
 * 单用户最多绑定 [BANK_CARD_MAX_COUNT] 张，绑定后可用于提现到账。
 */
@Composable
fun BankListScreen(
    onBack: () -> Unit,
    viewModel: BankListViewModel = viewModel {
        BankListViewModel(ServiceLocator.bankCardRepository)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 进入页面 / 增删后重新读取本地银行卡列表
    LaunchedEffect(Unit) { viewModel.refresh() }

    AppScaffold(title = "银行卡管理", onBack = onBack) { padding ->
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "已绑定 ${state.cardsCount} 张银行卡",
                        color = TextSecondary,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "最多可绑定 $BANK_CARD_MAX_COUNT 张",
                        color = TextMuted,
                        fontSize = 12.sp,
                    )
                }

                if (state.cards.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(BgCard)
                            .padding(vertical = 28.dp),
                    ) {
                        EmptyBox(
                            text = "暂未绑定银行卡\n添加银行卡后可用于提现到账",
                            actionText = "立即添加",
                            onAction = viewModel::openAddSheet,
                            modifier = Modifier.height(200.dp),
                        )
                    }
                } else {
                    state.cards.forEach { card ->
                        BankCardItem(
                            card = card,
                            onDelete = { viewModel.askDelete(card) },
                        )
                    }
                }

                Text(
                    text = "银行卡信息仅用于演示，请放心绑定；解绑后不影响历史提现记录。",
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }

            // ==== 底部操作栏（无卡时由空状态内的按钮承担，避免重复引导） ====
            if (state.cards.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgCard)
                        .padding(12.dp),
                ) {
                    Button(
                        onClick = viewModel::openAddSheet,
                        enabled = !state.reachLimit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text(
                            text = if (state.reachLimit) "已达绑定上限" else "添加银行卡",
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }

    if (state.showAddSheet) {
        AddBankCardDialog(
            form = state.form,
            onSelectBank = viewModel::selectBank,
            onCardNoChange = viewModel::onCardNoChanged,
            onHolderChange = viewModel::onHolderChanged,
            onDismiss = viewModel::closeAddSheet,
            onSave = viewModel::saveCard,
        )
    }

    state.deleteTarget?.let { target ->
        ConfirmDialog(
            title = "删除银行卡",
            message = "确认删除 ${target.bank}（尾号 ${
                (target.cardNo.filter { it.isDigit() }).takeLast(4)
            }）吗？删除后如需要可重新添加。",
            confirmText = "确认删除",
            confirmColor = BrandDanger,
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::cancelDelete,
        )
    }
}

/** 已绑定银行卡卡片 */
@Composable
private fun BankCardItem(
    card: BankCard,
    onDelete: () -> Unit,
) {
    val option = BankOptions.of(card.bank)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(option.from, option.to)))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x38FFFFFF)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = card.bank.take(1).ifEmpty { "银" },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.bank,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = card.cardType,
                    color = Color(0xD9FFFFFF),
                    fontSize = 12.sp,
                )
            }
            Text(
                text = "解绑",
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onDelete)
                    .background(Color(0x1FFFFFFF))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                color = Color.White,
                fontSize = 12.sp,
            )
        }

        Text(
            text = Formatters.maskCardNo(card.cardNo),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = card.holder, color = Color(0xEBFFFFFF), fontSize = 12.sp)
            Text(text = "绑定于 ${card.addTime}", color = Color(0xEBFFFFFF), fontSize = 12.sp)
        }
    }
}

/** 添加银行卡弹层 */
@Composable
private fun AddBankCardDialog(
    form: BankCardFormState,
    onSelectBank: (String) -> Unit,
    onCardNoChange: (String) -> Unit,
    onHolderChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "添加银行卡",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = "选择银行", color = TextSecondary, fontSize = 13.sp)
                BankOptions.all.chunked(3).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowOptions.forEach { option ->
                            val selected = form.bank == option.name
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        } else {
                                            BgPage
                                        },
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else BorderLight,
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .clickable { onSelectBank(option.name) }
                                    .padding(horizontal = 4.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = option.name,
                                    color = if (selected) MaterialTheme.colorScheme.primary else TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                )
                            }
                        }
                        repeat(3 - rowOptions.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                AppTextField(
                    value = form.cardNo,
                    onValueChange = onCardNoChange,
                    label = "银行卡号",
                    placeholder = "请输入银行卡号",
                    keyboardType = KeyboardType.Number,
                    maxLength = 30,
                )
                AppTextField(
                    value = form.holder,
                    onValueChange = onHolderChange,
                    label = "持卡人（选填）",
                    placeholder = "默认「本人」",
                    maxLength = 20,
                )
                if (form.error.isNotEmpty()) {
                    Text(text = form.error, color = BrandDanger, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(text = "保存", color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消", color = TextSecondary, fontSize = 15.sp)
            }
        },
        containerColor = BgCard,
    )
}
