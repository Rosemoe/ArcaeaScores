package io.github.rosemoe.arcaeaScores.arc

import android.annotation.SuppressLint
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

@SuppressLint("Range")
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
        val list = mutableListOf<ArcaeaPlayResult>()
        if (cursor.moveToFirst() && clearTypeCursor.moveToFirst()) {
            do {
                val result = ArcaeaPlayResult(
                    cursor.getString(cursor.getColumnIndex("songId")),
                    cursor.getInt(cursor.getColumnIndex("songDifficulty")),
                    cursor.getLong(cursor.getColumnIndex("score")),
                    cursor.getInt(cursor.getColumnIndex("perfectCount")),
                    cursor.getInt(cursor.getColumnIndex("shinyPerfectCount")),
                    cursor.getInt(cursor.getColumnIndex("nearCount")),
                    cursor.getInt(cursor.getColumnIndex("missCount"))
                )
                result.title = titles.queryForId(result.name)
                result.constant = constants.queryForId(result.name, result.difficulty)
                result.playPotential = calculatePlayPotential(result.constant, result.score)
                result.clearType =
                    clearTypeCursor.getInt(clearTypeCursor.getColumnIndex("clearType"))
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