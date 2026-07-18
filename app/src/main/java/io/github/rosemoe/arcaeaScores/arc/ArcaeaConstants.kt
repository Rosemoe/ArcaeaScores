package io.github.rosemoe.arcaeaScores.arc

import org.json.JSONObject
import java.io.InputStream

class ArcaeaConstants(constantsFile: InputStream) {

    private val mapping = mutableMapOf<String, DoubleArray>()
    private val difficultyKeys = arrayOf("pst", "prs", "ftr", "byd", "etr")

    init {
        val json = JSONObject(constantsFile.reader().readText())
        for (key in json.keys()) {
            val data = json.getJSONObject(key)
            val const = DoubleArray(5) { 0.0 }
            for (i in 0 until 5) {
                const[i] = data.optDouble(difficultyKeys[i], 0.0)
            }
            mapping[key] = const
        }
    }

    fun queryForChart(id: String, difficulty: Int): Double {
        val const = mapping[id.lowercase()] ?: EMPTY_CONSTANT
        return if (difficulty in 0..4) {
            const[difficulty]
        } else {
            0.0
        }
    }

}

val EMPTY_CONSTANT = DoubleArray(5) { 0.0 }
