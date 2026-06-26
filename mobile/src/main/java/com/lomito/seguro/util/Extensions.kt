package com.lomito.seguro.util

import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

fun View.visible() { visibility = View.VISIBLE }
fun View.gone() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun Fragment.toast(msg: String) {
    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}

fun String.formatTimestamp(): String {
    return try {
        val inputFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFmt.timeZone = TimeZone.getTimeZone("UTC")
        val outputFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = inputFmt.parse(this)
        outputFmt.format(date ?: Date())
    } catch (e: Exception) { this }
}

fun distanciaLabel(metros: Int): String = when {
    metros < 10 -> "Muy cerca"
    metros < 30 -> "${metros}m"
    metros < 100 -> "${metros}m ⚠️"
    else -> "${metros}m 🚨"
}
