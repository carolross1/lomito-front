package com.lomito.seguro.wear.data.models

/**
 * [Modelo de datos para una mascota perdida]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Representar la información de una mascota en estado de pérdida]
 * - [Almacenar datos del dueño y última ubicación]
 */
data class MascotaPerdida(
    val id: String,
    val nombre: String,
    val especie: String,
    val raza: String = "",
    val color: String = "",
    val fotoUrl: String? = null,
    val distanciaAlerta: Int = 50,
    val estado: String = "PERDIDA",
    val ownerId: String = "",
    val duenoNombre: String = "",
    val duenoTelefono: String = "",
    val ultimaUbicacionLat: Double? = null,
    val ultimaUbicacionLng: Double? = null
)