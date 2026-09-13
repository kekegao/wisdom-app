package com.monkey.wisdom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monkey.wisdom.ui.theme.BgCard
import com.monkey.wisdom.ui.theme.BgPage
import com.monkey.wisdom.ui.theme.BrandDanger
import com.monkey.wisdom.ui.theme.BrandSuccess
import com.monkey.wisdom.ui.theme.TextPrimary
import com.monkey.wisdom.ui.theme.TextSecondary

/**
 * 表单区块：白底卡片 + 标题 + 副标题（与 Web 端 form-card 一致）。
 */
@Composable
fun FormSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, color = TextPrimary, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            if (!subtitle.isNullOrBlank()) {
                Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
            }
        }
        content()
    }
}

/**
 * 单行输入项：标签 + 输入框 + 可选单位后缀。
 */
@Composable
fun FormInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    suffix: String? = null,
    maxLength: Int = 50,
    keyboardType: KeyboardType = KeyboardType.Text,
    // 小数输入过滤：仅允许数字与一个小数点
    decimalOnly: Boolean = false,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { input ->
                    val limited = if (decimalOnly) filterDecimal(input, maxLength) else input.take(maxLength)
                    onValueChange(limited)
                },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(text = placeholder, color = TextSecondary.copy(alpha = 0.6f), fontSize = 14.sp)
                },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = TextPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                shape = RoundedCornerShape(10.dp),
            )
            if (suffix != null) {
                Text(
                    text = suffix,
                    modifier = Modifier.padding(start = 8.dp),
                    color = TextSecondary,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

/**
 * 多行输入项（物品描述）。
 */
@Composable
fun FormTextArea(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    maxLength: Int = 200,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.take(maxLength)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(text = placeholder, color = TextSecondary.copy(alpha = 0.6f), fontSize = 14.sp)
            },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = TextPrimary),
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default,
            ),
            minLines = 3,
            maxLines = 5,
        )
    }
}

/**
 * 单选项标签组（物品类型）。
 */
@Composable
fun FormChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else BgPage)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { onSelect(option) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Medium else androidx.compose.ui.text.font.FontWeight.Normal,
                )
            }
        }
    }
}

/** 提示横幅（错误 / 成功） */
@Composable
fun FormBanner(
    text: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (isError) BrandDanger else BrandSuccess
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        color = color,
        fontSize = 13.sp,
    )
}

/** 小数输入过滤：仅保留数字与一个小数点 */
private fun filterDecimal(input: String, maxLength: Int): String {
    val builder = StringBuilder()
    var hasDot = false
    for (ch in input) {
        when {
            ch.isDigit() -> builder.append(ch)
            ch == '.' && !hasDot -> {
                hasDot = true
                builder.append(ch)
            }
        }
        if (builder.length >= maxLength) break
    }
    return builder.toString()
}
