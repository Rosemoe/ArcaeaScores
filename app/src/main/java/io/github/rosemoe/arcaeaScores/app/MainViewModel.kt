package io.github.rosemoe.arcaeaScores.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.rosemoe.arcaeaScores.R
import io.github.rosemoe.arcaeaScores.arc.readDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = getApplication<Application>()
    private val preferences = app.getSharedPreferences("prefs", Application.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(
        MainUiState(
            playerName = preferences.getString("player_name", app.getString(R.string.click_to_set_name)).orEmpty(),
            updateTime = preferences.getLong("date", 0)
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadScores()
    }

    fun requestScoreUpdate() {
        if (preferences.getBoolean("agree_using_root", false)) {
            refreshScores()
        } else {
            _uiState.update { it.copy(showRootPermissionDialog = true) }
        }
    }

    fun onRootPermissionResult(permitted: Boolean) {
        _uiState.update { it.copy(showRootPermissionDialog = false) }
        if (permitted) {
            preferences.edit().putBoolean("agree_using_root", true).apply()
            refreshScores()
        } else {
            showError(app.getString(R.string.tip_reject_root))
        }
    }

    fun dismissRootPermissionDialog() {
        _uiState.update { it.copy(showRootPermissionDialog = false) }
    }

    fun showNameDialog() {
        _uiState.update { it.copy(showNameDialog = true) }
    }

    fun dismissNameDialog() {
        _uiState.update { it.copy(showNameDialog = false) }
    }

    fun setPlayerName(name: String) {
        if (name.isBlank()) {
            showError(app.getString(R.string.name_empty))
            return
        }
        preferences.edit().putString("player_name", name).apply()
        _uiState.update { it.copy(playerName = name, showNameDialog = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun refreshScores() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, loadingMessage = app.getString(R.string.state_obtaining_root))
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    val packageName = app.packageName
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
                    val exitCode = process.waitFor()
                    if (exitCode != 0) {
                        throw IllegalStateException(
                            "Non-zero exit code: $exitCode\nError Output:\n${process.errorStream.reader().readText()}"
                        )
                    }
                }
            }.onSuccess {
                val updateTime = System.currentTimeMillis()
                preferences.edit().putLong("date", updateTime).apply()
                _uiState.update { it.copy(updateTime = updateTime) }
                loadScores()
            }.onFailure { error ->
                showError(error.stackTraceToString())
            }
        }
    }

    private fun loadScores() {
        if (!app.getDatabasePath("st3.db").exists()) {
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, loadingMessage = app.getString(R.string.state_reading_save))
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    readDatabase(app)
                }
            }.onSuccess { record ->
                _uiState.update {
                    it.copy(
                        scores = record.scores,
                        best30Potential = record.best30Potential,
                        maxPotential = record.maxPotential,
                        updateTime = preferences.getLong("date", 0),
                        isLoading = false,
                        loadingMessage = null
                    )
                }
            }.onFailure { error ->
                showError(error.stackTraceToString())
            }
        }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = message) }
    }
}
