// Paquete: com.lomito.seguro.models
package com.lomito.seguro.models

// Importa la dependencia necesaria: Date
import java.util.Date

// Clase de datos Alerta: modelo inmutable con propiedades de dominio
data class Alerta(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: Int,
    // Constante titulo: valor inmutable que no cambia tras su asignación
    val titulo: String = "",
    // Constante mensaje: valor inmutable que no cambia tras su asignación
    val mensaje: String = "",
    // Constante tipo: valor inmutable que no cambia tras su asignación
    val tipo: String = "GENERAL",
    // Constante fecha: valor inmutable que no cambia tras su asignación
    val fecha: Date = Date(),
    // Constante leida: valor inmutable que no cambia tras su asignación
    val leida: Boolean = false,
    // Constante mascotaId: valor inmutable que no cambia tras su asignación
    val mascotaId: String? = null,
    // Constante mascotaNombre: valor inmutable que no cambia tras su asignación
    val mascotaNombre: String? = null
)