package com.monkey.wisdom.core.storage

import android.content.Context
import android.content.SharedPreferences
import com.monkey.wisdom.core.common.Json
import com.monkey.wisdom.data.model.UserInfo

/**
 * 登录态本地存储（SharedPreferences）。
 *
 * 职责：
 * - 持久化 token 与用户信息，供 OkHttp 拦截器自动携带鉴权头；
 * - 应用重启后可恢复登录态（与 Web 端 localStorage 行为对齐）。
 */
object UserSession {

    private const val PREF_NAME = "wisdom_session"
    private const val KEY_TOKEN = "key_token"
    private const val KEY_USER_INFO = "key_user_info"

    private lateinit var appContext: Context

    private val prefs: SharedPreferences
        get() = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /** 在 Application 中调用一次 */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** 当前 token */
    val token: String?
        get() = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    /** 当前登录用户，本地数据损坏时返回 null */
    val currentUser: UserInfo?
        get() = prefs.getString(KEY_USER_INFO, null)?.let { json ->
            Json.fromJson<UserInfo>(json)
        }

    /** 是否已登录 */
    val isLoggedIn: Boolean
        get() = token != null

    /** 登录成功后保存会话 */
    fun save(user: UserInfo) {
        prefs.edit()
            .putString(KEY_TOKEN, user.token)
            .putString(KEY_USER_INFO, Json.toJson(user))
            .apply()
    }

    /** 退出登录，清空会话 */
    fun clear() {
        prefs.edit().clear().apply()
    }
}
