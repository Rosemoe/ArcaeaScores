package io.github.rosemoe.arcaeaScores.arc

import android.graphics.Color
import kotlin.math.max

fun clearTypeShortString(clearType: Int): String = when (clearType) {
    0 -> "TL" // Track Lost
    1 -> "TC" // Track Complete
    2 -> "FR" // Full Recall
    3 -> "PM" // Pure Memory
    4 -> "EC" // Easy Clear
    5 -> "HC" // Hard Clear
    else -> "UNK"
}

private val ScoreLimits = arrayOf(9900000L, 9800000L, 9500000L, 9200000L, 8900000L, 8600000L)
private val ScoreGrades = arrayOf("EX+", "EX", "AA", "A", "B", "C", "D")

fun scoreGrade(score: Long) : String {
    for (i in ScoreLimits.indices) {
        if (score >= ScoreLimits[i]) {
            return ScoreGrades[i]
        }
    }
    return ScoreGrades.last()
}

fun difficultyMainColor(difficulty: Int): Int = when (difficulty) {
    0 -> 0xFF3C95AC
    1 -> 0xFFB7C484
    2 -> 0xFF8B4A79
    3 -> 0xFF941F38
    4 -> 0xFF9D87B3
    else -> Color.GRAY
}.toInt()

fun calculatePlayPotential(constant: Double, score: Long) = when {
    score > 10000000 -> constant + 2.0
    score in 9800000..10000000 ->
        constant + 1.0 + (score - 9800000) / 200000.0

    else -> max(
        0.0,
        constant + (score - 9500000) / 300000.0
    )
}

fun toScoreText(score: Long): String {
    val text = score.toString().padStart(8, '0')
    return text.substring(0, 2) + "'" + text.substring(2, 5) + "'" + text.substring(5)
}