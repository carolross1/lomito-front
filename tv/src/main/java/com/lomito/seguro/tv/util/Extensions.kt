package com.lomito.seguro.tv.util

import com.lomito.seguro.tv.data.api.RetrofitClient

/**
 * El backend guarda foto_url como ruta relativa (ej: "/uploads/123.jpg").
 * Coil necesita una URL absoluta para poder cargarla, así que la resolvemos
 * contra el host del servidor. Si ya viene absoluta (http/https) se regresa
 * tal cual. Mismo criterio que usa el módulo mobile.
 */
fun String?.toAbsoluteUrl(): String? {
    if (this.isNullOrEmpty()) return null
    return if (startsWith("http://") || startsWith("https://")) this
    else RetrofitClient.SERVER_URL + (if (startsWith("/")) this else "/$this")
}
