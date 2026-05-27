package com.daojia.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.daojia.app.DjApp
import com.daojia.app.data.api.Result
import com.daojia.app.data.api.UpdateInfo
import com.daojia.app.ui.theme.*
import com.daojia.app.util.UpdateChecker

/**
 * 设置页
 *
 * 功能：
 * - 服务器地址配置
 * - Cookie管理（查看状态/更新）
 * - 版本信息
 * - 检查更新按钮
 * - 关于
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefsManager = DjApp.instance.prefsManager

    // 服务器地址
    var serverUrl by remember { mutableStateOf(prefsManager.serverUrl) }
    var showServerDialog by remember { mutableStateOf(false) }
    var serverInput by remember { mutableStateOf(prefsManager.serverUrl) }

    // Cookie
    var cookieContent by remember { mutableStateOf(prefsManager.cookieContent) }
    var showCookieDialog by remember { mutableStateOf(false) }
    var cookieInput by remember { mutableStateOf(prefsManager.cookieContent) }

    // 更新
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    // 版本信息
    val versionName = "1.0.0"
    val versionCode = 1

    // 检查更新
    fun LaunchedEffect(Unit) {
     LaunchedEffect(Unit) {
     checkUpdate()
 }
 } {
        isCheckingUpdate = true
        kotlinx.coroutines.MainScope().launch {
            when (val result = UpdateChecker.checkUpdate()) {
                is Result.Success -> {
                    if (result.data.version_code > versionCode) {
                        updateInfo = result.data
                        showUpdateDialog = true
                    } else {
                        Toast.makeText(context, "已是最新版本", Toast.LENGTH_SHORT).show()
                    }
                }
                is Result.Error -> {
                    Toast.makeText(context, "检查更新失败：${result.message}", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> {}
            }
            isCheckingUpdate = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "设置") },
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
                .verticalScroll(rememberScrollState())
        ) {
            // ==================== 服务器配置 ====================
            SectionTitle(title = "服务器配置")

            SettingCard(
                icon = Icons.Default.Dns,
                title = "服务器地址",
                subtitle = serverUrl,
                onClick = {
                    serverInput = serverUrl
                    showServerDialog = true
                }
            )

            // ==================== Cookie管理 ====================
            SectionTitle(title = "Cookie管理")

            SettingCard(
                icon = Icons.Default.Cookie,
                title = "Cookie状态",
                subtitle = if (prefsManager.isCookieValid) "有效" else "无效/未配置",
                subtitleColor = if (prefsManager.isCookieValid) Success else Error,
                onClick = { /* 可扩展：验证Cookie */ }
            )

            SettingCard(
                icon = Icons.Default.Edit,
                title = "更新Cookie",
                subtitle = if (cookieContent.isNotBlank()) "已配置 (${cookieContent.length}字符)" else "未配置",
                onClick = {
                    cookieInput = cookieContent
                    showCookieDialog = true
                }
            )

            // ==================== 版本信息 ====================
            SectionTitle(title = "版本信息")

            SettingCard(
                icon = Icons.Default.Info,
                title = "当前版本",
                subtitle = "v$versionName (Build $versionCode)",
                onClick = { }
            )

            SettingCard(
                icon = Icons.Default.SystemUpdate,
                title = "检查更新",
                subtitle = if (isCheckingUpdate) "正在检查..." else "点击检查新版本",
                onClick = { checkUpdate() },
                enabled = !isCheckingUpdate
            )

            // ==================== 关于 ====================
            SectionTitle(title = "关于")

            SettingCard(
                icon = Icons.Default.Business,
                title = "到家保洁",
                subtitle = "内部下单系统 v$versionName",
                onClick = { }
            )

            SettingCard(
                icon = Icons.Default.Description,
                title = "使用说明",
                subtitle = "仅供内部员工使用",
                onClick = { }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ==================== 服务器地址编辑对话框 ====================
    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text(text = "配置服务器地址") },
            text = {
                OutlinedTextField(
                    value = serverInput,
                    onValueChange = { serverInput = it },
                    label = { Text("服务器地址") },
                    placeholder = { Text("https://your-server.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (serverInput.isNotBlank()) {
                        prefsManager.serverUrl = serverInput.trimEnd('/')
                        serverUrl = serverInput.trimEnd('/')
                        Toast.makeText(context, "服务器地址已更新", Toast.LENGTH_SHORT).show()
                    }
                    showServerDialog = false
                }) {
                    Text(text = "保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text(text = "取消")
                }
            }
        )
    }

    // ==================== Cookie编辑对话框 ====================
    if (showCookieDialog) {
        AlertDialog(
            onDismissRequest = { showCookieDialog = false },
            title = { Text(text = "更新Cookie") },
            text = {
                OutlinedTextField(
                    value = cookieInput,
                    onValueChange = { cookieInput = it },
                    label = { Text("Cookie内容") },
                    placeholder = { Text("请粘贴Cookie内容") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    visualTransformation = PasswordVisualTransformation()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    prefsManager.cookieContent = cookieInput
                    cookieContent = cookieInput
                    Toast.makeText(context, "Cookie已更新", Toast.LENGTH_SHORT).show()
                    showCookieDialog = false
                }) {
                    Text(text = "保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCookieDialog = false }) {
                    Text(text = "取消")
                }
            }
        )
    }

    // ==================== 更新对话框 ====================
    if (showUpdateDialog && updateInfo != null) {
        AlertDialog(
            onDismissRequest = {
                // 非强制更新可关闭
                if (!updateInfo!!.force_update) {
                    showUpdateDialog = false
                }
            },
            title = { Text(text = "发现新版本") },
            text = {
                Column {
                    Text(
                        text = "新版本：v${updateInfo!!.version_name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "当前版本：v$versionName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = updateInfo!!.update_content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (updateInfo!!.force_update) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "此版本为强制更新，必须更新后才能继续使用",
                            style = MaterialTheme.typography.bodySmall,
                            color = Error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // 打开浏览器下载
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo!!.apk_url))
                    context.startActivity(intent)
                }) {
                    Text(text = "立即更新")
                }
            },
            dismissButton = {
                if (!updateInfo!!.force_update) {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text(text = "稍后再说")
                    }
                }
            }
        )
    }
}

/**
 * 分区标题
 */
@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

/**
 * 设置项卡片
 */
@Composable
private fun SettingCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    subtitleColor: androidx.compose.ui.graphics.Color = TextSecondary,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (enabled) onClick() },
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) Primary else TextHint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) TextPrimary else TextHint
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) subtitleColor else TextHint,
                    maxLines = 1
                )
            }
            if (enabled) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextHint
                )
            }
        }
    }
}
