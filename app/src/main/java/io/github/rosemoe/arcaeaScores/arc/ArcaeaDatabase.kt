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
    val titles = ScoreDataFiles.openSongList(context).use(::SongList)
    val constants = ScoreDataFiles.openConstants(context).use(::ArcaeaConstants)
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
                val clearType = cursor.getInt(cursor.getColumnIndexOrThrow("clearType"))
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
                        artist = titles.queryArtist(songId),
                        side = titles.querySide(songId),
                        releaseDate = titles.queryReleaseDate(songId, difficulty),
                        chartInfo = chartInfo,
                        artworkPaths = titles.queryForJacketPaths(
                            songsDirectory = File(context.filesDir, "songs"),
                            songId = songId,
                            difficulty = difficulty
                        ).map(File::getAbsolutePath),
                        chartConstant = chartConstant,
                        playPotential = if (chartConstant > 0.0) {
                            calculatePlayPotential(chartConstant, score, clearType)
                        } else {
                            0.0
                        },
                        clearType = clearType
                    )
                )
            }
        }
        list.sortDescending()

        val best10Total = list.asSequence().take(10).sumOf { it.playPotential }
        val best50Total = list.asSequence().take(50).sumOf { it.playPotential }
        return ArcaeaRecord(
            scores = list,
            best10Potential = best10Total / 10.0,
            best50Potential = best50Total / 50.0,
            playerPotential = (best10Total + best50Total) / 60.0
        )
    }
}
