package com.lomito.seguro.tv.data.api

import com.lomito.seguro.tv.data.model.Mascota
import com.lomito.seguro.tv.data.model.Refugio
import com.lomito.seguro.tv.data.model.ReporteVista
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Subconjunto de solo-lectura de la API de Lomito Seguro que necesita la
 * pantalla comunitaria de Smart TV: mural de mascotas perdidas/encontradas,
 * refugios locales y su historial de reportes/avistamientos.
 */
interface LomitoTvApi {

    @GET("mascotas/estado")
    suspend fun getMascotasByEstado(@Query("estado") estado: String): Response<List<Mascota>>

    @GET("mascotas/{id}")
    suspend fun getMascotaById(@Path("id") id: String): Response<Mascota>

    @GET("reportes/mascota/{mascotaId}")
    suspend fun getReportesByMascota(@Path("mascotaId") mascotaId: String): Response<List<ReporteVista>>

    @POST("reportes/tv")
    suspend fun reportarAvistamientoTv(@Body body: Map<String, String>): Response<Map<String, Any>>

    @PUT("reportes/{id}/confirmar")
    suspend fun confirmarReporte(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @GET("refugios")
    suspend fun getRefugios(): Response<List<Refugio>>

    @GET("refugios/{id}")
    suspend fun getRefugioById(@Path("id") id: String): Response<Refugio>
}
