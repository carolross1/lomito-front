package com.lomito.seguro.wear.data.models

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