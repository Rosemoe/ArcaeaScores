package io.github.rosemoe.arcaeaScores.arc

data class ArcaeaRecord(
    val scores: List<ArcaeaScore>,
    val best10Potential: Double,
    val best50Potential: Double,
    val playerPotential: Double
)
