package io.github.rosemoe.arcaeaScores.arc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArcaeaScoreTest {

    @Test
    fun lostRankedScore_isUnavailableBelowFutureDifficulty() {
        assertNull(score(difficulty = 1).lostRankedScore)
    }

    @Test
    fun lostRankedScore_usesShinyPureAndScoreLoss() {
        val score = score(
            difficulty = 2,
            pureCount = 1_000,
            maxPureCount = 990,
            score = 10_000_000
        )

        assertEquals(5.0, requireNotNull(score.lostRankedScore), 0.000_001)
    }

    @Test
    fun lostRankedScore_usesFallbackWhenNoteCountIsUnavailable() {
        val score = score(difficulty = 2, score = 9_900_000)

        assertEquals(380.0, requireNotNull(score.lostRankedScore), 0.000_001)
    }

    @Test
    fun lostRankedScore_isZeroForAnAllShinyPerfectScore() {
        val score = score(
            difficulty = 2,
            pureCount = 1_000,
            maxPureCount = 1_000
        )

        assertEquals(0.0, requireNotNull(score.lostRankedScore), 0.0)
    }

    @Test
    fun theoreticalScoreDistance_includesEveryNote() {
        val score = score(
            difficulty = 2,
            score = 9_999_000,
            pureCount = 900,
            farCount = 80,
            lostCount = 20
        )

        assertEquals(2_000L, score.theoreticalScoreDistance)
    }

    private fun score(
        difficulty: Int,
        score: Long = 10_000_000,
        pureCount: Int = 0,
        maxPureCount: Int = 0,
        farCount: Int = 0,
        lostCount: Int = 0
    ) = ArcaeaScore(
        songId = "example",
        difficulty = difficulty,
        score = score,
        pureCount = pureCount,
        maxPureCount = maxPureCount,
        farCount = farCount,
        lostCount = lostCount,
        clearType = 0,
        chartConstant = 10.0,
        playPotential = 0.0,
        title = "Example",
        chartInfo = null
    )
}
