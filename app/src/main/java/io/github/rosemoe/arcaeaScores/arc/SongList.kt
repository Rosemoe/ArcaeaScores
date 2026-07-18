package io.github.rosemoe.arcaeaScores.arc

import org.json.JSONObject
import java.io.InputStream

class ArcaeaTitles(songListJsonFile: InputStream) {

    private val songTitles = mutableMapOf<String, String>()
    private val chartTitles = mutableMapOf<Pair<String, Int>, String>()

    init {
        val root = JSONObject(songListJsonFile.reader().readText())
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
                val title = difficulty.optJSONObject("title_localized")?.optString("en")
                if (!title.isNullOrBlank()) {
                    chartTitles[songId to difficulty.getInt("ratingClass")] = title
                }
            }
        }
    }

    fun queryForSongId(id: String): String {
        return songTitles[id] ?: id
    }

    fun queryForChart(songId: String, difficulty: Int): String {
        return chartTitles[songId to difficulty] ?: queryForSongId(songId)
    }

}
