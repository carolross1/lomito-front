// mobile/util/SessionManager.kt
// Paquete: com.lomito.seguro.util
package com.lomito.seguro.util

// Importa el contexto de Android
import android.content.Context
// Importa SharedPreferences para persistencia local
import android.content.SharedPreferences

/**
 * [Gestor de la sesión local del usuario]
 *
 * Responsabilidades:
 * - [Almacenar y recuperar datos del usuario en SharedPreferences]
 * - [Manejar las operaciones de login y logout locales]
 */
// Declaración de la clase SessionManager
class SessionManager(context: Context) {
    // Constante prefs: valor inmutable que no cambia tras su asignación
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        // Constante KEY_USER_ID: valor fijo definido en tiempo de compilación
        private const val KEY_USER_ID = "user_id"
        // Constante KEY_USER_NAME: valor fijo definido en tiempo de compilación
        private const val KEY_USER_NAME = "user_name"
        // Constante KEY_USER_EMAIL: valor fijo definido en tiempo de compilación
        private const val KEY_USER_EMAIL = "user_email"
        // Constante KEY_USER_PHONE: valor fijo definido en tiempo de compilación
        private const val KEY_USER_PHONE = "user_phone"
        // Constante KEY_USER_AVATAR: valor fijo definido en tiempo de compilación
        private const val KEY_USER_AVATAR = "user_avatar"
        // Constante KEY_IS_LOGGED_IN: valor fijo definido en tiempo de compilación
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    // Función saveUser: define la lógica de esta operación
    fun saveUser(
        id: String,
        nombre: String,
        correo: String,
        telefono: String = "",
        avatarUrl: String? = null
    ) {
        // Inicia el editor para modificar los SharedPreferences
        prefs.edit().apply {
            putString(KEY_USER_ID, id)
            putString(KEY_USER_NAME, nombre)
            putString(KEY_USER_EMAIL, correo)
            putString(KEY_USER_PHONE, telefono)
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (avatarUrl != null) putString(KEY_USER_AVATAR, avatarUrl)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    // Función getUserId: define la lógica de esta operación
    fun getUserId(): String {
        // Retorna el valor al llamador de la función
        return prefs.getString(KEY_USER_ID, "") ?: ""
    }

    // Función getUserName: define la lógica de esta operación
    fun getUserName(): String {
        // Retorna el valor al llamador de la función
        return prefs.getString(KEY_USER_NAME, "") ?: ""
    }

    // Función getUserEmail: define la lógica de esta operación
    fun getUserEmail(): String {
        // Retorna el valor al llamador de la función
        return prefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    // Función getUserPhone: define la lógica de esta operación
    fun getUserPhone(): String {
        // Retorna el valor al llamador de la función
        return prefs.getString(KEY_USER_PHONE, "") ?: ""
    }

    // Función getUserAvatar: define la lógica de esta operación
    fun getUserAvatar(): String {
        // Retorna el valor al llamador de la función
        return prefs.getString(KEY_USER_AVATAR, "") ?: ""
    }

    // Función isLoggedIn: define la lógica de esta operación
    fun isLoggedIn(): Boolean {
        // Retorna el valor al llamador de la función
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Función getFullUserData: define la lógica de esta operación
    fun getFullUserData(): Map<String, String> {
        // Retorna el valor al llamador de la función
        return mapOf(
            "id" to getUserId(),
            "nombre" to getUserName(),
            "correo" to getUserEmail(),
            "telefono" to getUserPhone(),
            "avatar" to getUserAvatar()
        )
    }

    // Función logout: define la lógica de esta operación
    fun logout() {
        // Inicia el editor para modificar los SharedPreferences
        prefs.edit().clear().apply()
    }

    // Función updateUserId: define la lógica de esta operación
    fun updateUserId(userId: String) {
        // Inicia el editor para modificar los SharedPreferences
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }
}