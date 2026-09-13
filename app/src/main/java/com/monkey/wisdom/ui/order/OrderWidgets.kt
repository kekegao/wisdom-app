package com.monkey.wisdom.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monkey.wisdom.ui.theme.BgCard
import com.monkey.wisdom.ui.theme.BgPage
import com.monkey.wisdom.ui.theme.TextMuted
import com.monkey.wisdom.ui.theme.TextPrimary
import com.monkey.wisdom.ui.theme.TextSecondary

/** 状态标签配色 */
data class StatusStyle(val textColor: Color, val bgColor: Color)

/** 货主端状态配色：待接单 / 已接单 / 运输中 / 已完成 / 已取消 */
fun shipperStatusStyle(status: Int): StatusStyle = when (status) {
    1 -> StatusStyle(Color(0xFFB45309), Color(0xFFFEF3C7))
    2, 3 -> StatusStyle(Color(0xFF1D4ED8), Color(0xFFDBEAFE))
    4 -> StatusStyle(Color(0xFF0369A1), Color(0xFFE0F2FE))
    -1 -> StatusStyle(Color(0xFFB91C1C), Color(0xFFFEE2E2))
    else -> if (status in 5..10) {
        StatusStyle(Color(0xFF047857), Color(0xFFD1FAE5))
    } else {
        StatusStyle(Color(0xFF4B5563), Color(0xFFE5E7EB))
    }
}

/** 货主端列表状态文案：按阶段归并，与 Web 端 STATUS_TEXT 一致 */
fun shipperStatusText(status: Int, statusDesc: String? = null): String = when (status) {
    1 -> "待接单"
    2, 3 -> "已接单"
    4 -> "运输中"
    -1 -> "已取消"
    else -> if (status in 5..10) "已完成" else statusDesc?.takeIf { it.isNotBlank() } ?: "其他"
}

/** 承运端「我的运单」状态配色：进行中 / 已完成 */
fun carrierOrderStatusStyle(status: Int): StatusStyle =
    if (status in RUNNING_STATUSES) StatusStyle(Color(0xFF0369A1), Color(0xFFE0F2FE))
    else StatusStyle(Color(0xFF047857), Color(0xFFD1FAE5))

/** 承运端「我的运单」状态文案：优先后端 statusDesc */
fun carrierOrderStatusText(status: Int, statusDesc: String?): String =
    statusDesc?.takeIf { it.isNotBlank() } ?: CARRIER_STATUS_TEXT[status] ?: "状态$status"

/** 货源大厅状态配色：可摘单 / 已摘单 */
fun sourceStatusStyle(status: Int): StatusStyle =
    if (status == 1) StatusStyle(Color(0xFFB45309), Color(0xFFFEF3C7))
    else StatusStyle(Color(0xFF047857), Color(0xFFD1FAE5))

/** 货源大厅状态文案 */
fun sourceStatusText(status: Int): String = if (status == 1) "可摘单" else "已摘单"

/** 承运端进行中状态：摘单→结算申请 */
val RUNNING_STATUSES = listOf(2, 3, 4, 5, 6, 7)

private val CARRIER_STATUS_TEXT = mapOf(
    2 to "摘单",
    3 to "成交",
    4 to "发货",
    5 to "确认收货",
    6 to "回单确认",
    7 to "结算申请",
    8 to "结算",
    9 to "对账",
    10 to "发票",
)

// ==================== 文案格式化（与 Web 端一致） ====================

/** 运费展示：未填写显示「面议」 */
internal fun orderFeeText(raw: String?): String =
    if (raw.isNullOrBlank()) "面议" else "¥ ${raw.trim()}"

/** 重量展示：未填写显示「未填写」 */
internal fun orderWeightText(raw: String?): String =
    if (raw.isNullOrBlank()) "未填写" else "${raw.trim()} 吨"

/** 省市区拼接（过滤空段） */
internal fun regionText(province: String?, city: String?, area: String?): String =
    listOf(province, city, area).filterNotNull().filter { it.isNotBlank() }.joinToString(" ")

