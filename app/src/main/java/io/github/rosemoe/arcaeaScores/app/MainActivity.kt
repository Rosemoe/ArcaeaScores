package io.github.rosemoe.arcaeaScores.app

import android.app.ProgressDialog
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.Editable
import android.text.util.Linkify
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.rosemoe.arcaeaScores.R
import io.github.rosemoe.arcaeaScores.arc.ArcaeaConstants
import io.github.rosemoe.arcaeaScores.arc.ArcaeaTitles
import io.github.rosemoe.arcaeaScores.arc.readDatabase
import io.github.rosemoe.arcaeaScores.util.showMsgDialog
import io.github.rosemoe.arcaeaScores.util.showToast
import io.github.rosemoe.arcaeaScores.util.toScaledString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        prefs = getSharedPreferences("prefs", MODE_PRIVATE)
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
            it.text = prefs.getString("player_name", getString(R.string.click_to_set_name))
        }
        updateScoreList()
    }

    private fun onUpdateScoreClicked() {
        if (prefs.getBoolean("agree_using_root", false)) {
            refreshScores()
        } else {
            AlertDialog.Builder(this)
                .setTitle("注意")
                .setMessage("需要获取 Root 权限才可以访问 Arcaea 的数据目录！\n如果您信任本应用，我们将向您申请Root权限以继续操作。")
                .setPositiveButton("同意") { _, _ ->
                    prefs.edit {
                        putBoolean("agree_using_root", true)
                    }
                    refreshScores()
                }
                .setNegativeButton("不同意") { _, _ ->
                    showToast("不同意将无法查询成绩。")
                }.show()
        }
    }

    private fun onSetNameClicked() {
        val et = EditText(this)
        et.layoutParams = ViewGroup.LayoutParams(-1, -1)
        AlertDialog.Builder(this)
            .setTitle("设置名字")
            .setView(et)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (et.text.isEmpty()) {
                    showToast("名字不能为空")
                } else {
                    prefs.edit {
                        putString("player_name", et.text.toString())
                    }
                    findViewById<TextView>(R.id.player_name).text = et.text
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    @Suppress("DEPRECATION")
    private fun refreshScores() {
        val pd = ProgressDialog.show(this, "正在更新数据...", "...", true, false)

        suspend fun update(text: String) = withContext(Dispatchers.Main) {
            pd.setMessage(text)
        }

        lifecycleScope.launch {
            runCatching {
                // Copy `st3` database
                val process = Runtime.getRuntime().exec("su -mm")
                update("获取Root...")
                process.outputStream.writer().use {
                    it.write("mkdir /data/data/io.github.rosemoe.arcaeaScores/databases/\ncp -f /data/data/moe.low.arc/files/st3 /data/data/io.github.rosemoe.arcaeaScores/databases/st3.db\nchmod 777 /data/data/io.github.rosemoe.arcaeaScores/databases/\nexit\n")
                    it.flush()
                }
                update("获取Arcaea存档...")
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw Exception("Non-zero exit code: $exitCode\nError Output:\n${process.errorStream.reader().readText()}")
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    prefs.edit {
                        putLong("date", System.currentTimeMillis())
                    }
                    updateScoreList()
                    pd.cancel()
                    showToast("已完成更新！")
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    showMsgDialog("读取失败", "异常信息：$it")
                    pd.cancel()
                }
            }
        }
    }

    private fun updateScoreList() {
        if (!getDatabasePath("st3.db").exists()) {
            return
        }
        val updateTime = prefs.getLong("date", 0)
        lifecycleScope.launch {
            runCatching {
                readDatabase(this@MainActivity)
            }.onSuccess { record ->
                withContext(Dispatchers.Main) {
                    findViewById<ListView>(R.id.score_list).adapter =
                        ArcaeaScoreAdapter(this@MainActivity, record.records)
                    findViewById<TextView>(R.id.date).text =
                        "Update Time: " + SimpleDateFormat.getDateTimeInstance(
                            SimpleDateFormat.DEFAULT,
                            SimpleDateFormat.DEFAULT,
                            Locale.ENGLISH
                        ).format(Date(updateTime))
                    findViewById<TextView>(R.id.max_potential).text =
                        "Best30: " + record.best30Potential.toScaledString() + "  Max Ptt: " + record.maxPotential.toScaledString()
                }
            }.onFailure {
                it.printStackTrace()
                withContext(Dispatchers.Main) {
                    showMsgDialog("读取失败", "异常信息：$it")
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val text = Editable.Factory.getInstance().newEditable(
                    "Rosemoe开发的一个用 Root 读取 Arcaea 存档并计算 Best30 的工具。\n\n" +
                            "从这里获取更新：https://github.com/Rosemoe/ArcaeaScores/releases/latest/"
                )
                showMsgDialog("About", text).apply {
                    findViewById<TextView>(android.R.id.message).apply {
                        autoLinkMask = Linkify.WEB_URLS
                        isClickable = true
                        linksClickable = true
                        this.text = text
                    }
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

}