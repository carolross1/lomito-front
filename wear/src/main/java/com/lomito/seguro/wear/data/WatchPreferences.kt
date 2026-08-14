// wear/data/WatchPreferences.kt
// Paquete: com.lomito.seguro.wear.data
package com.lomito.seguro.wear.data

// Importa el contexto de Android
import android.content.Context
// Importa SharedPreferences para persistencia local
import android.content.SharedPreferences

/**
 * [Gestor de preferencias del reloj]
 *
 * Responsabilidades:
 * - [Guardar y recuperar datos locales como mascota activa, umbral y usuario]
 * - [Proveer una interfaz simplificada para acceder a SharedPreferences en la app de Wear OS]
 */
// Declaración de la clase WatchPreferences
class WatchPreferences(context: Context) {
    // Instancia de SharedPreferences utilizada para guardar datos locales
    // Constante prefs: valor inmutable que no cambia tras su asignación
    private val prefs: SharedPreferences = context.getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)

    // Constantes utilizadas como claves para guardar los valores en SharedPreferences
    companion object {
        // Clave para guardar el ID de la mascota activa
        // Constante KEY_MASCOTA_ACTIVA_ID: valor fijo definido en tiempo de compilación
        private const val KEY_MASCOTA_ACTIVA_ID = "mascota_activa_id"
        // Clave para guardar el nombre de la mascota activa
        // Constante KEY_MASCOTA_ACTIVA_NOMBRE: valor fijo definido en tiempo de compilación
        private const val KEY_MASCOTA_ACTIVA_NOMBRE = "mascota_activa_nombre"
        // Clave para guardar la distancia umbral de la mascota
        // Constante KEY_MASCOTA_UMBRAL: valor fijo definido en tiempo de compilación
        private const val KEY_MASCOTA_UMBRAL = "mascota_umbral"
        // Clave para guardar el ID del usuario
        // Constante KEY_USER_ID: valor fijo definido en tiempo de compilación
        private const val KEY_USER_ID = "user_id"
    }

    /**
     * Propiedad para acceder y modificar el ID de la mascota activa.
     * Si no existe, devuelve una cadena vacía.
     */
    // Variable mascotaActivaId: almacena el estado mutable de este componente
    var mascotaActivaId: String
        get() = prefs.getString(KEY_MASCOTA_ACTIVA_ID, "") ?: ""
        // Inicia el editor para modificar los SharedPreferences
        set(value) = prefs.edit().putString(KEY_MASCOTA_ACTIVA_ID, value).apply()

    /**
     * Propiedad para acceder y modificar el nombre de la mascota activa.
     * Si no existe, devuelve "Mascota" por defecto.
     */
    // Variable mascotaActivaNombre: almacena el estado mutable de este componente
    var mascotaActivaNombre: String
        get() = prefs.getString(KEY_MASCOTA_ACTIVA_NOMBRE, "Mascota") ?: "Mascota"
        // Inicia el editor para modificar los SharedPreferences
        set(value) = prefs.edit().putString(KEY_MASCOTA_ACTIVA_NOMBRE, value).apply()

    /**
     * Propiedad para acceder y modificar el umbral de distancia.
     * Si no existe, devuelve 50 por defecto.
     */
    // Variable mascotaUmbral: almacena el estado mutable de este componente
    var mascotaUmbral: Int
        get() = prefs.getInt(KEY_MASCOTA_UMBRAL, 50)
        // Inicia el editor para modificar los SharedPreferences
        set(value) = prefs.edit().putInt(KEY_MASCOTA_UMBRAL, value).apply()

    /**
     * Propiedad para acceder y modificar el ID del usuario.
     * Si no existe, devuelve "usuario_123" por defecto.
     */
    // Variable userId: almacena el estado mutable de este componente
    var userId: String
        get() = prefs.getString(KEY_USER_ID, "usuario_123") ?: "usuario_123"
        // Inicia el editor para modificar los SharedPreferences
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()
}