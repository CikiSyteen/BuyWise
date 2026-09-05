package com.buywise.app.ui.navigation

sealed class Screen(val route: String) {
    data object Settings : Screen("settings")
    data object Assessment : Screen("assessment")
}
