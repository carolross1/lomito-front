# Guía Paso a Paso: Construyendo el Módulo Móvil de Lomito Seguro

Esta guía documenta y desglosa paso a paso la arquitectura, configuración y construcción completa del módulo **Móvil (Android Smartphone)** de **Lomito Seguro**, explicando las decisiones técnicas, patrones de diseño y bloques de código esenciales para un proyecto profesional en **Kotlin** y **Jetpack Compose (Material 3)**.

---

## Objetivo de Esta Guía

Al estudiar y seguir esta guía, comprenderás:

1. Cómo estructurar un proyecto Android profesional con **Kotlin** y **Jetpack Compose** bajo los principios de **Arquitectura Limpia (Clean Architecture)** y **MVVM (Model-View-ViewModel)**.
2. Cómo implementar comunicación en red con el backend (Node.js) mediante **Retrofit** y `GsonConverterFactory`.
3. El uso de corrutinas (`Coroutines`) y flujos (`StateFlow`) para un manejo de estado reactivo y unidireccional (*Unidirectional Data Flow - UDF*).
4. Integración de navegación dinámica mediante **Navigation Compose**.
5. Manejo de módulos de inicio de sesión, reportes de mascotas perdidas, lista de refugios locales, y carga de fotografías (Multi-part).

---

## FASE 1: Configuración Inicial del Entorno y Build System

### Paso 1.1: Configuración de Dependencias

El archivo `build.gradle.kts` (a nivel de módulo `mobile`) incluye todas las dependencias necesarias.

> 📋 **INSTRUCCIÓN:** El archivo de dependencias incluye Retrofit para networking, Coil para carga de imágenes, y dependencias de Jetpack Compose:
```kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // Navegación en Compose
    implementation("androidx.navigation:navigation-compose:2.7.4")
    
    // Retrofit y GSON para consumo de API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // Coil para carga de imágenes asíncronas
    implementation("io.coil-kt:coil-compose:2.4.0")
}
```

---

## FASE 2: Capa de Datos (Data Layer)

### Paso 2.1: Modelos de Datos

Las clases de datos representan las entidades devueltas por el API de Node.js.

> 📋 **INSTRUCCIÓN:** Modelos de datos para Mascotas, Refugios y Usuarios.

```kotlin
/**
 * Modelo de datos para las mascotas registradas.
 */
data class Mascota(
    val id: Int,
    val nombre: String,
    val especie: String,
    val raza: String,
    val edad: String,
    val descripcion: String,
    val foto_url: String? = null
)

/**
 * Modelo de datos para los refugios locales.
 */
data class Refugio(
    val id: Int,
    val nombre: String,
    val direccion: String,
    val telefono: String,
    val horarios: String,
    val video_url: String? = null
)
```

### Paso 2.2: Cliente Retrofit (`RetrofitClient.kt`)

`RetrofitClient` provee la instancia única (Singleton) para conectarse al backend Node.js.

```kotlin
object RetrofitClient {
    // Para el emulador de Android Studio se usa 10.0.2.2 
    private const val BASE_URL = "http://10.0.2.2:3000/api/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
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
```

### Paso 2.3: Interfaz del API (`LomitoApi.kt`)

Define los endpoints REST (GET, POST, PUT) que conectan con Node.js.

```kotlin
interface LomitoApi {
    @GET("mascotas")
    suspend fun getMascotas(): List<Mascota>

    @GET("refugios")
    suspend fun getRefugios(): List<Refugio>
    
    @Multipart
    @POST("upload")
    suspend fun uploadFoto(@Part foto: MultipartBody.Part): Response<UploadResponse>
}
```

---

## FASE 3: Capa de Presentación (UI & ViewModels)

### Paso 3.1: ViewModel para Gestión de Estado (`DashboardViewModel.kt`)

El ViewModel se encarga de realizar la llamada de red y exponer un `StateFlow` a la UI.

```kotlin
class DashboardViewModel : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    fun cargarDatos() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val mascotas = RetrofitClient.api.getMascotas()
                val refugios = RetrofitClient.api.getRefugios()
                _state.value = _state.value.copy(
                    isLoading = false,
                    mascotas = mascotas,
                    refugios = refugios
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
```

### Paso 3.2: Jetpack Compose Navigation (`NavGraph.kt`)

Define las rutas (`Dashboard`, `DetalleMascota`, `ReportarMascota`) y el enrutamiento.

```kotlin
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                onNavigateToDetail = { mascotaId -> 
                    navController.navigate("detalle/$mascotaId") 
                }
            )
        }
        // ... otras rutas
    }
}
```

---
**Nota Final:** Con esta estructura, Lomito Seguro Mobile separa claramente sus responsabilidades, logrando un código mantenible y reactivo gracias a Jetpack Compose y las corrutinas de Kotlin.
