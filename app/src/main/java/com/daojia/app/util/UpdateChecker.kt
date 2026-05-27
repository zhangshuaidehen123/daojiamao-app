package com.daojia.app.util

import com.daojia.app.DjApp
import com.daojia.app.data.api.ApiClient
import com.daojia.app.data.api.Result
import com.daojia.app.data.api.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 更新检查器
 */
object UpdateChecker {

    private const val CHECK_INTERVAL = 24 * 60 * 60 * 1000L // 24小时

    /**
     * 检查更新
     * @param force 是否强制检查（忽略时间间隔）
     */
    suspend fun checkUpdate(force: Boolean = false): Result<UpdateInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = DjApp.instance.prefsManager

                // 检查时间间隔
                if (!force) {
                    val lastCheck = prefs.lastUpdateCheck
                    val now = System.currentTimeMillis()
                    if (now - lastCheck < CHECK_INTERVAL) {
                        return@withContext Result.Error("检查太频繁")
                    }
                }

                // 调用API检查更新
                val result = ApiClient.instance.checkUpdate()

                // 更新最后检查时间
                if (result is Result.Success) {
                    prefs.lastUpdateCheck = System.currentTimeMillis()
                }

                result
            } catch (e: Exception) {
                Result.Error("检查更新失败: ${e.message}")
            }
        }
    }

    /**
     * 获取上次检查时间
     */
    fun getLastCheckTime(): Long {
        return DjApp.instance.prefsManager.lastUpdateCheck
    }

    /**
     * 是否需要检查更新
     */
    fun shouldCheckUpdate(): Boolean {
        val lastCheck = DjApp.instance.prefsManager.lastUpdateCheck
        val now = System.currentTimeMillis()
        return now - lastCheck >= CHECK_INTERVAL
    }
}
