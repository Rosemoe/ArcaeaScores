package io.github.rosemoe.arcaeaScores.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.rosemoe.arcaeaScores.R
import io.github.rosemoe.arcaeaScores.app.MainUiState
import io.github.rosemoe.arcaeaScores.app.MainViewModel
import io.github.rosemoe.arcaeaScores.ui.components.AppDialogHost
import io.github.rosemoe.arcaeaScores.ui.screens.HomeScreen
import io.github.rosemoe.arcaeaScores.ui.screens.AboutScreen
import io.github.rosemoe.arcaeaScores.ui.screens.DataUpdateScreen
import io.github.rosemoe.arcaeaScores.ui.screens.SettingsScreen

private enum class AppDestination(val route: String, val labelRes: Int) {
    Home("home", R.string.nav_home),
    Settings("settings", R.string.nav_settings)
}

private const val ABOUT_ROUTE = "about"
private const val DATA_UPDATE_ROUTE = "data-update"

@Composable
fun ArcaeaScoresApp(state: MainUiState, viewModel: MainViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val uriHandler = LocalUriHandler.current

    val isTopLevelDestination = currentRoute in AppDestination.entries.map(AppDestination::route)
    val onDestinationSelected: (AppDestination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints {
        val showNavigationRail = isTopLevelDestination && maxWidth >= navigationRailBreakpoint
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            bottomBar = {
                if (isTopLevelDestination && !showNavigationRail) {
                    AppBottomNavigation(
                        currentRoute = currentRoute,
                        onDestinationSelected = onDestinationSelected
                    )
                }
            }
        ) { contentPadding ->
            Row(modifier = Modifier.fillMaxSize()) {
                if (showNavigationRail) {
                    AppNavigationRail(
                        currentRoute = currentRoute,
                        onDestinationSelected = onDestinationSelected
                    )
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = if (showNavigationRail) {
                        RoundedCornerShape(
                            topStart = navigationContentCornerRadius,
                            bottomStart = navigationContentCornerRadius
                        )
                    } else {
                        RoundedCornerShape(0.dp)
                    },
                    color = MaterialTheme.colorScheme.surface
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = AppDestination.Home.route,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        enterTransition = { slideInHorizontally { it } + fadeIn() },
                        exitTransition = { slideOutHorizontally { -it / 4 } + fadeOut() },
                        popEnterTransition = { slideInHorizontally { -it / 4 } + fadeIn() },
                        popExitTransition = { slideOutHorizontally { it } + fadeOut() }
                    ) {
                        composable(AppDestination.Home.route) {
                            HomeScreen(
                                state = state,
                                onUpdate = viewModel::requestScoreUpdate,
                                onSetName = viewModel::showNameDialog
                            )
                        }
                        composable(AppDestination.Settings.route) {
                            SettingsScreen(
                                playerName = state.playerName,
                                showArtwork = state.showArtwork,
                                artworkDataVersion = state.artworkDataVersion,
                                onEditPlayerName = viewModel::showNameDialog,
                                onShowArtworkChange = viewModel::setShowArtwork,
                                onUpdateArtworkData = { navController.navigate(DATA_UPDATE_ROUTE) },
                                onCheckUpdates = {
                                    uriHandler.openUri("https://github.com/Rosemoe/ArcaeaScores/releases/latest/")
                                },
                                onOpenAbout = { navController.navigate(ABOUT_ROUTE) }
                            )
                        }
                        composable(ABOUT_ROUTE) {
                            AboutScreen(
                                onBack = navController::popBackStack,
                                onOpenReleases = {
                                    uriHandler.openUri("https://github.com/Rosemoe/ArcaeaScores/releases/latest/")
                                },
                                onOpenSource = { uriHandler.openUri("https://github.com/Rosemoe/ArcaeaScores") }
                            )
                        }
                        composable(DATA_UPDATE_ROUTE) {
                            DataUpdateScreen(
                                onBack = navController::popBackStack,
                                onApplicationSelected = viewModel::updateArtworkData,
                                isUpdating = state.isLoading,
                                updateMessage = state.loadingMessage,
                                artworkDataVersion = state.artworkDataVersion,
                                completedArtworkUpdateVersion = state.completedArtworkUpdateVersion,
                                onArtworkUpdateCompleted = {
                                    viewModel.dismissArtworkUpdateCompleted()
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    AppDialogHost(
        state = state,
        onRootPermission = viewModel::onRootPermissionResult,
        onDismissRootPermission = viewModel::dismissRootPermissionDialog,
        onSetName = viewModel::setPlayerName,
        onDismissNameDialog = viewModel::dismissNameDialog,
        onDismissError = viewModel::dismissError
    )
}

@Composable
private fun AppBottomNavigation(
    currentRoute: String?,
    onDestinationSelected: (AppDestination) -> Unit
) {
    NavigationBar {
        AppDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onDestinationSelected(destination) },
                icon = { DestinationIcon(destination) },
                label = { Text(stringResource(destination.labelRes)) }
            )
        }
    }
}

@Composable
private fun AppNavigationRail(
    currentRoute: String?,
    onDestinationSelected: (AppDestination) -> Unit
) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        AppDestination.entries.forEach { destination ->
            NavigationRailItem(
                selected = currentRoute == destination.route,
                onClick = { onDestinationSelected(destination) },
                icon = { DestinationIcon(destination) },
                label = { Text(stringResource(destination.labelRes)) }
            )
        }
    }
}

@Composable
private fun DestinationIcon(destination: AppDestination) {
    Icon(
        imageVector = if (destination == AppDestination.Home) {
            Icons.Outlined.Home
        } else {
            Icons.Outlined.Settings
        },
        contentDescription = null
    )
}

private val navigationRailBreakpoint = 600.dp
private val navigationContentCornerRadius = 28.dp