/** 空值占位 */
internal fun displayOrDash(value: String?): String =
    if (value.isNullOrBlank()) "—" else value.trim()

// ==================== 组件 ====================

/** 状态徽标 */
@Composable
fun StatusBadge(
    text: String,
    style: StatusStyle,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(style.bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = style.textColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )
}

/** 顶部统计卡：总量 + 分项统计 + 右上角操作按钮 */
@Composable
fun OrderStatCard(
    title: String,
    total: Int,
    stats: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (stats.size == 2) {
            // 承运端「我的运单」：分项统计分别居中在标题与操作按钮的正下方
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    StatTitle(title = title, total = total)
                    StatColumn(
                        label = stats[0].first,
                        value = stats[0].second,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (actionText != null && onAction != null) {
                        StatActionButton(text = actionText, onClick = onAction)
                    }
                    StatColumn(
                        label = stats[1].first,
                        value = stats[1].second,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatTitle(title = title, total = total)
                if (actionText != null && onAction != null) {
                    StatActionButton(text = actionText, onClick = onAction)
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                stats.forEach { (label, value) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(text = value.toString(), color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(text = label, color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/** 统计卡标题：文案 + 总数量 */
@Composable
private fun StatTitle(title: String, total: Int) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = title, color = TextSecondary, fontSize = 13.sp)
        Text(
            text = total.toString(),
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** 统计卡右上角操作按钮 */
@Composable
private fun StatActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(34.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
    ) {
        Text(text = text, fontSize = 13.sp)
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: Int,
    horizontalAlignment: Alignment.Horizontal,
) {
    Column(
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = value.toString(), color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = TextMuted, fontSize = 11.sp)
    }
}

/** 状态筛选标签条（可横向滚动） */
@Composable
fun FilterTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) MaterialTheme.colorScheme.primary else BgCard)
                    .border(
                        width = 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(50),
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 7.dp),
            ) {
                Text(
                    text = label,
                    color = if (selected) Color.White else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}

/** 运输路线区块：发货 / 收货 */
@Composable
fun OrderRouteBlock(
    shipperRegion: String,
    shipperAddress: String?,
    carrierRegion: String,
    carrierAddress: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RouteRow(
            tag = "发货",
            tagColor = Color(0xFF2563EB),
            region = shipperRegion,
            address = shipperAddress,
        )
        RouteRow(
            tag = "收货",
            tagColor = Color(0xFF059669),
            region = carrierRegion,
            address = carrierAddress,
        )
    }
}

@Composable
private fun RouteRow(
    tag: String,
    tagColor: Color,
    region: String,
    address: String?,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(tagColor),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = tag,
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(tagColor.copy(alpha = 0.1f))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                    color = tagColor,
                    fontSize = 11.sp,
                )
                Text(
                    text = region.ifBlank { "—" },
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(text = address?.takeIf { it.isNotBlank() } ?: "—", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

/** 货物概要行：类型 + 重量 + 运费 */
@Composable
fun GoodsSummaryRow(
    goodsType: String?,
    weightText: String,
    feeText: String,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = displayOrDash(goodsType),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(BgPage)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                color = TextPrimary,
                fontSize = 12.sp,
            )
            Text(text = "重量 $weightText", color = TextSecondary, fontSize = 12.sp)
            Text(text = feeText, color = Color(0xFFDC2626), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        if (!description.isNullOrBlank()) {
            Text(text = description, color = TextSecondary, fontSize = 12.sp)
        }
    }
}

/** 卡片操作按钮 */
@Composable
fun CardActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
) {
    val containerColor = when {
        danger -> Color(0xFFDC2626)
        primary -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val contentColor = if (primary || danger) Color.White else TextSecondary

    if (primary || danger) {
        Button(
            onClick = onClick,
            modifier = modifier.height(32.dp),
            enabled = enabled,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        ) {
            Text(text = text, fontSize = 13.sp)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(32.dp),
            enabled = enabled,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        ) {
            Text(text = text, color = contentColor, fontSize = 13.sp)
        }
    }
}
