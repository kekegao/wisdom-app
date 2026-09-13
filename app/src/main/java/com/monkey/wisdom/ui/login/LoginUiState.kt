package com.monkey.wisdom.ui.login

import com.monkey.wisdom.core.constants.UserType
import com.monkey.wisdom.data.model.UserInfo

/** 页面模式 */
enum class AuthMode {
    /** 登录 */
    LOGIN,

    /** 注册 */
    REGISTER,
}

/**
 * 登录 / 注册页 UI 状态（单一数据源，由 ViewModel 持有并单向驱动 UI）。
 */
data class LoginUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val loading: Boolean = false,
    /** 表单错误提示 */
    val errorMessage: String? = null,
    /** 一次性提示（如注册成功） */
    val toastMessage: String? = null,
    /** 登录成功事件，由 UI 消费后跳转 */
    val loggedInUser: UserInfo? = null,

    // ==== 登录表单 ====
    val loginMobile: String = "",
    val loginPassword: String = "",

    // ==== 注册表单 ====
    /** 用户类型：1 货主，2 司机 */
    val registerUserType: Int = UserType.SHIPPER,
    val registerRealName: String = "",
    val registerMobile: String = "",
    val registerPassword: String = "",
    val registerConfirmPassword: String = "",
) {

    val title: String
        get() = if (mode == AuthMode.LOGIN) "欢迎登录" else "注册账号"

    val subtitle: String
        get() = if (mode == AuthMode.LOGIN) "请输入账号和密码登录系统" else "请填写以下信息完成注册"

    val submitText: String
        get() = when {
            loading && mode == AuthMode.LOGIN -> "登录中..."
            loading -> "注册中..."
            mode == AuthMode.LOGIN -> "登 录"
            else -> "注 册"
        }
}
