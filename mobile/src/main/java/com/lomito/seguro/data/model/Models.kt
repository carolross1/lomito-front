package com.lomito.seguro.data.model

import com.google.gson.annotations.SerializedName

data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val correo: String = "",
    val telefono: String = "",
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

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
data class CreateMascotaRequest(
    val nombre: String,
    val especie: String,
    @SerializedName("owner_id") val ownerId: String,
    val raza: String = "",
    val color: String = "",
    val edad: Int = 0,
    val peso: Double = 0.0,
    @SerializedName("distancia_alerta") val distanciaAlerta: Int = 50
)

data class Alerta(
    val id: String = "",
    @SerializedName("mascota_id") val mascotaId: String = "",
    @SerializedName("owner_id") val ownerId: String = "",
    val tipo: String = "",
    val mensaje: String = "",
    val distancia: Int = 0,
    val leida: Boolean = false,
    val timestamp: String = ""
)

data class ReporteVista(
    val id: String = "",
    @SerializedName("mascota_id") val mascotaId: String = "",
    @SerializedName("reportado_por_id") val reportadoPorId: String? = null,
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val direccion: String = "",
    val timestamp: String = ""
)

data class Refugio(
    val id: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val horarios: String = "",
    @SerializedName("video_url") val videoUrl: String? = null,
    @SerializedName("logo_url") val logoUrl: String? = null
)

data class LoginRequest(val correo: String, val contrasena: String)
data class RegisterRequest(val nombre: String, val correo: String, val telefono: String, val contrasena: String)
data class UbicacionRequest(val lat: Double, val lng: Double)
data class ReporteRequest(
    @SerializedName("mascota_id") val mascotaId: String,
    val latitud: Double,
    val longitud: Double,
    val direccion: String = ""
)
