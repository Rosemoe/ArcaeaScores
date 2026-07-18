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
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = getApplication<Application>()
    private val settingsRepository = SettingsRepository(app)
    private val _uiState = MutableStateFlow(
        MainUiState(
            playerName = app.getString(R.string.click_to_set_name)
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        playerName = settings.playerName,
                        updateTime = settings.updateTime,
                        rootPermissionGranted = settings.rootPermissionGranted,
                        showArtwork = settings.showArtwork,
                        artworkDataVersion = settings.artworkDataVersion
                    )
                }
            }
        }
        loadScores()
    }

    fun requestScoreUpdate() {
        if (_uiState.value.rootPermissionGranted) {
            refreshScores()
        } else {
            _uiState.update { it.copy(showRootPermissionDialog = true) }
        }
    }

    fun onRootPermissionResult(permitted: Boolean) {
        _uiState.update { it.copy(showRootPermissionDialog = false) }
        if (permitted) {
            _uiState.update { it.copy(rootPermissionGranted = true) }
            viewModelScope.launch { settingsRepository.setRootPermissionGranted(true) }
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
        viewModelScope.launch { settingsRepository.setPlayerName(name) }
        _uiState.update { it.copy(playerName = name, showNameDialog = false) }
    }

    fun setShowArtwork(showArtwork: Boolean) {
        viewModelScope.launch { settingsRepository.setShowArtwork(showArtwork) }
        _uiState.update { it.copy(showArtwork = showArtwork) }
    }

    fun updateArtworkData(packageName: String) {
        if (_uiState.value.isLoading) {
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, loadingMessage = app.getString(R.string.state_updating_artwork))
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    importArtworkData(packageName)
                }
            }.onSuccess { version ->
                viewModelScope.launch { settingsRepository.setArtworkDataVersion(version) }
                _uiState.update {
                    it.copy(
                        artworkDataVersion = version,
                        completedArtworkUpdateVersion = version,
                        isLoading = false,
                        loadingMessage = null
                    )
                }
            }.onFailure { error ->
                showError(error.stackTraceToString())
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissArtworkUpdateCompleted() {
        _uiState.update { it.copy(completedArtworkUpdateVersion = null) }
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
                viewModelScope.launch { settingsRepository.setUpdateTime(updateTime) }
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

    private fun importArtworkData(packageName: String): String {
        val packageInfo = app.packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName ?: error("The selected application has no version name.")
        val stagingDirectory = File(app.filesDir, "songs.staging")

        clearArtworkBackup()
        stagingDirectory.deleteRecursively()
        stagingDirectory.mkdirs()
        try {
            val artworkVersion = if (versionName.endsWith("c")) {
                val apkPath = packageInfo.applicationInfo?.sourceDir
                    ?: error("Unable to find the selected application APK.")
                copyArtworkFromApk(apkPath, stagingDirectory)
                versionName.removeSuffix("c")
            } else {
                copyArtworkFromApplicationData(packageName, stagingDirectory)
            }
            check(stagingDirectory.walkTopDown().any { it.isFile && it.extension.equals("jpg", true) }) {
                "No artwork JPG files were found."
            }
            replaceArtworkDirectory(stagingDirectory)
            return artworkVersion
        } finally {
            stagingDirectory.deleteRecursively()
        }
    }

    private fun copyArtworkFromApplicationData(packageName: String, destination: File): String {
        val sourceDirectory = "/data/data/$packageName/files/cb/active/songs"
        val sourceMetadata = "/data/data/$packageName/files/cb/meta.cb"
        val copiedMetadata = File(app.filesDir, "artwork-meta.cb")
        copiedMetadata.delete()

        try {
            executeRootCommand(
                """
                set -e
                source='$sourceDirectory'
                destination='${destination.absolutePath}'
                source_metadata='$sourceMetadata'
                destination_metadata='${copiedMetadata.absolutePath}'
                test -d "${'$'}source"
                test -f "${'$'}source_metadata"
                rm -rf "${'$'}destination"
                mkdir -p "${'$'}destination"
                (
                    cd "${'$'}source"
                    find . -type f -name '*.jpg' -print0 | tar -c -f - --null -T -
                ) | (
                    cd "${'$'}destination"
                    tar -x -f -
                )
                cp "${'$'}source_metadata" "${'$'}destination_metadata"
                chmod -R 755 "${'$'}destination"
                chmod 644 "${'$'}destination_metadata"
                """.trimIndent()
            )
            return JSONObject(copiedMetadata.readText()).get("versionNumber").toString()
        } finally {
            copiedMetadata.delete()
        }
    }

    private fun copyArtworkFromApk(apkPath: String, destination: File) {
        ZipFile(apkPath).use { archive ->
            val entries = archive.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.isDirectory || !entry.name.startsWith("assets/songs/") || !entry.name.endsWith(".jpg", true)) {
                    continue
                }
                val relativePath = entry.name.removePrefix("assets/songs/")
                val output = File(destination, relativePath)
                check(output.canonicalPath.startsWith(destination.canonicalPath + File.separator)) {
                    "Invalid artwork path in APK: ${entry.name}"
                }
                output.parentFile?.mkdirs()
                archive.getInputStream(entry).use { input ->
                    output.outputStream().use { outputStream -> input.copyTo(outputStream) }
                }
            }
        }
    }

    private fun replaceArtworkDirectory(stagingDirectory: File) {
        val destination = File(app.filesDir, "songs")
        val backup = File(app.filesDir, "songs.backup")
        clearArtworkBackup()
        val hasExistingArtwork = destination.exists()
        if (hasExistingArtwork && !destination.renameTo(backup)) {
            error("Unable to back up existing artwork data.")
        }
        try {
            check(stagingDirectory.renameTo(destination)) { "Unable to install artwork data." }
            backup.deleteRecursively()
        } catch (error: Throwable) {
            if (hasExistingArtwork && !destination.exists()) {
                backup.renameTo(destination)
            }
            throw error
        }
    }

    private fun clearArtworkBackup() {
        val backup = File(app.filesDir, "songs.backup")
        check(!backup.exists() || backup.deleteRecursively()) {
            "Unable to remove previous artwork backup."
        }
    }

    private fun executeRootCommand(command: String) {
        val process = Runtime.getRuntime().exec("su -mm")
        process.outputStream.bufferedWriter().use { writer ->
            writer.write(command)
            writer.newLine()
            writer.write("exit")
            writer.newLine()
        }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw IllegalStateException(
                "Root command failed with exit code $exitCode:\n${process.errorStream.bufferedReader().readText()}"
            )
        }
    }
}
