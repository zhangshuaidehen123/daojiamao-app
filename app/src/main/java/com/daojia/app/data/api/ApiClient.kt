package com.daojia.app.data.api

import com.daojia.app.DjApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
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
                    val baseHost = prefs.serverUrl.trimEnd('/')
                    val builder = originalRequest.newBuilder()
                    if (cookie.isNotBlank()) {
                        builder.header("Cookie", cookie)
                    }
                    builder.header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"
                    )
                    builder.header("Accept", "application/json, text/plain, */*")
                    builder.header("Origin", baseHost)
                    builder.header("Referer", baseHost + "/order/manager.do")
                    chain.proceed(builder.build())
                }
                .build()
        }
        val instance: ApiClient by lazy { ApiClient() }
    }

    private fun getBaseUrl(): String {
        return DjApp.instance.prefsManager.serverUrl.trimEnd('/')
    }

    private fun buildUrl(path: String): String {
        return getBaseUrl() + path
    }

    private suspend fun get(path: String, params: Map<String, String> = emptyMap()): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val urlBuilder = StringBuilder(buildUrl(path))
                if (params.isNotEmpty()) {
                    urlBuilder.append("?")
                    params.entries.forEachIndexed { index, entry ->
                        if (index > 0) urlBuilder.append("&")
                        urlBuilder.append(URLEncoder.encode(entry.key, "UTF-8"))
                        urlBuilder.append("=")
                        urlBuilder.append(URLEncoder.encode(entry.value, "UTF-8"))
                    }
                }
                val request = Request.Builder().url(urlBuilder.toString()).get().build()
                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    Result.Success(body)
                } else {
                    Result.Error("Request failed: " + response.code, response.code)
                }
            } catch (e: Exception) {
                Result.Error("Network error: " + e.message)
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
                                    is String -> "\"" + v + "\""
                                    is Number -> v.toString()
                                    is Boolean -> v.toString()
                                    else -> "\"" + v.toString() + "\""
                                }
                                "\"" + key + "\":" + value
                            }.joinToString(",")
                            "{" + entries + "}"
                        }
                        is String -> body
                        else -> body.toString()
                    }
                    jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())
                } else {
                    "".toRequestBody("application/json; charset=utf-8".toMediaType())
                }
                val request = Request.Builder().url(buildUrl(path)).post(jsonBody).build()
                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    Result.Success(responseBody)
                } else {
                    Result.Error("Request failed: " + response.code, response.code)
                }
            } catch (e: Exception) {
                Result.Error("Network error: " + e.message)
            }
        }
    }

    suspend fun checkUpdate(): Result<UpdateInfo> {
        return when (val result = get("/api/update/check")) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<UpdateInfo>>(result.data)
                    if (response.code == 0 && response.data != null) Result.Success(response.data)
                    else Result.Error(response.message)
                } catch (e: Exception) {
                    Result.Error("Parse error: " + e.message)
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    /**
     * 直接调用道家真实接口 /order/list.do 校验 Cookie
     * 旧版 APK 等价逻辑: code==1 有效, code==-1 已过期, 其它视为有效
     */
    suspend fun checkCookie(): Result<CookieStatus> {
        val cookie = DjApp.instance.prefsManager.cookieContent
        if (cookie.isBlank()) return Result.Error("Cookie 未配置")
        return when (val result = get("/order/list.do", mapOf("pageNo" to "1", "pageSize" to "1"))) {
            is Result.Success -> {
                try {
                    val raw = result.data
                    val obj = json.parseToJsonElement(raw).jsonObject
                    val code = obj["code"]?.jsonPrimitive?.intOrNull
                    when {
                        code == 1 -> Result.Success(CookieStatus(valid = true, user_name = ""))
                        code == -1 -> Result.Error("Cookie 已过期")
                        raw.contains("登录") -> Result.Error("Cookie 已过期")
                        else -> Result.Success(CookieStatus(valid = true, user_name = ""))
                    }
                } catch (e: Exception) {
                    Result.Error("Cookie 已过期或网络异常")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    suspend fun queryPackages(phone: String): Result<List<PackageInfo>> {
        return when (val result = get("/api/packages", mapOf("phone" to phone))) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<PackageListResponse>>(result.data)
                    if (response.data != null) Result.Success(response.data.packages)
                    else Result.Error(response.message)
                } catch (e: Exception) { Result.Error("Parse error: " + e.message) }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    suspend fun queryAddresses(phone: String): Result<List<AddressInfo>> {
        return when (val result = get("/api/addresses", mapOf("phone" to phone))) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<AddressListResponse>>(result.data)
                    if (response.data != null) Result.Success(response.data.addresses)
                    else Result.Error(response.message)
                } catch (e: Exception) { Result.Error("Parse error: " + e.message) }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    suspend fun searchSellerByName(name: String): Result<List<WorkerInfo>> {
        return when (val result = get("/api/sellers/search-by-name", mapOf("name" to name))) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<List<WorkerInfo>>>(result.data)
                    if (response.code == 0 && response.data != null) Result.Success(response.data)
                    else Result.Error(response.message)
                } catch (e: Exception) { Result.Error("Parse error: " + e.message) }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    suspend fun queryWorkers(serviceTime: String = ""): Result<List<WorkerInfo>> {
        val params = if (serviceTime.isNotBlank()) mapOf("service_time" to serviceTime) else emptyMap()
        return when (val result = get("/api/workers", params)) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<WorkerListResponse>>(result.data)
                    if (response.data != null) Result.Success(response.data.workers)
                    else Result.Error(response.message)
                } catch (e: Exception) { Result.Error("Parse error: " + e.message) }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    suspend fun createCategoryOrder(request: CategoryOrderRequest): Result<CreateOrderResponse> {
        return when (val result = post("/api/orders/category", request)) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<CreateOrderResponse>>(result.data)
                    if (response.data != null) Result.Success(response.data)
                    else Result.Error(response.message)
                } catch (e: Exception) { Result.Error("Parse error: " + e.message) }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    suspend fun createOrder(request: CreateOrderRequest): Result<CreateOrderResponse> {
        return when (val result = post("/api/orders/create", request)) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<CreateOrderResponse>>(result.data)
                    if (response.data != null) Result.Success(response.data)
                    else Result.Error(response.message)
                } catch (e: Exception) { Result.Error("Parse error: " + e.message) }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    suspend fun queryOrders(phone: String, page: Int = 1, size: Int = 20): Result<List<OrderInfo>> {
        return when (val result = get("/api/orders", mapOf("phone" to phone, "page" to page.toString(), "size" to size.toString()))) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<OrderListResponse>>(result.data)
                    if (response.data != null) Result.Success(response.data.orders)
                    else Result.Error(response.message)
                } catch (e: Exception) { Result.Error("Parse error: " + e.message) }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    suspend fun getMobileByOrder(orderId: String): Result<String> {
        return when (val result = get("/api/orders/mobile/" + orderId)) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<Map<String, String>>>(result.data)
                    if (response.code == 0 && response.data != null) Result.Success(response.data["mobile"] ?: "")
                    else Result.Error(response.message)
                } catch (e: Exception) { Result.Error("Parse error: " + e.message) }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    suspend fun queryOrderDetail(orderId: String): Result<OrderInfo> {
        return when (val result = get("/api/orders/" + orderId)) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<OrderInfo>>(result.data)
                    if (response.data != null) Result.Success(response.data)
                    else Result.Error(response.message)
                } catch (e: Exception) { Result.Error("Parse error: " + e.message) }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    suspend fun queryCycleBySellerAndMobile(sellerId: String, mobile: String, weekType: String = "1"): Result<CycleInfo> {
        return when (val result = get("/api/cycles/query", mapOf("seller_id" to sellerId, "mobile" to mobile, "week_type" to weekType))) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<CycleInfo>>(result.data)
                    if (response.data != null) Result.Success(response.data)
                    else Result.Error(response.message)
                } catch (e: Exception) { Result.Error("Parse error: " + e.message) }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    suspend fun queryCashInfo(orderNo: String): Result<CashSettleInfo> {
        return when (val result = get("/api/cash/info", mapOf("order_no" to orderNo))) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<CashSettleInfo>>(result.data)
                    if (response.data != null) Result.Success(response.data)
                    else Result.Error(response.message)
                } catch (e: Exception) { Result.Error("Parse error: " + e.message) }
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
                    if (response.data != null) Result.Success(response.data)
                    else Result.Error(response.message)
                } catch (e: Exception) { Result.Error("Parse error: " + e.message) }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }
}
