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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daojia.app.data.api.ApiClient
import com.daojia.app.data.api.Result
import com.daojia.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 周期查询界面
 *
 * 通过商家ID和手机号查询：
 * - 可用服务周期
 * - 首次服务时间
 */
@Composable
fun CycleQueryScreen(onBack: () -> Unit = {}) {
    var sellerId by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var weekType by remember { mutableStateOf("1") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var availableWeeks by remember { mutableStateOf<List<String>>(emptyList()) }
    var firstServiceTimes by remember { mutableStateOf<List<String>>(emptyList()) }
    var hasQueried by remember { mutableStateOf(false) }

    val api = remember { ApiClient.instance }
    val scope = rememberCoroutineScope()

    val weekTypeOptions = listOf("1" to "每周", "2" to "每两周", "3" to "每月")

    fun queryCycle() {
        val sid = sellerId.trim()
        val mob = mobile.trim()
        if (sid.isBlank()) { errorMessage = "请输入商家ID"; return }
        if (mob.isBlank()) { errorMessage = "请输入手机号"; return }

        scope.launch {
            isLoading = true
            errorMessage = null
            hasQueried = true
            when (val result = api.queryCycleBySellerAndMobile(sid, mob, weekType)) {
                is Result.Success -> {
                    availableWeeks = result.data.available_weeks
                    firstServiceTimes = result.data.first_service_times
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
                title = { Text("周期查询") },
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
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("输入商家ID和手机号查询可用服务周期", style = MaterialTheme.typography.bodySmall, color = Primary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 商家ID
            OutlinedTextField(
                value = sellerId,
                onValueChange = { sellerId = it },
                label = { Text("商家ID") },
                placeholder = { Text("保洁师ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 手机号
            OutlinedTextField(
                value = mobile,
                onValueChange = { mobile = it },
                label = { Text("手机号") },
                placeholder = { Text("客户手机号") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 周期类型
            Text("周期类型", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weekTypeOptions.forEach { (value, label) ->
                    FilterChip(
                        selected = weekType == value,
                        onClick = { weekType = value },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 查询按钮
            Button(
                onClick = { queryCycle() },
                enabled = sellerId.isNotBlank() && mobile.isNotBlank() && !isLoading,
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
                Spacer(modifier = Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f))) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Error, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(it, color = Error)
                    }
                }
            }

            // 查询结果
            if (hasQueried && errorMessage == null) {
                Spacer(modifier = Modifier.height(24.dp))

                // 可用周期
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("可用服务周期", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (availableWeeks.isNotEmpty()) {
                            availableWeeks.forEach { week ->
                                Text("- $week", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        } else {
                            Text("暂无可用周期", color = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 首次服务时间
                if (firstServiceTimes.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("可用首次服务时间", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            firstServiceTimes.forEach { time ->
                                Text("- $time", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
