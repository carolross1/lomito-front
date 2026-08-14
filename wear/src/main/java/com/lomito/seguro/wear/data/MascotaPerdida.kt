// Paquete: com.lomito.seguro.wear.data.models
package com.lomito.seguro.wear.data.models

/**
 * [Modelo de datos para una mascota perdida]
 *
 * Responsabilidades:
 * - [Representar la información de una mascota en estado de pérdida]
 * - [Almacenar datos del dueño y última ubicación]
 *
 * Esta data class se utiliza para estructurar los datos recibidos desde el backend
 * o almacenados localmente que representan a una mascota reportada como perdida.
 */
// Clase de datos MascotaPerdida: modelo inmutable con propiedades de dominio
data class MascotaPerdida(
    // Identificador único de la mascota
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String,
    
    // Nombre de la mascota
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String,
    
    // Especie de la mascota (ej. Perro, Gato)
    // Constante especie: valor inmutable que no cambia tras su asignación
    val especie: String,
    
    // Raza de la mascota, valor por defecto cadena vacía
    // Constante raza: valor inmutable que no cambia tras su asignación
    val raza: String = "",
    
    // Color principal de la mascota
    // Constante color: valor inmutable que no cambia tras su asignación
    val color: String = "",
    
    // URL de la fotografía de la mascota, puede ser nulo si no tiene
    // Constante fotoUrl: valor inmutable que no cambia tras su asignación
    val fotoUrl: String? = null,
    
    // Distancia configurada para activar la alerta en metros, por defecto 50m
    // Constante distanciaAlerta: valor inmutable que no cambia tras su asignación
    val distanciaAlerta: Int = 50,
    
    // Estado actual de la mascota, por defecto "PERDIDA"
    // Constante estado: valor inmutable que no cambia tras su asignación
    val estado: String = "PERDIDA",
    
    // Identificador único del dueño de la mascota
    // Constante ownerId: valor inmutable que no cambia tras su asignación
    val ownerId: String = "",
    
    // Nombre completo o de contacto del dueño
    // Constante duenoNombre: valor inmutable que no cambia tras su asignación
    val duenoNombre: String = "",
    
    // Teléfono de contacto del dueño para reportes
    // Constante duenoTelefono: valor inmutable que no cambia tras su asignación
    val duenoTelefono: String = "",
    
    // Latitud de la última ubicación conocida de la mascota, puede ser nulo
    // Constante ultimaUbicacionLat: valor inmutable que no cambia tras su asignación
    val ultimaUbicacionLat: Double? = null,
    
    // Longitud de la última ubicación conocida de la mascota, puede ser nulo
    // Constante ultimaUbicacionLng: valor inmutable que no cambia tras su asignación
    val ultimaUbicacionLng: Double? = null
)