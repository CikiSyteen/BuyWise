package com.buywise.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.buywise.app.data.local.PreferencesManager
import com.buywise.app.data.repository.AssessmentRepository
import com.buywise.app.ui.BuyWiseViewModelFactory
import com.buywise.app.ui.assessment.AssessmentRoute
import com.buywise.app.ui.assessment.AssessmentViewModel
import com.buywise.app.ui.history.HistoryDetailRoute
import com.buywise.app.ui.history.HistoryDetailViewModel
import com.buywise.app.ui.history.HistoryRoute
import com.buywise.app.ui.history.HistoryViewModel
import com.buywise.app.ui.settings.SettingsRoute
import com.buywise.app.ui.settings.SettingsViewModel

/** 底部导航的三个主 Tab */
private data class TopLevelTab(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val TOP_LEVEL_TABS = listOf(
    TopLevelTab(Screen.Assessment, "评估", Icons.Default.ShoppingCart),
    TopLevelTab(Screen.History, "记录", Icons.Default.List),
    TopLevelTab(Screen.Settings, "设置", Icons.Default.Settings)
)

@Composable
fun BuyWiseNavGraph(
    preferencesManager: PreferencesManager,
    repository: AssessmentRepository,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in TOP_LEVEL_TABS.map { it.screen.route }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TOP_LEVEL_TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.screen.route,
                            onClick = {
                                navController.navigate(tab.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Settings.route) {
                val vm: SettingsViewModel = viewModel(
                    factory = BuyWiseViewModelFactory { SettingsViewModel(preferencesManager) }
                )
                SettingsRoute(
                    viewModel = vm,
                    onNavigateToAssessment = {
                        navController.navigate(Screen.Assessment.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Assessment.route) {
                val vm: AssessmentViewModel = viewModel(
                    factory = BuyWiseViewModelFactory {
                        AssessmentViewModel(preferencesManager, repository)
                    }
                )
                AssessmentRoute(viewModel = vm)
            }

            composable(Screen.History.route) {
                val vm: HistoryViewModel = viewModel(
                    factory = BuyWiseViewModelFactory { HistoryViewModel(repository) }
                )
                HistoryRoute(
                    viewModel = vm,
                    onOpenDetail = { id ->
                        navController.navigate(Screen.HistoryDetail.create(id))
                    }
                )
            }

            composable(
                route = Screen.HistoryDetail.route,
                arguments = listOf(navArgument("recordId") { type = NavType.LongType })
            ) { entry ->
                val recordId = entry.arguments?.getLong("recordId") ?: return@composable
                val vm: HistoryDetailViewModel = viewModel(
                    key = "history_detail_$recordId",
                    factory = BuyWiseViewModelFactory {
                        HistoryDetailViewModel(recordId, repository)
                    }
                )
                HistoryDetailRoute(
                    viewModel = vm,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
