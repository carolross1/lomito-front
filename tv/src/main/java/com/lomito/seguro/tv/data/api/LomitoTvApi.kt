// Paquete: com.lomito.seguro.tv.data.api
package com.lomito.seguro.tv.data.api

// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.tv.data.model.Mascota
// Importa la dependencia necesaria: Refugio
import com.lomito.seguro.tv.data.model.Refugio
// Importa la dependencia necesaria: ReporteVista
import com.lomito.seguro.tv.data.model.ReporteVista
// Importa la dependencia necesaria: Response
import retrofit2.Response
// Importa la dependencia necesaria: Body
import retrofit2.http.Body
// Importa la dependencia necesaria: GET
import retrofit2.http.GET
// Importa la dependencia necesaria: Path
import retrofit2.http.Path
// Importa la dependencia necesaria: PUT
import retrofit2.http.PUT
// Importa la dependencia necesaria: POST
import retrofit2.http.POST
// Importa la dependencia necesaria: Query
import retrofit2.http.Query

/**
 * [Interfaz de red para la API de Lomito Seguro en TV]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Definir los endpoints necesarios para la app de TV]
 * - [Proveer métodos de lectura de mascotas y creación de reportes]
 */
// Interfaz LomitoTvApi: contrato que deben cumplir las implementaciones
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
