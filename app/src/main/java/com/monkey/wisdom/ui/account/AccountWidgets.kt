package com.monkey.wisdom.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monkey.wisdom.core.constants.BankOptions
import com.monkey.wisdom.core.util.Formatters
import com.monkey.wisdom.ui.theme.BgCard
import com.monkey.wisdom.ui.theme.BgPage
import com.monkey.wisdom.ui.theme.BorderLight
import com.monkey.wisdom.ui.theme.TextMuted
import com.monkey.wisdom.ui.theme.TextPrimary
import com.monkey.wisdom.ui.theme.TextSecondary
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 金额输入过滤：仅允许数字与一个小数点，最多两位小数（与 Web 端 onAmountInput 一致）。
 */
fun filterMoneyInput(raw: String, maxIntegerLength: Int = 10): String {
    val clean = raw.filter { it.isDigit() || it == '.' }
    val parts = clean.split('.')
    val integerPart = parts.firstOrNull().orEmpty().take(maxIntegerLength)
    val decimalPart = parts.getOrNull(1).orEmpty().take(2)
    return if (parts.size > 1) "$integerPart.$decimalPart" else integerPart
}

/**
 * 大字号金额输入框（¥ 前缀 + 无边框输入 + 可选右侧按钮）。
 */
@Composable
fun MoneyInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "请输入金额",
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgPage)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "¥", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(text = placeholder, color = TextMuted, fontSize = 18.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(filterMoneyInput(it)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }

        trailing?.invoke()
    }
}

/**
 * 快捷金额档位。
 */
@Composable
fun QuickAmountRow(
    amounts: List<Int>,
    currentAmount: Double,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        amounts.forEach { amount ->
            val selected = currentAmount == amount.toDouble()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else BgCard)
                    .border(
                        width = 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else BorderLight,
                        shape = RoundedCornerShape(50),
                    )
                    .clickable { onSelect(amount) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "¥$amount",
                    color = if (selected) MaterialTheme.colorScheme.primary else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

/** 冻结业务类型配色：运费托管（蓝）/ 提现冻结（橙） */
data class FrozenBizStyle(val textColor: Color, val bgColor: Color)

fun frozenBizStyle(bizTypeName: String?): FrozenBizStyle =
    if (bizTypeName == "提现冻结") {
        FrozenBizStyle(Color(0xFFB45309), Color(0xFFFEF3C7))
    } else {
        FrozenBizStyle(Color(0xFF1D4ED8), Color(0xFFDBEAFE))
    }

/** 冻结明细条目：业务类型徽标 + 单号 / 状态 + 冻结时间 + 冻结金额 */
@Composable
fun FrozenDetailItemRow(
    bizTypeName: String,
    orderNo: String,
    amount: String,
    frozenTime: String,
    statusText: String,
    modifier: Modifier = Modifier,
) {
    val style = frozenBizStyle(bizTypeName)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = bizTypeName,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(style.bgColor)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            color = style.textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "单号 $orderNo",
                    modifier = Modifier.weight(1f),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = statusText,
                    color = Color(0xFF22C55E),
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Text(text = "$frozenTime 冻结", color = TextMuted, fontSize = 11.sp)
        }

        Text(
            text = "-¥ ${Formatters.money(amount)}",
            color = Color(0xFFF59E0B),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * 金额输入解析：空值按 0 处理，统一取两位小数（与 Web 端 `roundMoney` 一致）。
 */
fun parseAmountInput(text: String): BigDecimal {
    val trimmed = text.trim().trimEnd('.')
    if (trimmed.isEmpty()) return BigDecimal.ZERO
    return (trimmed.toBigDecimalOrNull() ?: BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
}

/** 金额去尾零展示（用于「全部提现」等需要回填输入框的场景） */
fun BigDecimal.toInputText(): String = stripTrailingZeros().toPlainString()

/** 资金操作成功弹层（充值 / 提现共用） */
@Composable
fun AccountSuccessDialog(
    title: String,
    amount: BigDecimal,
    tip: String,
    onDone: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "¥ ${Formatters.money(amount)}",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = tip,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(text = "完成", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
            }
        },
        containerColor = BgCard,
    )
}

/** 银行品牌角标：渐变底 + 银行首字 */
@Composable
fun BankBadge(
    bank: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    fontSize: Int = 18,
) {
    val option = BankOptions.of(bank)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(option.from, option.to))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = bank.take(1).ifEmpty { "银" },
            color = Color.White,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
