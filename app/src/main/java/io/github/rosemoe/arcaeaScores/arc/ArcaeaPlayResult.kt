package io.github.rosemoe.arcaeaScores.arc

class ArcaeaPlayResult(
    val name: String,
    val difficulty: Int,
    val score: Long,
    val pure: Int,
    val maxPure: Int,
    val far: Int,
    val lost: Int
) : Comparable<ArcaeaPlayResult> {

    /**
     * -1 -> Not Loaded
     * 0 -> Track Lost
     * 1 -> Track Complete
     * 2 -> Full Recall
     * 3 -> Pure Memory
     * 4 -> Easy Clear
     * 5 -> Hard Clear
     */
    var clearType = -1

    var constant = 0.0

    var playPotential = 0.0

    var title: String = "Unk"

    override fun compareTo(other: ArcaeaPlayResult): Int {
        val cmp = playPotential.compareTo(other.playPotential)
        return if (cmp == 0) {
            name.compareTo(other.name)
        } else {
            cmp
        }
    }


}