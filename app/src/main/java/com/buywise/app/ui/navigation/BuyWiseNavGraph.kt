package com.buywise.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.buywise.app.data.local.PreferencesManager
import com.buywise.app.ui.BuyWiseViewModelFactory
import com.buywise.app.ui.assessment.AssessmentRoute
import com.buywise.app.ui.assessment.AssessmentViewModel
import com.buywise.app.ui.settings.SettingsRoute
import com.buywise.app.ui.settings.SettingsViewModel

@Composable
fun BuyWiseNavGraph(
    preferencesManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Settings.route,
        modifier = modifier
    ) {
        composable(Screen.Settings.route) {
            val vm: SettingsViewModel = viewModel(
                factory = BuyWiseViewModelFactory { SettingsViewModel(preferencesManager) }
            )
            SettingsRoute(
                viewModel = vm,
                onNavigateToAssessment = { navController.navigate(Screen.Assessment.route) }
            )
        }

        composable(Screen.Assessment.route) {
            val vm: AssessmentViewModel = viewModel(
                factory = BuyWiseViewModelFactory { AssessmentViewModel(preferencesManager) }
            )
            AssessmentRoute(viewModel = vm)
        }
    }
}
