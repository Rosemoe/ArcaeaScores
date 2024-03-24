package io.github.rosemoe.arcaeaScores.util

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast

fun Context.showMsgDialog(title: CharSequence, content: CharSequence): AlertDialog =
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(content)
        .setPositiveButton(android.R.string.ok, null)
        .show()

fun Context.showToast(msg: CharSequence, length: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, msg, length).show()

fun Context.showToast(resId: Int, length: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, this.getString(resId), length).show()