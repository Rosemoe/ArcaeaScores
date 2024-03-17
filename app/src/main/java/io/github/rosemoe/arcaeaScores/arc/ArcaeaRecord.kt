package io.github.rosemoe.arcaeaScores.arc

data class ArcaeaRecord(
    val records: List<ArcaeaPlayResult>,
    val maxPotential: Double,
    val best30Potential: Double
)