package com.monkey.wisdom.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.core.storage.UserSession
import com.monkey.wisdom.data.model.request.PublishOrderRequest
import com.monkey.wisdom.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 发布运单表单状态。
 *
 * 校验规则与提示文案与 Web 端 `publishOrder.vue` 完全一致，保证两端体验统一。
 */
data class PublishOrderFormState(
    // ==== 货物信息 ====
    /** 物品类型，空表示未选择（含「其他」自定义） */
    val goodsType: String = "",
    val customGoodsType: String = "",
    val goodsDescription: String = "",
    /** 物品重量（吨），字符串便于输入过程态 */
    val goodsWeight: String = "",
    /** 运费（元），空表示面议 */
    val transportMoney: String = "",

    // ==== 发货地址 ====
    val shipperProvince: String = "",
    val shipperCity: String = "",
    val shipperArea: String = "",
    val shipperAddress: String = "",

    // ==== 收货地址 ====
    val carrierProvince: String = "",
    val carrierCity: String = "",
    val carrierArea: String = "",
    val carrierAddress: String = "",

    // ==== 货主信息（自动带出，可修改） ====
    val shipperName: String = "",
    val shipperMobile: String = "",

    // ==== 承运方信息（选填） ====
    val carrierName: String = "",
    val carrierMobile: String = "",

    // ==== 提交态 ====
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val submitting: Boolean = false,
) {

    /** 是否选择了「其他」，需要展示自定义物品类型输入框 */
    val isCustomGoodsType: Boolean get() = goodsType == CUSTOM_GOODS_TYPE

    /** 真正提交的物品类型 */
    val effectiveGoodsType: String get() = if (isCustomGoodsType) customGoodsType.trim() else goodsType

    companion object {
        const val CUSTOM_GOODS_TYPE = "其他"
        val GOODS_TYPE_OPTIONS = listOf("建材", "钢铁", "煤炭", CUSTOM_GOODS_TYPE)
    }
}

/**
 * 发布运单 ViewModel：表单维护 + 校验 + 提交。
 */
class PublishOrderViewModel(
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PublishOrderFormState())
    val state: StateFlow<PublishOrderFormState> = _state.asStateFlow()

    init {
        prefillShipperInfo()
    }

    /** 表单字段统一更新入口 */
    fun update(transform: (PublishOrderFormState) -> PublishOrderFormState) {
        _state.update(transform)
    }

    /** 切换物品类型：从「其他」切回预设项时清空自定义输入 */
    fun onGoodsTypeSelected(type: String) {
        _state.update {
            it.copy(
                goodsType = type,
                customGoodsType = if (type == PublishOrderFormState.CUSTOM_GOODS_TYPE) it.customGoodsType else "",
                errorMessage = null,
            )
        }
    }

    /** 清空错误/成功提示 */
    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, successMessage = null) }
    }

    /** 提交发布 */
    fun submit() {
        val current = _state.value
        if (current.submitting) return

        val validationError = validate(current)
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError, successMessage = null) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(submitting = true, errorMessage = null, successMessage = null) }
            when (val result = orderRepository.publishOrder(buildRequest(current))) {
                is AppResult.Success -> _state.update { state ->
                    // 与 Web 端一致：发布成功后清空货物与地址，保留货主信息
                    state.copy(
                        submitting = false,
                        successMessage = "订单发布成功",
                        goodsType = "",
                        customGoodsType = "",
                        goodsDescription = "",
                        goodsWeight = "",
                        transportMoney = "",
                        shipperProvince = "",
                        shipperCity = "",
                        shipperArea = "",
                        shipperAddress = "",
                        carrierProvince = "",
                        carrierCity = "",
                        carrierArea = "",
                        carrierAddress = "",
                        carrierName = "",
                        carrierMobile = "",
                    )
                }

                is AppResult.Failure -> _state.update { state ->
                    state.copy(
                        submitting = false,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    /** 表单校验，返回错误文案；null 表示通过 */
    private fun validate(form: PublishOrderFormState): String? = when {
        form.effectiveGoodsType.isBlank() -> "请选择或填写物品类型"
        form.goodsDescription.isBlank() -> "请输入物品描述"
        form.goodsWeight.toDoubleOrNull()?.let { it > 0 } != true -> "请输入正确的物品重量"
        form.transportMoney.isNotBlank() &&
            (form.transportMoney.toDoubleOrNull()?.let { it >= 0 } != true) -> "请输入正确的运费金额"
        !isAddressComplete(form.shipperProvince, form.shipperCity, form.shipperArea, form.shipperAddress) ->
            "请完整填写发货地址"
        !isAddressComplete(form.carrierProvince, form.carrierCity, form.carrierArea, form.carrierAddress) ->
            "请完整填写收货地址"
        form.shipperName.isBlank() -> "请输入货主名称"
        !MOBILE_REGEX.matches(form.shipperMobile.trim()) -> "请输入正确的货主手机号"
        else -> null
    }

    private fun isAddressComplete(province: String, city: String, area: String, address: String): Boolean =
        province.isNotBlank() && city.isNotBlank() && area.isNotBlank() && address.isNotBlank()

    private fun buildRequest(form: PublishOrderFormState): PublishOrderRequest {
        val user = UserSession.currentUser
        return PublishOrderRequest(
            shipperUserId = user?.userId?.toString(),
            shipperUserName = user?.userName,
            shipperName = form.shipperName.trim(),
            shipperMobile = form.shipperMobile.trim(),
            // 承运方信息选填，未填写时不提交（Gson 自动省略 null）
            carrierUserId = null,
            carrierUserName = null,
            carrierName = form.carrierName.trim().ifBlank { null },
            carrierMobile = form.carrierMobile.trim().ifBlank { null },
            goodsType = form.effectiveGoodsType,
            goodsDescription = form.goodsDescription.trim(),
            goodsWeight = form.goodsWeight.toDoubleOrNull() ?: 0.0,
            transportMoney = form.transportMoney.toDoubleOrNull(),
            shipperProvince = form.shipperProvince.trim(),
            shipperCity = form.shipperCity.trim(),
            shipperArea = form.shipperArea.trim(),
            shipperAddress = form.shipperAddress.trim(),
            carrierProvince = form.carrierProvince.trim(),
            carrierCity = form.carrierCity.trim(),
            carrierArea = form.carrierArea.trim(),
            carrierAddress = form.carrierAddress.trim(),
        )
    }

    /** 登录用户信息自动带出货主名称/手机号 */
    private fun prefillShipperInfo() {
        val user = UserSession.currentUser ?: return
        val name = user.realName?.takeIf { it.isNotBlank() } ?: user.userName.orEmpty()
        _state.update {
            it.copy(
                shipperName = name,
                shipperMobile = user.mobile.orEmpty(),
                carrierName = "",
                carrierMobile = "",
            )
        }
    }

    private companion object {
        val MOBILE_REGEX = Regex("^1\\d{10}$")
    }
}
