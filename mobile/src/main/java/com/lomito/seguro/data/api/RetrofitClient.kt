// Paquete: com.lomito.seguro.data.api
package com.lomito.seguro.data.api

// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.BuildConfig
// Importa el cliente HTTP OkHttp
import okhttp3.OkHttpClient
// Importa la clase de logging de Android
import okhttp3.logging.HttpLoggingInterceptor
// Importa el cliente Retrofit para peticiones HTTP
import retrofit2.Retrofit
// Importa el parser JSON
import retrofit2.converter.gson.GsonConverterFactory
// Importa la dependencia necesaria: TimeUnit
import java.util.concurrent.TimeUnit

/**
 * [Cliente Retrofit configurado para la app]
 *
 * Responsabilidades:
 * - [Construir y proveer la instancia central de Retrofit]
 * - [Configurar interceptores y tiempos de espera de las peticiones]
 */
// Singleton RetrofitClient: instancia única compartida en toda la aplicación
object RetrofitClient {
    // ✅ La IP/URL real vive en /gradle.properties (LOMITO_BACKEND_URL),
    // se inyecta aquí vía BuildConfig. No hardcodees la IP en más archivos.
    // Constante SERVER_URL: valor inmutable que no cambia tras su asignación
    val SERVER_URL: String = BuildConfig.BACKEND_URL
    // Constante BASE_URL: valor inmutable que no cambia tras su asignación
    private val BASE_URL = "$SERVER_URL/api/"
    // Constante loggingInterceptor: valor inmutable que no cambia tras su asignación
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Constante okHttpClient: valor inmutable que no cambia tras su asignación
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Constante api: valor inmutable que no cambia tras su asignación
    val api: LomitoApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LomitoApi::class.java)
    }
}
