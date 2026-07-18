package io.github.rosemoe.arcaeaScores.arc

import android.content.Context
import java.io.File

private val ScoreQuery = """
    SELECT
        scores.songId,
        scores.songDifficulty,
        scores.score,
        scores.perfectCount,
        scores.shinyPerfectCount,
        scores.nearCount,
        scores.missCount,
        COALESCE(clearTypes.clearType, 0) AS clearType
    FROM scores
    LEFT JOIN clearTypes
        ON scores.songId = clearTypes.songId
        AND scores.songDifficulty = clearTypes.songDifficulty
""".trimIndent()

fun readDatabase(context: Context): ArcaeaRecord {
    val titles = SongList(context.assets.open("songlist.json"))
    val constants = ArcaeaConstants(context.assets.open("constants.json"))
    context.openOrCreateDatabase(
        "st3.db",
        Context.MODE_PRIVATE,
        null
    ).use { db ->
        val list = mutableListOf<ArcaeaScore>()
        db.rawQuery(ScoreQuery, null).use { cursor ->
            while (cursor.moveToNext()) {
                val songId = cursor.getString(cursor.getColumnIndexOrThrow("songId"))
                val difficulty = cursor.getInt(cursor.getColumnIndexOrThrow("songDifficulty"))
                val chartConstant = constants.queryForChart(songId, difficulty)
                val score = cursor.getLong(cursor.getColumnIndexOrThrow("score"))
                val chartInfo = titles.queryForChartInfo(songId, difficulty)
                list.add(
                    ArcaeaScore(
                        songId = songId,
                        difficulty = difficulty,
                        score = score,
                        pureCount = cursor.getInt(cursor.getColumnIndexOrThrow("perfectCount")),
                        maxPureCount = cursor.getInt(cursor.getColumnIndexOrThrow("shinyPerfectCount")),
                        farCount = cursor.getInt(cursor.getColumnIndexOrThrow("nearCount")),
                        lostCount = cursor.getInt(cursor.getColumnIndexOrThrow("missCount")),
                        title = chartInfo?.title ?: titles.queryForChart(songId, difficulty),
                        chartInfo = chartInfo,
                        artworkPaths = titles.queryForJacketPaths(
                            songsDirectory = File(context.filesDir, "songs"),
                            songId = songId,
                            difficulty = difficulty
                        ).map(File::getAbsolutePath),
                        chartConstant = chartConstant,
                        playPotential = if (chartConstant > 0.0) {
                            calculatePlayPotential(chartConstant, score)
                        } else {
                            0.0
                        },
                        clearType = cursor.getInt(cursor.getColumnIndexOrThrow("clearType"))
                    )
                )
            }
        }
        list.sortDescending()

        val b30 = list.asSequence().take(30).sumOf { it.playPotential } / 30.0
        val max = (b30 * 30.0 + list.asSequence().take(10).sumOf { it.playPotential }) / 40.0
        return ArcaeaRecord(list, max, b30)
    }
}
