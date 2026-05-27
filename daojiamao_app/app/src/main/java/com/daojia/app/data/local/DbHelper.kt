package com.daojia.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room数据库 - 预留扩展
 *
 * 当前使用SharedPreferences存储配置，Room数据库预留用于：
 * - 本地订单缓存
 * - 操作日志记录
 * - 离线数据支持
 *
 * 启用步骤：
 * 1. 在app/build.gradle.kts中添加Room依赖
 * 2. 取消下方注释
 * 3. 添加对应的Entity和Dao
 */
// @Database(
//     entities = [
//         // OrderCacheEntity::class,
//         // OperationLogEntity::class
//     ],
//     version = 1,
//     exportSchema = false
// )
abstract class DbHelper /* : RoomDatabase() */ {

    // 预留DAO
    // abstract fun orderCacheDao(): OrderCacheDao
    // abstract fun operationLogDao(): OperationLogDao

    companion object {
        // 数据库名称
        const val DATABASE_NAME = "daojia_db"
    }
}
