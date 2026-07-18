package io.github.rosemoe.arcaeaScores.app

import io.github.rosemoe.arcaeaScores.arc.ArcaeaScore

data class MainUiState(
    val playerName: String = "",
    val scores: List<ArcaeaScore> = emptyList(),
    val best30Potential: Double = 0.0,
    val maxPotential: Double = 0.0,
    val updateTime: Long = 0,
    val showArtwork: Boolean = true,
    val artworkDataVersion: String? = null,
    val completedArtworkUpdateVersion: String? = null,
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val showRootPermissionDialog: Boolean = false,
    val showNameDialog: Boolean = false,
    val error: String? = null
)
