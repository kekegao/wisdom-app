package com.monkey.wisdom.ui.update

import android.content.Context
import com.monkey.wisdom.BuildConfig
import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.core.update.ApkDownloader
import com.monkey.wisdom.core.update.ApkInstaller
import com.monkey.wisdom.data.model.AppVersionInfo
import com.monkey.wisdom.data.repository.AppVersionRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/** 检查更新页状态 */
data class AppUpdateUiState(
    val checking: Boolean = false,
    /** 服务端最新版本（仅当存在新版本时赋值） */
    val latest: AppVersionInfo? = null,
    val downloading: Boolean = false,
    /** 下载进度 0-100 */
    val progress: Int = 0,
    /** 已下载完成的 APK */
    val apkFile: File? = null,
    /** 一次性提示（已是最新 / 检查失败 / 下载失败） */
    val message: String? = null,
    /** 下载失败原因 */
    val downloadError: String? = null,
)

/**
 * 检查更新 ViewModel：请求后端版本号接口比对本地版本，下载并拉起安装。
 */
class AppUpdateViewModel(
    private val repository: AppVersionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AppUpdateUiState())
    val state: StateFlow<AppUpdateUiState> = _state.asStateFlow()

    /**
     * 检查更新
     *
     * @param manual true 表示用户手动触发：无新版本或失败都要提示；false 为启动时静默检查
     */
    fun checkUpdate(manual: Boolean = false) {
        if (_state.value.checking || _state.value.downloading) return
        viewModelScope.launch {
            _state.update { it.copy(checking = true, message = null) }
            when (val result = repository.latest()) {
                is AppResult.Success -> {
                    val info = result.data
                    val hasUpdate = info.versionCode > BuildConfig.VERSION_CODE
                    _state.update {
                        it.copy(
                            checking = false,
                            latest = info.takeIf { hasUpdate },
                            message = if (manual && !hasUpdate) "当前已是最新版本（v${BuildConfig.VERSION_NAME}）" else null,
                        )
                    }
                }

                is AppResult.Failure -> _state.update {
                    it.copy(checking = false, message = result.message.takeIf { manual })
                }
            }
        }
    }

    /** 下载最新 APK，完成后自动拉起安装界面 */
    fun downloadApk(context: Context) {
        val info = _state.value.latest ?: return
        if (_state.value.downloading) return
        val url = info.apkUrl?.takeIf { it.isNotBlank() } ?: defaultApkUrl()
        val appContext = context.applicationContext
        viewModelScope.launch {
            _state.update { it.copy(downloading = true, progress = 0, downloadError = null, message = null) }
            try {
                val file = ApkDownloader.download(
                    context = appContext,
                    url = url,
                    fileName = "monkey-wisdom-v${info.displayVersion}.apk",
                ) { percent -> _state.update { it.copy(progress = percent) } }
                _state.update { it.copy(downloading = false, apkFile = file) }
                ApkInstaller.install(appContext, file)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(downloading = false, downloadError = e.message ?: "下载失败，请稍后重试")
                }
            }
        }
    }

    /** 重新拉起安装界面（用户取消了安装时可再次点击） */
    fun installApk(context: Context) {
        val file = _state.value.apkFile ?: return
        runCatching { ApkInstaller.install(context.applicationContext, file) }
            .onFailure { _state.update { s -> s.copy(message = "未能打开安装界面，可在下载目录手动安装") } }
    }

    /** 用浏览器下载（应用内下载异常时的兜底入口） */
    fun downloadByBrowser(context: Context) {
        val info = _state.value.latest ?: return
        val url = info.apkUrl?.takeIf { it.isNotBlank() } ?: defaultApkUrl()
        runCatching { ApkInstaller.openInBrowser(context, url) }
            .onFailure { _state.update { s -> s.copy(message = "打开浏览器失败，请手动访问下载地址") } }
    }

    /** 关闭更新弹窗（强制更新除外） */
    fun dismissUpdate() {
        if (_state.value.latest?.forceUpdate == true) return
        _state.update { it.copy(latest = null, downloadError = null) }
    }

    /** 消费一次性提示 */
    fun consumeMessage() = _state.update { it.copy(message = null) }

    /** 后端未下发下载地址时的默认地址：服务端静态 APK 目录 */
    private fun defaultApkUrl(): String =
        BuildConfig.API_BASE_URL.trimEnd('/') + "/app/download/monkey-wisdom.apk"
}
