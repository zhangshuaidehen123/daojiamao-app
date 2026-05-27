package com.daojia.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 通用组件库
 *
 * 提供全局复用的UI组件，减少重复代码
 */

/**
 * 加载中组件
 *
 * 用于页面加载状态的统一展示
 */
@Composable
fun LoadingView(
    modifier: Modifier = Modifier,
    message: String = "加载中..."
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 空状态组件
 *
 * 用于列表无数据时的展示
 */
@Composable
fun EmptyView(
    modifier: Modifier = Modifier,
    message: String = "暂无数据",
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onAction) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

/**
 * 错误状态组件
 *
 * 用于网络请求失败时的展示
 */
@Composable
fun ErrorView(
    modifier: Modifier = Modifier,
    message: String = "请求失败",
    onRetry: () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(text = "重试")
            }
        }
    }
}

/**
 * 确认对话框
 *
 * 通用的确认/取消对话框
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确认",
    dismissText: String = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        }
    )
}

/**
 * 带间距的Column
 */
@Composable
fun SpacedColumn(
    modifier: Modifier = Modifier,
    verticalSpacing: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        content()
    }
}
