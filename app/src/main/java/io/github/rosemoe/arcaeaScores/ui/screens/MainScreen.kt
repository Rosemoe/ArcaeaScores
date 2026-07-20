package io.github.rosemoe.arcaeaScores.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import io.github.rosemoe.arcaeaScores.R
import io.github.rosemoe.arcaeaScores.app.MainUiState

private enum class MainDestination(val labelRes: Int) {
    Home(R.string.nav_home),
    Settings(R.string.nav_settings)
}

@Composable
fun MainScreen(
    state: MainUiState,
    onUpdateScores: () -> Unit,
    onSetPlayerName: () -> Unit,
    onShowArtworkChange: (Boolean) -> Unit,
    onUpdateArtworkData: () -> Unit,
    onUpdateSongData: () -> Unit,
    onCheckUpdates: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDestinationName by rememberSaveable { mutableStateOf(MainDestination.Home.name) }
    val destinationStateHolder = rememberSaveableStateHolder()
    val selectedDestination = MainDestination.entries.firstOrNull {
        it.name == selectedDestinationName
    } ?: MainDestination.Home
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val navigationSuiteType = if (
        adaptiveInfo.windowSizeClass.minHeightDp < WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND &&
        adaptiveInfo.windowSizeClass.minWidthDp >= WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
    ) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
    }
    val shouldPinTopbarForHeight = adaptiveInfo.windowSizeClass.minHeightDp >= 600
    val collapseTopbarOnScroll =
        navigationSuiteType == NavigationSuiteType.NavigationRail && !shouldPinTopbarForHeight

    NavigationSuiteScaffold(
        modifier = modifier,
        layoutType = navigationSuiteType,
        navigationSuiteItems = {
            MainDestination.entries.forEach { destination ->
                item(
                    icon = { MainDestinationIcon(destination) },
                    label = { Text(stringResource(destination.labelRes)) },
                    selected = selectedDestination == destination,
                    onClick = { selectedDestinationName = destination.name }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationRailContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = if (navigationSuiteType == NavigationSuiteType.NavigationRail) {
                RoundedCornerShape(
                    topStart = navigationContentCornerRadius,
                    bottomStart = navigationContentCornerRadius
                )
            } else {
                RoundedCornerShape(0.dp)
            },
            color = MaterialTheme.colorScheme.surface
        ) {
            destinationStateHolder.SaveableStateProvider(selectedDestination.name) {
                when (selectedDestination) {
                    MainDestination.Home -> HomeScreen(
                        state = state,
                        onUpdate = onUpdateScores,
                        onSetName = onSetPlayerName,
                        collapseTopbarOnScroll = collapseTopbarOnScroll
                    )

                    MainDestination.Settings -> SettingsScreen(
                        playerName = state.playerName,
                        showArtwork = state.showArtwork,
                        artworkDataVersion = state.artworkDataVersion,
                        songDataVersion = state.songDataVersion,
                        onEditPlayerName = onSetPlayerName,
                        onShowArtworkChange = onShowArtworkChange,
                        onUpdateArtworkData = onUpdateArtworkData,
                        onUpdateSongData = {
                            selectedDestinationName = MainDestination.Home.name
                            onUpdateSongData()
                        },
                        onCheckUpdates = onCheckUpdates,
                        onOpenAbout = onOpenAbout
                    )
                }
            }
        }
    }
}

@Composable
private fun MainDestinationIcon(destination: MainDestination) {
    Icon(
        imageVector = if (destination == MainDestination.Home) {
            Icons.Outlined.Home
        } else {
            Icons.Outlined.Settings
        },
        contentDescription = null
    )
}

private val navigationContentCornerRadius = 28.dp
