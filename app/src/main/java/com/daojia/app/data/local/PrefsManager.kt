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

class PrefsManager(private val context: Context) {

    companion object {
        const val DEFAULT_SERVER_URL = "https://xsg.daojia-inc.com"
        const val DEFAULT_CRM_URL = "https://jz-bjcrm.daojia-inc.com"

        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
        private val CRM_URL_KEY = stringPreferencesKey("crm_url")
        private val COOKIE_CONTENT_KEY = stringPreferencesKey("cookie_content")
        private val COOKIE_VALID_KEY = booleanPreferencesKey("cookie_valid")
        private val LAST_UPDATE_CHECK_KEY = longPreferencesKey("last_update_check")
    }

    var serverUrl: String
        get() = runBlocking {
            context.dataStore.data.map { prefs ->
                val saved = prefs[SERVER_URL_KEY]
                if (saved.isNullOrBlank() || saved.contains("10.0.2.2") || saved.contains("localhost")) {
                    DEFAULT_SERVER_URL
                } else saved
            }.first()
        }
        set(value) {
            runBlocking {
                context.dataStore.edit { prefs ->
                    prefs[SERVER_URL_KEY] = value
                }
            }
        }

    var crmUrl: String
        get() = runBlocking {
            context.dataStore.data.map { prefs ->
                prefs[CRM_URL_KEY] ?: DEFAULT_CRM_URL
            }.first()
        }
        set(value) {
            runBlocking {
                context.dataStore.edit { prefs ->
                    prefs[CRM_URL_KEY] = value
                }
            }
        }

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

    fun resetToDefaultServer() {
        serverUrl = DEFAULT_SERVER_URL
        crmUrl = DEFAULT_CRM_URL
    }
}
