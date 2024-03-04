package io.github.rosemoe.arcaeaScores

import org.json.JSONObject
import java.io.InputStream

class ArcaeaConstants(constantsFile: InputStream) {

    private val mapping = mutableMapOf<String, Constants>()

    init {
        val json = JSONObject(constantsFile.reader().readText())
        for (key in json.keys()) {
            val data = json.getJSONArray(key)
            if (key == "lasteternity") {
                mapping[key] = Constants(0.0, 0.0, 0.0, data.getJSONObject(3)?.getDouble("constant") ?: 0.0)
            } else {
                mapping[key] = Constants(
                    data.getJSONObject(0)?.getDouble("constant") ?: 0.0,
                    data.getJSONObject(1)?.getDouble("constant") ?: 0.0,
                    data.getJSONObject(2)?.getDouble("constant") ?: 0.0,
                    if (data.length() > 3) data.getJSONObject(3)?.getDouble("constant")
                        ?: 0.0 else 0.0,
                )
            }
        }
    }

    fun queryForId(id: String, difficulty: Int) : Double {
        val const = mapping[id.lowercase()] ?: EMPTY_CONSTANT
        return when (difficulty) {
            0-> const.past
            1 -> const.present
            2 -> const.future
            3 -> const.beyond
            else -> 0.0
        }
    }

}
val EMPTY_CONSTANT = Constants(0.0, 0.0, 0.0, 0.0)
data class Constants(val past: Double, val present: Double, val future: Double, val beyond: Double)