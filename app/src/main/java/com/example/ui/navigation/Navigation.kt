package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Game : Screen("game", R.string.nav_game, Icons.Default.Games)
    object History : Screen("history", R.string.nav_history, Icons.Default.History)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    object Viewer : Screen("viewer/{gameId}", R.string.nav_viewer, Icons.Default.History) {
        fun createRoute(gameId: Long) = "viewer/$gameId"
    }
}
