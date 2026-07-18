package io.github.rosemoe.arcaeaScores.app

import android.content.Context
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import io.github.rosemoe.arcaeaScores.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val LEGACY_PREFERENCES_NAME = "prefs"

private val Context.settingsDataStore by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, LEGACY_PREFERENCES_NAME))
    }
)

data class AppSettings(
    val playerName: String,
    val updateTime: Long,
    val rootPermissionGranted: Boolean,
    val showArtwork: Boolean,
    val artworkDataVersion: String?
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val playerName = stringPreferencesKey("player_name")
        val updateTime = longPreferencesKey("date")
        val rootPermissionGranted = booleanPreferencesKey("agree_using_root")
        val showArtwork = booleanPreferencesKey("show_artwork")
        val artworkDataVersion = stringPreferencesKey("artwork_data_version")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            AppSettings(
                playerName = preferences[Keys.playerName]
                    ?: context.getString(R.string.click_to_set_name),
                updateTime = preferences[Keys.updateTime] ?: 0L,
                rootPermissionGranted = preferences[Keys.rootPermissionGranted] ?: false,
                showArtwork = preferences[Keys.showArtwork] ?: true,
                artworkDataVersion = preferences[Keys.artworkDataVersion]
            )
        }

    suspend fun setPlayerName(name: String) = update { this[Keys.playerName] = name }

    suspend fun setUpdateTime(time: Long) = update { this[Keys.updateTime] = time }

    suspend fun setRootPermissionGranted(granted: Boolean) = update {
        this[Keys.rootPermissionGranted] = granted
    }

    suspend fun setShowArtwork(showArtwork: Boolean) = update {
        this[Keys.showArtwork] = showArtwork
    }

    suspend fun setArtworkDataVersion(version: String) = update {
        this[Keys.artworkDataVersion] = version
    }

    private suspend fun update(transform: MutablePreferences.() -> Unit) {
        context.settingsDataStore.edit(transform)
    }
}
