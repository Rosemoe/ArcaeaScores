package io.github.rosemoe.arcaeaScores.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import io.github.rosemoe.arcaeaScores.R
import io.github.rosemoe.arcaeaScores.app.MainUiState

@Composable
fun AppDialogHost(
    state: MainUiState,
    onRootPermission: (Boolean) -> Unit,
    onDismissRootPermission: () -> Unit,
    onSetName: (String) -> Unit,
    onDismissNameDialog: () -> Unit,
    onDismissError: () -> Unit
) {
    if (state.showRootPermissionDialog) {
        AlertDialog(
            onDismissRequest = onDismissRootPermission,
            title = { Text(stringResource(R.string.dialog_title_root)) },
            text = { Text(stringResource(R.string.dialog_msg_root)) },
            confirmButton = {
                TextButton(onClick = { onRootPermission(true) }) {
                    Text(stringResource(R.string.action_permit))
                }
            },
            dismissButton = {
                TextButton(onClick = { onRootPermission(false) }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (state.showNameDialog) {
        PlayerNameDialog(
            initialName = state.playerName,
            onDismiss = onDismissNameDialog,
            onConfirm = onSetName
        )
    }

    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.update_failed)) },
            text = {
                Text(
                    text = error,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun PlayerNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_player_name)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
