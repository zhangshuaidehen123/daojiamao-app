package com.daojia.app.ui.screens.order

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.daojia.app.data.api.CategoryInfo
import com.daojia.app.data.api.CategoryOrderRequest
import com.daojia.app.data.api.Result
import com.daojia.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 品类订单界面
 *
 * 流程：选择服务类型 -> 选择规格+数量 -> 填写信息(手机号/地址) -> 下单
 * 所有选项都通过选择器完成，只有手机号和地址需要手动输入
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CategoryOrderScreen(onBack: () -> Unit = {}) {
    var currentStep by remember { mutableIntStateOf(0) }

    // 服务类型选择
    var selectedCategory by remember { mutableStateOf<CategoryInfo?>(null) }
    var selectedSpec by remember { mutableStateOf("") }
    var selectedQuantity by remember { mutableIntStateOf(1) }

    // 下单表单（只有这些需要手动输入）
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var assignMode by remember { mutableStateOf("auto") }
    var sellerMobile by remember { mutableStateOf("") }
    var detailText by remember { mutableStateOf("") }

    // 服务时间（选择器）
    var serviceDate by remember { mutableStateOf("") }
    var serviceTime by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // 状态
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val api = remember { ApiClient.instance }
    val scope = rememberCoroutineScope()

    // 内置品类列表（从源代码config.py提取）
    val categories = remember {
        listOf(
            CategoryInfo("日常保洁", "1001", "DJ_BJ_RC", 3, listOf("2小时", "3小时", "4小时"), "小时", "日常家庭保洁服务"),
            CategoryInfo("深度保洁", "1002", "DJ_BJ_SD", 4, listOf("4小时", "5小时", "6小时"), "小时", "深度清洁，全屋无死角"),
            CategoryInfo("开荒保洁", "1003", "DJ_BJ_KH", 4, listOf("4小时", "6小时", "8小时"), "小时", "新房/装修后开荒保洁"),
            CategoryInfo("擦玻璃", "1020", "DJ_BJ_BL", 2, listOf("2小时", "3小时", "4小时"), "小时", "室内外玻璃清洁"),
            CategoryInfo("钟点工", "1029", "DJ_BJ_ZDG", 2, listOf("2小时", "3小时", "4小时"), "小时", "钟点工服务"),
            CategoryInfo("油烟机清洗", "1006", "DJ_JD_YYJ", 1, listOf("台式", "中式", "欧式", "侧吸"), "台", "油烟机深度拆洗"),
            CategoryInfo("冰箱清洗", "1007", "DJ_JD_BX", 1, listOf("单门", "双门", "三门", "对开门"), "台", "冰箱内外清洁消毒"),
            CategoryInfo("洗衣机清洗", "1008", "DJ_JD_XYJ", 1, listOf("波轮", "滚筒"), "台", "洗衣机内筒深度清洁"),
            CategoryInfo("空调清洗", "1009", "DJ_JD_KT", 1, listOf("挂机", "柜机", "中央空调"), "台", "空调深度清洗"),
            CategoryInfo("灶台清洗", "1010", "DJ_JD_ZT", 1, listOf("单眼灶", "双眼灶", "集成灶"), "台", "灶台深度清洁"),
            CategoryInfo("微波炉清洗", "1011", "DJ_JD_WBL", 1, listOf("台式", "嵌入式"), "台", "微波炉清洁除味"),
            CategoryInfo("热水器清洗", "1012", "DJ_JD_RSQ", 1, listOf("电热水器", "燃气热水器"), "台", "热水器内胆清洗除垢"),
            CategoryInfo("灯具清洗", "1023", "DJ_JD_DJ", 1, listOf("吸顶灯", "吊灯", "水晶灯"), "个", "灯具清洁服务"),
            CategoryInfo("除螨", "1013", "DJ_JY_CM", 1, listOf("床垫除螨", "沙发除螨", "地毯除螨", "全屋除螨"), "次", "专业除螨服务"),
            CategoryInfo("地板打蜡", "1014", "DJ_JY_DB", 2, listOf("实木地板", "复合地板"), "次", "地板清洁打蜡养护"),
            CategoryInfo("家具养护", "1015", "DJ_JY_JJ", 2, listOf("皮质家具", "实木家具", "布艺家具"), "次", "家具清洁养护"),
            CategoryInfo("沙发清洗", "1024", "DJ_JY_SF", 1, listOf("布艺沙发", "皮质沙发"), "套", "沙发深度清洁"),
            CategoryInfo("地毯清洗", "1025", "DJ_JY_DT", 1, listOf("化纤地毯", "羊毛地毯"), "平米", "地毯深度清洗"),
            CategoryInfo("窗帘清洗", "1026", "DJ_JY_CL", 1, listOf("普通窗帘", "纱帘", "遮光帘"), "副", "窗帘拆洗服务"),
            CategoryInfo("卫生间清洁", "1016", "DJ_ZX_WSJ", 2, listOf("2小时", "3小时"), "小时", "卫生间专项深度清洁"),
            CategoryInfo("厨房清洁", "1017", "DJ_ZX_CF", 2, listOf("2小时", "3小时"), "小时", "厨房专项深度清洁"),
            CategoryInfo("收纳整理", "1018", "DJ_ZX_SN", 3, listOf("3小时", "4小时", "5小时"), "小时", "全屋收纳整理服务"),
            CategoryInfo("装修后保洁", "1019", "DJ_ZX_ZX", 4, listOf("4小时", "6小时", "8小时"), "小时", "装修后全面清洁"),
            CategoryInfo("杀虫除蟑", "1021", "DJ_QT_SC", 1, listOf("全屋除蟑", "全屋杀虫"), "次", "专业杀虫除蟑服务"),
            CategoryInfo("管道疏通", "1022", "DJ_QT_GD", 1, listOf("厨房下水道", "卫生间下水道", "地漏"), "次", "管道疏通服务"),
            CategoryInfo("甲醛治理", "1027", "DJ_QT_JQ", 1, listOf("全屋治理", "单间治理"), "次", "甲醛检测与治理"),
            CategoryInfo("搬家保洁", "1028", "DJ_QT_BJ", 4, listOf("一居室", "两居室", "三居室"), "次", "搬家前后保洁服务"),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("品类订单") },
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
        ) {
            // 步骤指示器
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StepIndicator(1, "选服务", currentStep >= 0)
                    StepIndicator(2, "填信息", currentStep >= 1)
                }
            }

            if (currentStep == 0) {
                // Step 0: 选择服务类型
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        CategoryCard(
                            category = cat,
                            isSelected = selectedCategory?.name == cat.name,
                            onClick = {
                                selectedCategory = cat
                                selectedSpec = ""
                                selectedQuantity = 1
                            }
                        )
                    }
                }

                // 选中后显示规格+数量选择
                if (selectedCategory != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryContainer.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("已选：${selectedCategory!!.name}", fontWeight = FontWeight.Bold, color = Primary)
                            Spacer(modifier = Modifier.height(12.dp))

                            // 规格选择（FilterChip）
                            Text("选择规格：", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                flexWrap = androidx.compose.foundation.layout.FlexWrap.Wrap
                            ) {
                                selectedCategory!!.specs.forEach { spec ->
                                    FilterChip(
                                        selected = selectedSpec == spec,
                                        onClick = { selectedSpec = spec },
                                        label = { Text(spec) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 数量选择（+/- 按钮）
                            Text("选择数量：", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // 减号按钮
                                OutlinedIconButton(
                                    onClick = { if (selectedQuantity > 1) selectedQuantity-- },
                                    enabled = selectedQuantity > 1
                                ) {
                                    Icon(Icons.Default.Remove, "减少")
                                }
                                // 数量显示
                                Text(
                                    text = "$selectedQuantity ${selectedCategory!!.unit}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                                // 加号按钮
                                OutlinedIconButton(
                                    onClick = { if (selectedQuantity < 10) selectedQuantity++ },
                                    enabled = selectedQuantity < 10
                                ) {
                                    Icon(Icons.Default.Add, "增加")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { if (selectedSpec.isNotBlank()) currentStep = 1 },
                                enabled = selectedSpec.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("下一步")
                            }
                        }
                    }
                }
            } else {
                // Step 1: 填写下单信息
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 已选信息
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PrimaryContainer.copy(alpha = 0.2f))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("服务：${selectedCategory?.name ?: ""}", fontWeight = FontWeight.Bold)
                            Text("规格：$selectedSpec")
                            Text("数量：$selectedQuantity ${selectedCategory?.unit ?: ""}")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ========== 需要手动输入的字段 ==========

                    // 手机号（手动输入）
                    Text("客户手机号 *", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("请输入11位手机号") },
                        placeholder = { Text("13800138000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 地址（手动输入）
                    Text("服务地址 *", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("请输入详细服务地址") },
                        placeholder = { Text("XX小区X栋X单元X号") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ========== 选择器字段 ==========

                    // 服务日期（日期选择器）
                    Text("服务日期", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarToday, null, tint = Primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (serviceDate.isNotBlank()) serviceDate else "点击选择服务日期",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (serviceDate.isNotBlank()) TextPrimary else TextHint
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 服务时间（时间选择器）
                    Text("服务时间", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showTimePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccessTime, null, tint = Primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (serviceTime.isNotBlank()) serviceTime else "点击选择服务时间",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (serviceTime.isNotBlank()) TextPrimary else TextHint
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 指派模式（选择器）
                    Text("指派模式", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        FilterChip(
                            selected = assignMode == "auto",
                            onClick = { assignMode = "auto" },
                            label = { Text("自动分配") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = assignMode == "assign",
                            onClick = { assignMode = "assign" },
                            label = { Text("指定保洁师") }
                        )
                    }

                    // 指定保洁师时输入手机号
                    if (assignMode == "assign") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("保洁师手机号 *", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = sellerMobile,
                            onValueChange = { sellerMobile = it },
                            label = { Text("请输入保洁师手机号") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 需求详情（可选，手动输入）
                    Text("需求详情（可选）", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = detailText,
                        onValueChange = { detailText = it },
                        placeholder = { Text("补充说明，如特殊要求等") },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 错误提示
                    errorMessage?.let {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f))) {
                            Text(it, color = Error, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 下单按钮
                    Button(
                        onClick = {
                            if (mobile.isBlank() || mobile.length != 11) {
                                errorMessage = "请输入正确的11位手机号"
                                return@Button
                            }
                            if (address.isBlank()) {
                                errorMessage = "请输入服务地址"
                                return@Button
                            }
                            if (assignMode == "assign" && sellerMobile.isBlank()) {
                                errorMessage = "指定模式下需输入保洁师手机号"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("下单中...")
                        } else {
                            Text("确认下单", fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { currentStep = 0 }) {
                        Text("上一步")
                    }
                }
            }
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
                        serviceDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(java.util.Date(millis))
                    }
                    showDatePicker = false
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
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
                    serviceTime = "$hour:$minute"
                    showTimePicker = false
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            }
        )
    }

    // 下单逻辑
    if (isLoading) {
        LaunchedEffect(Unit) {
            val timeStr = if (serviceDate.isNotBlank() && serviceTime.isNotBlank()) "$serviceDate $serviceTime" else ""
            val specText = if (selectedQuantity > 1) "$selectedSpec $selectedQuantity${selectedCategory!!.unit}" else selectedSpec
            val request = CategoryOrderRequest(
                mobile = mobile,
                address = address,
                service_type = selectedCategory!!.name,
                spec_text = specText,
                service_time = timeStr,
                assign_mode = assignMode,
                seller_mobile = if (assignMode == "assign") sellerMobile else null,
                detail_text = if (detailText.isNotBlank()) detailText else null
            )
            when (val result = api.createCategoryOrder(request)) {
                is Result.Success -> {
                    successMessage = "下单成功！订单号：${result.data.order_no}"
                }
                is Result.Error -> {
                    errorMessage = result.message
                }
                is Result.Loading -> {}
            }
            isLoading = false
        }
    }

    // 成功提示
    successMessage?.let {
        AlertDialog(
            onDismissRequest = { successMessage = null },
            title = { Text("下单成功") },
            text = { Text(it) },
            confirmButton = {
                TextButton(onClick = {
                    successMessage = null
                    currentStep = 0
                    selectedCategory = null
                    selectedSpec = ""
                    selectedQuantity = 1
                    mobile = ""
                    address = ""
                    serviceDate = ""
                    serviceTime = ""
                    detailText = ""
                }) { Text("确定") }
            }
        )
    }
}

@Composable
private fun StepIndicator(step: Int, label: String, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = if (isActive) Primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(step.toString(), color = if (isActive) OnPrimary else TextSecondary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = if (isActive) Primary else TextSecondary)
    }
}

@Composable
private fun CategoryCard(category: CategoryInfo, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Primary) else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(category.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(category.description, fontSize = 11.sp, color = TextSecondary, maxLines = 2, textAlign = TextAlign.Center)
        }
    }
}
