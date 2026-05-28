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

/**
 * API客户端 - 通过本地 FastAPI 中间件调用到家平台
 *
 * 所有 API 调用走 PrefsManager.serverUrl 指向的 FastAPI 服务
 * （默认 http://192.168.1.100:5000，用户需在设置中改为电脑实际 IP）
 */
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
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        val instance: ApiClient by lazy { ApiClient() }
    }

    private fun getBaseUrl(): String =
        DjApp.instance.prefsManager.serverUrl.trimEnd('/')

    private fun buildUrl(path: String): String = "${getBaseUrl()}$path"

    // ==================== 通用请求方法 ====================

    private suspend fun get(path: String, params: Map<String, String> = emptyMap()): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val urlBuilder = StringBuilder(buildUrl(path))
                if (params.isNotEmpty()) {
                    urlBuilder.append("?")
                    params.entries.forEachIndexed { i, e ->
                        if (i > 0) urlBuilder.append("&")
                        urlBuilder.append("${URLEncoder.encode(e.key, "UTF-8")}=${URLEncoder.encode(e.value, "UTF-8")}")
                    }
                }
                val request = Request.Builder().url(urlBuilder.toString()).get().build()
                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) Result.Success(body)
                else Result.Error("请求失败：${response.code}", response.code)
            } catch (e: Exception) {
                Result.Error("网络异常：${e.message}")
            }
        }

    private suspend inline fun <reified T> postJson(path: String, body: T): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val jsonString = json.encodeToString(kotlinx.serialization.serializer<T>(), body)
                val jsonBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(buildUrl(path)).post(jsonBody).build()
                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) Result.Success(responseBody)
                else Result.Error("请求失败：${response.code}", response.code)
            } catch (e: Exception) {
                Result.Error("网络异常：${e.message}")
            }
        }

    private suspend fun postMap(path: String, body: Map<String, String>): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val jsonString = buildString {
                    append("{")
                    body.entries.forEachIndexed { i, e ->
                        if (i > 0) append(",")
                        append("\"${e.key}\":\"${e.value}\"")
                    }
                    append("}")
                }
                val jsonBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(buildUrl(path)).post(jsonBody).build()
                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) Result.Success(responseBody)
                else Result.Error("请求失败：${response.code}", response.code)
            } catch (e: Exception) {
                Result.Error("网络异常：${e.message}")
            }
        }

    // ==================== 版本更新 ====================

    suspend fun checkUpdate(): Result<UpdateInfo> = when (val r = get("/api/update/check")) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<UpdateInfo>>(r.data)
            if (resp.code == 0 && resp.data != null) Result.Success(resp.data)
            else Result.Error(resp.message)
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    // ==================== Cookie 验证（走 FastAPI 中间件） ====================

    /**
     * 验证 Cookie 有效性
     * 调用 FastAPI 的 /api/cookie/validate，由后端再去验证到家平台
     */
    suspend fun checkCookie(): Result<CookieStatus> = when (val r = get("/api/cookie/validate")) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<CookieStatus>>(r.data)
            if (resp.data != null) {
                DjApp.instance.prefsManager.isCookieValid = resp.data.valid
                Result.Success(resp.data)
            } else Result.Error(resp.message)
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    /**
     * 更新 Cookie 到 FastAPI 服务
     */
    suspend fun updateCookieOnServer(cookie: String): Result<Boolean> = when (val r = postMap(
        "/api/cookie/update", mapOf("cookie" to cookie)
    )) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<CookieStatus>>(r.data)
            Result.Success(resp.data?.valid ?: false)
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    // ==================== 套餐 ====================

    suspend fun queryPackages(phone: String): Result<List<PackageInfo>> = when (val r = get("/api/packages", mapOf("phone" to phone))) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<PackageListResponse>>(r.data)
            if (resp.data != null) Result.Success(resp.data.packages) else Result.Error(resp.message)
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    // ==================== 地址 ====================

    suspend fun queryAddresses(phone: String): Result<List<AddressInfo>> = when (val r = get("/api/addresses", mapOf("phone" to phone))) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<AddressListResponse>>(r.data)
            if (resp.data != null) Result.Success(resp.data.addresses) else Result.Error(resp.message)
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    // ==================== 保洁师 ====================

    suspend fun searchSellerByName(name: String): Result<List<WorkerInfo>> = when (val r = get("/api/sellers/search-by-name", mapOf("name" to name))) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<List<WorkerInfo>>>(r.data)
            if (resp.code == 0 && resp.data != null) Result.Success(resp.data) else Result.Error(resp.message)
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    suspend fun searchSellerByMobile(mobile: String): Result<WorkerInfo> = when (val r = get("/api/sellers/search-by-name/$mobile")) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<Map<String, String>>>(r.data)
            if (resp.code == 0 && resp.data != null) {
                Result.Success(WorkerInfo(
                    worker_id = resp.data["seller_id"] ?: "",
                    worker_name = "",
                    phone = mobile
                ))
            } else Result.Error(resp.message)
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    suspend fun queryWorkers(serviceTime: String = ""): Result<List<WorkerInfo>> {
        val params = if (serviceTime.isNotBlank()) mapOf("service_time" to serviceTime) else emptyMap()
        return when (val r = get("/api/workers", params)) {
            is Result.Success -> try {
                val resp = json.decodeFromString<ApiResponse<WorkerListResponse>>(r.data)
                if (resp.data != null) Result.Success(resp.data.workers) else Result.Error(resp.message)
            } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
            is Result.Error -> r
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 订单 ====================

    suspend fun createOrder(request: CreateOrderRequest): Result<CreateOrderResponse> = when (val r = postJson("/api/orders/create", request)) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<CreateOrderResponse>>(r.data)
            if (resp.data != null) Result.Success(resp.data) else Result.Error(resp.message)
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    /**
     * 创建品类订单 - 通过 FastAPI 自动查询优惠券、规格、价格
     */
    suspend fun createCategoryOrder(request: CategoryOrderRequest): Result<String> {
        // 把 categoryName 转成 FastAPI 期望的 service_type
        val body = mapOf(
            "mobile" to request.mobile,
            "address" to request.address,
            "service_type" to request.categoryName,
            "spec_name" to request.spec,
            "service_time" to request.serviceTime,
            "assign_mode" to (if (request.assignMode == "manual") "assign" else "auto"),
            "seller_mobile" to (request.sellerMobile ?: ""),
            "detail_text" to (request.remark ?: "")
        )
        return when (val r = postMap("/api/orders/category", body)) {
            is Result.Success -> try {
                val resp = json.decodeFromString<ApiResponse<Map<String, String>>>(r.data)
                if (resp.code == 0 && resp.data != null) {
                    Result.Success(resp.data["orderNo"] ?: resp.data["order_no"] ?: resp.data["orderId"] ?: "下单成功")
                } else Result.Error(resp.message)
            } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
            is Result.Error -> r
            is Result.Loading -> Result.Loading
        }
    }

    suspend fun queryOrders(phone: String, page: Int = 1, size: Int = 20): Result<List<OrderInfo>> = when (val r = get("/api/orders",
        mapOf("phone" to phone, "page" to page.toString(), "size" to size.toString())
    )) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<OrderListResponse>>(r.data)
            if (resp.data != null) Result.Success(resp.data.orders) else Result.Error(resp.message)
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    suspend fun queryOrderDetail(orderId: String): Result<OrderInfo> = when (val r = get("/api/orders/$orderId")) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<OrderInfo>>(r.data)
            if (resp.data != null) Result.Success(resp.data) else Result.Error(resp.message)
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    suspend fun getMobileByOrder(orderId: String): Result<String> = when (val r = get("/api/orders/$orderId/mobile")) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<Map<String, String>>>(r.data)
            if (resp.code == 0 && resp.data != null) {
                Result.Success(resp.data["mobile"] ?: resp.data["phone"] ?: "")
            } else Result.Error(resp.message)
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    suspend fun queryCycleBySellerAndMobile(sellerId: String, mobile: String, weekType: String = "1"): Result<CycleInfo> = when (val r = get("/api/cycle/query",
        mapOf("seller_id" to sellerId, "mobile" to mobile, "week_type" to weekType)
    )) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<CycleInfo>>(r.data)
            if (resp.code == 0 && resp.data != null) Result.Success(resp.data) else Result.Error(resp.message)
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    // ==================== 现金结算 ====================

    suspend fun queryCashInfo(orderNo: String): Result<CashSettleInfo> = when (val r = get("/api/cash/info", mapOf("order_no" to orderNo))) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<CashSettleInfo>>(r.data)
            if (resp.data != null) Result.Success(resp.data) else Result.Error(resp.message)
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    suspend fun settleCash(orderNo: String): Result<CashSettleResponse> = when (val r = postMap("/api/cash/settle", mapOf("order_no" to orderNo))) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<CashSettleResponse>>(r.data)
            if (resp.data != null) Result.Success(resp.data) else Result.Error(resp.message)
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }
}
