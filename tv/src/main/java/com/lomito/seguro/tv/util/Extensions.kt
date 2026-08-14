package com.lomito.seguro.tv.util

import com.lomito.seguro.tv.data.api.RetrofitClient

/**
 * [Función de extensión para convertir rutas relativas a URLs absolutas]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Concatenar la URL base del servidor a rutas relativas]
 * - [Retornar la URL original si ya es absoluta]
 */
fun String?.toAbsoluteUrl(): String? {
    if (this.isNullOrEmpty()) return null
    return if (startsWith("http://") || startsWith("https://")) this
    else RetrofitClient.SERVER_URL + (if (startsWith("/")) this else "/$this")
}
