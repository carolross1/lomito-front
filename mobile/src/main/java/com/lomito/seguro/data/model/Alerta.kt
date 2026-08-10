package com.lomito.seguro.models

import java.util.Date

data class Alerta(
    val id: Int,
    val titulo: String = "",
    val mensaje: String = "",
    val tipo: String = "GENERAL",
    val fecha: Date = Date(),
    val leida: Boolean = false,
    val mascotaId: String? = null,
    val mascotaNombre: String? = null
)