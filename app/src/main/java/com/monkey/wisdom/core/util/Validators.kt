package com.monkey.wisdom.core.util

/**
 * 表单校验规则，与 Web 端 login.vue 保持一致：校验不通过返回提示文案，通过返回 null。
 */
object Validators {

    private val MOBILE_REGEX = Regex("^1\\d{10}$")
    private const val MIN_PASSWORD_LENGTH = 6

    /** 是否为合法的 11 位手机号 */
    fun isValidMobile(mobile: String): Boolean = MOBILE_REGEX.matches(mobile)

    /** 登录表单校验 */
    fun validateLogin(mobile: String, password: String): String? = when {
        mobile.isBlank() -> "请输入账号"
        password.isEmpty() -> "请输入密码"
        else -> null
    }

    /** 注册表单校验 */
    fun validateRegister(
        realName: String,
        mobile: String,
        password: String,
        confirmPassword: String,
    ): String? = when {
        realName.isBlank() -> "请输入真实姓名"
        !isValidMobile(mobile) -> "请输入正确的 11 位手机号"
        password.isEmpty() -> "请输入密码"
        password.length < MIN_PASSWORD_LENGTH -> "密码长度不能少于 6 位"
        password != confirmPassword -> "两次输入的密码不一致"
        else -> null
    }
}
