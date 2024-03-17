package io.github.rosemoe.arcaeaScores.app

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.rosemoe.arcaeaScores.R
import io.github.rosemoe.arcaeaScores.arc.ArcaeaPlayResult
import io.github.rosemoe.arcaeaScores.arc.clearTypeShortString
import io.github.rosemoe.arcaeaScores.arc.difficultyMainColor

class ArcaeaScoreAdapter(private val activity: Activity, private val data: List<ArcaeaPlayResult>) :
    BaseAdapter() {

    override fun getCount() = data.size

    override fun getItem(position: Int) = data[position]

    override fun getItemId(position: Int) = 0L

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(activity)
            .inflate(R.layout.list_item, parent, false)
        if (view.layoutParams == null) {
            view.layoutParams = RecyclerView.LayoutParams(-1, -2)
        }
        val i = getItem(position)
        view.findViewById<TextView>(R.id.songId).text = i.title

        view.findViewById<View>(R.id.difficulty_color).setBackgroundColor(difficultyMainColor(i.difficulty))

        view.findViewById<TextView>(R.id.clearType).text = clearTypeShortString(i.clearType)
        view.findViewById<TextView>(R.id.potential).text =
            "Potential: ${i.constant} > ${
                String.format(
                    "%.5f",
                    i.playPotential
                )
            }"
        view.findViewById<TextView>(R.id.score).text = i.score.toString()
        view.findViewById<TextView>(R.id.rank).text = "#${position + 1}"
        view.findViewById<TextView>(R.id.notes).text =
            "P/F/L: ${i.pure}(+${i.maxPure}) / ${i.far} / ${i.lost}"
        return view
    }
}