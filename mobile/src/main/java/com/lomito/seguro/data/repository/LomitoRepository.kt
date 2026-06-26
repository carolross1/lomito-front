package com.lomito.seguro.data.repository

import com.lomito.seguro.data.api.RetrofitClient
import com.lomito.seguro.data.model.*

class LomitoRepository {
    private val api = RetrofitClient.api

    suspend fun login(correo: String, contrasena: String) =
        api.login(LoginRequest(correo, contrasena))

    suspend fun register(nombre: String, correo: String, telefono: String, contrasena: String) =
        api.register(RegisterRequest(nombre, correo, telefono, contrasena))

    suspend fun getMascotas(ownerId: String) = api.getMascotas(ownerId)

    suspend fun getMascotaById(id: String) = api.getMascotaById(id)

    suspend fun createMascota(request: CreateMascotaRequest) = api.createMascota(request)

    suspend fun updateMascota(id: String, data: Map<String, Any>) = api.updateMascota(id, data)

    suspend fun deleteMascota(id: String) = api.deleteMascota(id)

    suspend fun updateUbicacion(id: String, lat: Double, lng: Double) =
        api.updateUbicacion(id, UbicacionRequest(lat, lng))

    suspend fun getUltimoReporte(mascotaId: String) = api.getUltimoReporte(mascotaId)

    suspend fun getAlertas(ownerId: String) = api.getAlertas(ownerId)

    suspend fun getAlertasNoLeidas(ownerId: String) = api.getAlertasNoLeidas(ownerId)

    suspend fun marcarLeida(id: String) = api.marcarLeida(id)

    suspend fun marcarTodasLeidas(ownerId: String) = api.marcarTodasLeidas(ownerId)

    suspend fun reportarVista(mascotaId: String, lat: Double, lng: Double, direccion: String = "") =
        api.reportarVista(ReporteRequest(mascotaId, lat, lng, direccion))

    suspend fun confirmarReporte(id: String) = api.confirmarReporte(id)

    suspend fun getRefugios() = api.getRefugios()

    suspend fun getMascotasByEstado(estado: String) = api.getMascotasByEstado(estado)
}
