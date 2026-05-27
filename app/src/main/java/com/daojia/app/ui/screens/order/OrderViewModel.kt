package com.daojia.app.ui.screens.order

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daojia.app.DjApp
import com.daojia.app.data.api.*
import com.daojia.app.data.repository.OrderRepository
import kotlinx.coroutines.launch

/**
 * 下单页状态
 */
data class OrderUiState(
    // 当前步骤（0-3）
    val currentStep: Int = 0,

    // 订单类型：0-单次单 1-周期单
    val orderType: Int = 0,

    // Step1：手机号和套餐
    val phone: String = "",
    val packages: List<PackageInfo> = emptyList(),
    val selectedPackage: PackageInfo? = null,
    val isLoadingPackages: Boolean = false,

    // Step2：地址和分配方式
    val addresses: List<AddressInfo> = emptyList(),
    val selectedAddress: AddressInfo? = null,
    val customAddress: String = "",
    val assignType: Int = 0, // 0-随机 1-指定

    // Step3：时间和保洁师
    val serviceDate: String = "",       // 日期 yyyy-MM-dd
    val serviceTime: String = "",       // 时间 HH:mm
    val workers: List<WorkerInfo> = emptyList(),
    val selectedWorker: WorkerInfo? = null,
    val isLoadingWorkers: Boolean = false,

    // 通用状态
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val orderResult: CreateOrderResponse? = null
)

/**
 * 下单页ViewModel
 *
 * 管理下单流程的状态和业务逻辑
 */
class OrderViewModel : ViewModel() {

    private val repository = OrderRepository()
    private val prefsManager = DjApp.instance.prefsManager

    var uiState by mutableStateOf(OrderUiState())
        private set

    // ==================== 步骤控制 ====================

    /**
     * 下一步
     */
    fun nextStep() {
        if (uiState.currentStep < 3) {
            uiState = uiState.copy(currentStep = uiState.currentStep + 1)
        }
    }

    /**
     * 上一步
     */
    fun previousStep() {
        if (uiState.currentStep > 0) {
            uiState = uiState.copy(currentStep = uiState.currentStep - 1)
        }
    }

    // ==================== 订单类型 ====================

    /**
     * 切换订单类型
     */
    fun setOrderType(type: Int) {
        uiState = uiState.copy(orderType = type)
    }

    // ==================== Step1：手机号和套餐 ====================

    /**
     * 更新手机号
     */
    fun updatePhone(phone: String) {
        uiState = uiState.copy(phone = phone)
    }

    /**
     * 查询套餐
     */
    fun queryPackages() {
        val phone = uiState.phone.trim()
        if (phone.isBlank() || phone.length != 11) {
            uiState = uiState.copy(errorMessage = "请输入正确的11位手机号")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isLoadingPackages = true, errorMessage = null)
            when (val result = repository.queryPackages(phone)) {
                is Result.Success -> {
                    uiState = uiState.copy(
                        isLoadingPackages = false,
                        packages = result.data
                    )
                }
                is Result.Error -> {
                    uiState = uiState.copy(
                        isLoadingPackages = false,
                        errorMessage = result.message
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * 选择套餐
     */
    fun selectPackage(pkg: PackageInfo) {
        uiState = uiState.copy(selectedPackage = pkg)
    }

    // ==================== Step2：地址和分配方式 ====================

    /**
     * 查询地址
     */
    fun queryAddresses() {
        val phone = uiState.phone.trim()
        if (phone.isBlank()) return

        viewModelScope.launch {
            when (val result = repository.queryAddresses(phone)) {
                is Result.Success -> {
                    uiState = uiState.copy(addresses = result.data)
                }
                is Result.Error -> {
                    // 地址查询失败不阻塞流程，允许手动输入
                    uiState = uiState.copy(addresses = emptyList())
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * 选择地址
     */
    fun selectAddress(address: AddressInfo) {
        uiState = uiState.copy(selectedAddress = address)
    }

    /**
     * 更新自定义地址
     */
    fun updateCustomAddress(address: String) {
        uiState = uiState.copy(customAddress = address)
    }

    /**
     * 设置分配方式
     */
    fun setAssignType(type: Int) {
        uiState = uiState.copy(assignType = type)
    }

    // ==================== Step3：时间和保洁师 ====================

    /**
     * 设置服务日期
     */
    fun setServiceDate(date: String) {
        uiState = uiState.copy(serviceDate = date)
    }

    /**
     * 设置服务时间
     */
    fun setServiceTime(time: String) {
        uiState = uiState.copy(serviceTime = time)
    }

    /**
     * 查询保洁师
     */
    fun queryWorkers() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingWorkers = true)
            val timeStr = "${uiState.serviceDate} ${uiState.serviceTime}"
            when (val result = repository.queryWorkers(timeStr)) {
                is Result.Success -> {
                    uiState = uiState.copy(
                        isLoadingWorkers = false,
                        workers = result.data
                    )
                }
                is Result.Error -> {
                    uiState = uiState.copy(
                        isLoadingWorkers = false,
                        errorMessage = result.message
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * 选择保洁师
     */
    fun selectWorker(worker: WorkerInfo) {
        uiState = uiState.copy(selectedWorker = worker)
    }

    // ==================== 确认下单 ====================

    /**
     * 提交订单
     */
    fun submitOrder() {
        // 参数校验
        val state = uiState
        if (state.phone.isBlank()) {
            uiState = uiState.copy(errorMessage = "请输入手机号")
            return
        }
        if (state.selectedPackage == null) {
            uiState = uiState.copy(errorMessage = "请选择套餐")
            return
        }
        val address = state.selectedAddress?.address ?: state.customAddress
        if (address.isBlank()) {
            uiState = uiState.copy(errorMessage = "请选择或输入服务地址")
            return
        }
        if (state.serviceDate.isBlank() || state.serviceTime.isBlank()) {
            uiState = uiState.copy(errorMessage = "请选择服务时间")
            return
        }
        if (state.assignType == 1 && state.selectedWorker == null) {
            uiState = uiState.copy(errorMessage = "请选择保洁师")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)

            val request = CreateOrderRequest(
                phone = state.phone,
                package_id = state.selectedPackage!!.package_id,
                address_id = state.selectedAddress?.address_id ?: "",
                address = address,
                assign_type = state.assignType,
                worker_id = state.selectedWorker?.worker_id ?: "",
                service_time = "${state.serviceDate} ${state.serviceTime}",
                order_type = state.orderType,
                cookie = prefsManager.cookieContent
            )

            when (val result = repository.createOrder(request)) {
                is Result.Success -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        successMessage = "下单成功！订单号：${result.data.order_no}",
                        orderResult = result.data
                    )
                }
                is Result.Error -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    // ==================== 通用 ====================

    /**
     * 清除错误信息
     */
    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }

    /**
     * 清除成功信息
     */
    fun clearSuccess() {
        uiState = uiState.copy(successMessage = null, orderResult = null)
    }

    /**
     * 重置表单
     */
    fun resetForm() {
        uiState = OrderUiState()
    }
}
