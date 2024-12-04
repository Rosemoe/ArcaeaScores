package io.github.rosemoe.arcaeaScores.arc

import org.json.JSONObject
import java.io.InputStream

class ArcaeaTitles(songListJsonFile: InputStream) {

    private val mapping = mutableMapOf<String, String>()

    init {
        val root = JSONObject(songListJsonFile.reader().readText())
        val songs = root.getJSONArray("songs")
        for (i in 0 until songs.length()) {
            val song = songs.getJSONObject(i)
            if (song.has("deleted") && song.getBoolean("deleted")) {
                // ignore deleted songs
                continue
            }
            mapping[song.getString("id")] = song.getJSONObject("title_localized").getString("en")
        }
    }

    fun queryForId(id: String) : String {
        return mapping[id] ?: id
    }

}