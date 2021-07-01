package io.github.rosemoe.arcaeaScores

import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.lang.StringBuilder

class ArcaeaTitles(songListJsonFile: File) {

    private val mapping = mutableMapOf<String, String>()

    init {
        var br: BufferedReader? = null
        try {
            br = BufferedReader(FileReader(songListJsonFile))
            var line = br.readLine()
            val sb = StringBuilder()
            while (line != null) {
                sb.append(line).append('\n')
                line = br.readLine()
            }
            br.close()
            val root = JSONObject(sb.toString())
            val songs = root.getJSONArray("songs")
            for (i in 0 until songs.length()) {
                val song = songs.getJSONObject(i)
                mapping.put(song.getString("id"), song.getJSONObject("title_localized").getString("en"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (br != null) {
                try {
                    br.close()
                } catch (e2: IOException) {
                    e2.printStackTrace()
                }
            }
            throw e
        }
    }

    fun queryForId(id: String) : String {
        return mapping[id]!!
    }

}