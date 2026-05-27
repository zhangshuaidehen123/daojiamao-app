package com.daojia.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.daojia.app.ui.screens.cash.CashSettleScreen
import com.daojia.app.ui.screens.history.HistoryScreen
import com.daojia.app.ui.screens.home.HomeScreen
import com.daojia.app.ui.screens.order.OrderScreen
import com.daojia.app.ui.screens.seller.SellerSearchScreen
import com.daojia.app.ui.screens.settings.SettingsScreen

/**
 * 导航路由定义
 */
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "首页", Icons.Default.Home)
    data object Order : Screen("order", "下单", Icons.Default.PostAdd)
    data object History : Screen("history", "订单", Icons.Default.ListAlt)
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)
    data object CashSettle : Screen("cash_settle", "现金结算", Icons.Default.ListAlt)
    data object SellerSearch : Screen("seller_search", "查保洁师", Icons.Default.ListAlt)
}

/**
 * 底部导航栏项目
 */
private val bottomNavItems = listOf(
    Screen.Home,
    Screen.Order,
    Screen.History,
    Screen.Settings
)

/**
 * 应用导航组件
 *
 * 包含底部导航栏和各页面路由
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // 当前路由
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 判断是否显示底部导航栏
    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    // 避免创建多个实例
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 首页
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToOrder = {
                        navController.navigate(Screen.Order.route)
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route)
                    },
                    onNavigateToCashSettle = {
                        navController.navigate(Screen.CashSettle.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToSellerSearch = {
                        navController.navigate(Screen.SellerSearch.route)
                    }
                )
            }

            // 下单页
            composable(Screen.Order.route) {
                OrderScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // 订单历史
            composable(Screen.History.route) {
                HistoryScreen()
            }

            // 设置页
            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            // 现金结算
            composable(Screen.CashSettle.route) {
                CashSettleScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // 查保洁师
            composable(Screen.SellerSearch.route) {
                SellerSearchScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
