package com.lomito.seguro.tv.data.model

import com.google.gson.annotations.SerializedName

/**
 * [Clase de datos que representa a una Mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Contener la información básica y de estado de una mascota]
 * - [Mapear los atributos desde el modelo remoto de datos]
 */
data class Mascota(
    val id: String = "",
    val nombre: String = "",
    val especie: String = "",
    val raza: String = "",
    val edad: Int = 0,
    val color: String = "",
    val peso: Double = 0.0,
    @SerializedName("foto_url") val fotoUrl: String? = null,
    @SerializedName("distancia_alerta") val distanciaAlerta: Int = 50,
    val estado: String = "EN_CASA",
    val activa: Boolean = true,
    @SerializedName("owner_id") val ownerId: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null
)

/**
 * [Clase de datos que representa un Reporte de Vista de mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Almacenar los datos de ubicación, dirección y tiempo del avistamiento]
 * - [Vincular el reporte con una mascota y usuario específicos]
 */
data class ReporteVista(
    val id: String = "",
    @SerializedName("mascota_id") val mascotaId: String = "",
    @SerializedName("reportado_por_id") val reportadoPorId: String? = null,
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val direccion: String = "",
    val timestamp: String = ""
)

/**
 * [Clase de datos que representa un Refugio para animales]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mantener la información de contacto y horarios del refugio]
 * - [Proporcionar enlaces a recursos multimedia del refugio]
 */
data class Refugio(
    val id: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val horarios: String = "",
    @SerializedName("video_url") val videoUrl: String? = null,
    @SerializedName("logo_url") val logoUrl: String? = null
)
