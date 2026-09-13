package com.monkey.wisdom.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * 表单错误提示：内容为空时自动隐藏，避免占位。
 */
@Composable
fun FormErrorText(
    message: String?,
    modifier: Modifier = Modifier,
) {
    if (message.isNullOrBlank()) return

    Text(
        text = message,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.error,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
    )
}
