// Paquete: com.lomito.seguro.tv.data.repository
package com.lomito.seguro.tv.data.repository

// Importa el cliente Retrofit para peticiones HTTP
import com.lomito.seguro.tv.data.api.RetrofitClient
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.tv.data.model.Mascota
// Importa la dependencia necesaria: Refugio
import com.lomito.seguro.tv.data.model.Refugio
// Importa la dependencia necesaria: ReporteVista
import com.lomito.seguro.tv.data.model.ReporteVista

/**
 * [Repositorio de datos para el módulo TV]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Proveer acceso a los datos de mascotas perdidas, reportes y refugios]
 * - [Manejar errores de red devolviendo valores seguros para evitar crasheos en TV]
 */
// Repositorio LomitoTvRepository: capa de datos que abstrae las fuentes de información
class LomitoTvRepository(private val api: com.lomito.seguro.tv.data.api.LomitoTvApi = RetrofitClient.api) {

    suspend fun getMuralMascotas(): List<Mascota> {
        // Retorna el valor al llamador de la función
        return try {
            // Constante perdidas: valor inmutable que no cambia tras su asignación
            val perdidas = api.getMascotasByEstado("PERDIDA")
            // Constante encontradas: valor inmutable que no cambia tras su asignación
            val encontradas = api.getMascotasByEstado("ENCONTRADA")
            // Constante resultado: valor inmutable que no cambia tras su asignación
            val resultado = mutableListOf<Mascota>()
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (perdidas.isSuccessful) resultado += perdidas.body().orEmpty()
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (encontradas.isSuccessful) resultado += encontradas.body().orEmpty()
            resultado
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMascotaById(id: String): Mascota? {
        // Retorna el valor al llamador de la función
        return try {
            // Constante response: valor inmutable que no cambia tras su asignación
            val response = api.getMascotaById(id)
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getReportesDeMascota(mascotaId: String): List<ReporteVista> {
        // Retorna el valor al llamador de la función
        return try {
            // Constante response: valor inmutable que no cambia tras su asignación
            val response = api.getReportesByMascota(mascotaId)
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (response.isSuccessful) response.body().orEmpty() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ✅ Crea y confirma el avistamiento en un solo paso (usado por la TV).
    // No depende de que exista un reporte previo para la mascota.
    suspend fun reportarAvistamiento(mascotaId: String, contacto: String): Boolean {
        // Retorna el valor al llamador de la función
        return try {
            api.reportarAvistamientoTv(mapOf("mascota_id" to mascotaId, "contacto" to contacto)).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun confirmarReporte(reporteId: String, contacto: String): Boolean {
        // Retorna el valor al llamador de la función
        return try {
            api.confirmarReporte(reporteId, mapOf("contacto" to contacto)).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getRefugios(): List<Refugio> {
        // Retorna el valor al llamador de la función
        return try {
            // Constante response: valor inmutable que no cambia tras su asignación
            val response = api.getRefugios()
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (response.isSuccessful) response.body().orEmpty() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRefugioById(id: String): Refugio? {
        // Retorna el valor al llamador de la función
        return try {
            // Constante response: valor inmutable que no cambia tras su asignación
            val response = api.getRefugioById(id)
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }
}
