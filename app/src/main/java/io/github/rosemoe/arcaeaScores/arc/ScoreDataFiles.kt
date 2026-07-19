package io.github.rosemoe.arcaeaScores.arc

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

private const val SCORES_API_BASE_URL = "https://assets.yurisaki.top/arcaea/scores_api"
private const val SCORE_DATA_DIRECTORY = "score-data"
private const val SCORE_DATA_STAGING_DIRECTORY = "score-data.staging"
private const val SCORE_DATA_BACKUP_DIRECTORY = "score-data.backup"
private const val SONG_LIST_FILE_NAME = "songlist.json"
private const val CONSTANTS_FILE_NAME = "constants.json"
private const val VERSION_FILE_NAME = "version.json"

data class ScoreDataVersion(
    val arcaeaVersion: String?,
    val updatedAt: Long
) {
    val displayVersion: String
        get() = arcaeaVersion ?: updatedAt.toString()

    fun toJson(): String = JSONObject()
        .put("arcaea_version", arcaeaVersion)
        .put("updated_at", updatedAt)
        .toString()
}

object ScoreDataFiles {

    fun openSongList(context: Context): InputStream = openDataFileOrAsset(context, SONG_LIST_FILE_NAME)

    fun openConstants(context: Context): InputStream = openDataFileOrAsset(context, CONSTANTS_FILE_NAME)

    fun readLocalVersion(context: Context): ScoreDataVersion? {
        val versionFile = File(dataDirectory(context), VERSION_FILE_NAME)
        if (!hasUpdatedData(context) || !versionFile.isFile) {
            return null
        }
        return runCatching {
            parseVersion(JSONObject(versionFile.readText()))
        }.getOrNull()
    }

    fun readCurrentVersion(context: Context): ScoreDataVersion? =
        readLocalVersion(context) ?: readBuiltInVersion(context)

    fun update(context: Context): ScoreDataVersion {
        val remoteVersion = fetchVersion()
        val localVersion = readLocalVersion(context)
        if (localVersion?.updatedAt == remoteVersion.updatedAt) {
            return localVersion
        }
        if (!hasUpdatedData(context)) {
            val builtInVersion = readBuiltInVersion(context)
            if (builtInVersion?.updatedAt == remoteVersion.updatedAt) {
                return builtInVersion
            }
        }

        val songList = fetchJson("songlist")
        val constants = fetchJson("constants")
        validateSongList(songList)
        validateConstants(constants)
        installData(context, songList, constants, remoteVersion)
        return remoteVersion
    }

    private fun openDataFileOrAsset(context: Context, fileName: String): InputStream {
        val localFile = File(dataDirectory(context), fileName)
        return if (hasUpdatedData(context) && localFile.isFile) {
            localFile.inputStream()
        } else {
            context.assets.open(fileName)
        }
    }

    private fun readBuiltInVersion(context: Context): ScoreDataVersion? = runCatching {
        context.assets.open(VERSION_FILE_NAME).use { input ->
            parseVersion(JSONObject(input.bufferedReader().readText()))
        }
    }.getOrNull()

    private fun hasUpdatedData(context: Context): Boolean {
        val directory = dataDirectory(context)
        return listOf(SONG_LIST_FILE_NAME, CONSTANTS_FILE_NAME, VERSION_FILE_NAME)
            .all { File(directory, it).isFile }
    }

    private fun fetchVersion(): ScoreDataVersion {
        val root = JSONObject(fetchJson("version"))
        check(root.optBoolean("success")) { "Score data version request was unsuccessful." }
        return parseVersion(root.getJSONObject("data"))
    }

    private fun parseVersion(json: JSONObject): ScoreDataVersion {
        val arcaeaVersion = json.optString("arcaea_version").takeIf { it.isNotBlank() }
        val updatedAt = json.getLong("updated_at")
        check(updatedAt > 0) { "Invalid score data update timestamp." }
        return ScoreDataVersion(arcaeaVersion = arcaeaVersion, updatedAt = updatedAt)
    }

    private fun fetchJson(path: String): String {
        val connection = (URL("$SCORES_API_BASE_URL/$path").openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
            val responseCode = connection.responseCode
            val response = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            check(responseCode in 200..299) {
                "Score data request failed with HTTP $responseCode: $response"
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun validateSongList(songList: String) {
        JSONObject(songList).getJSONArray("songs")
    }

    private fun validateConstants(constants: String) {
        JSONObject(constants)
    }

    private fun installData(
        context: Context,
        songList: String,
        constants: String,
        version: ScoreDataVersion
    ) {
        val destination = dataDirectory(context)
        val staging = File(context.filesDir, SCORE_DATA_STAGING_DIRECTORY)
        val backup = File(context.filesDir, SCORE_DATA_BACKUP_DIRECTORY)
        staging.deleteRecursively()
        if (backup.exists()) {
            check(backup.deleteRecursively()) { "Unable to remove score data backup." }
        }
        check(staging.mkdirs()) { "Unable to create score data staging directory." }

        try {
            File(staging, SONG_LIST_FILE_NAME).writeText(songList)
            File(staging, CONSTANTS_FILE_NAME).writeText(constants)
            File(staging, VERSION_FILE_NAME).writeText(version.toJson())

            val hadExistingData = destination.exists()
            if (hadExistingData) {
                check(destination.renameTo(backup)) { "Unable to back up existing score data." }
            }
            try {
                check(staging.renameTo(destination)) { "Unable to install score data." }
                if (backup.exists()) {
                    backup.deleteRecursively()
                }
            } catch (error: Throwable) {
                if (hadExistingData && !destination.exists()) {
                    backup.renameTo(destination)
                }
                throw error
            }
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun dataDirectory(context: Context): File = File(context.filesDir, SCORE_DATA_DIRECTORY)
}
