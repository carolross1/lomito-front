package com.lomito.seguro.tv.data.repository

import com.lomito.seguro.tv.data.api.RetrofitClient
import com.lomito.seguro.tv.data.model.Mascota
import com.lomito.seguro.tv.data.model.Refugio
import com.lomito.seguro.tv.data.model.ReporteVista

/**
 * Capa de datos del módulo TV. Cada función atrapa errores de red y devuelve
 * una lista vacía / null en vez de propagar la excepción, porque en una
 * pantalla comunitaria (sin usuario logueado que pueda reintentar) es
 * preferible mostrar "sin datos" a tronar la Activity.
 */
class LomitoTvRepository(private val api: com.lomito.seguro.tv.data.api.LomitoTvApi = RetrofitClient.api) {

    suspend fun getMuralMascotas(): List<Mascota> {
        return try {
            val perdidas = api.getMascotasByEstado("PERDIDA")
            val encontradas = api.getMascotasByEstado("ENCONTRADA")
            val resultado = mutableListOf<Mascota>()
            if (perdidas.isSuccessful) resultado += perdidas.body().orEmpty()
            if (encontradas.isSuccessful) resultado += encontradas.body().orEmpty()
            resultado
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMascotaById(id: String): Mascota? {
        return try {
            val response = api.getMascotaById(id)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getReportesDeMascota(mascotaId: String): List<ReporteVista> {
        return try {
            val response = api.getReportesByMascota(mascotaId)
            if (response.isSuccessful) response.body().orEmpty() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ✅ Crea y confirma el avistamiento en un solo paso (usado por la TV).
    // No depende de que exista un reporte previo para la mascota.
    suspend fun reportarAvistamiento(mascotaId: String, contacto: String): Boolean {
        return try {
            api.reportarAvistamientoTv(mapOf("mascota_id" to mascotaId, "contacto" to contacto)).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun confirmarReporte(reporteId: String, contacto: String): Boolean {
        return try {
            api.confirmarReporte(reporteId, mapOf("contacto" to contacto)).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getRefugios(): List<Refugio> {
        return try {
            val response = api.getRefugios()
            if (response.isSuccessful) response.body().orEmpty() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRefugioById(id: String): Refugio? {
        return try {
            val response = api.getRefugioById(id)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }
}
