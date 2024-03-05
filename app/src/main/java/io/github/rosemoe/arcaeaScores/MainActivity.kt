package io.github.rosemoe.arcaeaScores

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private val titles: ArcaeaTitles by lazy {
        ArcaeaTitles(assets.open("songlist.json"))
    }
    private val constants: ArcaeaConstants by lazy {
        ArcaeaConstants(assets.open("constants.json"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        try {
            updateUi(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        findViewById<ListView>(R.id.score_list).dividerHeight = 0
        findViewById<FloatingActionButton>(R.id.fab).let {
            it.imageTintList = ColorStateList.valueOf(Color.WHITE)
            it.imageTintMode = PorterDuff.Mode.SRC_ATOP
            it.setOnClickListener {
                onUpdateScoreClicked()
            }
        }
        findViewById<TextView>(R.id.player_name).let {
            it.setOnClickListener {
                onSetNameClicked()
            }
            it.text = prefs.getString("player_name", "点我设置名字")
        }
    }

    private fun onUpdateScoreClicked() {
        if (prefs.getBoolean("agreeRootUsage", false)) {
            refreshScores()
        } else {
            AlertDialog.Builder(this)
                .setTitle("注意")
                .setMessage("需要获取Root权限才可以读取Arcaea的数据目录！\n如果你信任本应用，我们将向您申请Root权限以继续操作。")
                .setPositiveButton("同意") { _, _ ->
                    prefs.edit {
                        putBoolean("agreeRootUsage", true)
                    }
                    refreshScores()
                }
                .setNegativeButton("不同意") { _, _ ->
                    Toast.makeText(this, "不同意将无法查询成绩。", Toast.LENGTH_SHORT).show()
                }.show()
        }
    }

    private fun onSetNameClicked() {
        val et = EditText(this)
        et.layoutParams = ViewGroup.LayoutParams(-1, -1)
        AlertDialog.Builder(this)
            .setTitle("设置名字")
            .setView(et)
            .setPositiveButton("好") { _, _ ->
                if (et.text.isEmpty()) {
                    Toast.makeText(this, "名字不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                prefs.edit {
                    putString("player_name", et.text.toString())
                }
                findViewById<TextView>(R.id.player_name).text = et.text
            }
            .setNegativeButton("取消", null)
            .show()
    }

    @Suppress("DEPRECATION")
    private fun refreshScores() {
        val pd = ProgressDialog.show(this, "正在更新数据...", "...", true, false)
        fun update(text: String) {
            runOnUiThread {
                pd.setMessage(text)
            }
        }
        Thread {
            try {
                // Load local states
                val process = Runtime.getRuntime().exec("su -mm")
                update("获取Root...")
                BufferedWriter(OutputStreamWriter(process.outputStream)).apply {
                    write("mkdir /data/data/io.github.rosemoe.arcaeaScores/databases/\ncp -f /data/data/moe.low.arc/files/st3 /data/data/io.github.rosemoe.arcaeaScores/databases/st3.db\nchmod 777 /data/data/io.github.rosemoe.arcaeaScores/databases/\nexit\n")
                    flush()
                }
                update("获取Arcaea存档...")
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "读取失败\n错误输出:\n" + process.errorStream.reader().readText(),
                            Toast.LENGTH_SHORT
                        ).show()
                        pd.cancel()
                    }
                    return@Thread
                }
                runOnUiThread {
                    updateUi()
                    pd.cancel()
                    Toast.makeText(this, "已完成更新！", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "读取失败\nException:\n$e",
                        Toast.LENGTH_SHORT
                    ).show()
                    pd.cancel()
                }
            }
        }.start()
    }

    @SuppressLint("Range")
    private fun updateUi(fromStart: Boolean = false) {
        if (!fromStart) {
            val editor = prefs.edit()
            editor.putLong("date", System.currentTimeMillis())
            editor.apply()
        }
        val data = prefs.getLong("date", 0)
        Thread {
            try {
                openOrCreateDatabase(
                    "/data/data/io.github.rosemoe.arcaeaScores/databases/st3.db",
                    MODE_PRIVATE,
                    null
                ).use { db ->
                    val cursor = db.query(
                        "scores",
                        arrayOf(
                            "songId",
                            "songDifficulty",
                            "score",
                            "perfectCount",
                            "shinyPerfectCount",
                            "nearCount",
                            "missCount"
                        ),
                        null,
                        null,
                        null,
                        null,
                        null
                    )
                    val clearTypeCursor =
                        db.query("clearTypes", arrayOf("clearType"), null, null, null, null, null)
                    val list = mutableListOf<PlayResult>()
                    if (cursor.moveToFirst() && clearTypeCursor.moveToFirst()) {
                        do {
                            val result = PlayResult(
                                cursor.getString(cursor.getColumnIndex("songId")),
                                cursor.getInt(cursor.getColumnIndex("songDifficulty")),
                                cursor.getLong(cursor.getColumnIndex("score")),
                                cursor.getInt(cursor.getColumnIndex("perfectCount")),
                                cursor.getInt(cursor.getColumnIndex("shinyPerfectCount")),
                                cursor.getInt(cursor.getColumnIndex("nearCount")),
                                cursor.getInt(cursor.getColumnIndex("missCount"))
                            )
                            result.title = titles.queryForId(result.name)
                            result.constant = constants.queryForId(result.name, result.difficulty)
                            result.playPotential = when {
                                result.score > 10000000 -> result.constant + 2.0
                                result.score in 9800000..10000000 -> result.constant + 1.0 + (result.score - 9800000) / 200000.0
                                else -> max(
                                    0.0,
                                    result.constant + (result.score - 9500000) / 300000.0
                                )
                            }
                            result.clearType =
                                clearTypeCursor.getInt(clearTypeCursor.getColumnIndex("clearType"))
                            list.add(result)
                        } while (cursor.moveToNext() && clearTypeCursor.moveToNext())
                        list.sort()
                        list.reverse()
                    }
                    cursor.close()
                    clearTypeCursor.close()

                    var max = 0.0
                    var b30 = 0.0
                    for (i in 0 until 30.coerceAtMost(list.size)) {
                        max += if (i < 10) {
                            list[i].playPotential * 2
                        } else {
                            list[i].playPotential
                        }
                        b30 += list[i].playPotential
                    }
                    max /= 40.0
                    b30 /= 30.0
                    runOnUiThread {
                        findViewById<ListView>(R.id.score_list).adapter = ScoreAdapter(list)
                        findViewById<TextView>(R.id.date).text =
                            "Update Time：" + SimpleDateFormat.getDateTimeInstance(
                                SimpleDateFormat.DEFAULT,
                                SimpleDateFormat.DEFAULT,
                                Locale.ENGLISH
                            ).format(Date(data))
                        findViewById<TextView>(R.id.max_potential).text =
                            "Best30: " + BigDecimal(b30).setScale(2, RoundingMode.FLOOR)
                                .toPlainString() + "  Max Possible Ptt:" + BigDecimal(max).setScale(
                                2,
                                RoundingMode.FLOOR
                            ).toPlainString()
                    }
                }
            } catch (e: Exception) {
                if (!fromStart) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "读取失败了\n异常信息:\n$e",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }.start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                AlertDialog.Builder(this)
                    .setTitle("About ArcaeaScores")
                    .setMessage("Rosemoe开发的一个用Root读取Arcaea存档并计算Best30的工具。\n从这里获取更新：https://github.com/Rosemoe/ArcaeaScores/releases/latest/")
                    .setPositiveButton("好", null)
                    .show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    inner class ScoreAdapter(private val data: List<PlayResult>) :
        BaseAdapter() {

        override fun getCount(): Int {
            return data.size
        }

        override fun getItem(position: Int): PlayResult {
            return data[position]
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@MainActivity)
                .inflate(R.layout.list_item, parent, false)
            if (view.layoutParams == null) {
                view.layoutParams = RecyclerView.LayoutParams(-1, -2)
            }
            val i = getItem(position)
            view.findViewById<TextView>(R.id.songId).text = i.title

            val color = when (i.difficulty) {
                0 -> 0xFF3C95AC
                1 -> 0xFFB7C484
                2 -> 0xFF733064
                3 -> 0xFF941F38
                else -> Color.GRAY
            }.toInt()
            view.findViewById<View>(R.id.difficulty_color).setBackgroundColor(color)

            view.findViewById<TextView>(R.id.clearType).text = when (getItem(position).clearType) {
                0 -> "[TL]"
                1 -> "[TC]"
                2 -> "[FR]"
                3 -> "[PM]"
                4 -> "[EC]"
                5 -> "[HC]"
                else -> "[UNK]"
            }
            view.findViewById<TextView>(R.id.potential).text =
                "Potential: ${i.constant} > ${
                    String.format(
                        "%.5f",
                        getItem(position).playPotential
                    )
                }"
            view.findViewById<TextView>(R.id.score).text = getItem(position).score.toString()
            view.findViewById<TextView>(R.id.rank).text = "#${position + 1}"
            view.findViewById<TextView>(R.id.notes).text =
                "Pure:${i.pure} (+${i.maxPure}) Far:${i.far} Lost:${i.lost}"
            return view
        }
    }

}