package io.github.rosemoe.arcaeaScores.arc

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

private val ScoreQueryColumns = arrayOf(
    "songId",
    "songDifficulty",
    "score",
    "perfectCount",
    "shinyPerfectCount",
    "nearCount",
    "missCount"
)

fun readDatabase(context: Context): ArcaeaRecord {
    val titles = ArcaeaTitles(context.assets.open("songlist.json"))
    val constants = ArcaeaConstants(context.assets.open("constants.json"))
    context.openOrCreateDatabase(
        "st3.db",
        AppCompatActivity.MODE_PRIVATE,
        null
    ).use { db ->
        val cursor =
            db.query("scores", ScoreQueryColumns, null, null, null, null, null)
        val clearTypeCursor =
            db.query("clearTypes", arrayOf("clearType"), null, null, null, null, null)
        val list = mutableListOf<ArcaeaScore>()
        if (cursor.moveToFirst() && clearTypeCursor.moveToFirst()) {
            do {
                val songId = cursor.getString(cursor.getColumnIndexOrThrow("songId"))
                val difficulty = cursor.getInt(cursor.getColumnIndexOrThrow("songDifficulty"))
                val chartConstant = constants.queryForChart(songId, difficulty)
                val score = cursor.getLong(cursor.getColumnIndexOrThrow("score"))
                val result = ArcaeaScore(
                    songId = songId,
                    difficulty = difficulty,
                    score = score,
                    pureCount = cursor.getInt(cursor.getColumnIndexOrThrow("perfectCount")),
                    maxPureCount = cursor.getInt(cursor.getColumnIndexOrThrow("shinyPerfectCount")),
                    farCount = cursor.getInt(cursor.getColumnIndexOrThrow("nearCount")),
                    lostCount = cursor.getInt(cursor.getColumnIndexOrThrow("missCount")),
                    title = titles.queryForChart(songId, difficulty),
                    chartConstant = chartConstant,
                    playPotential = if (chartConstant > 0.0) {
                        calculatePlayPotential(chartConstant, score)
                    } else {
                        0.0
                    },
                    clearType =
                        clearTypeCursor.getInt(clearTypeCursor.getColumnIndexOrThrow("clearType"))
                )
                list.add(result)
            } while (cursor.moveToNext() && clearTypeCursor.moveToNext())
            list.sort()
            list.reverse()
        }
        cursor.close()
        clearTypeCursor.close()

        val b30 = list.asSequence().take(30).sumOf { it.playPotential } / 30.0
        val max = (b30 * 30.0 + list.asSequence().take(10).sumOf { it.playPotential }) / 40.0
        return ArcaeaRecord(list, max, b30)
    }
}
