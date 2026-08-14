// Paquete: com.lomito.seguro.tv.data.model
package com.lomito.seguro.tv.data.model

// Importa la dependencia necesaria: SerializedName
import com.google.gson.annotations.SerializedName

/**
 * [Clase de datos que representa a una Mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Contener la información básica y de estado de una mascota]
 * - [Mapear los atributos desde el modelo remoto de datos]
 */
// Clase de datos Mascota: modelo inmutable con propiedades de dominio
data class Mascota(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String = "",
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String = "",
    // Constante especie: valor inmutable que no cambia tras su asignación
    val especie: String = "",
    // Constante raza: valor inmutable que no cambia tras su asignación
    val raza: String = "",
    // Constante edad: valor inmutable que no cambia tras su asignación
    val edad: Int = 0,
    // Constante color: valor inmutable que no cambia tras su asignación
    val color: String = "",
    // Constante peso: valor inmutable que no cambia tras su asignación
    val peso: Double = 0.0,
    @SerializedName("foto_url") val fotoUrl: String? = null,
    @SerializedName("distancia_alerta") val distanciaAlerta: Int = 50,
    // Constante estado: valor inmutable que no cambia tras su asignación
    val estado: String = "EN_CASA",
    // Constante activa: valor inmutable que no cambia tras su asignación
    val activa: Boolean = true,
    @SerializedName("owner_id") val ownerId: String = "",
    // Constante latitud: valor inmutable que no cambia tras su asignación
    val latitud: Double? = null,
    // Constante longitud: valor inmutable que no cambia tras su asignación
    val longitud: Double? = null
)

/**
 * [Clase de datos que representa un Reporte de Vista de mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Almacenar los datos de ubicación, dirección y tiempo del avistamiento]
 * - [Vincular el reporte con una mascota y usuario específicos]
 */
// Clase de datos ReporteVista: modelo inmutable con propiedades de dominio
data class ReporteVista(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String = "",
    @SerializedName("mascota_id") val mascotaId: String = "",
    @SerializedName("reportado_por_id") val reportadoPorId: String? = null,
    // Constante latitud: valor inmutable que no cambia tras su asignación
    val latitud: Double = 0.0,
    // Constante longitud: valor inmutable que no cambia tras su asignación
    val longitud: Double = 0.0,
    // Constante direccion: valor inmutable que no cambia tras su asignación
    val direccion: String = "",
    // Constante timestamp: valor inmutable que no cambia tras su asignación
    val timestamp: String = ""
)

/**
 * [Clase de datos que representa un Refugio para animales]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mantener la información de contacto y horarios del refugio]
 * - [Proporcionar enlaces a recursos multimedia del refugio]
 */
// Clase de datos Refugio: modelo inmutable con propiedades de dominio
data class Refugio(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String = "",
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String = "",
    // Constante direccion: valor inmutable que no cambia tras su asignación
    val direccion: String = "",
    // Constante telefono: valor inmutable que no cambia tras su asignación
    val telefono: String = "",
    // Constante horarios: valor inmutable que no cambia tras su asignación
    val horarios: String = "",
    @SerializedName("video_url") val videoUrl: String? = null,
    @SerializedName("logo_url") val logoUrl: String? = null
)
