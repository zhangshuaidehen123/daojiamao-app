package com.daojia.app.data.api

import kotlinx.serialization.Serializable

// ==================== 通用响应 ====================

/**
 * 统一API响应封装
 */
@Serializable
data class ApiResponse<T>(
    val code: Int = 0,
    val message: String = "",
    val data: T? = null
)

/**
 * 统一Result封装（用于内部传递）
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int = -1) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// ==================== 版本更新 ====================

@Serializable
data class UpdateInfo(
    val version_code: Int = 0,
    val version_name: String = "",
    val update_content: String = "",
    val apk_url: String = "",
    val force_update: Boolean = false
)

// ==================== 套餐相关 ====================

@Serializable
data class PackageInfo(
    val package_id: String = "",
    val package_name: String = "",
    val price: Double = 0.0,
    val duration: Int = 0,        // 服务时长（分钟）
    val description: String = ""
)

@Serializable
data class PackageListResponse(
    val packages: List<PackageInfo> = emptyList()
)

// ==================== 地址相关 ====================

@Serializable
data class AddressInfo(
    val address_id: String = "",
    val address: String = "",
    val city: String = "",
    val district: String = "",
    val detail: String = "",
    val contact_name: String = "",
    val contact_phone: String = "",
    val is_default: Boolean = false
)

@Serializable
data class AddressListResponse(
    val addresses: List<AddressInfo> = emptyList()
)

// ==================== 保洁师相关 ====================

@Serializable
data class WorkerInfo(
    val worker_id: String = "",
    val worker_name: String = "",
    val phone: String = "",
    val rating: Double = 0.0,
    val order_count: Int = 0,
    val available: Boolean = false
)

@Serializable
data class WorkerListResponse(
    val workers: List<WorkerInfo> = emptyList()
)

// ==================== 订单相关 ====================

@Serializable
data class OrderInfo(
    val order_id: String = "",
    val order_no: String = "",
    val phone: String = "",
    val package_name: String = "",
    val address: String = "",
    val worker_name: String = "",
    val service_time: String = "",
    val status: Int = 0,           // 0-待分配 1-已分配 2-进行中 3-已完成 4-已取消
    val status_text: String = "",
    val amount: Double = 0.0,
    val cash_amount: Double = 0.0,
    val create_time: String = "",
    val order_type: Int = 0         // 0-单次单 1-周期单
)

@Serializable
data class OrderListResponse(
    val orders: List<OrderInfo> = emptyList(),
    val total: Int = 0
)

@Serializable
data class CreateOrderRequest(
    val phone: String = "",
    val package_id: String = "",
    val address_id: String = "",
    val address: String = "",
    val assign_type: Int = 0,       // 0-随机 1-指定
    val worker_id: String = "",
    val service_time: String = "",
    val order_type: Int = 0,        // 0-单次单 1-周期单
    val cycle_days: List<Int> = emptyList(), // 周期单的星期几
    val cookie: String = ""
)

@Serializable
data class CreateOrderResponse(
    val order_id: String = "",
    val order_no: String = ""
)

// ==================== 现金结算 ====================

@Serializable
data class CashSettleInfo(
    val order_id: String = "",
    val order_no: String = "",
    val cash_amount: Double = 0.0,
    val status: Int = 0,
    val status_text: String = ""
)

@Serializable
data class CashSettleResponse(
    val success: Boolean = false,
    val message: String = ""
)

// ==================== Cookie验证 ====================

@Serializable
data class CookieStatus(
    val valid: Boolean = false,
    val message: String = ""
)
