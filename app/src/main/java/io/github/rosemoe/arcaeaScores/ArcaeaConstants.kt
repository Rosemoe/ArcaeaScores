package io.github.rosemoe.arcaeaScores

import org.json.JSONObject
import java.io.InputStream

class ArcaeaConstants(constantsFile: InputStream) {

    private val mapping = mutableMapOf<String, DoubleArray>()

    init {
        val json = JSONObject(constantsFile.reader().readText())
        for (key in json.keys()) {
            val data = json.getJSONArray(key)
            val const = DoubleArray(5) { 0.0 }
            for (i in 0 until 5) {
                if (i < data.length())
                    const[i] = (data.get(i) as? JSONObject)?.getDouble("constant") ?: 0.0
            }
            mapping[key] = const
        }
    }

    fun queryForId(id: String, difficulty: Int): Double {
         val const = mapping[id.lowercase()] ?: EMPTY_CONSTANT
        return if (difficulty in 0..4) {
            const[difficulty]
        } else {
            0.0
        }
    }

}

val EMPTY_CONSTANT = DoubleArray(5) { 0.0 }