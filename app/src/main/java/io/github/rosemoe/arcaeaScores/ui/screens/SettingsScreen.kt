package io.github.rosemoe.arcaeaScores.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.rosemoe.arcaeaScores.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    playerName: String,
    showArtwork: Boolean,
    artworkDataVersion: String?,
    onEditPlayerName: () -> Unit,
    onShowArtworkChange: (Boolean) -> Unit,
    onUpdateArtworkData: () -> Unit,
    onUpdateSongData: () -> Unit,
    onCheckUpdates: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                scrollBehavior = topAppBarScrollBehavior
            )
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
        ) {
            item {
                SettingsSectionHeader(stringResource(R.string.settings_section_profile))
            }
            item {
                SettingsItem(
                    title = stringResource(R.string.set_player_name),
                    subtitle = playerName,
                    icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    onClick = onEditPlayerName
                )
            }
            item {
                SettingsSectionHeader(stringResource(R.string.settings_section_display))
            }
            item {
                SettingsSwitchItem(
                    title = stringResource(R.string.show_artwork),
                    subtitle = stringResource(R.string.show_artwork_description),
                    checked = showArtwork,
                    icon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                    onCheckedChange = onShowArtworkChange
                )
            }
            item {
                SettingsSectionHeader(stringResource(R.string.settings_section_data))
            }
            item {
                SettingsItem(
                    title = stringResource(R.string.update_artwork_data),
                    subtitle = artworkDataVersion?.let {
                        stringResource(R.string.artwork_data_version, it)
                    } ?: stringResource(R.string.no_artwork_data),
                    icon = { Icon(Icons.Outlined.Update, contentDescription = null) },
                    onClick = onUpdateArtworkData
                )
            }
            item {
                SettingsItem(
                    title = stringResource(R.string.update_song_data),
                    subtitle = stringResource(R.string.update_song_data_description),
                    icon = { Icon(Icons.Outlined.LibraryMusic, contentDescription = null) },
                    onClick = onUpdateSongData
                )
            }
            item {
                SettingsSectionHeader(stringResource(R.string.settings_section_app))
            }
            item {
                SettingsItem(
                    title = stringResource(R.string.check_updates),
                    icon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) },
                    onClick = onCheckUpdates
                )
            }
            item { HorizontalDivider() }
            item {
                SettingsItem(
                    title = stringResource(R.string.about_app),
                    icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    onClick = onOpenAbout
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: @Composable () -> Unit,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = icon,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = icon,
        modifier = Modifier.clickable(onClick = onClick),
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
