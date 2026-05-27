package com.daojia.app.ui.screens.cash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daojia.app.data.api.CashSettleInfo
import com.daojia.app.data.api.Result
import com.daojia.app.data.repository.OrderRepository
import com.daojia.app.ui.theme.*

/**
 * 现金结算页
 *
 * 功能：
 * - 输入订单号查询现金信息
 * - 显示订单现金金额
 * - 确认结算
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashSettleScreen(
    onBack: () -> Unit = {}
) {
    var orderNo by remember { mutableStateOf("") }
    var cashInfo by remember { mutableStateOf<CashSettleInfo?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isSettling by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var hasQueried by remember { mutableStateOf(false) }

    val repository = remember { OrderRepository() }

    // 查询现金信息
    fun queryCashInfo() {
        val no = orderNo.trim()
        if (no.isBlank()) {
            errorMessage = "请输入订单号"
            return
        }

        kotlinx.coroutines.MainScope().launch {
            isLoading = true
            errorMessage = null
            cashInfo = null
            hasQueried = true
            when (val result = repository.queryCashInfo(no)) {
                is Result.Success -> {
                    cashInfo = result.data
                }
                is Result.Error -> {
                    errorMessage = result.message
                }
                is Result.Loading -> {}
            }
            isLoading = false
        }
    }

    // 确认结算
    fun settleCash() {
        val no = orderNo.trim()
        if (no.isBlank()) return

        kotlinx.coroutines.MainScope().launch {
            isSettling = true
            errorMessage = null
            when (val result = repository.settleCash(no)) {
                is Result.Success -> {
                    if (result.data.success) {
                        successMessage = "结算成功！"
                        // 重新查询更新状态
                        queryCashInfo()
                    } else {
                        errorMessage = result.data.message.ifBlank { "结算失败" }
                    }
                }
                is Result.Error -> {
                    errorMessage = result.message
                }
                is Result.Loading -> {}
            }
            isSettling = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "现金结算") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
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
                .padding(16.dp)
        ) {
            // 说明文字
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = PrimaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "输入订单号查询现金信息，确认后进行现金结算",
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 订单号输入
            Text(
                text = "订单号",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = orderNo,
                onValueChange = {
                    orderNo = it
                    // 输入变化时清除之前的结果
                    if (hasQueried) {
                        cashInfo = null
                        hasQueried = false
                    }
                },
                label = { Text("请输入订单号") },
                placeholder = { Text("例如：DJ2024010100001") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (orderNo.isNotEmpty()) {
                        IconButton(onClick = { orderNo = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 查询按钮
            Button(
                onClick = { queryCashInfo() },
                modifier = Modifier.fillMaxWidth(),
                enabled = orderNo.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = OnPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("查询中...")
                } else {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("查询")
                }
            }

            // 错误提示
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Error.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
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

            // 现金信息展示
            if (cashInfo != null) {
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "现金结算信息",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // 现金金额（大字展示）
                        Text(
                            text = "%.2f".format(cashInfo!!.cash_amount),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                        Text(
                            text = "元",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // 详细信息
                        InfoRow(label = "订单号", value = cashInfo!!.order_no)
                        InfoRow(
                            label = "结算状态",
                            value = cashInfo!!.status_text,
                            valueColor = if (cashInfo!!.status == 1) Success else Warning
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 结算按钮
                Button(
                    onClick = { settleCash() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isSettling && cashInfo!!.status != 1,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSettling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = OnPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("结算中...")
                    } else if (cashInfo!!.status == 1) {
                        Text("已结算")
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("确认结算", fontSize = 16.sp)
                    }
                }
            }

            // 成功提示
            successMessage?.let { message ->
                LaunchedEffect(message) {
                    kotlinx.coroutines.delay(3000)
                    successMessage = null
                }
                Snackbar(
                    modifier = Modifier.padding(top = 8.dp),
                    action = {
                        TextButton(onClick = { successMessage = null }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(text = message)
                }
            }
        }
    }
}

/**
 * 信息行
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = TextPrimary
) {
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
            color = valueColor
        )
    }
}
