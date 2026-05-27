package com.daojia.app.util

import com.daojia.app.DjApp
import com.daojia.app.data.api.ApiClient
import com.daojia.app.data.api.Result
import com.daojia.app.data.api.UpdateInfo

/**
 * 版本更新检查工具
 *
 * 功能：
 * - 调用 GET /api/update/check 获取服务端版本
 * - 对比本地versionCode
 * - 如果服务端版本更高，返回更新信息
 * - 支持强制更新（force_update）
 * - 提供下载链接（apk_url）
 *
 * 使用方式：
 * val result = UpdateChecker.checkUpdate()
 * when (result) {
 *     is Result.Success -> { /* 有新版本 result.data */ }
 *     is Result.Error -> { /* 检查失败 result.message */ }
 *     is Result.Loading -> { /* 加载中 */ }
 * }
 */
object UpdateChecker {

    // 本地版本信息
    private const val LOCAL_VERSION_CODE = 1
    private const val LOCAL_VERSION_NAME = "1.0.0"

    // 检查更新间隔（毫秒）：1小时
    private const val CHECK_INTERVAL = 60 * 60 * 1000L

    private val apiClient = ApiClient.instance

    /**
     * 检查版本更新
     *
     * @param force 是否强制检查（忽略时间间隔限制）
     * @return Result<UpdateInfo> 如果有新版本返回更新信息，否则返回Error
     */
    suspend fun checkUpdate(force: Boolean = false): Result<UpdateInfo> {
        val prefs = DjApp.instance.prefsManager

        // 非强制检查时，判断是否在检查间隔内
        if (!force) {
            val lastCheck = prefs.lastUpdateCheck
            val now = System.currentTimeMillis()
            if (now - lastCheck < CHECK_INTERVAL) {
                return Result.Error("距离上次检查不足1小时，请稍后再试")
            }
        }

        // 调用API检查更新
        return when (val result = apiClient.checkUpdate()) {
            is Result.Success -> {
                // 更新最后检查时间
                prefs.lastUpdateCheck = System.currentTimeMillis()

                val serverVersion = result.data
                if (serverVersion.version_code > LOCAL_VERSION_CODE) {
                    // 有新版本
                    Result.Success(serverVersion)
                } else {
                    // 已是最新版本
                    Result.Error("已是最新版本")
                }
            }
            is Result.Error -> {
                Result.Error(result.message)
            }
            is Result.Loading -> {
                Result.Loading
            }
        }
    }

    /**
     * 获取本地版本信息
     */
    fun getLocalVersion(): Pair<Int, String> {
        return Pair(LOCAL_VERSION_CODE, LOCAL_VERSION_NAME)
    }

    /**
     * 判断是否需要自动检查更新
     *
     * 首次启动或距离上次检查超过间隔时返回true
     */
    fun shouldCheckUpdate(): Boolean {
        val prefs = DjApp.instance.prefsManager
        val lastCheck = prefs.lastUpdateCheck
        val now = System.currentTimeMillis()
        return lastCheck == 0L || (now - lastCheck >= CHECK_INTERVAL)
    }
}
