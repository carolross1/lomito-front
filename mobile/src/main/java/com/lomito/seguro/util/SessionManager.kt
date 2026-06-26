package com.lomito.seguro.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.lomito.seguro.data.model.Usuario

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lomito_session", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveUser(usuario: Usuario) {
        prefs.edit().putString("user", gson.toJson(usuario)).apply()
    }

    fun getUser(): Usuario? {
        val json = prefs.getString("user", null) ?: return null
        return gson.fromJson(json, Usuario::class.java)
    }

    fun getUserId(): String = getUser()?.id ?: ""

    fun isLoggedIn(): Boolean = getUser() != null

    fun logout() {
        prefs.edit().clear().apply()
    }
}
