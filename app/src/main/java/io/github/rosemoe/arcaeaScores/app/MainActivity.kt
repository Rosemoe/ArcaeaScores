package io.github.rosemoe.arcaeaScores.app

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import io.github.rosemoe.arcaeaScores.R
import io.github.rosemoe.arcaeaScores.arc.ArcaeaRecord
import io.github.rosemoe.arcaeaScores.arc.ArcaeaScore
import io.github.rosemoe.arcaeaScores.arc.clearTypeShortString
import io.github.rosemoe.arcaeaScores.arc.difficultyMainColor
import io.github.rosemoe.arcaeaScores.arc.readDatabase
import io.github.rosemoe.arcaeaScores.arc.scoreGrade
import io.github.rosemoe.arcaeaScores.arc.toScoreText
import io.github.rosemoe.arcaeaScores.ui.theme.ArcaeaScoresTheme
import io.github.rosemoe.arcaeaScores.util.toScaledString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences
    private var uiState by mutableStateOf(MainUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        uiState = uiState.copy(
            playerName = prefs.getString("player_name", getString(R.string.click_to_set_name)).orEmpty(),
            updateTime = prefs.getLong("date", 0)
        )
        setContent {
            ArcaeaScoresTheme {
                ArcaeaScoresApp(
                    state = uiState,
                    onUpdate = ::onUpdateScoreClicked,
                    onRootPermission = ::onRootPermissionResult,
                    onDismissRootPermission = { uiState = uiState.copy(showRootPermissionDialog = false) },
                    onSetName = ::setPlayerName,
                    onShowNameDialog = { uiState = uiState.copy(showNameDialog = true) },
                    onDismissNameDialog = { uiState = uiState.copy(showNameDialog = false) },
                    onShowAboutDialog = { uiState = uiState.copy(showAboutDialog = true) },
                    onDismissAboutDialog = { uiState = uiState.copy(showAboutDialog = false) },
                    onDismissError = { uiState = uiState.copy(error = null) },
                    onOpenReleases = ::openReleases
                )
            }
        }
        updateScoreList()
    }

    private fun onUpdateScoreClicked() {
        if (prefs.getBoolean("agree_using_root", false)) {
            refreshScores()
        } else {
            uiState = uiState.copy(showRootPermissionDialog = true)
        }
    }

    private fun onRootPermissionResult(permitted: Boolean) {
        uiState = uiState.copy(showRootPermissionDialog = false)
        if (permitted) {
            prefs.edit().putBoolean("agree_using_root", true).apply()
            refreshScores()
        } else {
            uiState = uiState.copy(error = getString(R.string.tip_reject_root))
        }
    }

    private fun setPlayerName(name: String) {
        prefs.edit().putString("player_name", name).apply()
        uiState = uiState.copy(playerName = name, showNameDialog = false)
    }

    private fun refreshScores() {
        lifecycleScope.launch {
            uiState = uiState.copy(
                isLoading = true,
                loadingMessage = getString(R.string.state_obtaining_root)
            )
            runCatching {
                withContext(Dispatchers.IO) {
                    val packageName = application.packageName
                    val arcaeaPackageName = "moe.low.arc"
                    val process = Runtime.getRuntime().exec("su -mm")
                    process.outputStream.writer().use {
                        it.write(
                            "mkdir /data/data/$packageName/databases/\n" +
                                "cp -f /data/data/$arcaeaPackageName/files/st3 /data/data/$packageName/databases/st3.db\n" +
                                "chmod 777 /data/data/$packageName/databases\n" +
                                "chmod 777 /data/data/$packageName/databases/st3.db\n" +
                                "exit\n"
                        )
                        it.flush()
                    }
                    withContext(Dispatchers.Main) {
                        uiState = uiState.copy(loadingMessage = getString(R.string.state_reading_save))
                    }
                    val exitCode = process.waitFor()
                    if (exitCode != 0) {
                        throw IllegalStateException(
                            "Non-zero exit code: $exitCode\nError Output:\n${
                                process.errorStream.reader().readText()
                            }"
                        )
                    }
                }
            }.onSuccess {
                val updateTime = System.currentTimeMillis()
                prefs.edit().putLong("date", updateTime).apply()
                uiState = uiState.copy(updateTime = updateTime)
                updateScoreList()
            }.onFailure {
                uiState = uiState.copy(isLoading = false, loadingMessage = null, error = it.stackTraceToString())
            }
        }
    }

    private fun updateScoreList() {
        if (!getDatabasePath("st3.db").exists()) {
            return
        }
        lifecycleScope.launch {
            uiState = uiState.copy(isLoading = true, loadingMessage = getString(R.string.state_reading_save))
            runCatching {
                withContext(Dispatchers.IO) {
                    readDatabase(this@MainActivity)
                }
            }.onSuccess(::showRecord).onFailure {
                uiState = uiState.copy(isLoading = false, loadingMessage = null, error = it.stackTraceToString())
            }
        }
    }

    private fun showRecord(record: ArcaeaRecord) {
        uiState = uiState.copy(
            scores = record.scores,
            best30Potential = record.best30Potential,
            maxPotential = record.maxPotential,
            updateTime = prefs.getLong("date", 0),
            isLoading = false,
            loadingMessage = null
        )
    }

    private fun openReleases() {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/Rosemoe/ArcaeaScores/releases/latest/")
            )
        )
    }
}

