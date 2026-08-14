package com.regannian.legend.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.regannian.legend.data.levels.LevelRepository
import com.regannian.legend.data.progress.PlayerProgress
import com.regannian.legend.ui.screen.codex.CodexScreen
import com.regannian.legend.ui.screen.day.DayScreen
import com.regannian.legend.ui.screen.ending.EndingScreen
import com.regannian.legend.ui.screen.menu.MenuScreen
import com.regannian.legend.ui.screen.result.ResultScreen
import com.regannian.legend.ui.screen.settings.SettingsScreen
import com.regannian.legend.ui.screen.upgrade.UpgradeScreen

object Routes {
    const val MENU = "menu"
    const val UPGRADE = "upgrade"
    const val SETTINGS = "settings"
    const val CODEX = "codex"
    const val ENDING = "ending"
    const val DAY = "day/{dayId}"
    const val RESULT = "result/{dayId}/{coins}/{sat}/{orders}/{stars}"

    fun day(dayId: Int): String = "day/$dayId"

    fun result(dayId: Int, coins: Int, sat: Int, orders: Int, stars: Int): String =
        "result/$dayId/$coins/$sat/$orders/$stars"
}

/**
 * 菜单 → Day N → 结算（可进下一天）→ 回菜单。
 */
@Composable
fun ReganmianNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.MENU,
    ) {
        composable(Routes.MENU) {
            MenuScreen(
                onStartDay = { dayId ->
                    navController.navigate(Routes.day(dayId)) {
                        launchSingleTop = true
                    }
                },
                onOpenUpgrade = {
                    navController.navigate(Routes.UPGRADE) {
                        launchSingleTop = true
                    }
                },
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS) {
                        launchSingleTop = true
                    }
                },
                onOpenCodex = {
                    navController.navigate(Routes.CODEX) {
                        launchSingleTop = true
                    }
                },
                onOpenEnding = {
                    navController.navigate(Routes.ENDING) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CODEX) {
            CodexScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ENDING) {
            EndingScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.UPGRADE) {
            UpgradeScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.DAY,
            arguments = listOf(navArgument("dayId") { type = NavType.IntType }),
        ) { entry ->
            val dayId = entry.arguments?.getInt("dayId") ?: 1
            DayScreen(
                dayId = dayId,
                onEndDay = { summary ->
                    PlayerProgress.recordDay(summary)
                    navController.navigate(
                        Routes.result(
                            dayId = summary.dayId,
                            coins = summary.coins,
                            sat = summary.satPercent,
                            orders = summary.orderCount,
                            stars = summary.stars,
                        ),
                    ) {
                        popUpTo(Routes.MENU) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(
            route = Routes.RESULT,
            arguments = listOf(
                navArgument("dayId") { type = NavType.IntType },
                navArgument("coins") { type = NavType.IntType },
                navArgument("sat") { type = NavType.IntType },
                navArgument("orders") { type = NavType.IntType },
                navArgument("stars") { type = NavType.IntType },
            ),
        ) { entry ->
            val dayId = entry.arguments?.getInt("dayId") ?: 1
            ResultScreen(
                dayId = dayId,
                coins = entry.arguments?.getInt("coins") ?: 0,
                satPercent = entry.arguments?.getInt("sat") ?: 0,
                orderCount = entry.arguments?.getInt("orders") ?: 0,
                stars = entry.arguments?.getInt("stars") ?: 0,
                onOpenUpgrade = {
                    navController.navigate(Routes.UPGRADE) {
                        launchSingleTop = true
                    }
                },
                onOpenEnding = {
                    navController.navigate(Routes.ENDING) {
                        launchSingleTop = true
                    }
                },
                onNextDay = if (dayId < LevelRepository.MAX_DAY) {
                    {
                        navController.navigate(Routes.day(dayId + 1)) {
                            popUpTo(Routes.MENU) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                } else {
                    null
                },
                onBackToMenu = {
                    navController.navigate(Routes.MENU) {
                        popUpTo(Routes.MENU) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
