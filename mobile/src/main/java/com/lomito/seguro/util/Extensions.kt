package com.lomito.seguro.util

import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.lomito.seguro.data.api.RetrofitClient
import java.text.SimpleDateFormat
import java.util.*

/**
 * El backend guarda foto_url como ruta relativa (ej: "/uploads/123.jpg").
 * Glide/Coil necesitan una URL absoluta para poder cargarla, así que la
 * resolvemos contra el host del servidor. Si ya viene absoluta (http/https)
 * se regresa tal cual.
 */
fun String?.toAbsoluteUrl(): String? {
    if (this.isNullOrEmpty()) return null
    return if (startsWith("http://") || startsWith("https://")) this
    else RetrofitClient.SERVER_URL + (if (startsWith("/")) this else "/$this")
}

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
