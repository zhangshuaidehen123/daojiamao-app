package com.daojia.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.daojia.app.ui.screens.cash.CashSettleScreen
import com.daojia.app.ui.screens.history.HistoryScreen
import com.daojia.app.ui.screens.home.HomeScreen
import com.daojia.app.ui.screens.order.CategoryOrderScreen
import com.daojia.app.ui.screens.order.CycleQueryScreen
import com.daojia.app.ui.screens.order.OrderDetailScreen
import com.daojia.app.ui.screens.order.OrderScreen
import com.daojia.app.ui.screens.seller.SellerSearchScreen
import com.daojia.app.ui.screens.settings.SettingsScreen

/**
 * 应用导航配置
 */
object AppNavigation {
    // 路由常量
    const val HOME = "home"
    const val ORDER = "order"
    const val CATEGORY_ORDER = "category_order"
    const val ORDER_DETAIL = "order_detail"
    const val CYCLE_QUERY = "cycle_query"
    const val HISTORY = "history"
    const val CASH_SETTLE = "cash_settle"
    const val SELLER_SEARCH = "seller_search"
    const val SETTINGS = "settings"
}

/**
 * 导航图
 */
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = AppNavigation.HOME) {
        composable(AppNavigation.HOME) {
            HomeScreen(
                onNavigateToOrder = { navController.navigate(AppNavigation.ORDER) },
                onNavigateToCategoryOrder = { navController.navigate(AppNavigation.CATEGORY_ORDER) },
                onNavigateToHistory = { navController.navigate(AppNavigation.HISTORY) },
                onNavigateToCashSettle = { navController.navigate(AppNavigation.CASH_SETTLE) },
                onNavigateToSellerSearch = { navController.navigate(AppNavigation.SELLER_SEARCH) },
                onNavigateToOrderDetail = { navController.navigate(AppNavigation.ORDER_DETAIL) },
                onNavigateToCycleQuery = { navController.navigate(AppNavigation.CYCLE_QUERY) },
                onNavigateToSettings = { navController.navigate(AppNavigation.SETTINGS) },
            )
        }

        composable(AppNavigation.ORDER) {
            OrderScreen(onBack = { navController.popBackStack() })
        }

        composable(AppNavigation.CATEGORY_ORDER) {
            CategoryOrderScreen(onBack = { navController.popBackStack() })
        }

        composable(AppNavigation.ORDER_DETAIL) {
            OrderDetailScreen(onBack = { navController.popBackStack() })
        }

        composable(AppNavigation.CYCLE_QUERY) {
            CycleQueryScreen(onBack = { navController.popBackStack() })
        }

        composable(AppNavigation.HISTORY) {
            HistoryScreen()
        }

        composable(AppNavigation.CASH_SETTLE) {
            CashSettleScreen(onBack = { navController.popBackStack() })
        }

        composable(AppNavigation.SELLER_SEARCH) {
            SellerSearchScreen(onBack = { navController.popBackStack() })
        }

        composable(AppNavigation.SETTINGS) {
            SettingsScreen()
        }
    }
}
