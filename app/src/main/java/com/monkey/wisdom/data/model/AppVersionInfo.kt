package com.monkey.wisdom.data.model

import com.google.gson.annotations.SerializedName

/**
 * 服务端下发的最新 APP 版本信息。
 *
 * 字段做了多套别名兼容，后端用 snake_case 或 Web 端命名也能正常解析。
 */
data class AppVersionInfo(
    /** 版本号（整型，用于与本地 BuildConfig.VERSION_CODE 比较） */
    @SerializedName(value = "versionCode", alternate = ["appVersionCode", "version_code"])
    val versionCode: Int = 0,
    /** 版本名，如 1.2 */
    @SerializedName(value = "versionName", alternate = ["appVersionName", "version_name"])
    val versionName: String? = null,
    /** APK 下载地址；为空时客户端回落到默认下载地址 */
    @SerializedName(value = "apkUrl", alternate = ["downloadUrl", "url", "apk_url"])
    val apkUrl: String? = null,
    /** 更新说明 */
    @SerializedName(value = "updateLog", alternate = ["remark", "description", "update_log"])
    val updateLog: String? = null,
    /** 是否强制更新：为 true 时弹窗不可跳过 */
    @SerializedName(value = "forceUpdate", alternate = ["force", "force_update"])
    val forceUpdate: Boolean = false,
    /** APK 大小（字节，用于展示「约 13 MB」） */
    @SerializedName(value = "apkSize", alternate = ["size", "apk_size"])
    val apkSize: Long = 0L,
) {

    /** 展示用版本名，后端缺失时用版本号兜底 */
    val displayVersion: String get() = versionName?.takeIf { it.isNotBlank() } ?: versionCode.toString()

    /** 更新说明，缺失时给出默认文案；对历史乱码做 UTF-8 兜底修复 */
    val displayLog: String
        get() = updateLog?.takeIf { it.isNotBlank() }?.fixUtf8Mojibake()
            ?: "优化了部分功能体验，建议及时更新"

    companion object {
        /**
         * 修复「UTF-8 字节被误按 ISO-8859-1 解码」导致的 mojibake 乱码。
         * 仅当检测到典型乱码字符时才做转换，正常文本无影响。
         */
        private fun String.fixUtf8Mojibake(): String {
            // 典型特征：UTF-8 多字节中文被 Latin-1 解码后会出现这些字符
            if (!contains(Regex("[ÃÆÅÇÈÌÍÏÐÑÒÓØÙÚÛÜÝÞßæøðñ]"))) {
                return this
            }
            return try {
                String(toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
            } catch (_: Exception) {
                this
            }
        }
    }
}
