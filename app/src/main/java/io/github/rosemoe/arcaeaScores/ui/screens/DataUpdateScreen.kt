package io.github.rosemoe.arcaeaScores.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.rosemoe.arcaeaScores.R

private const val ARCAEA_MAIN_ACTIVITY = "low.moe.AppActivity"

private data class DataSourceApp(
    val label: String,
    val packageName: String,
    val icon: Drawable
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DataUpdateScreen(
    onBack: () -> Unit,
    onApplicationSelected: (String) -> Unit,
    isUpdating: Boolean,
    updateMessage: String?,
    artworkDataVersion: String?,
    completedArtworkUpdateVersion: String?,
    onArtworkUpdateCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val applications = remember(context) { findCompatibleApplications(context) }
    val defaultPackageName = applications.singleOrNull()?.packageName
    var selectedPackageName by rememberSaveable(defaultPackageName) { mutableStateOf(defaultPackageName) }
    val canConfirm = selectedPackageName != null && !isUpdating

    completedArtworkUpdateVersion?.let { version ->
        AlertDialog(
            onDismissRequest = onArtworkUpdateCompleted,
            title = { Text(stringResource(R.string.artwork_update_complete)) },
            text = { Text(stringResource(R.string.artwork_data_version, version)) },
            confirmButton = {
                TextButton(onClick = onArtworkUpdateCompleted) {
                    Text(stringResource(R.string.action_confirm))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.update_artwork_data)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { selectedPackageName?.takeIf { !isUpdating }?.let(onApplicationSelected) },
                modifier = Modifier.padding(end = 16.dp, bottom = 16.dp),
                icon = {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null
                    )
                },
                text = { Text(stringResource(R.string.action_confirm)) },
                containerColor = if (canConfirm) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (canConfirm) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            item {
                Text(
                    text = stringResource(R.string.select_application),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
            if (artworkDataVersion != null) {
                item {
                    Text(
                        text = stringResource(R.string.artwork_data_version, artworkDataVersion),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            if (isUpdating) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    Text(
                        text = updateMessage.orEmpty(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            if (applications.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_compatible_applications),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(applications, key = DataSourceApp::packageName) { application ->
                    val selected = application.packageName == selectedPackageName
                    ListItem(
                        headlineContent = { Text(application.label) },
                        supportingContent = { Text(application.packageName) },
                        leadingContent = {
                            val icon = remember(application.packageName) {
                                application.icon.toBitmap().asImageBitmap()
                            }
                            Image(
                                bitmap = icon,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                contentScale = ContentScale.Fit
                            )
                        },
                        trailingContent = {
                            RadioButton(
                                selected = selected,
                                enabled = !isUpdating,
                                onClick = { selectedPackageName = application.packageName }
                            )
                        },
                        modifier = Modifier.clickable(enabled = !isUpdating) {
                            selectedPackageName = application.packageName
                        }
                    )
                }
            }
        }
    }
}

private fun findCompatibleApplications(context: Context): List<DataSourceApp> {
    val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return context.packageManager.queryIntentActivities(launchIntent, 0)
        .asSequence()
        .filter { it.activityInfo.name == ARCAEA_MAIN_ACTIVITY }
        .map {
            DataSourceApp(
                label = it.loadLabel(context.packageManager).toString(),
                packageName = it.activityInfo.packageName,
                icon = it.loadIcon(context.packageManager)
            )
        }
        .distinctBy(DataSourceApp::packageName)
        .sortedBy(DataSourceApp::label)
        .toList()
}
