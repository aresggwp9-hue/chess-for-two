package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.navigation.Screen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.GameViewerScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ChessForTwoTheme
import com.example.ui.viewmodel.ChessViewModel
import com.example.ui.viewmodel.HistoryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChessForTwoTheme {
                MainAppContent()
            }
        }
    }
}

@Composable
fun MainAppContent() {
    val navController = rememberNavController()
    val chessViewModel: ChessViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()
    val chessState by chessViewModel.uiState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Game,
        Screen.History,
        Screen.Settings
    )

    val isViewerScreen = currentRoute?.startsWith("viewer") == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isViewerScreen) {
                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Game.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = stringResource(screen.titleRes)
                                )
                            },
                            label = { Text(stringResource(screen.titleRes)) },
                            modifier = Modifier.testTag("nav_tab_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Game.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Game.route) {
                GameScreen(viewModel = chessViewModel)
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    viewModel = historyViewModel,
                    onContinueGame = { gameId ->
                        chessViewModel.loadGameToPlay(gameId)
                        navController.navigate(Screen.Game.route) {
                            popUpTo(Screen.Game.route) { inclusive = true }
                        }
                    },
                    onViewGame = { gameId ->
                        navController.navigate(Screen.Viewer.createRoute(gameId))
                    }
                )
            }

            composable(
                route = Screen.Viewer.route,
                arguments = listOf(
                    navArgument("gameId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getLong("gameId") ?: 0L
                GameViewerScreen(
                    gameId = gameId,
                    historyViewModel = historyViewModel,
                    boardTheme = chessState.boardTheme,
                    pieceStyle = chessState.pieceStyle,
                    onBackClick = { navController.popBackStack() },
                    onContinueFromHere = { targetGameId, stepIndex ->
                        chessViewModel.loadGameToPlay(targetGameId)
                        if (stepIndex >= 0) {
                            chessViewModel.previewMove(stepIndex)
                            chessViewModel.continueFromPreview()
                        }
                        navController.navigate(Screen.Game.route) {
                            popUpTo(Screen.Game.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = chessViewModel)
            }
        }
    }
}
