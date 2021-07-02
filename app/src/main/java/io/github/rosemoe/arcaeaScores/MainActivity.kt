package io.github.rosemoe.arcaeaScores

import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 9999
            )
        }
        prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        try {
            openFileOutput("text.txt", MODE_PRIVATE).close()
        } catch (ignored: Exception) {

        }
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
                if (prefs.getBoolean("agreeRootUsage", false)) {
                    refreshScores()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("前方高能")
                        .setMessage("需要获取Root权限才可以读取Arcaea的数据目录！\n如果你信任本应用，我们将向您申请Root权限以继续操作。")
                        .setPositiveButton("爷同意") { _, _ ->
                            Toast.makeText(this, "好耶", Toast.LENGTH_SHORT).show()
                            val editor = prefs.edit()
                            editor.putBoolean("agreeRootUsage", true)
                            editor.apply()
                            refreshScores()
                        }
                        .setNegativeButton("去你妈的") { _, _ ->
                            Toast.makeText(this, "你妈的，关我屁事！！", Toast.LENGTH_SHORT).show()
                        }.show()
                }
            }
        }
        findViewById<TextView>(R.id.player_name).let {
            it.setOnClickListener {
                val et = EditText(this)
                et.layoutParams = ViewGroup.LayoutParams(-1, -1)
                AlertDialog.Builder(this)
                    .setTitle("设置名字")
                    .setView(et)
                    .setPositiveButton("好") { _, _ ->
                        if (et.text.isEmpty()) {
                            Toast.makeText(this, "这样不可以~", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        val editor = prefs.edit()
                        editor.putString("player_name", et.text.toString())
                        editor.apply()
                        (it as TextView).text = et.text
                    }
                    .setNegativeButton("不了", null)
                    .show()
            }
            it.text = prefs.getString("player_name", "点我设置名字")
        }
    }

    @Suppress("DEPRECATION")
    private fun refreshScores() {
        val pd = ProgressDialog.show(this, "少女祈祷中...", "...", true, false)
        fun update(text: String) {
            runOnUiThread {
                pd.setMessage(text)
            }
        }
        Thread {
            try {
                // Load local states
                val proc = Runtime.getRuntime().exec("su")
                update("获取Root...")
                BufferedWriter(OutputStreamWriter(proc.outputStream)).apply {
                    write("mkdir /data/data/io.github.rosemoe.arcaeaScores/databases/\ncp -f /data/data/moe.low.arc/files/st3 /data/data/io.github.rosemoe.arcaeaScores/databases/st3.db\nchmod 777 /data/data/io.github.rosemoe.arcaeaScores/databases/\nexit\n")
                    flush()
                }
                update("读取Arcaea存档...")
                readOutput(proc.inputStream, "Stdout")
                readOutput(proc.errorStream, "Stderr")
                val exitCode = proc.waitFor()
                if (exitCode != 0) {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "读取失败了QAQ\nError Stream:\n" + readStream(proc.errorStream),
                            Toast.LENGTH_SHORT
                        ).show()
                        pd.cancel()
                    }
                    return@Thread
                }
                runOnUiThread {
                    updateUi()
                    pd.cancel()
                    Toast.makeText(this, "完事！", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "读取失败了QAQ\nException:\n$e",
                        Toast.LENGTH_SHORT
                    ).show()
                    pd.cancel()
                }
            }
        }.start()
    }

    private fun ensureFiles() {
        val path = filesDir.absolutePath
        if (!File(path + "/songlist.json").exists()) {
            copyStreamTo(assets.open("builtinSongList.json"), File(path + "/songlist.json"))
        }
        if (!File(path + "/constants.txt").exists()) {
            copyStreamTo(assets.open("builtinConstants.txt"), File(path + "/constants.txt"))
        }
    }

    private fun copyStreamTo(stream: InputStream, file: File) {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
        val fw = BufferedOutputStream(FileOutputStream(file))
        val buffer = ByteArray(8192)
        var count = stream.read(buffer)
        while (count != -1) {
            fw.write(buffer, 0, count)
            count = stream.read(buffer)
        }
        fw.close()
        stream.close()
    }

    private fun readOutput(stream: InputStream, name: String) {
        Thread {
            val br = BufferedReader(InputStreamReader(stream))
            var line = br.readLine()
            while (line != null) {
                Log.e("ArcaeaScores", "$name: $line")
                line = br.readLine()
            }
        }.start()
    }

    var titles: ArcaeaTitles? = null
    var constants: ArcaeaConstants? = null

    fun createInfo() {
        val path = filesDir.absolutePath
        titles = titles ?: ArcaeaTitles(File(path + "/songlist.json"))
        constants = constants ?: ArcaeaConstants(File(path + "/constants.txt"))
    }

    private fun updateUi(fromStart: Boolean = false) {
        if (!fromStart) {
            val editor = prefs.edit()
            editor.putLong("date", System.currentTimeMillis())
            editor.apply()
        }
        val data = prefs.getLong("date", 0)
        Thread {
            try {
                ensureFiles()
                createInfo()
                val db = openOrCreateDatabase(
                    "/data/data/io.github.rosemoe.arcaeaScores/databases/st3.db",
                    MODE_PRIVATE,
                    null
                )
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
                val list = mutableListOf<PlayResult>()
                if (cursor.moveToFirst()) {
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
                        result.title = titles!!.queryForId(result.name)
                        result.constant = constants!!.queryForTitle(result.title, result.difficulty)
                        result.playPotential = when (result.score) {
                            in 10000000..20000000 -> result.constant + 2.0
                            in 9800000..10000000 -> result.constant + 1.0 + (result.score - 9800000) / 200000.0
                            else -> Math.max(
                                0.0,
                                result.constant + (result.score - 9500000) / 300000.0
                            )
                        }
                        list.add(result)
                    } while (cursor.moveToNext())
                    list.sort()
                    list.reverse()
                }
                cursor.close()
                var max = 0.0
                var b30 = 0.0
                for (i in 0 until Math.min(30, list.size)) {
                    if (i < 10) {
                        max += list[i].playPotential * 2
                    } else {
                        max += list[i].playPotential
                    }
                    b30 += list[i].playPotential
                }
                max /= 40.0
                b30 /= 30.0
                runOnUiThread {
                    findViewById<ListView>(R.id.score_list).adapter = Adp(list, this)
                    findViewById<TextView>(R.id.date).text =
                        "刷新时间：" + SimpleDateFormat.getDateTimeInstance().format(Date(data))
                    findViewById<TextView>(R.id.max_potential).text =
                        "Best30: " + BigDecimal(b30).setScale(2, RoundingMode.FLOOR)
                            .toPlainString() + "  最高可能的潜力值:" + BigDecimal(max).setScale(
                            2,
                            RoundingMode.FLOOR
                        ).toPlainString()
                }
            } catch (e: Exception) {
                if (!fromStart) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "读取失败了QAQ\nException:\n$e",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }.start()
    }

    class Adp constructor(private val data: List<PlayResult>, private val context: Context) :
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
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.list_item, null)
            if (view.layoutParams == null) {
                view.layoutParams = RecyclerView.LayoutParams(-1, -2)
            }
            view.findViewById<TextView>(R.id.songId).apply {
                text = getItem(position).title
                val color = when (getItem(position).difficulty) {
                    0 -> 0xFF3C95AC
                    1 -> 0xFFB7C484
                    2 -> 0xFF733064
                    3 -> 0xFF941F38
                    else -> Color.GRAY
                }.toInt()
                setTextColor(color)
            }
            view.findViewById<TextView>(R.id.potential).text =
                "单曲潜力值: ${String.format("%.5f", getItem(position).playPotential)}"
            view.findViewById<TextView>(R.id.score).text = getItem(position).score.toString()
            view.findViewById<TextView>(R.id.rank).text = "#${position + 1}"
            val i = getItem(position)
            view.findViewById<TextView>(R.id.notes).text =
                "Pure:${i.pure} (+${i.maxPure}) Far:${i.far} Lost:${i.lost}"
            return view
        }
    }


    private fun readStream(stream: InputStream): String {
        val sb = StringBuilder()
        val br = BufferedReader(InputStreamReader(stream))
        var line: String? = br.readLine()
        while (line != null) {
            sb.append(line).append('\n')
            line = br.readLine()
        }
        return sb.toString()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

}