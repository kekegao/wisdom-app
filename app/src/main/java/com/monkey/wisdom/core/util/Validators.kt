package com.monkey.wisdom.core.util

/**
 * 表单校验规则，与 Web 端 login.vue 保持一致：校验不通过返回提示文案，通过返回 null。
 */
object Validators {

    private val MOBILE_REGEX = Regex("^1\\d{10}$")
    /** 大陆车牌号：省份简称 + 发证机关字母 + 5~7 位字母数字（覆盖蓝牌/黄牌/新能源） */
    private val PLATE_NO_REGEX = Regex("^[\\u4e00-\\u9fa5][A-Za-z][A-Za-z0-9]{4,7}$")
    private const val MIN_PASSWORD_LENGTH = 6

    /** 是否为合法的 11 位手机号 */
    fun isValidMobile(mobile: String): Boolean = MOBILE_REGEX.matches(mobile)

    /** 是否为合法车牌号 */
    fun isValidPlateNo(plateNo: String): Boolean = PLATE_NO_REGEX.matches(plateNo.trim())

    /** 登录表单校验 */
    fun validateLogin(mobile: String, password: String): String? = when {
        mobile.isBlank() -> "请输入账号"
        password.isEmpty() -> "请输入密码"
        else -> null
    }

    /** 注册表单校验（司机注册时必须填写车牌号） */
    fun validateRegister(
        userType: Int,
        realName: String,
        mobile: String,
        plateNo: String,
        password: String,
        confirmPassword: String,
    ): String? = when {
        realName.isBlank() -> "请输入真实姓名"
        !isValidMobile(mobile) -> "请输入正确的 11 位手机号"
        userType == com.monkey.wisdom.core.constants.UserType.CARRIER && plateNo.isBlank() -> "请输入车牌号"
        userType == com.monkey.wisdom.core.constants.UserType.CARRIER && !isValidPlateNo(plateNo) -> "请输入正确的车牌号"
        password.isEmpty() -> "请输入密码"
        password.length < MIN_PASSWORD_LENGTH -> "密码长度不能少于 6 位"
        password != confirmPassword -> "两次输入的密码不一致"
        else -> null
    }
}
