package io.github.rosemoe.arcaeaScores.arc

import android.graphics.Color
import kotlin.math.max

fun clearTypeShortString(clearType: Int): String = when (clearType) {
    0 -> "[TL]"
    1 -> "[TC]"
    2 -> "[FR]"
    3 -> "[PM]"
    4 -> "[EC]"
    5 -> "[HC]"
    else -> "[UNK]"
}

fun difficultyMainColor(difficulty: Int): Int = when (difficulty) {
    0 -> 0xFF3C95AC
    1 -> 0xFFB7C484
    2 -> 0xFF733064
    3 -> 0xFF941F38
    4 -> 0xFF503B6A
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