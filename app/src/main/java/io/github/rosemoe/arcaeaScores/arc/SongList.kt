package io.github.rosemoe.arcaeaScores.arc

import org.json.JSONObject
import java.io.File
import java.io.InputStream

data class ChartInfo(
    val title: String?,
    val rating: Int?,
    val ratingPlus: Boolean,
    val jacketOverride: Boolean,
    val chartDesigner: String?,
    val jacketDesigner: String?,
    val releaseDate: Long?
) {
    val displayRating: String?
        get() = rating?.let { "$it${if (ratingPlus) "+" else ""}" }
}

private data class SongInfo(
    val remoteDl: Boolean,
    val artist: String?,
    val side: Int?,
    val releaseDate: Long?
)

class SongList(songListJson: InputStream) {

    private val songTitles = mutableMapOf<String, String>()
    private val songInfo = mutableMapOf<String, SongInfo>()
    private val chartTitles = mutableMapOf<Pair<String, Int>, String>()
    private val chartInfo = mutableMapOf<Pair<String, Int>, ChartInfo>()

    init {
        val root = JSONObject(songListJson.reader().readText())
        val songs = root.getJSONArray("songs")
        for (i in 0 until songs.length()) {
            val song = songs.getJSONObject(i)
            if (song.has("deleted") && song.getBoolean("deleted")) {
                // ignore deleted songs
                continue
            }
            val songId = song.getString("id")
            songTitles[songId] = song.getJSONObject("title_localized").getString("en")
            songInfo[songId] = SongInfo(
                remoteDl = song.optBoolean("remote_dl"),
                artist = song.optString("artist").takeIf { it.isNotBlank() },
                side = song.takeIf { it.has("side") }?.getInt("side"),
                releaseDate = song.takeIf { it.has("date") }?.getLong("date")
            )

            val difficulties = song.getJSONArray("difficulties")
            for (difficultyIndex in 0 until difficulties.length()) {
                val difficulty = difficulties.getJSONObject(difficultyIndex)
                val chartKey = songId to difficulty.getInt("ratingClass")
                val title = difficulty.optJSONObject("title_localized")?.optString("en")
                if (!title.isNullOrBlank()) {
                    chartTitles[chartKey] = title
                }
                chartInfo[chartKey] = ChartInfo(
                    title = title?.takeIf { it.isNotBlank() },
                    rating = difficulty.takeIf { it.has("rating") }?.getInt("rating"),
                    ratingPlus = difficulty.optBoolean("ratingPlus"),
                    jacketOverride = difficulty.optBoolean("jacketOverride"),
                    chartDesigner = difficulty.optString("chartDesigner").takeIf { it.isNotBlank() },
                    jacketDesigner = difficulty.optString("jacketDesigner").takeIf { it.isNotBlank() },
                    releaseDate = difficulty.takeIf { it.has("date") }?.getLong("date")
                )
            }
        }
    }

    fun queryForSongId(id: String): String {
        return songTitles[id] ?: id
    }

    fun queryForChart(songId: String, difficulty: Int): String {
        return chartTitles[songId to difficulty] ?: queryForSongId(songId)
    }

    fun queryForChartInfo(songId: String, difficulty: Int): ChartInfo? {
        return chartInfo[songId to difficulty]
    }

    fun queryArtist(songId: String): String? = songInfo[songId]?.artist

    fun querySide(songId: String): Int? = songInfo[songId]?.side

    fun queryReleaseDate(songId: String, difficulty: Int): Long? {
        return chartInfo[songId to difficulty]?.releaseDate ?: songInfo[songId]?.releaseDate
    }

    fun queryForJacketPaths(songsDirectory: File, songId: String, difficulty: Int): List<File> {
        val resourceDirectory = File(
            songsDirectory,
            if (songInfo[songId]?.remoteDl == true) "dl_$songId" else songId
        )
        val imageName = buildString {
            append("1080_")
            append(if (chartInfo[songId to difficulty]?.jacketOverride == true) difficulty else "base")
            append("_256.jpg")
        }
        return listOf(
            File(resourceDirectory, imageName),
            File(resourceDirectory, imageName.removePrefix("1080_"))
        ).distinct()
    }

}
