package io.github.rosemoe.arcaeaScores.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.rosemoe.arcaeaScores.app.MainUiState
import io.github.rosemoe.arcaeaScores.app.MainViewModel
import io.github.rosemoe.arcaeaScores.ui.components.AppDialogHost
import io.github.rosemoe.arcaeaScores.ui.screens.AboutScreen
import io.github.rosemoe.arcaeaScores.ui.screens.DataUpdateScreen
import io.github.rosemoe.arcaeaScores.ui.screens.MainScreen
import io.github.rosemoe.arcaeaScores.ui.screens.ScoreDetailScreen

private const val MAIN_ROUTE = "main"
private const val ABOUT_ROUTE = "about"
private const val DATA_UPDATE_ROUTE = "data-update"
private const val SCORE_DETAIL_ROUTE = "score-detail"
private const val SCORE_DETAIL_ROUTE_PATTERN = "$SCORE_DETAIL_ROUTE/{songId}/{difficulty}"

@Composable
fun ArcaeaScoresApp(state: MainUiState, viewModel: MainViewModel) {
    val navController = rememberNavController()
    val uriHandler = LocalUriHandler.current

    NavHost(
        navController = navController,
        startDestination = MAIN_ROUTE,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { slideInHorizontally { it } + fadeIn() },
        exitTransition = { slideOutHorizontally { -it / 4 } + fadeOut() },
        popEnterTransition = { slideInHorizontally { -it / 4 } + fadeIn() },
        popExitTransition = { slideOutHorizontally { it } + fadeOut() }
    ) {
        composable(MAIN_ROUTE) {
            MainScreen(
                state = state,
                onUpdateScores = viewModel::requestScoreUpdate,
                onSetPlayerName = viewModel::showNameDialog,
                onShowArtworkChange = viewModel::setShowArtwork,
                onUpdateArtworkData = { navController.navigate(DATA_UPDATE_ROUTE) },
                onUpdateSongData = viewModel::updateSongData,
                onCheckUpdates = {
                    uriHandler.openUri("https://github.com/Rosemoe/ArcaeaScores/releases/latest/")
                },
                onOpenAbout = { navController.navigate(ABOUT_ROUTE) },
                onOpenScore = { score ->
                    navController.navigate("$SCORE_DETAIL_ROUTE/${score.songId}/${score.difficulty}")
                }
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
        composable(
            route = SCORE_DETAIL_ROUTE_PATTERN,
            arguments = listOf(
                navArgument("songId") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.IntType }
            )
        ) { entry ->
            val songId = entry.arguments?.getString("songId")
            val difficulty = entry.arguments?.getInt("difficulty")
            val score = state.scores.firstOrNull {
                it.songId == songId && it.difficulty == difficulty
            }
            ScoreDetailScreen(score = score, onBack = navController::popBackStack)
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
