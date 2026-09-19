package com.monkey.wisdom.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monkey.wisdom.BuildConfig
import com.monkey.wisdom.di.ServiceLocator
import com.monkey.wisdom.ui.components.AppScaffold
import com.monkey.wisdom.ui.components.PrimaryButton
import com.monkey.wisdom.ui.components.SectionCard
import com.monkey.wisdom.ui.components.showToast
import com.monkey.wisdom.ui.theme.BrandDanger
import com.monkey.wisdom.ui.theme.TextMuted
import com.monkey.wisdom.ui.theme.TextPrimary
import com.monkey.wisdom.ui.theme.TextSecondary

/**
 * 「检查更新」页：展示当前版本，支持手动检查、下载安装与浏览器兜底下载。
 *
 * 进入即自动检查一次（不弹「已是最新」提示），点击按钮为手动检查（有结果提示）。
 */
@Composable
fun AppUpdateScreen(
    onBack: () -> Unit,
    viewModel: AppUpdateViewModel = viewModel { AppUpdateViewModel(ServiceLocator.appVersionRepository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.checkUpdate(manual = false) }

    state.message?.let { message ->
        LaunchedEffect(message) {
            showToast(context, message)
            viewModel.consumeMessage()
        }
    }

    AppUpdateDialog(
        state = state,
        onUpdate = { viewModel.downloadApk(context) },
        onInstall = { viewModel.installApk(context) },
        onBrowserDownload = { viewModel.downloadByBrowser(context) },
        onDismiss = viewModel::dismissUpdate,
    )

    AppScaffold(title = "检查更新", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "当前版本", color = TextSecondary, fontSize = 13.sp)
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）",
                        color = TextPrimary,
                        fontSize = 22.sp,
                    )
                    Text(
                        text = "智运宝 · 智慧物流运单平台",
                        color = TextMuted,
                        fontSize = 12.sp,
                    )
                }
            }

            PrimaryButton(
                text = "检查更新",
                loading = state.checking || state.downloading,
                onClick = { viewModel.checkUpdate(manual = true) },
            )

            state.downloadError?.let { error ->
                Text(text = error, color = BrandDanger, fontSize = 12.sp)
            }

            if (state.apkFile != null) {
                Text(
                    text = "已下载：${state.apkFile?.name ?: ""}",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
                PrimaryButton(text = "安装新版本", onClick = { viewModel.installApk(context) })
            }

            Text(
                text = "更新将下载最新安装包，下载完成后会跳转到系统安装界面；若无法安装，可用浏览器打开下载地址。",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )

            Text(
                text = "服务地址：${BuildConfig.API_BASE_URL}",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
            )
        }
    }
}

/**
 * 启动后的静默检查宿主：有新版本时弹出更新弹窗，无新版本不打扰用户。
 *
 * 在主导航中挂载一次即可，覆盖货主端与承运方端。
 */
@Composable
fun AppUpdateHost(
    viewModel: AppUpdateViewModel = viewModel { AppUpdateViewModel(ServiceLocator.appVersionRepository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.checkUpdate(manual = false) }

    AppUpdateDialog(
        state = state,
        onUpdate = { viewModel.downloadApk(context) },
        onInstall = { viewModel.installApk(context) },
        onBrowserDownload = { viewModel.downloadByBrowser(context) },
        onDismiss = viewModel::dismissUpdate,
    )
}
