package com.daojia.app.ui.screens.order

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.daojia.app.data.api.PackageInfo
import com.daojia.app.data.api.WorkerInfo
import com.daojia.app.ui.theme.*

/**
 * 下单页 - 步骤式表单
 *
 * Step1：输入手机号 -> 查询套餐 -> 选择套餐
 * Step2：选择/输入服务地址 -> 选择分配方式
 * Step3：选择服务时间 -> 选择保洁师
 * 确认页：展示所有选择的信息 -> 确认下单
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    onBack: () -> Unit = {},
    viewModel: OrderViewModel = viewModel()
) {
    val state = viewModel.uiState

    // 步骤标题列表
    val stepTitles = listOf("输入手机号", "选择地址", "选择时间", "确认下单")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "手动下单") },
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
        ) {
            // 步骤指示器
            StepIndicator(
                currentStep = state.currentStep,
                stepTitles = stepTitles
            )

            // 订单类型切换
            OrderTypeToggle(
                orderType = state.orderType,
                onOrderTypeChange = { viewModel.setOrderType(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 内容区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (state.currentStep) {
                    0 -> Step1Content(viewModel = viewModel)
                    1 -> Step2Content(viewModel = viewModel)
                    2 -> Step3Content(viewModel = viewModel)
                    3 -> ConfirmContent(viewModel = viewModel)
                }
            }

            // 底部操作按钮
            BottomActions(
                currentStep = state.currentStep,
                isLoading = state.isLoading,
                onNext = { viewModel.nextStep() },
                onPrevious = { viewModel.previousStep() },
                onSubmit = { viewModel.submitOrder() },
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    // 错误提示Snackbar
    state.errorMessage?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("关闭")
                }
            }
        ) {
            Text(text = error)
        }
    }

    // 成功提示对话框
    state.successMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSuccess() },
            title = { Text(text = "下单成功") },
            text = { Text(text = message) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetForm()
                    onBack()
                }) {
                    Text(text = "完成")
                }
            }
        )
    }
}

/**
 * 步骤指示器
 */
@Composable
private fun StepIndicator(
    currentStep: Int,
    stepTitles: List<String>
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            stepTitles.forEachIndexed { index, title ->
                StepItem(
                    step = index + 1,
                    title = title,
                    isActive = index == currentStep,
                    isCompleted = index < currentStep
                )
            }
        }
    }
}

/**
 * 单个步骤项
 */
@Composable
private fun RowScope.StepItem(
    step: Int,
    title: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        // 步骤圆圈
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = when {
                isCompleted -> Success
                isActive -> Primary
                else -> com.daojia.app.ui.theme.Divider
            },
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = OnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = step.toString(),
                        color = if (isActive) OnPrimary else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) Primary else TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 订单类型切换
 */
@Composable
private fun OrderTypeToggle(
    orderType: Int,
    onOrderTypeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = orderType == 0,
            onClick = { onOrderTypeChange(0) },
            label = { Text("单次单") },
            leadingIcon = if (orderType == 0) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else null
        )
        FilterChip(
            selected = orderType == 1,
            onClick = { onOrderTypeChange(1) },
            label = { Text("周期单") },
            leadingIcon = if (orderType == 1) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            } else null
        )
    }
}

/**
 * Step1：输入手机号 -> 查询套餐 -> 选择套餐
 */
