package com.monkey.wisdom.core.update

import android.content.Context
import android.os.Environment
import com.monkey.wisdom.core.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

/**
 * APK 下载器：基于全局 OkHttpClient，把 APK 写到应用私有下载目录。
 *
 * 存私有目录（Android 10+ 无需存储权限），配合 FileProvider 交给系统安装器安装。
 */
object ApkDownloader {

    /**
     * 下载 APK。
     *
     * @param onProgress 进度回调（0-100），在 IO 线程回调，仅用于更新进度状态
     * @return 下载完成的 APK 文件
     */
    suspend fun download(
        context: Context,
        url: String,
        fileName: String,
        onProgress: (Int) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val dir = (context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir).apply {
            mkdirs()
        }
        val target = File(dir, fileName)
        val temp = File(dir, "$fileName.tmp")
        if (target.exists()) target.delete()
        if (temp.exists()) temp.delete()

        val response = NetworkModule.okHttpClient.newCall(Request.Builder().url(url).build()).execute()
        if (!response.isSuccessful) {
            response.close()
            throw IllegalStateException("下载失败（HTTP ${response.code}）")
        }

        val body = response.body ?: throw IllegalStateException("下载内容为空")
        val totalBytes = body.contentLength()
        onProgress(0)

        body.byteStream().use { input ->
            temp.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var downloaded = 0L
                var lastPercent = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    downloaded += read
                    if (totalBytes > 0) {
                        val percent = (downloaded * 100 / totalBytes).toInt()
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(percent)
                        }
                    }
                }
            }
        }
        response.close()

        if (!temp.renameTo(target)) throw IllegalStateException("APK 保存失败，请重试")
        onProgress(100)
        target
    }
}
