// Paquete: com.lomito.seguro.util
package com.lomito.seguro.util

// Importa componentes de la interfaz gráfica
import android.view.View
// Importa la dependencia necesaria: Toast
import android.widget.Toast
// Importa la clase Fragment de AndroidX
import androidx.fragment.app.Fragment
// Importa el cliente Retrofit para peticiones HTTP
import com.lomito.seguro.data.api.RetrofitClient
// Importa la dependencia necesaria: SimpleDateFormat
import java.text.SimpleDateFormat
// Importa la dependencia necesaria: *
import java.util.*

/**
 * El backend guarda foto_url como ruta relativa (ej: "/uploads/123.jpg").
 * Glide/Coil necesitan una URL absoluta para poder cargarla, así que la
 * resolvemos contra el host del servidor. Si ya viene absoluta (http/https)
 * se regresa tal cual.
 */
// Función String: define la lógica de esta operación
fun String?.toAbsoluteUrl(): String? {
    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
    if (this.isNullOrEmpty()) return null
    // Retorna el valor al llamador de la función
    return if (startsWith("http://") || startsWith("https://")) this
    // Accede al cliente Retrofit singleton para realizar peticiones de red
    else RetrofitClient.SERVER_URL + (if (startsWith("/")) this else "/$this")
}

// Función View: define la lógica de esta operación
fun View.visible() { visibility = View.VISIBLE }
// Función View: define la lógica de esta operación
fun View.gone() { visibility = View.GONE }
// Función View: define la lógica de esta operación
fun View.invisible() { visibility = View.INVISIBLE }

// Función Fragment: define la lógica de esta operación
fun Fragment.toast(msg: String) {
    // Muestra un mensaje emergente breve al usuario
    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}

// Función String: define la lógica de esta operación
fun String.formatTimestamp(): String {
    // Retorna el valor al llamador de la función
    return try {
        // Constante inputFmt: valor inmutable que no cambia tras su asignación
        val inputFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFmt.timeZone = TimeZone.getTimeZone("UTC")
        // Constante outputFmt: valor inmutable que no cambia tras su asignación
        val outputFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        // Constante date: valor inmutable que no cambia tras su asignación
        val date = inputFmt.parse(this)
        outputFmt.format(date ?: Date())
    } catch (e: Exception) { this }
}

// Función distanciaLabel: define la lógica de esta operación
fun distanciaLabel(metros: Int): String = when {
    metros < 10 -> "Muy cerca"
    metros < 30 -> "${metros}m"
    metros < 100 -> "${metros}m ⚠️"
    else -> "${metros}m 🚨"
}
