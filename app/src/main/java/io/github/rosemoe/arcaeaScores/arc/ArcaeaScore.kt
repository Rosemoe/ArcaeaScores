package io.github.rosemoe.arcaeaScores.arc

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

    override fun compareTo(other: ArcaeaScore): Int {
        val cmp = playPotential.compareTo(other.playPotential)
        return if (cmp == 0) {
            songId.compareTo(other.songId)
        } else {
            cmp
        }
    }

}
