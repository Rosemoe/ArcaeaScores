package io.github.rosemoe.arcaeaScores.arc

private const val RANKED_SCORE_MULTIPLIER = 100.0

enum class ArcaeaScoreSortOrder {
    Potential,
    TheoreticalDistance,
    LostRankedScore,
    ChartConstant;

    fun comparator(): Comparator<ArcaeaScore> {
        val primaryComparator = when (this) {
            Potential -> compareByDescending<ArcaeaScore> { it.playPotential }
            TheoreticalDistance -> compareByDescending { it.theoreticalScoreDistance }
            LostRankedScore -> compareBy { it.lostRankedScore ?: Double.POSITIVE_INFINITY }
            ChartConstant -> compareByDescending { it.chartConstant }
        }
        return primaryComparator.thenByDescending { it.chartConstant }
    }
}

class ArcaeaScore(
    val songId: String,
    val difficulty: Int,
    val score: Long,
    val pureCount: Int,
    val maxPureCount: Int,
    val farCount: Int,
    val lostCount: Int,
    /**
     * 0 -> Track Lost
     * 1 -> Track Complete
     * 2 -> Full Recall
     * 3 -> Pure Memory
     * 4 -> Easy Clear
     * 5 -> Hard Clear
     */
    var clearType: Int,
    var chartConstant: Double,
    var playPotential: Double,
    var title: String,
    val chartInfo: ChartInfo?
) : Comparable<ArcaeaScore> {

    val theoreticalScoreDistance: Long
        get() = 10_000_000L + pureCount + farCount + lostCount - score

    /**
     * Potential loss relative to a maximally ranked score. This metric only applies to
     * Future, Beyond, and Eternal charts.
     */
    val lostRankedScore: Double?
        get() {
            if (difficulty < 2) {
                return null
            }

            val noteCount = pureCount + farCount + lostCount
            val pureLoss = if (noteCount == 0) {
                (0.995 - 0.0).coerceIn(0.0, 0.095)
            } else {
                (0.995 - maxPureCount.toDouble() / noteCount).coerceIn(0.0, 0.095)
            }
            val scoreLoss = 28.5 * (1 - score.toDouble() / 10_000_000.0).coerceIn(0.0, 0.01)
            return RANKED_SCORE_MULTIPLIER * chartConstant * (pureLoss + scoreLoss)
        }

    override fun compareTo(other: ArcaeaScore): Int {
        val cmp = playPotential.compareTo(other.playPotential)
        return if (cmp == 0) {
            songId.compareTo(other.songId)
        } else {
            cmp
        }
    }

}
