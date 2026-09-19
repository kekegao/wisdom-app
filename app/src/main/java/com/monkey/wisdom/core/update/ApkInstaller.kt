package com.monkey.wisdom.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * APK 安装：通过 FileProvider 授予临时读权限，交给系统安装器。
 */
object ApkInstaller {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    /** 拉起系统安装界面；下载目录为应用私有目录，必须经 FileProvider 授权 */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** 兜底：用浏览器打开下载地址 */
    fun openInBrowser(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
