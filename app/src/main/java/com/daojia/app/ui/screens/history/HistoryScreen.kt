package com.daojia.app.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.daojia.app.data.api.OrderInfo
import com.daojia.app.data.api.Result
import com.daojia.app.data.repository.OrderRepository
import com.daojia.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 订单历史页 - 查询和展示订单列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    var searchPhone by remember { mutableStateOf("") }
    var orders by remember { mutableStateOf<List<OrderInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedOrder by remember { mutableStateOf<OrderInfo?>(null) }

    val repository = remember { OrderRepository() }
    val scope = rememberCoroutineScope()

    // 搜索订单
    fun searchOrders() {
        val phone = searchPhone.trim()
        if (phone.isBlank() || phone.length != 11) {
            errorMessage = "请输入正确的11位手机号"
            return
        }

        scope.launch {
            isLoading = true
            errorMessage = null
            when (val result = repository.queryOrders(phone)) {
                is Result.Success -> {
                    orders = result.data
                    if (result.data.isEmpty()) {
                        errorMessage = "暂无订单记录"
                    }
                }
                is Result.Error -> {
                    errorMessage = result.message
                }
                is Result.Loading -> {}
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "订单查询") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 搜索栏
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchPhone,
                        onValueChange = { searchPhone = it },
                        label = { Text("输入手机号查询") },
                        placeholder = { Text("客户手机号") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            if (searchPhone.isNotEmpty()) {
                                IconButton(onClick = { searchPhone = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除")
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { searchOrders() },
                        enabled = searchPhone.length == 11 && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = OnPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // 错误提示
            errorMessage?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Error.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Error
                        )
                    }
                }
            }

            // 订单列表
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (orders.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders) { order ->
                        OrderCard(
                            order = order,
                            onClick = { selectedOrder = order }
                        )
                    }
                }
            } else if (errorMessage == null) {
                // 空状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = null,
                            tint = TextHint,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "请输入手机号查询订单",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }

    // 订单详情对话框
    selectedOrder?.let { order ->
        AlertDialog(
            onDismissRequest = { selectedOrder = null },
            title = { Text(text = "订单详情") },
            text = {
                Column {
                    DetailRow(label = "订单号", value = order.order_no)
                    DetailRow(label = "客户手机", value = order.phone)
                    DetailRow(label = "套餐", value = order.package_name)
                    DetailRow(label = "服务地址", value = order.address)
                    DetailRow(label = "保洁师", value = order.worker_name.ifBlank { "未分配" })
                    DetailRow(label = "服务时间", value = order.service_time)
                    DetailRow(label = "订单状态", value = order.status_text)
                    DetailRow(label = "订单金额", value = "%.2f元".format(order.amount))
                    DetailRow(label = "现金金额", value = "%.2f元".format(order.cash_amount))
                    DetailRow(label = "下单时间", value = order.create_time)
                    DetailRow(label = "订单类型", value = if (order.order_type == 0) "单次单" else "周期单")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedOrder = null }) {
                    Text(text = "关闭")
                }
            }
        )
    }
}

/**
 * 订单卡片
 */
@Composable
private fun OrderCard(
    order: OrderInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 顶部：订单号 + 状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "订单号：${order.order_no}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                // 状态标签
                Surface(
                    color = getStatusColor(order.status).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = order.status_text,
                        style = MaterialTheme.typography.labelSmall,
                        color = getStatusColor(order.status),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 中间信息
            InfoRow(icon = Icons.Default.Person, text = "客户：${order.phone}")
            InfoRow(icon = Icons.Default.Home, text = "套餐：${order.package_name}")
            InfoRow(icon = Icons.Default.LocationOn, text = "地址：${order.address}")
            InfoRow(icon = Icons.Default.AccessTime, text = "时间：${order.service_time}")

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 底部：金额 + 时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "金额：%.2f元".format(order.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Text(
                    text = order.create_time,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * 信息行
 */
@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            maxLines = 1
        )
    }
}

/**
 * 详情行
 */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 根据订单状态获取颜色
 */
private fun getStatusColor(status: Int): androidx.compose.ui.graphics.Color {
    return when (status) {
        0 -> Warning    // 待分配
        1 -> Info       // 已分配
        2 -> Primary    // 进行中
        3 -> Success    // 已完成
        4 -> Error      // 已取消
        else -> TextSecondary
    }
}
