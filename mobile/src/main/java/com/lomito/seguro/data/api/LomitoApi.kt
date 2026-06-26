package com.lomito.seguro.data.api

import com.lomito.seguro.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface LomitoApi {

    // AUTH
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<Usuario>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Usuario>

    @GET("auth/profile/{id}")
    suspend fun getProfile(@Path("id") id: String): Response<Usuario>

    @PUT("auth/profile/{id}")
    suspend fun updateProfile(@Path("id") id: String, @Body data: Map<String, String>): Response<Usuario>

    // MASCOTAS
    @GET("mascotas")
    suspend fun getMascotas(@Query("ownerId") ownerId: String): Response<List<Mascota>>

    @GET("mascotas/{id}")
    suspend fun getMascotaById(@Path("id") id: String): Response<Mascota>

    @GET("mascotas/estado")
    suspend fun getMascotasByEstado(@Query("estado") estado: String): Response<List<Mascota>>


    @POST("mascotas")
    suspend fun createMascota(@Body mascota: CreateMascotaRequest): Response<Mascota>

    @PUT("mascotas/{id}")
    suspend fun updateMascota(@Path("id") id: String, @Body data: Map<String, Any>): Response<Mascota>

    @DELETE("mascotas/{id}")
    suspend fun deleteMascota(@Path("id") id: String): Response<Map<String, String>>

    @PUT("mascotas/{id}/ubicacion")
    suspend fun updateUbicacion(@Path("id") id: String, @Body data: UbicacionRequest): Response<Mascota>

    @GET("mascotas/{id}/ultimo-reporte")
    suspend fun getUltimoReporte(@Path("id") id: String): Response<ReporteVista>

    @GET("mascotas/{id}/reportes")
    suspend fun getReportesMascota(@Path("id") id: String): Response<List<ReporteVista>>

    // ALERTAS
    @GET("alertas")
    suspend fun getAlertas(@Query("ownerId") ownerId: String): Response<List<Alerta>>

    @GET("alertas/no-leidas")
    suspend fun getAlertasNoLeidas(@Query("ownerId") ownerId: String): Response<List<Alerta>>

    @PUT("alertas/{id}/leida")
    suspend fun marcarLeida(@Path("id") id: String): Response<Map<String, Any>>

    @PUT("alertas/leidas/{ownerId}")
    suspend fun marcarTodasLeidas(@Path("ownerId") ownerId: String): Response<Map<String, Any>>

    // REPORTES
    @POST("reportes")
    suspend fun reportarVista(@Body reporte: ReporteRequest): Response<ReporteVista>

    @GET("reportes/mascota/{mascotaId}")
    suspend fun getReportesByMascota(@Path("mascotaId") mascotaId: String): Response<List<ReporteVista>>

    @PUT("reportes/{id}/confirmar")
    suspend fun confirmarReporte(@Path("id") id: String): Response<Map<String, Any>>

    // REFUGIOS
    @GET("refugios")
    suspend fun getRefugios(): Response<List<Refugio>>

    @GET("refugios/{id}")
    suspend fun getRefugioById(@Path("id") id: String): Response<Refugio>
}
