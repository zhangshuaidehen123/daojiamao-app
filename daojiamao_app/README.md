# 到家保洁 - 内部下单App

## 项目简介

到家保洁手动下单App，供内部几十人使用。基于 Kotlin + Jetpack Compose + Material3 开发。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Kotlin | 1.9.22 | 开发语言 |
| AGP | 8.2.2 | Android Gradle Plugin |
| Compose BOM | 2024.02.00 | 声明式UI框架 |
| Material3 | BOM管理 | Material Design 3 |
| OkHttp | 4.12.0 | 网络请求 |
| Kotlin Serialization | 1.6.2 | JSON序列化 |
| Navigation Compose | 2.7.7 | 页面导航 |
| Min SDK | 24 (Android 7.0) | 覆盖99%设备 |
| Target SDK | 34 (Android 14) | 目标SDK |

## 编译环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.5+（建议使用Gradle Wrapper）

## 编译步骤

### 1. 克隆项目

```bash
git clone <repository-url>
cd daojiamao_app
```

### 2. 配置Gradle Wrapper

项目不包含Gradle Wrapper二进制文件，需要手动生成：

```bash
# 方式一：使用gradle init生成
gradle wrapper --gradle-version 8.5

# 方式二：从其他项目复制gradle/wrapper目录
```

### 3. 编译项目

```bash
# Debug版本
./gradlew assembleDebug

# Release版本
./gradlew assembleRelease
```

### 4. 安装到设备

```bash
# 安装Debug版本
./gradlew installDebug

# 或直接使用adb
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 项目结构

```
app/src/main/java/com/daojia/app/
├── DjApp.kt                    # Application类
├── MainActivity.kt             # 主Activity
├── data/
│   ├── api/
│   │   ├── ApiClient.kt        # OkHttp API客户端
│   │   └── ApiModels.kt        # 数据模型定义
│   ├── local/
│   │   ├── PrefsManager.kt     # SharedPreferences封装
│   │   └── DbHelper.kt         # Room数据库（预留）
│   └── repository/
│       └── OrderRepository.kt  # 数据仓库
├── ui/
│   ├── theme/                  # 主题配色
│   ├── navigation/             # 导航路由
│   ├── screens/                # 功能页面
│   │   ├── home/               # 首页
│   │   ├── order/              # 下单页
│   │   ├── history/            # 订单历史
│   │   ├── cash/               # 现金结算
│   │   └── settings/           # 设置页
│   └── components/             # 通用组件
└── util/
    └── UpdateChecker.kt        # 版本更新检查
```

## 功能模块

### 1. 首页 (HomeScreen)
- Cookie状态指示灯（绿/红）
- 4个快捷操作卡片
- 今日统计信息

### 2. 下单页 (OrderScreen)
- 4步骤表单：手机号 -> 地址 -> 时间 -> 确认
- 支持单次单和周期单
- 支持随机/指定保洁师分配

### 3. 订单查询 (HistoryScreen)
- 手机号搜索
- 订单列表卡片展示
- 订单详情查看

### 4. 现金结算 (CashSettleScreen)
- 订单号查询
- 现金金额展示
- 确认结算

### 5. 设置页 (SettingsScreen)
- 服务器地址配置
- Cookie管理
- 版本检查更新
- 强制更新支持

## API接口约定

所有接口基础URL可在设置页配置，默认格式：

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 检查更新 | GET | /api/update/check | 版本更新检查 |
| Cookie验证 | GET | /api/cookie/check | 验证Cookie有效性 |
| 查询套餐 | GET | /api/packages?phone=xxx | 查询可用套餐 |
| 查询地址 | GET | /api/addresses?phone=xxx | 查询用户地址 |
| 查询保洁师 | GET | /api/workers?service_time=xxx | 查询可用保洁师 |
| 创建订单 | POST | /api/orders/create | 手动创建订单 |
| 查询订单 | GET | /api/orders?phone=xxx | 查询订单列表 |
| 订单详情 | GET | /api/orders/{id} | 查询订单详情 |
| 现金查询 | GET | /api/cash/info?order_no=xxx | 查询现金信息 |
| 现金结算 | POST | /api/cash/settle | 确认现金结算 |
| 今日统计 | GET | /api/stats/today | 获取今日统计 |

## 扩展指南

### 添加Room数据库
1. 在 `app/build.gradle.kts` 添加Room依赖
2. 取消 `DbHelper.kt` 中的注释
3. 创建Entity和Dao

### 添加新页面
1. 在 `ui/screens/` 下创建新目录
2. 在 `AppNavigation.kt` 添加路由
3. 在 `Screen` sealed class 添加定义

### 修改主题色
编辑 `ui/theme/Color.kt` 中的颜色值即可全局生效。

## Release签名

Release版本需要配置签名信息，在 `app/build.gradle.kts` 的 `buildTypes.release` 中添加：

```kotlin
signingConfig = signingConfigs.create("release") {
    storeFile = file("your-keystore.jks")
    storePassword = "your-password"
    keyAlias = "your-alias"
    keyPassword = "your-key-password"
}
```
