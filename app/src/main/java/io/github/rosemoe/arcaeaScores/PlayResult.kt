package io.github.rosemoe.arcaeaScores

class PlayResult constructor(val name:String, val difficulty: Int, val score: Long, val pure: Int, val maxPure: Int, val far: Int, val lost: Int) : Comparable<PlayResult> {

    /**
     * -1 for not loaded
     */
    var clearType = -1

    var constant = 0.0

    var playPotential = 0.0

    var title: String = "Unk"

    override fun compareTo(other: PlayResult): Int {
        val cmp = compare(playPotential, other.playPotential)
        if (cmp == 0) {
            return name.compareTo(other.name)
        } else {
            return cmp
        }
    }

    fun compare(x: Double, y: Double): Int {
        return (if (x < y) -1 else if (x == y) 0 else 1)
    }


}