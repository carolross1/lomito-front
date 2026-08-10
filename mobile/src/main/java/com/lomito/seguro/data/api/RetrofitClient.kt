package com.lomito.seguro.data.api

import com.lomito.seguro.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // ✅ La IP/URL real vive en /gradle.properties (LOMITO_BACKEND_URL),
    // se inyecta aquí vía BuildConfig. No hardcodees la IP en más archivos.
    val SERVER_URL: String = BuildConfig.BACKEND_URL
    private val BASE_URL = "$SERVER_URL/api/"
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: LomitoApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LomitoApi::class.java)
    }
}