@Composable
private fun Step1Content(viewModel: OrderViewModel) {
    val state = viewModel.uiState

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 手机号输入
        Text(
            text = "客户手机号",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.phone,
            onValueChange = { viewModel.updatePhone(it) },
            label = { Text("请输入11位手机号") },
            placeholder = { Text("例如：13800138000") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (state.phone.length == 11) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Success
                    )
                }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 查询套餐按钮
        Button(
            onClick = { viewModel.queryPackages() },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.phone.length == 11 && !state.isLoadingPackages
        ) {
            if (state.isLoadingPackages) {
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
                Text("查询套餐")
            }
        }

        // 套餐列表
        if (state.packages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "选择套餐",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            state.packages.forEach { pkg ->
                PackageCard(
                    pkg = pkg,
                    isSelected = state.selectedPackage?.package_id == pkg.package_id,
                    onSelect = { viewModel.selectPackage(pkg) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * 套餐卡片
 */
@Composable
private fun PackageCard(
    pkg: PackageInfo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = if (isSelected) BorderStroke(2.dp, Primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryContainer.copy(alpha = 0.3f) else Surface
        ),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pkg.package_name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (pkg.description.isNotBlank()) {
                    Text(
                        text = pkg.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = "时长：${pkg.duration}分钟",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Text(
                text = "%.0f元".format(pkg.price),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Primary)
            }
        }
    }
}

/**
 * Step2：选择/输入服务地址 -> 选择分配方式
 */
@Composable
private fun Step2Content(viewModel: OrderViewModel) {
    val state = viewModel.uiState

    // 进入Step2时自动查询地址
    LaunchedEffect(Unit) {
        viewModel.queryAddresses()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 已有地址列表
        if (state.addresses.isNotEmpty()) {
            Text(
                text = "已有地址",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            state.addresses.forEach { address ->
                AddressCard(
                    address = address.address,
                    detail = address.detail,
                    isSelected = state.selectedAddress?.address_id == address.address_id,
                    onSelect = { viewModel.selectAddress(address) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // 自定义地址输入
        Text(
            text = "或输入新地址",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.customAddress,
            onValueChange = { viewModel.updateCustomAddress(it) },
            label = { Text("请输入详细服务地址") },
            placeholder = { Text("例如：XX小区X栋X单元X号") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 分配方式
        Text(
            text = "保洁师分配方式",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AssignTypeCard(
                title = "随机分配",
                subtitle = "系统自动分配可用保洁师",
                icon = Icons.Default.Shuffle,
                isSelected = state.assignType == 0,
                onSelect = { viewModel.setAssignType(0) },
                modifier = Modifier.weight(1f)
            )
            AssignTypeCard(
                title = "指定保洁师",
                subtitle = "手动选择保洁师",
                icon = Icons.Default.Person,
                isSelected = state.assignType == 1,
                onSelect = { viewModel.setAssignType(1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 地址卡片
 */
@Composable
private fun AddressCard(
    address: String,
    detail: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = if (isSelected) BorderStroke(2.dp, Primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryContainer.copy(alpha = 0.3f) else Surface
        ),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (isSelected) Primary else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (detail.isNotBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Primary)
            }
        }
    }
}

/**
 * 分配方式卡片
 */
@Composable
private fun AssignTypeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        border = if (isSelected) BorderStroke(2.dp, Primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryContainer.copy(alpha = 0.3f) else Surface
        ),
        onClick = onSelect
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Primary else TextSecondary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Step3：选择服务时间 -> 选择保洁师
 */
@Composable
private fun Step3Content(viewModel: OrderViewModel) {
    val state = viewModel.uiState

    // 日期选择器状态
    var showDatePicker by remember { mutableStateOf(false) }
    // 时间选择器状态
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 服务日期
        Text(
            text = "服务日期",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showDatePicker = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (state.serviceDate.isNotBlank()) state.serviceDate else "请选择服务日期",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.serviceDate.isNotBlank()) TextPrimary else TextHint
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 服务时间
        Text(
            text = "服务时间",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showTimePicker = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (state.serviceTime.isNotBlank()) state.serviceTime else "请选择服务时间",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.serviceTime.isNotBlank()) TextPrimary else TextHint
                )
            }
        }

        // 日期选择对话框
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                .format(java.util.Date(millis))
                            viewModel.setServiceDate(date)
                        }
                        showDatePicker = false
                    }) {
                        Text("确认")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("取消")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // 时间选择对话框
        if (showTimePicker) {
            val timePickerState = rememberTimePickerState()
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("选择服务时间") },
                text = { TimePicker(state = timePickerState) },
                confirmButton = {
                    TextButton(onClick = {
                        val hour = timePickerState.hour.toString().padStart(2, '0')
                        val minute = timePickerState.minute.toString().padStart(2, '0')
                        viewModel.setServiceTime("$hour:$minute")
                        showTimePicker = false
                    }) {
                        Text("确认")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("取消")
                    }
                }
            )
        }

        // 指定保洁师时显示保洁师列表
        if (state.assignType == 1) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "选择保洁师",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (state.isLoadingWorkers) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.workers.isNotEmpty()) {
                state.workers.forEach { worker ->
                    WorkerCard(
                        worker = worker,
                        isSelected = state.selectedWorker?.worker_id == worker.worker_id,
                        onSelect = { viewModel.selectWorker(worker) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Text(
                    text = "请先选择服务日期和时间",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * 保洁师卡片
 */
@Composable
private fun WorkerCard(
    worker: WorkerInfo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = if (isSelected) BorderStroke(2.dp, Primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryContainer.copy(alpha = 0.3f) else Surface
        ),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像占位
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.medium,
                color = Primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Primary)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = worker.worker_name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "完成 ${worker.order_count} 单 | 评分 ${worker.rating}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            if (!worker.available) {
                Surface(
                    color = Error.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "不可用",
                        style = MaterialTheme.typography.labelSmall,
                        color = Error,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Primary)
            }
        }
    }
}

/**
 * 确认页：展示所有选择的信息
 */
@Composable
private fun ConfirmContent(viewModel: OrderViewModel) {
    val state = viewModel.uiState

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "请确认订单信息",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 订单信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                ConfirmRow(label = "订单类型", value = if (state.orderType == 0) "单次单" else "周期单")
                ConfirmRow(label = "客户手机号", value = state.phone)
                ConfirmRow(label = "选择套餐", value = state.selectedPackage?.package_name ?: "未选择")
                state.selectedPackage?.let {
                    ConfirmRow(label = "套餐价格", value = "%.0f元".format(it.price))
                    ConfirmRow(label = "服务时长", value = "${it.duration}分钟")
                }
                val address = state.selectedAddress?.address ?: state.customAddress
                ConfirmRow(label = "服务地址", value = address.ifBlank { "未选择" })
                ConfirmRow(
                    label = "分配方式",
                    value = if (state.assignType == 0) "随机分配" else "指定保洁师"
                )
                if (state.assignType == 1 && state.selectedWorker != null) {
                    ConfirmRow(label = "保洁师", value = state.selectedWorker!!.worker_name)
                }
                ConfirmRow(
                    label = "服务时间",
                    value = if (state.serviceDate.isNotBlank() && state.serviceTime.isNotBlank())
                        "${state.serviceDate} ${state.serviceTime}" else "未选择"
                )
            }
        }
    }
}

/**
 * 确认信息行
 */
@Composable
private fun ConfirmRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.width(100.dp)
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
 * 底部操作按钮
 */
@Composable
private fun BottomActions(
    currentStep: Int,
    isLoading: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 上一步按钮（第一步不显示）
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("上一步")
                }
            }

            // 下一步/确认下单按钮
            if (currentStep < 3) {
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text("下一步")
                }
            } else {
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = OnPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("提交中...")
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("确认下单")
                    }
                }
            }
        }
    }
}
