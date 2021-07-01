package io.github.rosemoe.arcaeaScores

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

/**
 *  Reads wiki constant page code
 */
class ArcaeaConstants constructor(constantsFile: File) {

    private val mapping = mutableMapOf<String, Constants>()

    init {
        var br: BufferedReader? = null
        try {
            br = BufferedReader(FileReader(constantsFile))
            var line = br.readLine()
            while (line != null) {
                val arr = line.split("||")
                if (arr.size >= 4) {
                    var title = arr[0].substring(3, arr[0].length - 2)
                    if (title.contains("|")) {
                        title = title.substring(title.indexOf("|") + 1)
                    }
                    mapping.put(
                        title.lowercase(),
                        Constants(
                            arr[3].toDouble(),
                            arr[2].toDouble(),
                            arr[1].toDouble(),
                            if (arr[4].isNotEmpty()) arr[4].toDouble() else 0.0
                        )
                    )
                }
                line = br.readLine()
            }
            br.close()
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

    fun queryForTitle(title: String, difficulty: Int) : Double {
        println(title)
        val const = mapping[title.lowercase()]!!
        return when (difficulty) {
            0-> const.past
            1 -> const.present
            2 -> const.future
            3 -> const.beyond
            else -> 0.0
        }
    }

}

data class Constants(val past: Double, val present: Double, val future: Double, val beyond: Double)