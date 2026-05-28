package com.daojia.app.data.api

import com.daojia.app.DjApp
import com.daojia.app.data.local.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * API客户端 - 基于OkHttp的网络请求封装
 *
 * 所有API调用均通过此类进行，返回统一的Result封装
 * 基础URL从PrefsManager动态读取，支持随时切换服务器
 */
class ApiClient {

    companion object {
        private const val TAG = "ApiClient"

        // JSON序列化配置
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        // OkHttp客户端（单例）
        private val okHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val originalRequest = chain.request()
                    val prefs = DjApp.instance.prefsManager

                    // 自动附加Cookie和浏览器请求头
                    val cookie = prefs.cookieContent
                    val requestBuilder = originalRequest.newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "application/json, text/plain, */*")
                        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")

                    if (cookie.isNotBlank()) {
                        requestBuilder.header("Cookie", cookie)
                    }

                    val newRequest = requestBuilder.build()
                    chain.proceed(newRequest)
                }
                .build()
        }

        // 单例实例
        val instance: ApiClient by lazy { ApiClient() }
    }

    /**
     * 获取基础URL
     */
    private fun getBaseUrl(): String {
        val url = DjApp.instance.prefsManager.serverUrl
        return url.trimEnd('/')
    }

    /**
     * 构建完整URL
     */
    private fun buildUrl(path: String): String {
        return "${getBaseUrl()}$path"
    }

    /**
     * 执行GET请求
     */
    private suspend fun get(path: String, params: Map<String, String> = emptyMap()): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val urlBuilder = StringBuilder(buildUrl(path))
                if (params.isNotEmpty()) {
                    urlBuilder.append("?")
                    params.entries.forEachIndexed { index, entry ->
                        if (index > 0) urlBuilder.append("&")
                        urlBuilder.append("${URLEncoder.encode(entry.key, "UTF-8")}=${URLEncoder.encode(entry.value, "UTF-8")}")
                    }
                }

                val request = Request.Builder()
                    .url(urlBuilder.toString())
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    Result.Success(body)
                } else {
                    Result.Error("请求失败：${response.code}", response.code)
                }
            } catch (e: Exception) {
                Result.Error("网络异常：${e.message}")
            }
        }
    }

    /**
     * 执行POST请求（JSON body）
     */
    private suspend fun post(path: String, body: Any?): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = if (body != null) {
                    val jsonString = json.encodeToString(
                        kotlinx.serialization.serializer<Any>(),
                        body
                    )
                    jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())
                } else {
                    "".toRequestBody("application/json; charset=utf-8".toMediaType())
                }

                val request = Request.Builder()
                    .url(buildUrl(path))
                    .post(jsonBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    Result.Success(responseBody)
                } else {
                    Result.Error("请求失败：${response.code}", response.code)
                }
            } catch (e: Exception) {
                Result.Error("网络异常：${e.message}")
            }
        }
    }

    // ==================== 版本更新API ====================

    /**
     * 检查版本更新
     * GET /api/update/check
     */
    suspend fun checkUpdate(): Result<UpdateInfo> {
        return when (val result = get("/api/update/check")) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<UpdateInfo>>(result.data)
                    if (response.code == 0 && response.data != null) {
                        Result.Success(response.data)
                    } else {
                        Result.Error(response.message)
                    }
                } catch (e: Exception) {
                    Result.Error("数据解析失败：${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== Cookie验证API ====================

    /**
     * 验证Cookie有效性 - 直接调用到家平台API
     * GET /order/list.do
     */
    suspend fun checkCookie(): Result<CookieStatus> {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = DjApp.instance.prefsManager
                val cookie = prefs.cookieContent

                if (cookie.isBlank()) {
                    Result.Success(CookieStatus(valid = false, message = "Cookie为空"))
                } else {
                    // 直接调用到家平台的订单列表接口验证Cookie
                    val request = Request.Builder()
                        .url("${getBaseUrl()}/order/list.do")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                        .header("Cookie", cookie)
                        .get()
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    val body = response.body?.string() ?: ""

                    // 验证逻辑：检查响应中是否包含登录相关关键词
                    val isValid = when {
                        // HTTP 302重定向到登录页
                        response.code == 302 -> false
                        // 响应内容包含登录关键词
                        body.contains("login", ignoreCase = true) ||
                        body.contains("登录", ignoreCase = true) ||
                        body.contains("请先登录", ignoreCase = true) ||
                        body.contains("未登录", ignoreCase = true) -> false
                        // 成功获取到订单页面
                        response.isSuccessful && body.isNotEmpty() -> true
                        // 其他情况视为无效
                        else -> false
                    }

                    // 更新本地Cookie有效状态
                    prefs.isCookieValid = isValid

                    Result.Success(
                        CookieStatus(
                            valid = isValid,
                            message = if (isValid) "Cookie有效" else "Cookie已过期或无效"
                        )
                    )
                }
            } catch (e: Exception) {
                Result.Error("Cookie验证失败：${e.message}")
            }
        }
    }

    // ==================== 套餐API ====================

    /**
     * 根据手机号查询可用套餐
     * GET /api/packages?phone=xxx
     */
    suspend fun queryPackages(phone: String): Result<List<PackageInfo>> {
        return when (val result = get("/api/packages", mapOf("phone" to phone))) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<PackageListResponse>>(result.data)
                    if (response.data != null) {
                        Result.Success(response.data.packages)
                    } else {
                        Result.Error(response.message)
                    }
                } catch (e: Exception) {
                    Result.Error("数据解析失败：${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 地址API ====================

    /**
     * 查询用户地址列表
     * GET /api/addresses?phone=xxx
     */
    suspend fun queryAddresses(phone: String): Result<List<AddressInfo>> {
        return when (val result = get("/api/addresses", mapOf("phone" to phone))) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<AddressListResponse>>(result.data)
                    if (response.data != null) {
                        Result.Success(response.data.addresses)
                    } else {
                        Result.Error(response.message)
                    }
                } catch (e: Exception) {
                    Result.Error("数据解析失败：${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 保洁师API ====================

    /**
     * 通过保洁师姓名搜索
     * GET /api/sellers/search-by-name?name=何章
     */
    suspend fun searchSellerByName(name: String): Result<List<WorkerInfo>> {
        return when (val result = get("/api/sellers/search-by-name", mapOf("name" to name))) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<List<WorkerInfo>>>(result.data)
                    if (response.code == 0 && response.data != null) {
                        Result.Success(response.data)
                    } else {
                        Result.Error(response.message)
                    }
                } catch (e: Exception) {
                    Result.Error("数据解析失败：${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    /**
     * 通过保洁师手机号查询ID
     * GET /api/sellers/search-by-name/{mobile}
     */
    suspend fun searchSellerByMobile(mobile: String): Result<WorkerInfo> {
        return when (val result = get("/api/sellers/search-by-name/$mobile")) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<Map<String, String>>>(result.data)
                    if (response.code == 0 && response.data != null) {
                        Result.Success(WorkerInfo(
                            sellerId = response.data["seller_id"] ?: "",
                            sellerName = "",
                            mobile = mobile,
                            status = "",
                        ))
                    } else {
                        Result.Error(response.message)
                    }
                } catch (e: Exception) {
                    Result.Error("数据解析失败：${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    /**
     * 查询可用保洁师列表
     * GET /api/workers?service_time=xxx
     */
    suspend fun queryWorkers(serviceTime: String = ""): Result<List<WorkerInfo>> {
        val params = if (serviceTime.isNotBlank()) {
            mapOf("service_time" to serviceTime)
        } else {
            emptyMap()
        }
        return when (val result = get("/api/workers", params)) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<WorkerListResponse>>(result.data)
                    if (response.data != null) {
                        Result.Success(response.data.workers)
                    } else {
                        Result.Error(response.message)
                    }
                } catch (e: Exception) {
                    Result.Error("数据解析失败：${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 订单API ====================

    /**
     * 创建订单（手动下单）
     * POST /api/orders/create
     */
    suspend fun createOrder(request: CreateOrderRequest): Result<CreateOrderResponse> {
        return when (val result = post("/api/orders/create", request)) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<CreateOrderResponse>>(result.data)
                    if (response.data != null) {
                        Result.Success(response.data)
                    } else {
                        Result.Error(response.message)
                    }
                } catch (e: Exception) {
                    Result.Error("数据解析失败：${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    /**
     * 查询订单列表
     * GET /api/orders?phone=xxx&page=1&size=20
     */
    suspend fun queryOrders(
        phone: String,
        page: Int = 1,
        size: Int = 20
    ): Result<List<OrderInfo>> {
        return when (val result = get(
            "/api/orders",
            mapOf("phone" to phone, "page" to page.toString(), "size" to size.toString())
        )) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<OrderListResponse>>(result.data)
                    if (response.data != null) {
                        Result.Success(response.data.orders)
                    } else {
                        Result.Error(response.message)
                    }
                } catch (e: Exception) {
                    Result.Error("数据解析失败：${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    /**
     * 查询订单详情
     * GET /api/orders/{order_id}
     */
    suspend fun queryOrderDetail(orderId: String): Result<OrderInfo> {
        return when (val result = get("/api/orders/$orderId")) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<OrderInfo>>(result.data)
                    if (response.data != null) {
                        Result.Success(response.data)
                    } else {
                        Result.Error(response.message)
                    }
                } catch (e: Exception) {
                    Result.Error("数据解析失败：${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 现金结算API ====================

    /**
     * 查询订单现金信息
     * GET /api/cash/info?order_no=xxx
     */
    suspend fun queryCashInfo(orderNo: String): Result<CashSettleInfo> {
        return when (val result = get("/api/cash/info", mapOf("order_no" to orderNo))) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<CashSettleInfo>>(result.data)
                    if (response.data != null) {
                        Result.Success(response.data)
                    } else {
                        Result.Error(response.message)
                    }
                } catch (e: Exception) {
                    Result.Error("数据解析失败：${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    /**
     * 确认现金结算
     * POST /api/cash/settle
     */
    suspend fun settleCash(orderNo: String): Result<CashSettleResponse> {
        val body = mapOf("order_no" to orderNo)
        return when (val result = post("/api/cash/settle", body)) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<CashSettleResponse>>(result.data)
                    if (response.data != null) {
                        Result.Success(response.data)
                    } else {
                        Result.Error(response.message)
                    }
                } catch (e: Exception) {
                    Result.Error("数据解析失败：${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 统计API（预留） ====================

    /**
     * 获取今日统计信息
     * GET /api/stats/today
     */
    suspend fun getTodayStats(): Result<Map<String, Any>> {
        return when (val result = get("/api/stats/today")) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<Map<String, Any>>>(result.data)
                    if (response.data != null) {
                        Result.Success(response.data)
                    } else {
                        Result.Error(response.message)
                    }
                } catch (e: Exception) {
                    Result.Error("数据解析失败：${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }
}
