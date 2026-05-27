package com.daojia.app.data.local

/**
 * 数据库帮助类 - 预留扩展
 *
 * 当前使用SharedPreferences存储配置，数据库预留用于：
 * - 本地订单缓存
 * - 操作日志记录
 * - 离线数据支持
 *
 * 启用步骤：
 * 1. 在app/build.gradle.kts中添加Room依赖
 * 2. 添加对应的Entity和Dao
 * 3. 取消下方注释
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
