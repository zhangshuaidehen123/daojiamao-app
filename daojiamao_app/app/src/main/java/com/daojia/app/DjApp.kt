package com.daojia.app

import android.app.Application
import com.daojia.app.data.local.PrefsManager

/**
 * 到家保洁App - Application类
 * 负责全局初始化
 */
class DjApp : Application() {

    // 全局PrefsManager实例（延迟初始化）
    val prefsManager: PrefsManager by lazy {
        PrefsManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // 预初始化PrefsManager
        prefsManager
    }

    companion object {
        // 全局Application实例
        lateinit var instance: DjApp
            private set
    }
}
