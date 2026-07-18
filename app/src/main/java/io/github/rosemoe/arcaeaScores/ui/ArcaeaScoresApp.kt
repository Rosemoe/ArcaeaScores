package io.github.rosemoe.arcaeaScores.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.stringResource
import io.github.rosemoe.arcaeaScores.R
import io.github.rosemoe.arcaeaScores.app.MainUiState
import io.github.rosemoe.arcaeaScores.app.MainViewModel
import io.github.rosemoe.arcaeaScores.ui.components.AppDialogHost
import io.github.rosemoe.arcaeaScores.ui.screens.HomeScreen
import io.github.rosemoe.arcaeaScores.ui.screens.SettingsScreen

private enum class AppTab(val labelRes: Int) {
    Home(R.string.nav_home),
    Settings(R.string.nav_settings)
}

@Composable
fun ArcaeaScoresApp(state: MainUiState, viewModel: MainViewModel) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (tab == AppTab.Home) Icons.Outlined.Home else Icons.Outlined.Settings,
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(tab.labelRes)) },
                        colors = NavigationBarItemDefaults.colors()
                    )
                }
            }
        }
    ) { contentPadding ->
        when (selectedTab) {
            AppTab.Home -> HomeScreen(
                state = state,
                onUpdate = viewModel::requestScoreUpdate,
                onSetName = viewModel::showNameDialog,
                modifier = Modifier.padding(contentPadding)
            )

            AppTab.Settings -> SettingsScreen(modifier = Modifier.padding(contentPadding))
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
