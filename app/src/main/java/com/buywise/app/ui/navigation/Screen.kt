package com.buywise.app.ui.navigation

sealed class Screen(val route: String) {
    data object Settings : Screen("settings")
    data object Assessment : Screen("assessment")
    data object History : Screen("history")
    data object HistoryDetail : Screen("history/{recordId}") {
        fun create(recordId: Long) = "history/$recordId"
    }
}
