package com.monkey.wisdom.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monkey.wisdom.ui.theme.TextMuted

/**
 * 统一样式的表单输入框。
 *
 * @param isPassword 密码框：默认掩码显示，并提供「显示 / 隐藏」切换
 * @param maxLength  最大输入长度，超出部分自动截断
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
    maxLength: Int = DEFAULT_MAX_LENGTH,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val visualTransformation =
        if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None

    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.take(maxLength)) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        label = { Text(text = label) },
        placeholder = if (placeholder.isEmpty()) {
            null
        } else {
            { Text(text = placeholder, color = TextMuted, fontSize = 14.sp) }
        },
        trailingIcon = if (isPassword) {
            {
                Text(
                    text = if (passwordVisible) "隐藏" else "显示",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { passwordVisible = !passwordVisible }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        } else {
            null
        },
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        shape = RoundedCornerShape(8.dp),
    )
}

/** 默认最大输入长度（手机号 11、密码 20、姓名 20 均不超过该值） */
private const val DEFAULT_MAX_LENGTH = 20
