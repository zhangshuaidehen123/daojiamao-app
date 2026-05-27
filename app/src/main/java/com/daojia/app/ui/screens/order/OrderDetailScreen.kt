package com.daojia.app.ui.screens.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daojia.app.data.api.ApiClient
import com.daojia.app.data.api.Result
import com.daojia.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 订单详情查询界面
 *
 * 通过订单ID查询：
 * - 用户真实手机号
 * - 服务地址
 * - 订单状态等
 */
@Composable
fun OrderDetailScreen(onBack: () -> Unit = {}) {
    var orderId by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasQueried by remember { mutableStateOf(false) }

    val api = remember { ApiClient.instance }
    val scope = rememberCoroutineScope()

    fun queryOrder() {
        val id = orderId.trim()
        if (id.isBlank()) {
            errorMessage = "请输入订单号"
            return
        }
        scope.launch {
            isLoading = true
            errorMessage = null
            hasQueried = true
            when (val result = api.getMobileByOrder(id)) {
                is Result.Success -> {
                    mobile = result.data
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
                title = { Text("订单详情查询") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 说明
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PrimaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("输入订单号查询用户真实手机号", style = MaterialTheme.typography.bodySmall, color = Primary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 订单号输入
            Text("订单号", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = orderId,
                onValueChange = {
                    orderId = it
                    if (hasQueried) { mobile = ""; hasQueried = false }
                },
                label = { Text("请输入订单号") },
                placeholder = { Text("例如：DJ2024010100001") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (orderId.isNotEmpty()) {
                        IconButton(onClick = { orderId = "" }) {
                            Icon(Icons.Default.Clear, "清除")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 查询按钮
            Button(
                onClick = { queryOrder() },
                enabled = orderId.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("查询中...")
                } else {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("查询")
                }
            }

            // 错误提示
            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f))) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Error, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(it, color = Error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // 查询结果
            if (hasQueried && mobile.isNotBlank()) {
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
                        Text("查询结果", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // 手机号（大字展示）
                        Text("用户手机号", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(mobile, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Primary, textAlign = TextAlign.Center)

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("订单号：$orderId", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 复制按钮
                OutlinedButton(
                    onClick = {
                        val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(mobile))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("复制手机号")
                }
            }
        }
    }
}
