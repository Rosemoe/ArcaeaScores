package io.github.rosemoe.arcaeaScores.arc

data class ArcaeaRecord(
    val scores: List<ArcaeaScore>,
    val maxPotential: Double,
    val best30Potential: Double
)