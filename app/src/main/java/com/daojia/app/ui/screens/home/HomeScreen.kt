package com.daojia.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daojia.app.ui.theme.*

/**
 * 首页 - 功能入口
 */
@Composable
fun HomeScreen(
    onNavigateToOrder: () -> Unit = {},
    onNavigateToCategoryOrder: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToCashSettle: () -> Unit = {},
    onNavigateToSellerSearch: () -> Unit = {},
    onNavigateToOrderDetail: () -> Unit = {},
    onNavigateToCycleQuery: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部标题栏
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Primary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "到家保洁",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnPrimary
                )
                Text(
                    text = "内部下单系统",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnPrimary.copy(alpha = 0.8f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ==================== 下单区域 ====================
            SectionTitle("下单功能")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.ShoppingCart,
                    title = "套餐单次单",
                    subtitle = "套餐下单",
                    color = Primary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToOrder
                )
                QuickActionCard(
                    icon = Icons.Default.Repeat,
                    title = "套餐周期单",
                    subtitle = "周期下单",
                    color = Primary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToOrder
                )
            }

            QuickActionCard(
                icon = Icons.Default.Category,
                title = "品类订单",
                subtitle = "日常保洁、油烟机清洗等28种服务",
                color = Primary,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToCategoryOrder
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ==================== 查询区域 ====================
            SectionTitle("查询功能")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.Search,
                    title = "订单查询",
                    subtitle = "按手机号查",
                    color = Info,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToHistory
                )
                QuickActionCard(
                    icon = Icons.Default.Phone,
                    title = "订单查手机",
                    subtitle = "按订单ID查",
                    color = Info,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToOrderDetail
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.PersonSearch,
                    title = "查保洁师",
                    subtitle = "按姓名查ID",
                    color = Info,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSellerSearch
                )
                QuickActionCard(
                    icon = Icons.Default.EventRepeat,
                    title = "周期查询",
                    subtitle = "商家ID+手机号",
                    color = Info,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCycleQuery
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ==================== 其他功能 ====================
            SectionTitle("其他功能")

            QuickActionCard(
                icon = Icons.Default.Payment,
                title = "现金结算",
                subtitle = "查询并确认现金结算",
                color = Warning,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToCashSettle
            )

            Spacer(modifier = Modifier.weight(1f))

            // 底部设置入口
            TextButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("设置")
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextHint, modifier = Modifier.size(20.dp))
        }
    }
}
