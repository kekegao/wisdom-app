package com.monkey.wisdom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monkey.wisdom.core.constants.OrderStatus
import com.monkey.wisdom.ui.theme.BrandInfo
import com.monkey.wisdom.ui.theme.BrandPrimary
import com.monkey.wisdom.ui.theme.BrandSuccess
import com.monkey.wisdom.ui.theme.BrandWarning

/**
 * 运单状态标签：按状态阶段区分颜色，便于列表快速识别进度。
 */
@Composable
fun StatusTag(
    status: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    val color = statusColor(status)
    Text(
        text = text,
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )
}

/** 便捷重载：直接使用本地状态枚举兜底文案 */
@Composable
fun StatusTag(
    status: Int,
    modifier: Modifier = Modifier,
) {
    StatusTag(status = status, text = OrderStatus.labelOf(status), modifier = modifier)
}

private fun statusColor(status: Int): Color = when (status) {
    OrderStatus.PUBLISHED.code -> BrandPrimary
    OrderStatus.ACCEPTED.code -> BrandWarning
    OrderStatus.DEALED.code -> Color(0xFF0EA5E9)
    OrderStatus.SHIPPED.code -> Color(0xFF8B5CF6)
    OrderStatus.RECEIPT_CONFIRMED.code -> BrandSuccess
    OrderStatus.RECEIPT_UPLOADED.code -> Color(0xFF059669)
    OrderStatus.SETTLE_APPLIED.code -> BrandWarning
    else -> BrandInfo
}
