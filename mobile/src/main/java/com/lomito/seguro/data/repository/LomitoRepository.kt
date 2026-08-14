// Paquete: com.lomito.seguro.data.repository
package com.lomito.seguro.data.repository

// Importa el cliente Retrofit para peticiones HTTP
import com.lomito.seguro.data.api.RetrofitClient
// Importa la dependencia necesaria: *
import com.lomito.seguro.data.model.*
// Importa la dependencia necesaria: MultipartBody
import okhttp3.MultipartBody

/**
 * [Repositorio de datos para abstraer las llamadas de red]
 *
 * Responsabilidades:
 * - [Servir como intermediario entre los ViewModels y la API]
 * - [Gestionar los métodos de login, mascotas, alertas y refugios]
 */
// Repositorio LomitoRepository: capa de datos que abstrae las fuentes de información
class LomitoRepository {
    // Constante api: valor inmutable que no cambia tras su asignación
    private val api = RetrofitClient.api

    suspend fun login(correo: String, contrasena: String) =
        api.login(LoginRequest(correo, contrasena))

    suspend fun uploadFoto(foto: MultipartBody.Part) = api.uploadFoto(foto)

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
