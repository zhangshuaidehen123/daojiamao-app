package com.daojia.app.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences封装 - 本地配置管理
 */
class PrefsManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "daojia_prefs"

        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_COOKIE_CONTENT = "cookie_content"
        private const val KEY_COOKIE_VALID = "cookie_valid"
        private const val KEY_VERSION_CODE = "version_code"
        private const val KEY_VERSION_NAME = "version_name"
        private const val KEY_LAST_UPDATE_CHECK = "last_update_check"

        // 默认值：FastAPI 中间件服务地址
        // 提示：真机使用时请改为电脑实际 IP，例如 http://192.168.1.100:5000
        const val DEFAULT_SERVER_URL = "http://192.168.1.100:5000"
        const val DEFAULT_COOKIE = ""
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    var cookieContent: String
        get() = prefs.getString(KEY_COOKIE_CONTENT, DEFAULT_COOKIE) ?: DEFAULT_COOKIE
        set(value) {
            prefs.edit().putString(KEY_COOKIE_CONTENT, value).apply()
            isCookieValid = value.isNotBlank()
        }

    /**
     * Cookie是否有效（API验证后的状态）
     */
    var isCookieValid: Boolean
        get() = prefs.getBoolean(KEY_COOKIE_VALID, cookieContent.isNotBlank())
        set(value) = prefs.edit().putBoolean(KEY_COOKIE_VALID, value).apply()

    var versionCode: Int
        get() = prefs.getInt(KEY_VERSION_CODE, 1)
        set(value) = prefs.edit().putInt(KEY_VERSION_CODE, value).apply()

    var versionName: String
        get() = prefs.getString(KEY_VERSION_NAME, "1.0.0") ?: "1.0.0"
        set(value) = prefs.edit().putString(KEY_VERSION_NAME, value).apply()

    var lastUpdateCheck: Long
        get() = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, value).apply()

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}
