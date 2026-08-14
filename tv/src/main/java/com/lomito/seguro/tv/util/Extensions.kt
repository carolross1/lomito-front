// Paquete: com.lomito.seguro.tv.util
package com.lomito.seguro.tv.util

// Importa el cliente Retrofit para peticiones HTTP
import com.lomito.seguro.tv.data.api.RetrofitClient

/**
 * [Función de extensión para convertir rutas relativas a URLs absolutas]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Concatenar la URL base del servidor a rutas relativas]
 * - [Retornar la URL original si ya es absoluta]
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
