// wear/data/WatchPreferences.kt
package com.lomito.seguro.wear.data

import android.content.Context
import android.content.SharedPreferences

class WatchPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MASCOTA_ACTIVA_ID = "mascota_activa_id"
        private const val KEY_MASCOTA_ACTIVA_NOMBRE = "mascota_activa_nombre"
        private const val KEY_MASCOTA_UMBRAL = "mascota_umbral"
        private const val KEY_USER_ID = "user_id"
    }

    var mascotaActivaId: String
        get() = prefs.getString(KEY_MASCOTA_ACTIVA_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MASCOTA_ACTIVA_ID, value).apply()

    var mascotaActivaNombre: String
        get() = prefs.getString(KEY_MASCOTA_ACTIVA_NOMBRE, "Mascota") ?: "Mascota"
        set(value) = prefs.edit().putString(KEY_MASCOTA_ACTIVA_NOMBRE, value).apply()

    var mascotaUmbral: Int
        get() = prefs.getInt(KEY_MASCOTA_UMBRAL, 50)
        set(value) = prefs.edit().putInt(KEY_MASCOTA_UMBRAL, value).apply()

    var userId: String
        get() = prefs.getString(KEY_USER_ID, "usuario_123") ?: "usuario_123"
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()
}