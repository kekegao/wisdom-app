package com.monkey.wisdom.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monkey.wisdom.ui.theme.BgCard
import com.monkey.wisdom.ui.theme.BrandDanger
import com.monkey.wisdom.ui.theme.BrandSuccess
import com.monkey.wisdom.ui.theme.TextPrimary
import com.monkey.wisdom.ui.theme.TextSecondary

/** 轻提示，与 Web 端 toast 行为一致（短时浮层提示） */
fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

/**
 * 通用提示弹窗。
 *
 * 后端接口返回的结果（尤其是失败原因）必须使用弹窗展示：
 * Toast / 行内小字容易被用户忽略，导致「点了没反应」的错觉。
 */
@Composable
fun MessageDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    confirmText: String = "我知道了",
    titleColor: Color = TextPrimary,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Start,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = confirmText, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
            }
        },
        containerColor = BgCard,
    )
}

/**
 * 后端接口返回失败时的统一弹窗。
 *
 * 各页面只需把 ViewModel 中的失败文案透传进来即可。
 */
@Composable
fun ErrorDialog(
    message: String?,
    onDismiss: () -> Unit,
    title: String = "操作失败",
) {
    if (message.isNullOrBlank()) return
    MessageDialog(title = title, message = message, onDismiss = onDismiss, titleColor = BrandDanger)
}

/**
 * 后端接口返回成功时的统一弹窗。
 *
 * 成功结果同样必须弹窗展示，用户点击「确定」后弹窗消失，
 * 避免轻提示一闪而过让用户误以为操作没生效。
 */
@Composable
fun SuccessDialog(
    message: String?,
    onDismiss: () -> Unit,
    title: String = "操作成功",
) {
    if (message.isNullOrBlank()) return
    MessageDialog(
        title = title,
        message = message,
        onDismiss = onDismiss,
        confirmText = "确定",
        titleColor = BrandSuccess,
    )
}

/** 白色圆角卡片容器 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    contentPadding: Int = 16,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .padding(contentPadding.dp),
        content = content,
    )
}

/** 左标签右数值行 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary,
    valueBold: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = TextSecondary, fontSize = 14.sp)
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = if (valueBold) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

/** 可点击行（右侧箭头由调用方决定是否展示） */
@Composable
fun ClickableRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, color = TextPrimary, fontSize = 15.sp)
            if (!subtitle.isNullOrBlank()) {
                Text(text = subtitle, color = TextSecondary, fontSize = 12.sp)
            }
        }
        trailing?.invoke()
    }
}

/** 二次确认弹窗（运单操作、解绑银行卡等危险操作统一使用） */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "确定",
    dismissText: String = "取消",
    confirmColor: Color = MaterialTheme.colorScheme.primary,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Text(
                text = message,
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Start,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText, color = confirmColor, fontSize = 15.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText, color = TextSecondary, fontSize = 15.sp)
            }
        },
        containerColor = BgCard,
    )
}

/** 阻塞式加载遮罩（提交类操作进行中展示，避免重复提交） */
@Composable
fun LoadingDialog(
    visible: Boolean,
    text: String = "处理中...",
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = { },
        confirmButton = { },
        containerColor = BgCard,
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                Text(text = text, fontSize = 14.sp, color = TextPrimary)
            }
        },
    )
}

/** 内容占位（页面未实现阶段使用，保证工程可编译运行） */
@Composable
fun PlaceholderScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    message: String = "该页面正在迁移中",
) {
    AppScaffold(title = title, onBack = onBack) { padding ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            EmptyBox(text = message)
        }
    }
}
