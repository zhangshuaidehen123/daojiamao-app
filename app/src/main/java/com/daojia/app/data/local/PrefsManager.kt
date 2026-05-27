package com.daojia.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "daojia_prefs")

/**
 * 偏好设置管理器
 *
 * 管理应用的全局配置：
 * - 服务器地址（默认已内置）
 * - Cookie内容
 * - 其他持久化数据
 */
class PrefsManager(private val context: Context) {

    companion object {
        // 默认服务器地址（内置）
        const val DEFAULT_SERVER_URL = "http://10.0.2.2:5000"

        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
        private val COOKIE_CONTENT_KEY = stringPreferencesKey("cookie_content")
        private val COOKIE_VALID_KEY = booleanPreferencesKey("cookie_valid")
        private val LAST_UPDATE_CHECK_KEY = longPreferencesKey("last_update_check")
    }

    /**
     * 服务器地址（带默认值）
     */
    var serverUrl: String
        get() = runBlocking {
            context.dataStore.data.map { prefs ->
                prefs[SERVER_URL_KEY] ?: DEFAULT_SERVER_URL
            }.first()
        }
        set(value) {
            runBlocking {
                context.dataStore.edit { prefs ->
                    prefs[SERVER_URL_KEY] = value
                }
            }
        }

    /**
     * Cookie内容
     */
    var cookieContent: String
        get() = runBlocking {
            context.dataStore.data.map { prefs ->
                prefs[COOKIE_CONTENT_KEY] ?: ""
            }.first()
        }
        set(value) {
            runBlocking {
                context.dataStore.edit { prefs ->
                    prefs[COOKIE_CONTENT_KEY] = value
                }
            }
        }

    /**
     * Cookie是否有效
     */
    var isCookieValid: Boolean
        get() = runBlocking {
            context.dataStore.data.map { prefs ->
                prefs[COOKIE_VALID_KEY] ?: false
            }.first()
        }
        set(value) {
            runBlocking {
                context.dataStore.edit { prefs ->
                    prefs[COOKIE_VALID_KEY] = value
                }
            }
        }

    /**
     * 上次检查更新时间
     */
    var lastUpdateCheck: Long
        get() = runBlocking {
            context.dataStore.data.map { prefs ->
                prefs[LAST_UPDATE_CHECK_KEY] ?: 0L
            }.first()
        }
        set(value) {
            runBlocking {
                context.dataStore.edit { prefs ->
                    prefs[LAST_UPDATE_CHECK_KEY] = value
                }
            }
        }

    /**
     * 重置为默认服务器地址
     */
    fun resetToDefaultServer() {
        serverUrl = DEFAULT_SERVER_URL
    }
}
