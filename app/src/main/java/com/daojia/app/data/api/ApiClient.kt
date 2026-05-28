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
 * 所有 API 路径严格匹配 daojiamao_api 后端的实际路由
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

    private suspend fun postRaw(path: String, jsonString: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
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

    /**
     * 把 Map 转成 JSON 字符串（支持基本类型 + null）
     */
    private fun mapToJson(map: Map<String, Any?>): String = buildString {
        append("{")
        var first = true
        for ((k, v) in map) {
            if (v == null) continue
            if (!first) append(",")
            first = false
            append("\"$k\":")
            when (v) {
                is Number, is Boolean -> append(v.toString())
                else -> append("\"${v.toString().replace("\\", "\\\\").replace("\"", "\\\"")}\"")
            }
        }
        append("}")
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

    /** GET /api/cookie/validate */
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

    /** POST /api/cookie/update?cookie=xxx （注意 FastAPI 用的是 query 参数）*/
    suspend fun updateCookieOnServer(cookie: String): Result<Boolean> {
        val encoded = URLEncoder.encode(cookie, "UTF-8")
        return when (val r = postRaw("/api/cookie/update?cookie=$encoded", "")) {
            is Result.Success -> try {
                val resp = json.decodeFromString<ApiResponse<CookieStatus>>(r.data)
                Result.Success(resp.data?.valid ?: false)
            } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
            is Result.Error -> r
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 套餐（套餐 = Combo）====================

    /**
     * 查询用户套餐列表
     * GET /api/combos/{mobile}
     */
    suspend fun queryPackages(phone: String): Result<List<PackageInfo>> = when (val r = get("/api/combos/$phone")) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<kotlinx.serialization.json.JsonElement>>(r.data)
            if (resp.code == 0 && resp.data != null) {
                // 后端返回结构灵活：可能是 {list:[]} / {combos:[]} / 直接数组
                val items = extractComboList(resp.data)
                Result.Success(items)
            } else Result.Error(resp.message.ifBlank { "查询失败" })
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    private fun extractComboList(data: kotlinx.serialization.json.JsonElement): List<PackageInfo> {
        val arr = when {
            data is kotlinx.serialization.json.JsonArray -> data
            data is kotlinx.serialization.json.JsonObject -> {
                data["list"] as? kotlinx.serialization.json.JsonArray
                    ?: data["combos"] as? kotlinx.serialization.json.JsonArray
                    ?: data["orderList"] as? kotlinx.serialization.json.JsonArray
                    ?: return emptyList()
            }
            else -> return emptyList()
        }
        return arr.mapNotNull { item ->
            try {
                val obj = item as kotlinx.serialization.json.JsonObject
                fun str(vararg keys: String): String {
                    for (k in keys) {
                        val v = obj[k]
                        if (v != null && v !is kotlinx.serialization.json.JsonNull) {
                            return v.toString().trim('"')
                        }
                    }
                    return ""
                }
                fun num(vararg keys: String): Int = str(*keys).toIntOrNull() ?: 0
                fun dbl(vararg keys: String): Double = str(*keys).toDoubleOrNull() ?: 0.0
                PackageInfo(
                    package_id = str("comboId", "id", "combo_id"),
                    package_name = str("comboName", "name", "combo_name", "serviceName"),
                    remaining_count = num("remainCount", "remaining_count", "remainingCount", "remain"),
                    total_count = num("totalCount", "total_count", "totalNum"),
                    expire_date = str("expireDate", "expire_date", "expireTime"),
                    description = str("description", "desc", "remark"),
                    duration = num("duration", "serviceHour"),
                    price = dbl("price", "amount")
                )
            } catch (e: Exception) { null }
        }
    }

    // ==================== 地址（套餐对应的服务地址）====================

    /**
     * 查询套餐的服务地址（需要先有套餐ID）
     * GET /api/combos/{combo_id}/addresses
     */
    suspend fun queryAddresses(comboId: String): Result<List<AddressInfo>> = when (val r = get("/api/combos/$comboId/addresses")) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<kotlinx.serialization.json.JsonElement>>(r.data)
            if (resp.code == 0 && resp.data != null) {
                Result.Success(extractAddressList(resp.data))
            } else Result.Error(resp.message.ifBlank { "查询失败" })
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    private fun extractAddressList(data: kotlinx.serialization.json.JsonElement): List<AddressInfo> {
        val arr = when {
            data is kotlinx.serialization.json.JsonArray -> data
            data is kotlinx.serialization.json.JsonObject -> {
                data["list"] as? kotlinx.serialization.json.JsonArray
                    ?: data["addresses"] as? kotlinx.serialization.json.JsonArray
                    ?: data["serviceInfoList"] as? kotlinx.serialization.json.JsonArray
                    ?: return emptyList()
            }
            else -> return emptyList()
        }
        return arr.mapNotNull { item ->
            try {
                val obj = item as kotlinx.serialization.json.JsonObject
                fun str(vararg keys: String): String {
                    for (k in keys) {
                        val v = obj[k]
                        if (v != null && v !is kotlinx.serialization.json.JsonNull) return v.toString().trim('"')
                    }
                    return ""
                }
                AddressInfo(
                    address_id = str("serviceInfoId", "id", "address_id"),
                    address = str("address", "fullAddress"),
                    detail = str("detail", "houseNo", "doorplate"),
                    contact_name = str("contactName", "linkman"),
                    contact_phone = str("contactPhone", "mobile", "phone"),
                    is_default = str("isDefault", "is_default") in listOf("true", "1")
                )
            } catch (e: Exception) { null }
        }
    }

    // ==================== 保洁师 ====================

    /** GET /api/sellers/search-by-name?name=xxx */
    suspend fun searchSellerByName(name: String): Result<List<WorkerInfo>> = when (val r = get(
        "/api/sellers/search-by-name", mapOf("name" to name)
    )) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<kotlinx.serialization.json.JsonElement>>(r.data)
            if (resp.code == 0 && resp.data != null) {
                Result.Success(extractWorkerList(resp.data))
            } else Result.Error(resp.message.ifBlank { "搜索失败" })
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    private fun extractWorkerList(data: kotlinx.serialization.json.JsonElement): List<WorkerInfo> {
        val arr = when {
            data is kotlinx.serialization.json.JsonArray -> data
            data is kotlinx.serialization.json.JsonObject -> {
                data["list"] as? kotlinx.serialization.json.JsonArray
                    ?: data["sellers"] as? kotlinx.serialization.json.JsonArray
                    ?: data["workers"] as? kotlinx.serialization.json.JsonArray
                    ?: return emptyList()
            }
            else -> return emptyList()
        }
        return arr.mapNotNull { item ->
            try {
                val obj = item as kotlinx.serialization.json.JsonObject
                fun str(vararg keys: String): String {
                    for (k in keys) {
                        val v = obj[k]
                        if (v != null && v !is kotlinx.serialization.json.JsonNull) return v.toString().trim('"')
                    }
                    return ""
                }
                fun dbl(vararg keys: String): Double = str(*keys).toDoubleOrNull() ?: 0.0
                fun num(vararg keys: String): Int = str(*keys).toIntOrNull() ?: 0
                WorkerInfo(
                    worker_id = str("sellerId", "id", "worker_id", "seller_id"),
                    worker_name = str("sellerName", "name", "worker_name"),
                    phone = str("mobile", "phone", "tel"),
                    rating = dbl("rating", "score"),
                    order_count = num("orderCount", "order_count"),
                    available = true
                )
            } catch (e: Exception) { null }
        }
    }

    /** GET /api/sellers/search-by-name/{mobile} */
    suspend fun searchSellerByMobile(mobile: String): Result<WorkerInfo> = when (val r = get("/api/sellers/search-by-name/$mobile")) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<Map<String, String>>>(r.data)
            if (resp.code == 0 && resp.data != null) {
                Result.Success(WorkerInfo(
                    worker_id = resp.data["seller_id"] ?: "",
                    worker_name = "",
                    phone = mobile
                ))
            } else Result.Error(resp.message.ifBlank { "查询失败" })
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    /**
     * 搜索附近保洁师
     * GET /api/sellers/search?service_id=xxx&service_time=xxx&...
     */
    suspend fun queryWorkers(serviceTime: String = "", serviceId: String = "", address: String = ""): Result<List<WorkerInfo>> {
        if (serviceTime.isBlank() || serviceId.isBlank()) {
            return Result.Error("查询保洁师需要 service_id 和 service_time")
        }
        val params = mutableMapOf("service_id" to serviceId, "service_time" to serviceTime, "duration" to "3")
        if (address.isNotBlank()) params["address"] = address
        return when (val r = get("/api/sellers/search", params)) {
            is Result.Success -> try {
                val resp = json.decodeFromString<ApiResponse<kotlinx.serialization.json.JsonElement>>(r.data)
                if (resp.code == 0 && resp.data != null) Result.Success(extractWorkerList(resp.data))
                else Result.Error(resp.message.ifBlank { "查询失败" })
            } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
            is Result.Error -> r
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 订单 ====================

    /**
     * 创建订单（套餐单次单）
     * POST /api/orders/combo/single
     */
    suspend fun createOrder(request: CreateOrderRequest): Result<CreateOrderResponse> {
        val body = mapToJson(mapOf(
            "combo_id" to request.package_id,
            "seller_id" to request.worker_id,
            "service_info_id" to request.address_id,
            "service_time" to request.service_time
        ))
        return when (val r = postRaw("/api/orders/combo/single", body)) {
            is Result.Success -> try {
                val resp = json.decodeFromString<ApiResponse<kotlinx.serialization.json.JsonElement>>(r.data)
                if (resp.code == 0) {
                    val orderNo = when (val d = resp.data) {
                        is kotlinx.serialization.json.JsonObject ->
                            d["orderNo"]?.toString()?.trim('"')
                                ?: d["order_no"]?.toString()?.trim('"')
                                ?: d["orderId"]?.toString()?.trim('"') ?: ""
                        else -> ""
                    }
                    Result.Success(CreateOrderResponse(order_id = orderNo, order_no = orderNo, status = "success"))
                } else Result.Error(resp.message.ifBlank { "下单失败" })
            } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
            is Result.Error -> r
            is Result.Loading -> Result.Loading
        }
    }

    /**
     * 创建品类订单 - 通过 FastAPI 自动查询优惠券、规格、价格
     * POST /api/orders/category
     */
    suspend fun createCategoryOrder(request: CategoryOrderRequest): Result<String> {
        val body = mapToJson(mapOf(
            "mobile" to request.mobile,
            "address" to request.address,
            "service_type" to request.categoryName,
            "spec_text" to request.spec,        // FastAPI 字段是 spec_text 不是 spec_name
            "service_time" to request.serviceTime,
            "assign_mode" to (if (request.assignMode == "manual") "assign" else "auto"),
            "seller_mobile" to (request.sellerMobile.takeIf { !it.isNullOrBlank() }),
            "detail_text" to (request.remark.takeIf { !it.isNullOrBlank() })
        ))
        return when (val r = postRaw("/api/orders/category", body)) {
            is Result.Success -> try {
                val resp = json.decodeFromString<ApiResponse<kotlinx.serialization.json.JsonElement>>(r.data)
                if (resp.code == 0) {
                    val orderNo = when (val d = resp.data) {
                        is kotlinx.serialization.json.JsonObject ->
                            d["orderNo"]?.toString()?.trim('"')
                                ?: d["order_no"]?.toString()?.trim('"')
                                ?: d["orderId"]?.toString()?.trim('"') ?: "下单成功"
                        else -> "下单成功"
                    }
                    Result.Success(orderNo)
                } else Result.Error(resp.message.ifBlank { "下单失败" })
            } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
            is Result.Error -> r
            is Result.Loading -> Result.Loading
        }
    }

    /**
     * 查询订单列表
     * POST /api/orders/query
     */
    suspend fun queryOrders(phone: String, page: Int = 1, size: Int = 20): Result<List<OrderInfo>> {
        val body = mapToJson(mapOf(
            "mobile" to phone,
            "page" to page,
            "page_size" to size
        ))
        return when (val r = postRaw("/api/orders/query", body)) {
            is Result.Success -> try {
                val resp = json.decodeFromString<ApiResponse<kotlinx.serialization.json.JsonElement>>(r.data)
                if (resp.code == 0 && resp.data != null) {
                    Result.Success(extractOrderList(resp.data))
                } else Result.Error(resp.message.ifBlank { "查询失败" })
            } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
            is Result.Error -> r
            is Result.Loading -> Result.Loading
        }
    }

    private fun extractOrderList(data: kotlinx.serialization.json.JsonElement): List<OrderInfo> {
        val arr = when {
            data is kotlinx.serialization.json.JsonArray -> data
            data is kotlinx.serialization.json.JsonObject -> {
                data["list"] as? kotlinx.serialization.json.JsonArray
                    ?: data["orders"] as? kotlinx.serialization.json.JsonArray
                    ?: data["orderList"] as? kotlinx.serialization.json.JsonArray
                    ?: return emptyList()
            }
            else -> return emptyList()
        }
        return arr.mapNotNull { item ->
            try {
                val obj = item as kotlinx.serialization.json.JsonObject
                fun str(vararg keys: String): String {
                    for (k in keys) {
                        val v = obj[k]
                        if (v != null && v !is kotlinx.serialization.json.JsonNull) return v.toString().trim('"')
                    }
                    return ""
                }
                fun dbl(vararg keys: String): Double = str(*keys).toDoubleOrNull() ?: 0.0
                fun num(vararg keys: String): Int = str(*keys).toIntOrNull() ?: 0
                OrderInfo(
                    order_no = str("orderNo", "order_no", "orderId"),
                    phone = str("mobile", "phone", "userMobile"),
                    package_name = str("serviceName", "package_name", "comboName"),
                    address = str("address"),
                    worker_name = str("sellerName", "worker_name"),
                    service_time = str("serviceTime", "service_time"),
                    status = num("status"),
                    status_text = str("statusText", "status_text", "statusName"),
                    amount = dbl("amount", "totalAmount"),
                    cash_amount = dbl("cashAmount", "cash_amount"),
                    create_time = str("createTime", "create_time"),
                    order_type = num("orderType", "order_type")
                )
            } catch (e: Exception) { null }
        }
    }

    /** 暂未在后端实现，保留兼容 */
    suspend fun queryOrderDetail(orderId: String): Result<OrderInfo> {
        return Result.Error("订单详情接口暂未提供，请使用订单列表查询")
    }

    /** GET /api/orders/mobile/{order_id} */
    suspend fun getMobileByOrder(orderId: String): Result<String> = when (val r = get("/api/orders/mobile/$orderId")) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<Map<String, String>>>(r.data)
            if (resp.code == 0 && resp.data != null) {
                Result.Success(resp.data["mobile"] ?: resp.data["phone"] ?: "")
            } else Result.Error(resp.message.ifBlank { "查询失败" })
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    /** GET /api/sellers/cycle-query */
    suspend fun queryCycleBySellerAndMobile(sellerId: String, mobile: String, weekType: String = "1"): Result<CycleInfo> = when (val r = get(
        "/api/sellers/cycle-query",
        mapOf("seller_id" to sellerId, "mobile" to mobile, "week_type" to weekType)
    )) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<CycleInfo>>(r.data)
            if (resp.code == 0 && resp.data != null) Result.Success(resp.data)
            else Result.Error(resp.message.ifBlank { "查询失败" })
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    // ==================== 现金结算 ====================

    /** GET /api/cash/info/{order_id} */
    suspend fun queryCashInfo(orderNo: String): Result<CashSettleInfo> = when (val r = get("/api/cash/info/$orderNo")) {
        is Result.Success -> try {
            val resp = json.decodeFromString<ApiResponse<kotlinx.serialization.json.JsonElement>>(r.data)
            if (resp.code == 0 && resp.data != null) {
                val obj = resp.data as? kotlinx.serialization.json.JsonObject
                if (obj != null) {
                    fun str(vararg keys: String): String {
                        for (k in keys) {
                            val v = obj[k]
                            if (v != null && v !is kotlinx.serialization.json.JsonNull) return v.toString().trim('"')
                        }
                        return ""
                    }
                    fun dbl(vararg keys: String): Double = str(*keys).toDoubleOrNull() ?: 0.0
                    fun num(vararg keys: String): Int = str(*keys).toIntOrNull() ?: 0
                    Result.Success(CashSettleInfo(
                        order_no = str("orderNo", "order_no").ifBlank { orderNo },
                        cash_amount = dbl("cashAmount", "cash_amount", "amount"),
                        status = num("status"),
                        status_text = str("statusText", "status_text")
                    ))
                } else Result.Error("数据格式错误")
            } else Result.Error(resp.message.ifBlank { "查询失败" })
        } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
        is Result.Error -> r
        is Result.Loading -> Result.Loading
    }

    /** POST /api/cash/pay */
    suspend fun settleCash(orderNo: String, amount: Double = 0.0): Result<CashSettleResponse> {
        val body = mapToJson(mapOf("order_id" to orderNo, "amount" to amount))
        return when (val r = postRaw("/api/cash/pay", body)) {
            is Result.Success -> try {
                val resp = json.decodeFromString<ApiResponse<kotlinx.serialization.json.JsonElement>>(r.data)
                Result.Success(CashSettleResponse(
                    success = resp.code == 0,
                    message = resp.message.ifBlank { if (resp.code == 0) "结算成功" else "结算失败" }
                ))
            } catch (e: Exception) { Result.Error("数据解析失败：${e.message}") }
            is Result.Error -> r
            is Result.Loading -> Result.Loading
        }
    }

    // ==================== 品类列表 ====================

    /** GET /api/categories */
    suspend fun queryCategories(): Result<String> = get("/api/categories")
}