private data class MainUiState(
    val playerName: String = "",
    val scores: List<ArcaeaScore> = emptyList(),
    val best30Potential: Double = 0.0,
    val maxPotential: Double = 0.0,
    val updateTime: Long = 0,
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val showRootPermissionDialog: Boolean = false,
    val showNameDialog: Boolean = false,
    val showAboutDialog: Boolean = false,
    val error: String? = null
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ArcaeaScoresApp(
    state: MainUiState,
    onUpdate: () -> Unit,
    onRootPermission: (Boolean) -> Unit,
    onDismissRootPermission: () -> Unit,
    onSetName: (String) -> Unit,
    onShowNameDialog: () -> Unit,
    onDismissNameDialog: () -> Unit,
    onShowAboutDialog: () -> Unit,
    onDismissAboutDialog: () -> Unit,
    onDismissError: () -> Unit,
    onOpenReleases: () -> Unit
) {
    val context = LocalContext.current
    val titleFont = remember { FontFamily(android.graphics.Typeface.createFromAsset(context.assets, "fonts/L2-Regular.ttf")) }
    val scoreFont = remember { FontFamily(android.graphics.Typeface.createFromAsset(context.assets, "fonts/GeosansLight.ttf")) }
    val exoFont = remember { FontFamily(android.graphics.Typeface.createFromAsset(context.assets, "fonts/Exo-Regular.ttf")) }
    val exoSemiBoldFont = remember { FontFamily(android.graphics.Typeface.createFromAsset(context.assets, "fonts/Exo-SemiBold.ttf")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = onShowAboutDialog) {
                        Text(stringResource(R.string.about_app))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onUpdate) {
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.refresh),
                    contentDescription = stringResource(R.string.pd_dialog_title)
                )
            }
        }
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    PlayerSummary(
                        state = state,
                        exoFont = exoFont,
                        exoSemiBoldFont = exoSemiBoldFont,
                        onClick = onShowNameDialog
                    )
                }
                itemsIndexed(
                    items = state.scores,
                    key = { _, score -> "${score.songId}-${score.difficulty}" }
                ) { index, score ->
                    ScoreCard(
                        score = score,
                        rank = index + 1,
                        titleFont = titleFont,
                        scoreFont = scoreFont,
                        exoFont = exoFont
                    )
                }
            }
            if (state.isLoading) {
                LoadingOverlay(state.loadingMessage.orEmpty())
            }
        }
    }

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

    if (state.showAboutDialog) {
        AlertDialog(
            onDismissRequest = onDismissAboutDialog,
            title = { Text(stringResource(R.string.about_app)) },
            text = { Text(stringResource(R.string.dialog_msg_about)) },
            confirmButton = {
                TextButton(onClick = onOpenReleases) {
                    Text("Releases")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissAboutDialog) {
                    Text(stringResource(android.R.string.ok))
                }
            }
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
private fun PlayerSummary(
    state: MainUiState,
    exoFont: FontFamily,
    exoSemiBoldFont: FontFamily,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = state.playerName,
                fontFamily = exoSemiBoldFont,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Best30: ${state.best30Potential.toScaledString()}  Max Ptt: ${state.maxPotential.toScaledString()}",
                fontFamily = exoFont
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (state.updateTime > 0) {
                    "Update Time: " + SimpleDateFormat.getDateTimeInstance(
                        SimpleDateFormat.DEFAULT,
                        SimpleDateFormat.DEFAULT,
                        Locale.ENGLISH
                    ).format(Date(state.updateTime))
                } else {
                    "Update Time: -"
                },
                fontFamily = exoFont,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun ScoreCard(
    score: ArcaeaScore,
    rank: Int,
    titleFont: FontFamily,
    scoreFont: FontFamily,
    exoFont: FontFamily
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(Color(difficultyMainColor(score.difficulty)))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = score.title,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        fontFamily = titleFont,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "[${scoreGrade(score.score)}/${clearTypeShortString(score.clearType)}]",
                        fontFamily = exoFont
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = "#$rank", fontFamily = FontFamily.Monospace)
                }
                Text(
                    text = "Potential: ${score.chartConstant} > ${String.format(Locale.getDefault(), "%.5f", score.playPotential)}",
                    fontFamily = exoFont,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = toScoreText(score.score), fontFamily = scoreFont, fontSize = 19.sp)
                Text(
                    text = "P/F/L: ${score.pureCount}(+${score.maxPureCount}) / ${score.farCount} / ${score.lostCount}",
                    fontFamily = exoFont,
                    fontSize = 17.sp
                )
            }
        }
    }
}

@Composable
private fun LoadingOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(message)
            }
        }
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
        title = { Text("设置名字") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
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
