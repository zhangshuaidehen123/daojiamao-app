package com.daojia.app.data.api

import com.daojia.app.DjApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ApiClient {

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        private val okHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val originalRequest = chain.request()
                    val prefs = DjApp.instance.prefsManager
                    val cookie = prefs.cookieContent
                    val newRequest = if (cookie.isNotBlank()) {
                        originalRequest.newBuilder()
                            .header("Cookie", cookie)
                            .build()
                    } else {
                        originalRequest
                    }
                    chain.proceed(newRequest)
                }
                .build()
        }

        val instance: ApiClient by lazy { ApiClient() }
    }

    private fun getBaseUrl(): String {
        return DjApp.instance.prefsManager.serverUrl.trimEnd('/')
    }

    private fun buildUrl(path: String): String {
        return "${getBaseUrl()}$path"
    }

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
                    Result.Error("Request failed: ${response.code}", response.code)
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}")
            }
        }
    }

    private suspend fun post(path: String, body: Any?): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = if (body != null) {
                    val jsonString = when (body) {
                        is Map<*, *> -> {
                            val entries = body.entries.map { (k, v) ->
                                val key = k.toString()
                                val value = when (v) {
                                    is String -> "\"${v}\""
                                    is Number -> v.toString()
                                    is Boolean -> v.toString()
                                    else -> "\"${v.toString()}\""
                                }
                                "\"${key}\":${value}"
                            }.joinToString(",")
                            "{${entries}}"
                        }
                        is String -> body
                        else -> body.toString()
                    }
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
                    Result.Error("Request failed: ${response.code}", response.code)
                }
            } catch (e: Exception) {
                Result.Error("Network error: ${e.message}")
            }
        }
    }

    // ==================== 版本更新 ====================
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
                    Result.Error("Parse error: ${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== Cookie验证 ====================
    suspend fun checkCookie(): Result<CookieStatus> {
        return when (val result = get("/api/cookie/check")) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<CookieStatus>>(result.data)
                    if (response.data != null) {
                        Result.Success(response.data)
                    } else {
                        Result.Error(response.message)
                    }
                } catch (e: Exception) {
                    Result.Error("Parse error: ${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 套餐查询 ====================
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
                    Result.Error("Parse error: ${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 地址查询 ====================
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
                    Result.Error("Parse error: ${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 保洁师搜索 ====================
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
                    Result.Error("Parse error: ${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

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
                    Result.Error("Parse error: ${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 品类订单 ====================
    suspend fun createCategoryOrder(request: CategoryOrderRequest): Result<CreateOrderResponse> {
        return when (val result = post("/api/orders/category", request)) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<CreateOrderResponse>>(result.data)
                    if (response.data != null) {
                        Result.Success(response.data)
                    } else {
                        Result.Error(response.message)
                    }
                } catch (e: Exception) {
                    Result.Error("Parse error: ${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 套餐订单 ====================
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
                    Result.Error("Parse error: ${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 订单查询 ====================
    suspend fun queryOrders(phone: String, page: Int = 1, size: Int = 20): Result<List<OrderInfo>> {
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
                    Result.Error("Parse error: ${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 订单详情（通过订单ID查手机号）====================
    suspend fun getMobileByOrder(orderId: String): Result<String> {
        return when (val result = get("/api/orders/mobile/$orderId")) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<Map<String, String>>>(result.data)
                    if (response.code == 0 && response.data != null) {
                        Result.Success(response.data["mobile"] ?: "")
                    } else {
                        Result.Error(response.message)
                    }
                } catch (e: Exception) {
                    Result.Error("Parse error: ${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

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
                    Result.Error("Parse error: ${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 周期查询（商家ID+手机号）====================
    suspend fun queryCycleBySellerAndMobile(sellerId: String, mobile: String, weekType: String = "1"): Result<CycleInfo> {
        return when (val result = get(
            "/api/cycles/query",
            mapOf("seller_id" to sellerId, "mobile" to mobile, "week_type" to weekType)
        )) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<CycleInfo>>(result.data)
                    if (response.data != null) {
                        Result.Success(response.data)
                    } else {
                        Result.Error(response.message)
                    }
                } catch (e: Exception) {
                    Result.Error("Parse error: ${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 现金结算 ====================
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
                    Result.Error("Parse error: ${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

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
                    Result.Error("Parse error: ${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 品类列表 ====================
    suspend fun getCategories(): Result<List<CategoryInfo>> {
        return when (val result = get("/api/categories")) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<List<CategoryInfo>>>(result.data)
                    if (response.data != null) {
                        Result.Success(response.data)
                    } else {
                        Result.Error(response.message)
                    }
                } catch (e: Exception) {
                    Result.Error("Parse error: ${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }
}
