package com.daojia.app.ui.screens.seller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daojia.app.data.api.ApiClient
import com.daojia.app.data.api.Result
import com.daojia.app.data.api.WorkerInfo
import kotlinx.coroutines.launch

/**
 * 保洁师搜索页面
 *
 * 支持按姓名或手机号搜索保洁师，显示保洁师ID、姓名、手机号、状态
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerSearchScreen(
    onBack: () -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }
    var searchType by remember { mutableStateOf("name") } // "name" 或 "mobile"
    var sellers by remember { mutableStateOf<List<WorkerInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 搜索保洁师
    fun doSearch() {
        if (searchText.isBlank()) return
        isLoading = true
        errorMessage = ""
        hasSearched = true

        scope.launch {
            if (searchType == "name") {
                when (val result = ApiClient.instance.searchSellerByName(searchText.trim())) {
                    is Result.Success -> {
                        sellers = result.data
                        if (sellers.isEmpty()) errorMessage = "未找到姓名包含「${searchText}」的保洁师"
                    }
                    is Result.Error -> errorMessage = result.message
                    is Result.Loading -> {}
                }
            } else {
                when (val result = ApiClient.instance.searchSellerByName(searchText.trim())) {
                    is Result.Success -> {
                        sellers = result.data
                        if (sellers.isEmpty()) errorMessage = "未找到手机号为「${searchText}」的保洁师"
                    }
                    is Result.Error -> errorMessage = result.message
                    is Result.Loading -> {}
                }
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("查保洁师") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 搜索类型切换
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = searchType == "name",
                    onClick = { searchType = "name" },
                    label = { Text("按姓名搜索") },
                    leadingIcon = if (searchType == "name") {{ Icon(Icons.Default.Search, null) }} else null
                )
                FilterChip(
                    selected = searchType == "mobile",
                    onClick = { searchType = "mobile" },
                    label = { Text("按手机号搜索") },
                    leadingIcon = if (searchType == "mobile") {{ Icon(Icons.Default.Search, null) }} else null
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 搜索框
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text(if (searchType == "name") "输入保洁师姓名（如：何章）" else "输入保洁师手机号") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (searchText.isNotBlank()) {
                        IconButton(onClick = { searchText = "" }) {
                            Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { doSearch() })
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 搜索按钮
            Button(
                onClick = { doSearch() },
                modifier = Modifier.fillMaxWidth(),
                enabled = searchText.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("搜索")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 错误提示
            if (errorMessage.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 结果列表
            if (sellers.isNotEmpty()) {
                Text(
                    text = "找到 ${sellers.size} 个保洁师",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sellers) { seller ->
                        SellerCard(seller = seller)
                    }
                }
            } else if (hasSearched && errorMessage.isBlank() && !isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "未找到匹配的保洁师",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 保洁师信息卡片
 */
@Composable
fun SellerCard(seller: WorkerInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 姓名行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = seller.worker_name.ifBlank { "未知姓名" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (seller.available) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "在线",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ID行（重点显示）
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "保洁师ID：",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = seller.worker_id,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 手机号行
            if (seller.phone.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "手机号：",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = seller.phone,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 距离行
            // 评分信息
            if (seller.rating > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "距离：",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${seller.rating}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
