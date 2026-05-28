package com.daojia.app.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences封装 - 本地配置管理
 *
 * 管理：服务器地址、Cookie、版本号等本地配置
 * 预留扩展：可迁移至DataStore或Room
 */
class PrefsManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "daojia_prefs"

        // 配置Key
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_COOKIE_CONTENT = "cookie_content"
        private const val KEY_COOKIE_VALID = "cookie_valid"
        private const val KEY_VERSION_CODE = "version_code"
        private const val KEY_VERSION_NAME = "version_name"
        private const val KEY_LAST_UPDATE_CHECK = "last_update_check"

        // 默认值
        const val DEFAULT_SERVER_URL = "https://xsg.daojia-inc.com"
        const val DEFAULT_COOKIE = ""
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ==================== 服务器地址 ====================

    /**
     * 获取服务器地址
     */
    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    // ==================== Cookie管理 ====================

    /**
     * 获取Cookie内容
     */
    var cookieContent: String
        get() = prefs.getString(KEY_COOKIE_CONTENT, DEFAULT_COOKIE) ?: DEFAULT_COOKIE
        set(value) {
            prefs.edit().putString(KEY_COOKIE_CONTENT, value).apply()
            // Cookie更新后重置验证状态
            isCookieValid = value.isNotBlank()
        }

    /**
     * Cookie是否有效（通过API验证后的状态）
     */
    var isCookieValid: Boolean
        get() = prefs.getBoolean(KEY_COOKIE_VALID, cookieContent.isNotBlank())
        set(value) = prefs.edit().putBoolean(KEY_COOKIE_VALID, value).apply()

    // ==================== 版本信息 ====================

    /**
     * 获取本地版本号
     */
    var versionCode: Int
        get() = prefs.getInt(KEY_VERSION_CODE, 1)
        set(value) = prefs.edit().putInt(KEY_VERSION_CODE, value).apply()

    /**
     * 获取本地版本名
     */
    var versionName: String
        get() = prefs.getString(KEY_VERSION_NAME, "1.0.0") ?: "1.0.0"
        set(value) = prefs.edit().putString(KEY_VERSION_NAME, value).apply()

    // ==================== 更新检查 ====================

    /**
     * 获取上次更新检查时间戳
     */
    var lastUpdateCheck: Long
        get() = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, value).apply()

    // ==================== 通用方法 ====================

    /**
     * 保存字符串
     */
    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    /**
     * 读取字符串
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    /**
     * 保存整数
     */
    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    /**
     * 读取整数
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }

    /**
     * 保存布尔值
     */
    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    /**
     * 读取布尔值
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    /**
     * 清除所有配置
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /**
     * 移除指定配置
     */
    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}
