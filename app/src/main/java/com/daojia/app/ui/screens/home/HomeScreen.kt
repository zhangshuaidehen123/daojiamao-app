package com.daojia.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daojia.app.DjApp
import com.daojia.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 首页快捷操作数据
 */
private data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color,
    val onClick: () -> Unit
)

/**
 * 首页 - 主界面
 *
 * 功能：
 * - 顶部App名称 + Cookie状态指示灯
 * - 4个快捷操作卡片
 * - 底部统计信息区域
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToOrder: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToCashSettle: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSellerSearch: () -> Unit = {}
) {
    val prefsManager = DjApp.instance.prefsManager
    var cookieValid by remember { mutableStateOf(prefsManager.isCookieValid) }
    var showCookieDialog by remember { mutableStateOf(false) }

    // 快捷操作列表
    val quickActions = remember {
        listOf(
            QuickAction(
                title = "快速下单",
                icon = Icons.Default.PostAdd,
                color = Primary,
                onClick = onNavigateToOrder
            ),
            QuickAction(
                title = "品类下单",
                icon = Icons.Default.Category,
                color = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                onClick = onNavigateToOrder
            ),
            QuickAction(
                title = "订单查询",
                icon = Icons.Default.ListAlt,
                color = Secondary,
                onClick = onNavigateToHistory
            ),
            QuickAction(
                title = "现金结算",
                icon = Icons.Default.Payment,
                color = Warning,
                onClick = onNavigateToCashSettle
            ),
            QuickAction(
                title = "查保洁师",
                icon = Icons.Default.PersonSearch,
                color = androidx.compose.ui.graphics.Color(0xFF7B1FA2),
                onClick = onNavigateToSellerSearch
            ),
            QuickAction(
                title = "系统设置",
                icon = Icons.Default.Settings,
                color = TextSecondary,
                onClick = onNavigateToSettings
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "到家保洁",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "内部下单系统",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    // Cookie状态指示灯
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        // 状态灯
                        Surface(
                            modifier = Modifier.size(10.dp),
                            shape = MaterialTheme.shapes.extraSmall,
                            color = if (cookieValid) Success else Error
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (cookieValid) "Cookie有效" else "Cookie无效",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Cookie状态卡片
            if (!cookieValid) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Error.copy(alpha = 0.1f)
                    ),
                    onClick = { showCookieDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cookie未配置或已失效",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Error
                            )
                            Text(
                                text = "点击前往设置页面配置Cookie",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 快捷操作区域标题
            Text(
                text = "快捷操作",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 快捷操作网格（2列）
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(quickActions) { action ->
                    QuickActionCard(action = action)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 底部统计信息区域
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = PrimaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(label = "今日下单", value = "0单")
                    StatItem(label = "待服务", value = "0单")
                    StatItem(label = "已完成", value = "0单")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Cookie无效提示对话框
    if (showCookieDialog) {
        AlertDialog(
            onDismissRequest = { showCookieDialog = false },
            title = { Text(text = "Cookie未配置") },
            text = { Text(text = "请先前往设置页面配置服务器地址和Cookie，否则无法正常使用下单功能。") },
            confirmButton = {
                TextButton(onClick = {
                    showCookieDialog = false
                    onNavigateToSettings()
                }) {
                    Text(text = "前往设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCookieDialog = false }) {
                    Text(text = "稍后再说")
                }
            }
        )
    }
}

/**
 * 快捷操作卡片
 */
@Composable
private fun QuickActionCard(action: QuickAction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = action.onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 图标圆形背景
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.medium,
                color = action.color.copy(alpha = 0.1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.title,
                        tint = action.color,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 统计项
 */
@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
