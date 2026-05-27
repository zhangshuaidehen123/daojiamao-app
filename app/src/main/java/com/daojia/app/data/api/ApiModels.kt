package com.daojia.app.data.api
import kotlinx.serialization.Serializable
/**
 * API数据模型
 */
// ==================== 通用响应包装 ====================
@Serializable
data class ApiResponse<T>(
    val code: Int,
    val data: T? = null,
    val message: String = ""
)
// ==================== 结果封装 ====================
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int = -1) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
// ==================== 套餐信息 ====================
@Serializable
data class PackageInfo(
    val package_id: String,
    val package_name: String,
    val remaining_count: Int,
    val total_count: Int,
    val expire_date: String,
    val description: String = "",
    val duration: Int = 0,
    val price: Double = 0.0
)
@Serializable
data class PackageListResponse(
    val packages: List<PackageInfo>
)
// ==================== 地址信息 ====================
@Serializable
data class AddressInfo(
    val address_id: String,
    val address: String,
    val detail: String = "",
    val contact_name: String = "",
    val contact_phone: String = "",
    val is_default: Boolean = false
)
@Serializable
data class AddressListResponse(
    val addresses: List<AddressInfo>
)
// ==================== 保洁师信息 ====================
@Serializable
data class WorkerInfo(
    val worker_id: String,
    val worker_name: String,
    val phone: String,
    val rating: Double = 0.0,
    val order_count: Int = 0,
    val available: Boolean = true
)
@Serializable
data class WorkerListResponse(
    val workers: List<WorkerInfo>
)
// ==================== 订单信息 ====================
@Serializable
data class OrderInfo(
    val order_no: String,
    val phone: String,
    val package_name: String,
    val address: String,
    val worker_name: String,
    val service_time: String,
    val status: Int,
    val status_text: String,
    val amount: Double,
    val cash_amount: Double,
    val create_time: String,
    val order_type: Int
)
@Serializable
data class OrderListResponse(
    val orders: List<OrderInfo>
)
// ==================== 下单请求/响应 ====================
@Serializable
data class CreateOrderRequest(
    val phone: String,
    val package_id: String,
    val address_id: String,
    val address: String,
    val assign_type: Int,
    val worker_id: String,
    val service_time: String,
    val order_type: Int,
    val cookie: String
)
@Serializable
data class CreateOrderResponse(
    val order_id: String,
    val order_no: String,
    val status: String
)
// ==================== 品类订单 ====================
// 品类订单请求 - 字段名与CategoryOrderScreen.kt中使用的名称一致
@Serializable
data class CategoryOrderRequest(
    val categoryId: String,      // 服务ID
    val categoryName: String,    // 服务名称
    val spec: String,            // 规格
    val quantity: Int,          // 数量
    val mobile: String,          // 手机号
    val address: String,         // 地址
    val serviceDate: String,     // 服务日期
    val serviceTime: String,     // 服务时间
    val assignMode: String = "auto",    // 分配方式
    val sellerMobile: String? = null,   // 保洁师手机号
    val remark: String? = null          // 备注
)
// 品类信息 - 字段名与CategoryOrderScreen.kt中使用的名称一致
data class CategoryInfo(
    val name: String,           // 服务名称
    val id: String,              // 服务ID (对应service_id)
    val spuCode: String,        // SPU编码 (对应spu_code)
    val defaultDuration: Int,   // 默认时长 (对应default_duration)
    val specs: List<String>,    // 规格列表
    val unit: String,           // 单位
    val desc: String            // 描述 (对应description)
)
// ==================== 套餐订单 ====================
@Serializable
data class ComboSingleRequest(
    val combo_id: String,
    val seller_id: String,
    val service_info_id: String,
    val service_time: String
)
@Serializable
data class ComboCycleRequest(
    val combo_id: String,
    val seller_id: String,
    val service_info_id: String,
    val week_type: String,
    val server_time_cycles: String,
    val begin_server_time: String
)
// ==================== 现金结算 ====================
@Serializable
data class CashSettleInfo(
    val order_no: String,
    val cash_amount: Double,
    val status: Int,
    val status_text: String
)
@Serializable
data class CashSettleResponse(
    val success: Boolean,
    val message: String
)
// ==================== Cookie状态 ====================
@Serializable
data class CookieStatus(
    val valid: Boolean,
    val user_name: String = ""
)
// ==================== 更新信息 ====================
@Serializable
data class UpdateInfo(
    val version_code: Int,
    val version_name: String,
    val update_content: String,
    val apk_url: String,
    val force_update: Boolean
)
// ==================== 周期查询 ====================
@Serializable
data class CycleQueryRequest(
    val seller_id: String,
    val mobile: String,
    val week_type: String = "1"
)
@Serializable
data class CycleInfo(
    val available_weeks: List<String>,
    val first_service_times: List<String>
)
// ==================== 订单详情 ====================
@Serializable
data class OrderDetailResponse(
    val order_no: String,
    val mobile: String,
    val address: String,
    val service_type: String,
    val service_time: String,
    val worker_name: String,
    val worker_mobile: String,
    val status: String,
    val amount: Double
)
