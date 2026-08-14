// Paquete: com.lomito.seguro.tv.data.api
package com.lomito.seguro.tv.data.api

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

// Singleton RetrofitClient: instancia única compartida en toda la aplicación
object RetrofitClient {
    // Misma API que el módulo mobile. Para el emulador de Android Studio se usa 10.0.2.2 
    // en lugar de localhost. Si pruebas en TV física, pon la IP local de tu PC (ej. 192.168.x.x)
    // Constante SERVER_URL: valor fijo definido en tiempo de compilación
    const val SERVER_URL = "http://10.0.2.2:3000"
    // Constante BASE_URL: valor fijo definido en tiempo de compilación
    private const val BASE_URL = "$SERVER_URL/api/"

    // Constante loggingInterceptor: valor inmutable que no cambia tras su asignación
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Constante okHttpClient: valor inmutable que no cambia tras su asignación
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Constante api: valor inmutable que no cambia tras su asignación
    val api: LomitoTvApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LomitoTvApi::class.java)
    }
}
