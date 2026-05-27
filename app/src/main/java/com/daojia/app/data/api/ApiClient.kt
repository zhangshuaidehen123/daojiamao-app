package com.daojia.app.data.api

import com.daojia.app.DjApp
import com.daojia.app.data.local.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ApiClient {

    companion object {
        private const val TAG = "ApiClient"

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
        val url = DjApp.instance.prefsManager.serverUrl
        return url.trimEnd('/')
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

    /**
     * POST with JSON body - uses kotlinx.serialization for @Serializable classes,
     * and manual JSON encoding for Map types
     */
    private suspend fun post(path: String, body: Any?): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = if (body != null) {
                    val jsonString = when (body) {
                        is Map<*, *> -> {
                            // Manual JSON encoding for Map types
                            val entries = body.entries.map { (k, v) ->
                                """"${k}":${if (v is String) ""\"${v}\"" else v}"""
                            }.joinToString(",")
                            "{$entries}"
                        }
                        is String -> body
                        else -> {
                            // Use kotlinx.serialization for @Serializable classes
                            json.encodeToString(
                                kotlinx.serialization.serializer(body::class),
                                body
                            )
                        }
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

    suspend fun searchSellerByMobile(mobile: String): Result<WorkerInfo> {
        return when (val result = get("/api/sellers/search-by-name/$mobile")) {
            is Result.Success -> {
                try {
                    val response = json.decodeFromString<ApiResponse<Map<String, String>>>(result.data)
                    if (response.code == 0 && response.data != null) {
                        Result.Success(WorkerInfo(
                            worker_id = response.data["seller_id"] ?: "",
                            worker_name = "",
                            phone = mobile,
                        ))
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
                    Result.Error("Parse error: ${e.message}")
                }
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }
}
