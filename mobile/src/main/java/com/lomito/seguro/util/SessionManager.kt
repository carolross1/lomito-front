// mobile/util/SessionManager.kt
package com.lomito.seguro.util

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_AVATAR = "user_avatar"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun saveUser(
        id: String,
        nombre: String,
        correo: String,
        telefono: String = "",
        avatarUrl: String? = null
    ) {
        prefs.edit().apply {
            putString(KEY_USER_ID, id)
            putString(KEY_USER_NAME, nombre)
            putString(KEY_USER_EMAIL, correo)
            putString(KEY_USER_PHONE, telefono)
            if (avatarUrl != null) putString(KEY_USER_AVATAR, avatarUrl)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getUserId(): String {
        return prefs.getString(KEY_USER_ID, "") ?: ""
    }

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "") ?: ""
    }

    fun getUserEmail(): String {
        return prefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    fun getUserPhone(): String {
        return prefs.getString(KEY_USER_PHONE, "") ?: ""
    }

    fun getUserAvatar(): String {
        return prefs.getString(KEY_USER_AVATAR, "") ?: ""
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getFullUserData(): Map<String, String> {
        return mapOf(
            "id" to getUserId(),
            "nombre" to getUserName(),
            "correo" to getUserEmail(),
            "telefono" to getUserPhone(),
            "avatar" to getUserAvatar()
        )
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun updateUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }
}