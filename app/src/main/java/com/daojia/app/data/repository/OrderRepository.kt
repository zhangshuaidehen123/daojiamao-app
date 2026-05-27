package com.daojia.app.data.repository

import com.daojia.app.data.api.*

/**
 * 订单数据仓库
 *
 * 封装API调用，为ViewModel提供数据接口
 * 预留本地缓存扩展（Room）
 */
class OrderRepository {

    private val apiClient = ApiClient.instance

    // ==================== 套餐相关 ====================

    /**
     * 查询可用套餐
     */
    suspend fun queryPackages(phone: String): Result<List<PackageInfo>> {
        return apiClient.queryPackages(phone)
    }

    // ==================== 地址相关 ====================

    /**
     * 查询用户地址
     */
    suspend fun queryAddresses(phone: String): Result<List<AddressInfo>> {
        return apiClient.queryAddresses(phone)
    }

    // ==================== 保洁师相关 ====================

    /**
     * 查询可用保洁师
     */
    suspend fun queryWorkers(serviceTime: String = ""): Result<List<WorkerInfo>> {
        return apiClient.queryWorkers(serviceTime)
    }

    // ==================== 订单相关 ====================

    /**
     * 创建订单
     */
    suspend fun createOrder(request: CreateOrderRequest): Result<CreateOrderResponse> {
        return apiClient.createOrder(request)
    }

    /**
     * 查询订单列表
     */
    suspend fun queryOrders(phone: String, page: Int = 1, size: Int = 20): Result<List<OrderInfo>> {
        return apiClient.queryOrders(phone, page, size)
    }

    /**
     * 查询订单详情
     */
    suspend fun queryOrderDetail(orderId: String): Result<OrderInfo> {
        return apiClient.queryOrderDetail(orderId)
    }

    // ==================== 现金结算 ====================

    /**
     * 查询现金信息
     */
    suspend fun queryCashInfo(orderNo: String): Result<CashSettleInfo> {
        return apiClient.queryCashInfo(orderNo)
    }

    /**
     * 确认现金结算
     */
    suspend fun settleCash(orderNo: String): Result<CashSettleResponse> {
        return apiClient.settleCash(orderNo)
    }

    // ==================== Cookie ====================

    /**
     * 检查Cookie状态
     */
    suspend fun checkCookie(): Result<CookieStatus> {
        return apiClient.checkCookie()
    }

    // ==================== 统计（预留） ====================

    /**
     * 获取今日统计
     */
    suspend fun getTodayStats(): Result<Map<String, Any>> {
        return apiClient.getTodayStats()
    }
}
