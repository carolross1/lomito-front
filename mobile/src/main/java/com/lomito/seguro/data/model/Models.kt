// Paquete: com.lomito.seguro.data.model
package com.lomito.seguro.data.model

// Importa la dependencia necesaria: SerializedName
import com.google.gson.annotations.SerializedName

// Clase de datos Usuario: modelo inmutable con propiedades de dominio
data class Usuario(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String = "",
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String = "",
    // Constante correo: valor inmutable que no cambia tras su asignación
    val correo: String = "",
    // Constante telefono: valor inmutable que no cambia tras su asignación
    val telefono: String = "",
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

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
// Clase de datos CreateMascotaRequest: modelo inmutable con propiedades de dominio
data class CreateMascotaRequest(
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String,
    // Constante especie: valor inmutable que no cambia tras su asignación
    val especie: String,
    @SerializedName("owner_id") val ownerId: String,
    // Constante raza: valor inmutable que no cambia tras su asignación
    val raza: String = "",
    // Constante color: valor inmutable que no cambia tras su asignación
    val color: String = "",
    // Constante edad: valor inmutable que no cambia tras su asignación
    val edad: Int = 0,
    // Constante peso: valor inmutable que no cambia tras su asignación
    val peso: Double = 0.0,
    @SerializedName("distancia_alerta") val distanciaAlerta: Int = 50,
    @SerializedName("foto_url") val fotoUrl: String? = null
)

// Clase de datos UploadResponse: modelo inmutable con propiedades de dominio
data class UploadResponse(
    @SerializedName("foto_url") val fotoUrl: String
)

// Clase de datos Alerta: modelo inmutable con propiedades de dominio
data class Alerta(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String = "",
    @SerializedName("mascota_id") val mascotaId: String = "",
    @SerializedName("owner_id") val ownerId: String = "",
    // Constante tipo: valor inmutable que no cambia tras su asignación
    val tipo: String = "",
    // Constante mensaje: valor inmutable que no cambia tras su asignación
    val mensaje: String = "",
    // Constante distancia: valor inmutable que no cambia tras su asignación
    val distancia: Int = 0,
    // Constante leida: valor inmutable que no cambia tras su asignación
    val leida: Boolean = false,
    // Constante timestamp: valor inmutable que no cambia tras su asignación
    val timestamp: String = ""
)

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

// Clase de datos LoginRequest: modelo inmutable con propiedades de dominio
data class LoginRequest(val correo: String, val contrasena: String)
// Clase de datos RegisterRequest: modelo inmutable con propiedades de dominio
data class RegisterRequest(val nombre: String, val correo: String, val telefono: String, val contrasena: String)
// Clase de datos UbicacionRequest: modelo inmutable con propiedades de dominio
data class UbicacionRequest(val lat: Double, val lng: Double)
// Clase de datos ReporteRequest: modelo inmutable con propiedades de dominio
data class ReporteRequest(
    @SerializedName("mascota_id") val mascotaId: String,
    // Constante latitud: valor inmutable que no cambia tras su asignación
    val latitud: Double,
    // Constante longitud: valor inmutable que no cambia tras su asignación
    val longitud: Double,
    // Constante direccion: valor inmutable que no cambia tras su asignación
    val direccion: String = ""
)
