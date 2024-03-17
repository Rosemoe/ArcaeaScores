package io.github.rosemoe.arcaeaScores.util

import java.math.BigDecimal
import java.math.RoundingMode

fun Double.toScaledString(count: Int = 2): String = BigDecimal(this).setScale(
    2,
    RoundingMode.FLOOR
).toPlainString()