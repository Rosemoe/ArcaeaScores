package io.github.rosemoe.arcaeaScores.arc

import org.json.JSONObject
import java.io.InputStream

data class ChartInfo(
    val title: String?,
    val rating: Int?,
    val ratingPlus: Boolean
) {
    val displayRating: String?
        get() = rating?.let { "$it${if (ratingPlus) "+" else ""}" }
}

class SongList(songListJson: InputStream) {

    private val songTitles = mutableMapOf<String, String>()
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
                    ratingPlus = difficulty.optBoolean("ratingPlus")
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

}
