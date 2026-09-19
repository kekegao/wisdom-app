package com.monkey.wisdom.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monkey.wisdom.ui.theme.BgCard
import com.monkey.wisdom.ui.theme.BrandDanger
import com.monkey.wisdom.ui.theme.TextMuted
import com.monkey.wisdom.ui.theme.TextPrimary
import com.monkey.wisdom.ui.theme.TextSecondary

/**
 * 发现新版本弹窗：展示版本号、更新说明，下载中显示进度条。
 */
@Composable
fun AppUpdateDialog(
    state: AppUpdateUiState,
    onUpdate: () -> Unit,
    onInstall: () -> Unit,
    onBrowserDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    val latest = state.latest ?: return
    val forceUpdate = latest.forceUpdate

    AlertDialog(
        // 强制更新时不允许点击外部关闭
        onDismissRequest = { if (!forceUpdate && !state.downloading) onDismiss() },
        containerColor = BgCard,
        title = {
            Text(
                text = "发现新版本 v${latest.displayVersion}",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
        },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                if (latest.apkSize > 0) {
                    Text(
                        text = "安装包大小：约 ${formatSize(latest.apkSize)}",
                        fontSize = 12.sp,
                        color = TextMuted,
                    )
                }
                Text(text = latest.displayLog, fontSize = 14.sp, color = TextSecondary)

                if (state.downloading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    ) {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "正在下载… ${state.progress}%",
                            modifier = Modifier.padding(top = 6.dp),
                            fontSize = 12.sp,
                            color = TextMuted,
                        )
                    }
                }

                state.downloadError?.let { error ->
                    Text(text = error, fontSize = 12.sp, color = BrandDanger)
                }
            }
        },
        confirmButton = {
            when {
                state.downloading -> TextButton(onClick = {}, enabled = false) {
                    Text(text = "下载中", color = TextMuted)
                }

                state.apkFile != null -> TextButton(onClick = onInstall) {
                    Text(text = "安装", color = MaterialTheme.colorScheme.primary)
                }

                else -> TextButton(onClick = onUpdate) {
                    Text(text = "立即更新", color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        dismissButton = {
            if (forceUpdate) {
                // 强制更新：不提供「稍后再说」，但保留浏览器下载兜底
                TextButton(onClick = onBrowserDownload) {
                    Text(text = "浏览器下载", color = TextSecondary)
                }
            } else {
                TextButton(onClick = { if (!state.downloading) onDismiss() }) {
                    Text(text = "稍后再说", color = TextSecondary)
                }
            }
        },
    )
}

/** 字节数转可读大小 */
private fun formatSize(bytes: Long): String {
    val mb = bytes / 1024.0 / 1024.0
    return if (mb >= 1) "%.1f MB".format(mb) else "%.0f KB".format(bytes / 1024.0)
}
