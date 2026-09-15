package com.monkey.wisdom.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monkey.wisdom.di.ServiceLocator
import com.monkey.wisdom.ui.components.AppScaffold
import com.monkey.wisdom.ui.components.FormBanner
import com.monkey.wisdom.ui.components.FormChips
import com.monkey.wisdom.ui.components.FormInput
import com.monkey.wisdom.ui.components.FormSection
import com.monkey.wisdom.ui.components.FormTextArea
import com.monkey.wisdom.ui.components.showToast
import com.monkey.wisdom.ui.theme.BgCard

/**
 * 发布运单页（货主端）。
 *
 * 对应 Web 端 `/publishOrder`，字段、校验与提示文案保持一致。
 */
@Composable
fun PublishOrderScreen(
    onBack: () -> Unit,
    onViewOrders: () -> Unit,
    viewModel: PublishOrderViewModel = viewModel { PublishOrderViewModel(ServiceLocator.orderRepository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 发布成功后直接回到订单列表，不停留在已提交的表单页
    LaunchedEffect(state.published) {
        if (state.published) {
            showToast(context, "订单发布成功")
            onViewOrders()
        }
    }

    AppScaffold(title = "发布运单", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ==== 货物信息 ====
                FormSection(title = "货物信息", subtitle = "先描述要运输的货物") {
                    Text(text = "物品类型", color = androidx.compose.ui.graphics.Color(0xFF64748B), fontSize = 13.sp)
                    FormChips(
                        options = PublishOrderFormState.GOODS_TYPE_OPTIONS,
                        selected = state.goodsType,
                        onSelect = viewModel::onGoodsTypeSelected,
                    )
                    if (state.isCustomGoodsType) {
                        FormInput(
                            label = "自定义物品类型",
                            value = state.customGoodsType,
                            onValueChange = { value -> viewModel.update { it.copy(customGoodsType = value, errorMessage = null) } },
                            placeholder = "请输入物品类型",
                            maxLength = 20,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FormInput(
                            label = "物品重量",
                            value = state.goodsWeight,
                            onValueChange = { value -> viewModel.update { it.copy(goodsWeight = value, errorMessage = null) } },
                            modifier = Modifier.weight(1f),
                            placeholder = "0",
                            suffix = "吨",
                            maxLength = 10,
                            keyboardType = KeyboardType.Decimal,
                            decimalOnly = true,
                        )
                        FormInput(
                            label = "运费",
                            value = state.transportMoney,
                            onValueChange = { value -> viewModel.update { it.copy(transportMoney = value, errorMessage = null) } },
                            modifier = Modifier.weight(1f),
                            placeholder = "0",
                            suffix = "元",
                            maxLength = 12,
                            keyboardType = KeyboardType.Decimal,
                            decimalOnly = true,
                        )
                    }
                    FormTextArea(
                        label = "物品描述",
                        value = state.goodsDescription,
                        onValueChange = { value -> viewModel.update { it.copy(goodsDescription = value, errorMessage = null) } },
                        placeholder = "请描述货物情况，如品类、数量、包装等",
                    )
                }

                // ==== 发货地址 ====
                AddressSection(
                    title = "发货地址",
                    subtitle = "货物从哪里发出",
                    province = state.shipperProvince,
                    city = state.shipperCity,
                    area = state.shipperArea,
                    address = state.shipperAddress,
                    onProvinceChange = { value -> viewModel.update { it.copy(shipperProvince = value, errorMessage = null) } },
                    onCityChange = { value -> viewModel.update { it.copy(shipperCity = value, errorMessage = null) } },
                    onAreaChange = { value -> viewModel.update { it.copy(shipperArea = value, errorMessage = null) } },
                    onAddressChange = { value -> viewModel.update { it.copy(shipperAddress = value, errorMessage = null) } },
                )

                // ==== 收货地址 ====
                AddressSection(
                    title = "收货地址",
                    subtitle = "货物要送到哪里",
                    province = state.carrierProvince,
                    city = state.carrierCity,
                    area = state.carrierArea,
                    address = state.carrierAddress,
                    onProvinceChange = { value -> viewModel.update { it.copy(carrierProvince = value, errorMessage = null) } },
                    onCityChange = { value -> viewModel.update { it.copy(carrierCity = value, errorMessage = null) } },
                    onAreaChange = { value -> viewModel.update { it.copy(carrierArea = value, errorMessage = null) } },
                    onAddressChange = { value -> viewModel.update { it.copy(carrierAddress = value, errorMessage = null) } },
                )

                // ==== 货主信息 ====
                FormSection(title = "货主信息", subtitle = "已自动带出，可修改") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FormInput(
                            label = "货主名称",
                            value = state.shipperName,
                            onValueChange = { value -> viewModel.update { it.copy(shipperName = value, errorMessage = null) } },
                            modifier = Modifier.weight(1f),
                            placeholder = "货主名称",
                            maxLength = 30,
                        )
                        FormInput(
                            label = "货主手机号",
                            value = state.shipperMobile,
                            onValueChange = { value ->
                                viewModel.update { it.copy(shipperMobile = value.filter { ch -> ch.isDigit() }, errorMessage = null) }
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = "货主手机号",
                            maxLength = 11,
                            keyboardType = KeyboardType.Phone,
                        )
                    }
                }

                // ==== 承运方信息（选填） ====
                FormSection(title = "承运方信息", subtitle = "选填") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FormInput(
                            label = "承运方名称",
                            value = state.carrierName,
                            onValueChange = { value -> viewModel.update { it.copy(carrierName = value) } },
                            modifier = Modifier.weight(1f),
                            placeholder = "承运方名称",
                            maxLength = 30,
                        )
                        FormInput(
                            label = "承运方手机号",
                            value = state.carrierMobile,
                            onValueChange = { value ->
                                viewModel.update { it.copy(carrierMobile = value.filter { ch -> ch.isDigit() }) }
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = "承运方手机号",
                            maxLength = 11,
                            keyboardType = KeyboardType.Phone,
                        )
                    }
                }

                // ==== 提示信息 ====
                state.errorMessage?.let { message ->
                    FormBanner(text = message, isError = true)
                }
            }

            // ==== 底部提交栏 ====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCard)
                    .padding(12.dp),
            ) {
                Button(
                    onClick = viewModel::submit,
                    enabled = !state.submitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(text = if (state.submitting) "发布中…" else "发布订单", fontSize = 16.sp)
                }
            }
        }
    }
}

/** 地址输入区块：省 / 市 / 区 + 详细地址 */
@Composable
private fun AddressSection(
    title: String,
    subtitle: String,
    province: String,
    city: String,
    area: String,
    address: String,
    onProvinceChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onAreaChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
) {
    FormSection(title = title, subtitle = subtitle) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormInput(
                label = "省份",
                value = province,
                onValueChange = onProvinceChange,
                modifier = Modifier.weight(1f),
                placeholder = "广东省",
                maxLength = 20,
            )
            FormInput(
                label = "城市",
                value = city,
                onValueChange = onCityChange,
                modifier = Modifier.weight(1f),
                placeholder = "深圳市",
                maxLength = 20,
            )
            FormInput(
                label = "区县",
                value = area,
                onValueChange = onAreaChange,
                modifier = Modifier.weight(1f),
                placeholder = "南山区",
                maxLength = 20,
            )
        }
        FormInput(
            label = "详细地址",
            value = address,
            onValueChange = onAddressChange,
            placeholder = "街道、门牌号、园区等",
            maxLength = 100,
        )
    }
}
