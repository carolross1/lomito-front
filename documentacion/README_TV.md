# Guia Paso a Paso: Construyendo el Modulo TV (Android TV) de Lomito Seguro

Esta guia documenta y desglosa la construccion del modulo **TV (Android TV)** de **Lomito Seguro**.

---

## Objetivo de Esta Guia

1. Como construir una app para **Android TV** optimizada para control remoto y pantalla grande.
2. Como integrar **ExoPlayer / Media3** para reproduccion de video en tiempo real (streaming HLS/RTSP).
3. Como conectar la TV con el backend **Spring Boot** usando **Retrofit**.
4. Como implementar el patron **MVVM** adaptado para Android TV con foco en D-pad navigation.

## Arquitectura del Módulo TV

```text
tv/
├── LomitoTvApp.kt        # Application class
├── data/
│   ├── api/              # Interfaz Retrofit + Cliente HTTP
│   ├── model/            # Data classes del dominio TV
│   └── repository/       # Repositorio de datos TV
├── ui/
│   ├── dashboard/        # Pantalla principal con lista de mascotas
│   ├── detalle/          # Vista de detalle de mascota
│   ├── perfil/           # Perfil de mascota en pantalla grande
│   ├── refugio/          # Difusión en vivo del refugio (ExoPlayer)
│   └── theme/            # Tema visual Material Design para TV
└── util/                 # Utilidades y extensiones
```
## FASE 1: `com/lomito/seguro/tv`

### Paso 1.1: `LomitoTvApp.kt`

**Aplicación TV**. Clase Application del módulo TV; inicializa componentes globales como Retrofit y configura el contexto de la app.

```kotlin
// Paquete: com.lomito.seguro.tv
package com.lomito.seguro.tv

// Importa la dependencia necesaria: Application
import android.app.Application

/**
 * [Clase principal de la aplicación Lomito Tv]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Inicializar el contexto global de la aplicación]
 * - [Servir como punto de entrada de la app en Android TV]
 */
// Declaración de la clase LomitoTvApp
class LomitoTvApp : Application()
```

## FASE 2: `com/lomito/seguro/tv/data/api`

### Paso 2.1: `LomitoTvApi.kt`

**Interfaz API para TV**. Define los endpoints REST del backend accesibles desde el módulo TV con Retrofit.

```kotlin
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
```

### Paso 2.2: `RetrofitClient.kt`

**Cliente Retrofit singleton** para la capa de red. Configura el cliente OkHttp con interceptores, establece la URL base del servidor y construye la instancia de Retrofit con el conversor Gson para serialización JSON.

```kotlin
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
```

## FASE 3: `com/lomito/seguro/tv/data/model`

### Paso 3.1: `Models.kt`

**Modelos de datos** del dominio. Define las data classes que representan las entidades principales: Mascota, Usuario, Reporte, Refugio, etc.

```kotlin
// Paquete: com.lomito.seguro.tv.data.model
package com.lomito.seguro.tv.data.model

// Importa la dependencia necesaria: SerializedName
import com.google.gson.annotations.SerializedName

/**
 * [Clase de datos que representa a una Mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Contener la información básica y de estado de una mascota]
 * - [Mapear los atributos desde el modelo remoto de datos]
 */
// Clase de datos Mascota: modelo inmutable con propiedades de dominio
data class Mascota(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String = "",
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String = "",
    // Constante especie: valor inmutable que no cambia tras su asignación
    val especie: String = "",
    // Constante raza: valor inmutable que no cambia tras su asignación
    val raza: String = "",
    // Constante edad: valor inmutable que no cambia tras su asignación
    val edad: Int = 0,
    // Constante color: valor inmutable que no cambia tras su asignación
    val color: String = "",
    // Constante peso: valor inmutable que no cambia tras su asignación
    val peso: Double = 0.0,
    @SerializedName("foto_url") val fotoUrl: String? = null,
    @SerializedName("distancia_alerta") val distanciaAlerta: Int = 50,
    // Constante estado: valor inmutable que no cambia tras su asignación
    val estado: String = "EN_CASA",
    // Constante activa: valor inmutable que no cambia tras su asignación
    val activa: Boolean = true,
    @SerializedName("owner_id") val ownerId: String = "",
    // Constante latitud: valor inmutable que no cambia tras su asignación
    val latitud: Double? = null,
    // Constante longitud: valor inmutable que no cambia tras su asignación
    val longitud: Double? = null
)

/**
 * [Clase de datos que representa un Reporte de Vista de mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Almacenar los datos de ubicación, dirección y tiempo del avistamiento]
 * - [Vincular el reporte con una mascota y usuario específicos]
 */
// Clase de datos ReporteVista: modelo inmutable con propiedades de dominio
data class ReporteVista(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String = "",
    @SerializedName("mascota_id") val mascotaId: String = "",
    @SerializedName("reportado_por_id") val reportadoPorId: String? = null,
    // Constante latitud: valor inmutable que no cambia tras su asignación
    val latitud: Double = 0.0,
    // Constante longitud: valor inmutable que no cambia tras su asignación
    val longitud: Double = 0.0,
    // Constante direccion: valor inmutable que no cambia tras su asignación
    val direccion: String = "",
    // Constante timestamp: valor inmutable que no cambia tras su asignación
    val timestamp: String = ""
)

/**
 * [Clase de datos que representa un Refugio para animales]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mantener la información de contacto y horarios del refugio]
 * - [Proporcionar enlaces a recursos multimedia del refugio]
 */
// Clase de datos Refugio: modelo inmutable con propiedades de dominio
data class Refugio(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String = "",
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String = "",
    // Constante direccion: valor inmutable que no cambia tras su asignación
    val direccion: String = "",
    // Constante telefono: valor inmutable que no cambia tras su asignación
    val telefono: String = "",
    // Constante horarios: valor inmutable que no cambia tras su asignación
    val horarios: String = "",
    @SerializedName("video_url") val videoUrl: String? = null,
    @SerializedName("logo_url") val logoUrl: String? = null
)
```

## FASE 4: `com/lomito/seguro/tv/data/repository`

### Paso 4.1: `LomitoTvRepository.kt`

**Archivo `LomitoTvRepository.kt`** del paquete `com/lomito/seguro/tv/data/repository`.

```kotlin
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
```

## FASE 5: `com/lomito/seguro/tv/ui/dashboard`

### Paso 5.1: `DashboardActivity.kt`

**Actividad del Dashboard**. Pantalla principal del módulo TV que muestra las mascotas en una cuadrícula optimizada para pantalla grande con control remoto.

```kotlin
// Paquete: com.lomito.seguro.tv.ui.dashboard
package com.lomito.seguro.tv.ui.dashboard

// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa la dependencia necesaria: viewModels
import androidx.activity.viewModels
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Arrangement
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Box
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Column
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.aspectRatio
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.fillMaxSize
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.fillMaxWidth
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.height
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.padding
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.width
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.RoundedCornerShape
// Importa componente de Jetpack Compose
import androidx.compose.runtime.Composable
// Importa componente de Jetpack Compose
import androidx.compose.runtime.collectAsState
// Importa componente de Jetpack Compose
import androidx.compose.runtime.getValue
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
import androidx.compose.ui.layout.ContentScale
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.style.TextOverflow
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.LazyRow
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.items
// Importa la dependencia necesaria: Card
import androidx.tv.material3.Card
// Importa la dependencia necesaria: CardDefaults
import androidx.tv.material3.CardDefaults
// Importa la dependencia necesaria: MaterialTheme
import androidx.tv.material3.MaterialTheme
// Importa la dependencia necesaria: Surface
import androidx.tv.material3.Surface
// Importa la dependencia necesaria: Text
import androidx.tv.material3.Text
// Importa componente de Jetpack Compose
import coil.compose.AsyncImage
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.tv.data.model.Mascota
// Importa la dependencia necesaria: Refugio
import com.lomito.seguro.tv.data.model.Refugio
// Importa la dependencia necesaria: MascotaDetalleActivity
import com.lomito.seguro.tv.ui.detalle.MascotaDetalleActivity
// Importa la dependencia necesaria: RefugioDifusionActivity
import com.lomito.seguro.tv.ui.refugio.RefugioDifusionActivity
// Importa la dependencia necesaria: LomitoFoundGreen
import com.lomito.seguro.tv.ui.theme.LomitoFoundGreen
// Importa la dependencia necesaria: LomitoAlertRed
import com.lomito.seguro.tv.ui.theme.LomitoAlertRed
// Importa la dependencia necesaria: LomitoOrange
import com.lomito.seguro.tv.ui.theme.LomitoOrange
// Importa la dependencia necesaria: LomitoSurfaceAlt
import com.lomito.seguro.tv.ui.theme.LomitoSurfaceAlt
// Importa la dependencia necesaria: LomitoTvTheme
import com.lomito.seguro.tv.ui.theme.LomitoTvTheme
// Importa la dependencia necesaria: toAbsoluteUrl
import com.lomito.seguro.tv.util.toAbsoluteUrl

/**
 * [Actividad principal del Dashboard para Android TV]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mostrar el mural comunitario de mascotas perdidas y encontradas]
 * - [Mostrar el directorio de refugios locales]
 */
// Activity DashboardActivity: pantalla principal que gestiona el ciclo de vida
class DashboardActivity : ComponentActivity() {

    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: DashboardViewModel by viewModels()

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)
        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            LomitoTvTheme {
                DashboardScreen(
                    viewModel = viewModel,
                    onMascotaClick = { mascota ->
                        startActivity(
                            Intent(this, MascotaDetalleActivity::class.java)
                                .putExtra(MascotaDetalleActivity.EXTRA_MASCOTA_ID, mascota.id)
                        )
                    },
                    onRefugioClick = { refugio ->
                        startActivity(
                            Intent(this, RefugioDifusionActivity::class.java)
                                .putExtra(RefugioDifusionActivity.EXTRA_REFUGIO_ID, refugio.id)
                        )
                    }
                )
            }
        }
    }
}

/**
 * [Pantalla componible del Dashboard]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - viewModel: [ViewModel que provee el estado de la pantalla]
 * - onMascotaClick: [Callback al seleccionar una mascota]
 * - onRefugioClick: [Callback al seleccionar un refugio]
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función DashboardScreen: define la lógica de esta operación
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onMascotaClick: (Mascota) -> Unit,
    onRefugioClick: (Refugio) -> Unit
) {
    // Constante state: valor inmutable que no cambia tras su asignación
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp)
        ) {
            DashboardHeader()

            Column(modifier = Modifier.padding(top = 32.dp)) {
                Text(
                    text = "MURAL DE MASCOTAS PERDIDAS / ENCONTRADAS",
                    color = LomitoOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Box(modifier = Modifier.padding(top = 16.dp)) {
                    // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
                    when {
                        state.cargando -> Text(
                            text = "Cargando mural comunitario…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.mascotas.isEmpty() -> Text(
                            text = "Sin reportes por el momento. ¡Buenas noticias!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            items(state.mascotas, key = { it.id }) { mascota ->
                                MascotaMuralCard(mascota = mascota, onClick = { onMascotaClick(mascota) })
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(top = 40.dp)) {
                Text(
                    text = "REFUGIOS LOCALES",
                    color = LomitoOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Box(modifier = Modifier.padding(top = 16.dp)) {
                    // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
                    when {
                        state.cargando -> Text(
                            text = "Cargando refugios…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.refugios.isEmpty() -> Text(
                            text = "No hay refugios registrados todavía.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            items(state.refugios, key = { it.id }) { refugio ->
                                RefugioCard(refugio = refugio, onClick = { onRefugioClick(refugio) })
                            }
                        }
                    }
                }
            }
        }
    }
}

// Anotación que marca esta función como una función de composición de UI
@Composable
private fun DashboardHeader() {
    Column {
        Text(
            text = "🐾 Lomito Seguro",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp
        )
        Text(
            text = "Comunidad · Dolores Hidalgo, Guanajuato",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp
        )
    }
}

// Anotación que marca esta función como una función de composición de UI
@Composable
private fun MascotaMuralCard(mascota: Mascota, onClick: () -> Unit) {
    // Constante perdida: valor inmutable que no cambia tras su asignación
    val perdida = mascota.estado.equals("PERDIDA", ignoreCase = true)

    Card(
        onClick = onClick,
        modifier = Modifier.width(220.dp),
        colors = CardDefaults.colors(containerColor = LomitoSurfaceAlt),
        border = CardDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(3.dp, LomitoOrange),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        shape = CardDefaults.shape(RoundedCornerShape(12.dp))
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.3f)) {
                AsyncImage(
                    model = mascota.fotoUrl.toAbsoluteUrl(),
                    contentDescription = mascota.nombre,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                EstadoBadge(
                    texto = if (perdida) "Perdida" else "Encontrada",
                    color = if (perdida) LomitoAlertRed else LomitoFoundGreen,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = mascota.nombre,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${mascota.raza.ifBlank { mascota.especie }} · ${mascota.edad} años",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Anotación que marca esta función como una función de composición de UI
@Composable
private fun RefugioCard(refugio: Refugio, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(260.dp).height(140.dp),
        colors = CardDefaults.colors(containerColor = LomitoSurfaceAlt),
        border = CardDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(3.dp, LomitoOrange),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        shape = CardDefaults.shape(RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = refugio.nombre,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = refugio.direccion,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (!refugio.videoUrl.isNullOrBlank()) {
                Text(
                    text = "● EN VIVO",
                    color = LomitoAlertRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

// Anotación que marca esta función como una función de composición de UI
@Composable
private fun EstadoBadge(texto: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        colors = androidx.tv.material3.SurfaceDefaults.colors(containerColor = color)
    ) {
        Text(
            text = texto,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
```

### Paso 5.2: `DashboardViewModel.kt`

**ViewModel del Dashboard TV**. Gestiona el estado del dashboard: carga de mascotas, manejo de errores y actualización de datos.

```kotlin
// Paquete: com.lomito.seguro.tv.ui.dashboard
package com.lomito.seguro.tv.ui.dashboard

// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModel
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.tv.data.model.Mascota
// Importa la dependencia necesaria: Refugio
import com.lomito.seguro.tv.data.model.Refugio
// Importa la dependencia necesaria: LomitoTvRepository
import com.lomito.seguro.tv.data.repository.LomitoTvRepository
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.delay
// Importa el observable de datos reactivos
import kotlinx.coroutines.flow.MutableStateFlow
// Importa el observable de datos reactivos
import kotlinx.coroutines.flow.StateFlow
// Importa el observable de datos reactivos
import kotlinx.coroutines.flow.asStateFlow
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch

// Clase de datos DashboardUiState: modelo inmutable con propiedades de dominio
data class DashboardUiState(
    // Constante cargando: valor inmutable que no cambia tras su asignación
    val cargando: Boolean = true,
    // Constante mascotas: valor inmutable que no cambia tras su asignación
    val mascotas: List<Mascota> = emptyList(),
    // Constante refugios: valor inmutable que no cambia tras su asignación
    val refugios: List<Refugio> = emptyList()
)

/** Cada cuánto se refresca el mural sin interacción del usuario (pantalla comunitaria). */
// Constante AUTO_REFRESH_MS: valor fijo definido en tiempo de compilación
private const val AUTO_REFRESH_MS = 30_000L

/**
 * [ViewModel del Dashboard]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Cargar y exponer el estado de las mascotas y refugios]
 * - [Manejar la lógica de auto-refresco periódico]
 */
// ViewModel DashboardViewModel: gestiona el estado y la lógica de negocio de la pantalla
class DashboardViewModel(
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo: LomitoTvRepository = LomitoTvRepository()
) : ViewModel() {

    // Constante _uiState: valor inmutable que no cambia tras su asignación
    private val _uiState = MutableStateFlow(DashboardUiState())
    // Constante uiState: valor inmutable que no cambia tras su asignación
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        cargar()
        autoRefresh()
    }

    // Función cargar: define la lógica de esta operación
    fun cargar() {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true)
            // Constante mascotas: valor inmutable que no cambia tras su asignación
            val mascotas = repo.getMuralMascotas()
            // Constante refugios: valor inmutable que no cambia tras su asignación
            val refugios = repo.getRefugios()
            _uiState.value = DashboardUiState(cargando = false, mascotas = mascotas, refugios = refugios)
        }
    }

    private fun autoRefresh() {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_MS)
                // Constante mascotas: valor inmutable que no cambia tras su asignación
                val mascotas = repo.getMuralMascotas()
                // Constante refugios: valor inmutable que no cambia tras su asignación
                val refugios = repo.getRefugios()
                _uiState.value = _uiState.value.copy(mascotas = mascotas, refugios = refugios)
            }
        }
    }
}
```

## FASE 6: `com/lomito/seguro/tv/ui/detalle`

### Paso 6.1: `MapaView.kt`

**Vista del mapa**. Componente personalizado para mostrar el mapa con la ubicación del refugio en la pantalla del TV.

```kotlin
// MapaView.kt - OSMDroid con SSL ignorado
// Paquete: com.lomito.seguro.tv.ui.detalle
package com.lomito.seguro.tv.ui.detalle

// Importa el contexto de Android
import android.content.Context
// Importa componente de Jetpack Compose
import androidx.compose.runtime.Composable
// Importa componente de Jetpack Compose
import androidx.compose.runtime.remember
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
import androidx.compose.ui.platform.LocalContext
// Importa componente de Jetpack Compose
import androidx.compose.ui.viewinterop.AndroidView
// Importa la dependencia necesaria: ReporteVista
import com.lomito.seguro.tv.data.model.ReporteVista
// Importa la dependencia necesaria: Configuration
import org.osmdroid.config.Configuration
// Importa la dependencia necesaria: TileSourceFactory
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
// Importa la dependencia necesaria: GeoPoint
import org.osmdroid.util.GeoPoint
// Importa componentes de la interfaz gráfica
import org.osmdroid.views.MapView
// Importa la dependencia necesaria: Marker
import org.osmdroid.views.overlay.Marker
// Importa la dependencia necesaria: Polyline
import org.osmdroid.views.overlay.Polyline
// Importa la dependencia necesaria: File
import java.io.File
// Importa la dependencia necesaria: HttpsURLConnection
import javax.net.ssl.HttpsURLConnection
// Importa el contexto de Android
import javax.net.ssl.SSLContext
// Importa la dependencia necesaria: TrustManager
import javax.net.ssl.TrustManager
// Importa la dependencia necesaria: X509TrustManager
import javax.net.ssl.X509TrustManager
// Importa la dependencia necesaria: X509Certificate
import java.security.cert.X509Certificate

/**
 * [Componente de Mapa interactivo para Android TV]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - lat: [Latitud central del mapa]
 * - lng: [Longitud central del mapa]
 * - reportes: [Lista de reportes a mostrar como marcadores]
 * - modifier: [Modificador para el componente visual]
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MapaView: define la lógica de esta operación
fun MapaView(
    lat: Double,
    lng: Double,
    reportes: List<ReporteVista> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Constante context: valor inmutable que no cambia tras su asignación
    val context = LocalContext.current

    // ✅ Configuración para ignorar SSL (solo para desarrollo)
    remember {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Constante trustAllCerts: valor inmutable que no cambia tras su asignación
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                // Sobreescribe la función checkClientTrusted de la clase padre
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                // Sobreescribe la función checkServerTrusted de la clase padre
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                // Sobreescribe la función getAcceptedIssuers de la clase padre
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            // Constante sslContext: valor inmutable que no cambia tras su asignación
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Configuration.getInstance().load(
            context,
            // Accede al almacenamiento clave-valor persistente de la aplicación
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().osmdroidBasePath = File(context.cacheDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        // ✅ Usar tile source con HTTP en lugar de HTTPS
        System.setProperty("http.agent", "")
        Unit
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setBuiltInZoomControls(true)
                setMultiTouchControls(true)
                setMinZoomLevel(5.0)
                setMaxZoomLevel(19.0)
            }
        },
        update = { mapView ->
            mapView.overlays.clear()

            // Constante puntos: valor inmutable que no cambia tras su asignación
            val puntos = mutableListOf<GeoPoint>()

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (reportes.isNotEmpty()) {
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                reportes.forEach { reporte ->
                    // Constante punto: valor inmutable que no cambia tras su asignación
                    val punto = GeoPoint(reporte.latitud, reporte.longitud)
                    puntos.add(punto)

                    // Constante marker: valor inmutable que no cambia tras su asignación
                    val marker = Marker(mapView)
                    marker.position = punto
                    marker.title = "Reporte #${reporte.id}"
                    marker.snippet = reporte.direccion
                    mapView.overlays.add(marker)
                }
            } else {
                // Constante punto: valor inmutable que no cambia tras su asignación
                val punto = GeoPoint(lat, lng)
                puntos.add(punto)

                // Constante marker: valor inmutable que no cambia tras su asignación
                val marker = Marker(mapView)
                marker.position = punto
                marker.title = "Última ubicación"
                mapView.overlays.add(marker)
            }

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (puntos.size > 1) {
                // Constante polyline: valor inmutable que no cambia tras su asignación
                val polyline = Polyline()
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                puntos.forEach { punto ->
                    polyline.addPoint(punto)
                }
                polyline.outlinePaint.color = android.graphics.Color.RED
                polyline.outlinePaint.strokeWidth = 5f
                mapView.overlays.add(polyline)
            }

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (puntos.isNotEmpty()) {
                mapView.controller.setCenter(puntos.first())
                mapView.controller.setZoom(15.0)
            }

            mapView.invalidate()
        }
    )
}
```

### Paso 6.2: `MascotaDetalleActivity.kt`

**Actividad de detalle de mascota (TV)**. Muestra la información completa de una mascota en la pantalla grande del TV.

```kotlin
// Paquete: com.lomito.seguro.tv.ui.detalle
package com.lomito.seguro.tv.ui.detalle

// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa la dependencia necesaria: viewModels
import androidx.activity.viewModels
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Arrangement
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Box
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Column
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Row
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Spacer
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.fillMaxHeight
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.fillMaxSize
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.fillMaxWidth
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.height
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.padding
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.size
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.width
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.RoundedCornerShape
// Importa componente de Jetpack Compose
import androidx.compose.runtime.Composable
// Importa componente de Jetpack Compose
import androidx.compose.runtime.LaunchedEffect
// Importa componente de Jetpack Compose
import androidx.compose.runtime.collectAsState
// Importa componente de Jetpack Compose
import androidx.compose.runtime.getValue
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
import androidx.compose.ui.draw.clip
// Importa componente de Jetpack Compose
import androidx.compose.ui.layout.ContentScale
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa la dependencia necesaria: Button
import androidx.tv.material3.Button
// Importa la dependencia necesaria: MaterialTheme
import androidx.tv.material3.MaterialTheme
// Importa la dependencia necesaria: Surface
import androidx.tv.material3.Surface
// Importa la dependencia necesaria: SurfaceDefaults
import androidx.tv.material3.SurfaceDefaults
// Importa la dependencia necesaria: Text
import androidx.tv.material3.Text
// Importa componente de Jetpack Compose
import coil.compose.AsyncImage
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.tv.data.model.Mascota
// Importa la dependencia necesaria: ReporteVista
import com.lomito.seguro.tv.data.model.ReporteVista
// Importa la dependencia necesaria: MascotaPerfilActivity
import com.lomito.seguro.tv.ui.perfil.MascotaPerfilActivity
// Importa la dependencia necesaria: LomitoAlertRed
import com.lomito.seguro.tv.ui.theme.LomitoAlertRed
// Importa la dependencia necesaria: LomitoFoundGreen
import com.lomito.seguro.tv.ui.theme.LomitoFoundGreen
// Importa la dependencia necesaria: LomitoOrange
import com.lomito.seguro.tv.ui.theme.LomitoOrange
// Importa la dependencia necesaria: LomitoSurfaceAlt
import com.lomito.seguro.tv.ui.theme.LomitoSurfaceAlt
// Importa la dependencia necesaria: LomitoTvTheme
import com.lomito.seguro.tv.ui.theme.LomitoTvTheme
// Importa la dependencia necesaria: toAbsoluteUrl
import com.lomito.seguro.tv.util.toAbsoluteUrl

/**
 * [Actividad de Detalle de Mascota para Android TV]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mostrar la información detallada de una mascota específica]
 * - [Permitir confirmar avistamientos solicitando número de contacto]
 */
// Activity MascotaDetalleActivity: pantalla principal que gestiona el ciclo de vida
class MascotaDetalleActivity : ComponentActivity() {

    companion object {
        // Constante EXTRA_MASCOTA_ID: valor fijo definido en tiempo de compilación
        const val EXTRA_MASCOTA_ID = "mascota_id"
    }

    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: MascotaDetalleViewModel by viewModels()

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)
        // Constante mascotaId: valor inmutable que no cambia tras su asignación
        val mascotaId = intent.getStringExtra(EXTRA_MASCOTA_ID).orEmpty()

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            LomitoTvTheme {
                MascotaDetalleScreen(
                    viewModel = viewModel,
                    mascotaId = mascotaId,
                    onVerPerfilCompleto = {
                        startActivity(
                            Intent(this, MascotaPerfilActivity::class.java)
                                .putExtra(MascotaPerfilActivity.EXTRA_MASCOTA_ID, mascotaId)
                        )
                    }
                )
            }
        }
    }
}

/**
 * [Pantalla componible de Detalle de Mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - viewModel: [ViewModel que gestiona el estado y lógica del detalle]
 * - mascotaId: [Identificador único de la mascota a mostrar]
 * - onVerPerfilCompleto: [Callback para navegar al perfil completo]
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MascotaDetalleScreen: define la lógica de esta operación
fun MascotaDetalleScreen(
    viewModel: MascotaDetalleViewModel,
    mascotaId: String,
    onVerPerfilCompleto: () -> Unit
) {
    LaunchedEffect(mascotaId) { viewModel.cargar(mascotaId) }
    // Constante state: valor inmutable que no cambia tras su asignación
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp)
    ) {
        // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
        when {
            state.cargando -> Text(
                text = "Cargando ficha…",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.mascota == null -> Text(
                text = "No se encontró información de esta mascota.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> MascotaDetalleContenido(
                mascota = state.mascota!!,
                ultimoReporte = state.ultimoReporte,
                enviando = state.enviando,
                onAyudar = { viewModel.abrirDialogoContacto() },
                onVerPerfilCompleto = onVerPerfilCompleto
            )
        }

        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (state.mostrandoDialogoContacto) {
            DialogoContacto(
                numero = state.numeroContacto,
                onDigito = { viewModel.agregarDigito(it) },
                onBorrar = { viewModel.borrarDigito() },
                onCancelar = { viewModel.cerrarDialogoContacto() },
                onConfirmar = { viewModel.confirmarAvistamiento() }
            )
        }

        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (state.mostrandoConfirmacionEnvio || state.errorEnvio) {
            DialogoResultadoEnvio(
                exito = state.mostrandoConfirmacionEnvio,
                mascotaNombre = state.mascota?.nombre ?: "la mascota",
                onCerrar = { viewModel.cerrarConfirmacionEnvio() }
            )
        }
    }
}

// Anotación que marca esta función como una función de composición de UI
@Composable
private fun MascotaDetalleContenido(
    mascota: Mascota,
    ultimoReporte: ReporteVista?,
    enviando: Boolean,
    onAyudar: () -> Unit,
    onVerPerfilCompleto: () -> Unit
) {
    // Constante perdida: valor inmutable que no cambia tras su asignación
    val perdida = mascota.estado.equals("PERDIDA", ignoreCase = true)

    Row(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = mascota.fotoUrl.toAbsoluteUrl(),
            contentDescription = mascota.nombre,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(420.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(LomitoSurfaceAlt)
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 40.dp)
        ) {
            Row {
                Text(
                    text = mascota.nombre,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    colors = SurfaceDefaults.colors(
                        containerColor = if (perdida) LomitoAlertRed else LomitoFoundGreen
                    )
                ) {
                    Text(
                        text = if (perdida) "Perdida" else "Encontrada",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            InfoRow(label = "Raza / especie", valor = "${mascota.raza.ifBlank { "—" }} · ${mascota.especie}")
            InfoRow(label = "Edad", valor = "${mascota.edad} años")
            InfoRow(label = "Color", valor = mascota.color.ifBlank { "No especificado" })
            InfoRow(
                label = "Última ubicación vista",
                valor = ultimoReporte?.direccion?.ifBlank { "Sin dirección registrada" }
                    ?: "Aún sin reportes de avistamiento"
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row {
                Button(onClick = onAyudar, enabled = !enviando) {
                    Text(
                        text = if (enviando) "Enviando…" else "Ayudar (confirmar avistado)"
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onVerPerfilCompleto) {
                    Text(text = "Ver perfil completo")
                }
            }
        }
    }
}

// Anotación que marca esta función como una función de composición de UI
@Composable
private fun InfoRow(label: String, valor: String) {
    Column(modifier = Modifier.padding(top = 18.dp)) {
        Text(
            text = label.uppercase(),
            color = LomitoOrange,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = valor,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/**
 * Teclado numérico para pedir el contacto de quien confirma el avistamiento.
 * Se navega con el D-pad del control remoto, por eso es un grid de botones
 * en vez de un campo de texto con teclado del sistema (mismo criterio que se
 * usó para el teclado del módulo wear).
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
private fun DialogoContacto(
    numero: String,
    onDigito: (String) -> Unit,
    onBorrar: () -> Unit,
    onCancelar: () -> Unit,
    onConfirmar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            colors = SurfaceDefaults.colors(containerColor = LomitoSurfaceAlt)
        ) {
            // ✅ Layout horizontal (teclado a la izquierda, info/acciones a la
            // derecha) en vez de todo apilado en una sola columna: una TV es
            // mucho más ancha que alta, y apilado se salía de la pantalla y
            // dejaba el botón "Confirmar" fuera de la vista/alcance del control.
            Row(
                modifier = Modifier.padding(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Constante filas: valor inmutable que no cambia tras su asignación
                val filas = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("Borrar", "0", "")
                )
                Column {
                    // Itera sobre cada elemento de la colección y ejecuta el bloque
                    filas.forEach { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Itera sobre cada elemento de la colección y ejecuta el bloque
                            fila.forEach { tecla ->
                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                if (tecla.isEmpty()) {
                                    Spacer(modifier = Modifier.size(56.dp))
                                } else {
                                    Button(
                                        onClick = { if (tecla == "Borrar") onBorrar() else onDigito(tecla) },
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Text(text = if (tecla == "Borrar") "⌫" else tecla, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                Column(
                    modifier = Modifier.width(320.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "¿A qué número te pueden contactar?",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Así el dueño puede pedirte más información",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
                    ) {
                        Text(
                            text = numero.ifEmpty { "__________" },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ✅ Botón para guardar/enviar el número: queda deshabilitado
                    // (y se ve apagado) hasta tener 10 dígitos.
                    Button(
                        onClick = onConfirmar,
                        enabled = numero.length >= 10
                    ) {
                        Text(text = "Confirmar avistamiento")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onCancelar) {
                        Text(text = "Cancelar")
                    }
                }
            }
        }
    }
}

/**
 * Mensaje de confirmación tras enviar el reporte: le dice claramente a quien
 * está frente a la TV si el aviso se mandó (y que el dueño fue notificado)
 * o si algo falló y debe intentar de nuevo.
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
private fun DialogoResultadoEnvio(
    exito: Boolean,
    mascotaNombre: String,
    onCerrar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            colors = SurfaceDefaults.colors(containerColor = LomitoSurfaceAlt)
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (exito) "✅ ¡Reporte enviado!" else "❌ No se pudo enviar",
                    color = if (exito) LomitoFoundGreen else LomitoAlertRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                )
                Text(
                    text = if (exito)
                        "Le avisamos al dueño de $mascotaNombre, se pondrá en contacto contigo."
                    else
                        "Ocurrió un problema al enviar tu reporte. Intenta de nuevo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
                )
                Button(onClick = onCerrar) {
                    Text(text = "Cerrar")
                }
            }
        }
    }
}
```

### Paso 6.3: `MascotaDetalleViewModel.kt`

**ViewModel de detalle de mascota (TV)**. Gestiona los datos y estado de la pantalla de detalle de mascota para TV.

```kotlin
// Paquete: com.lomito.seguro.tv.ui.detalle
package com.lomito.seguro.tv.ui.detalle

// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModel
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.tv.data.model.Mascota
// Importa la dependencia necesaria: ReporteVista
import com.lomito.seguro.tv.data.model.ReporteVista
// Importa la dependencia necesaria: LomitoTvRepository
import com.lomito.seguro.tv.data.repository.LomitoTvRepository
// Importa el observable de datos reactivos
import kotlinx.coroutines.flow.MutableStateFlow
// Importa el observable de datos reactivos
import kotlinx.coroutines.flow.StateFlow
// Importa el observable de datos reactivos
import kotlinx.coroutines.flow.asStateFlow
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch

// Clase de datos MascotaDetalleUiState: modelo inmutable con propiedades de dominio
data class MascotaDetalleUiState(
    // Constante cargando: valor inmutable que no cambia tras su asignación
    val cargando: Boolean = true,
    // Constante mascota: valor inmutable que no cambia tras su asignación
    val mascota: Mascota? = null,
    // Constante ultimoReporte: valor inmutable que no cambia tras su asignación
    val ultimoReporte: ReporteVista? = null,
    // Constante mascotaId: valor inmutable que no cambia tras su asignación
    val mascotaId: String = "",
    // Constante enviando: valor inmutable que no cambia tras su asignación
    val enviando: Boolean = false,
    // Constante mostrandoDialogoContacto: valor inmutable que no cambia tras su asignación
    val mostrandoDialogoContacto: Boolean = false,
    // Constante mostrandoConfirmacionEnvio: valor inmutable que no cambia tras su asignación
    val mostrandoConfirmacionEnvio: Boolean = false,
    // Constante errorEnvio: valor inmutable que no cambia tras su asignación
    val errorEnvio: Boolean = false,
    // Constante numeroContacto: valor inmutable que no cambia tras su asignación
    val numeroContacto: String = ""
)

/**
 * [ViewModel de Detalle de Mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Cargar los detalles y último reporte de la mascota]
 * - [Gestionar la lógica de envío de nuevos reportes de avistamiento]
 */
// ViewModel MascotaDetalleViewModel: gestiona el estado y la lógica de negocio de la pantalla
class MascotaDetalleViewModel(
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo: LomitoTvRepository = LomitoTvRepository()
) : ViewModel() {

    // Constante _uiState: valor inmutable que no cambia tras su asignación
    private val _uiState = MutableStateFlow(MascotaDetalleUiState())
    // Constante uiState: valor inmutable que no cambia tras su asignación
    val uiState: StateFlow<MascotaDetalleUiState> = _uiState.asStateFlow()

    // Función cargar: define la lógica de esta operación
    fun cargar(mascotaId: String) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true)
            // Constante mascota: valor inmutable que no cambia tras su asignación
            val mascota = repo.getMascotaById(mascotaId)
            // Constante reportes: valor inmutable que no cambia tras su asignación
            val reportes = repo.getReportesDeMascota(mascotaId)
            _uiState.value = MascotaDetalleUiState(
                cargando = false,
                mascota = mascota,
                mascotaId = mascotaId,
                ultimoReporte = reportes.maxByOrNull { it.timestamp }
            )
        }
    }

    // ✅ El botón "Ayudar" ya no confirma directo: primero abre el teclado
    // numérico para pedir un contacto con el que el dueño pueda comunicarse.
    // Función abrirDialogoContacto: define la lógica de esta operación
    fun abrirDialogoContacto() {
        _uiState.value = _uiState.value.copy(mostrandoDialogoContacto = true, numeroContacto = "")
    }

    // Función cerrarDialogoContacto: define la lógica de esta operación
    fun cerrarDialogoContacto() {
        _uiState.value = _uiState.value.copy(mostrandoDialogoContacto = false, numeroContacto = "")
    }

    // Función agregarDigito: define la lógica de esta operación
    fun agregarDigito(digito: String) {
        // Constante actual: valor inmutable que no cambia tras su asignación
        val actual = _uiState.value.numeroContacto
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (actual.length < 10) {
            _uiState.value = _uiState.value.copy(numeroContacto = actual + digito)
        }
    }

    // Función borrarDigito: define la lógica de esta operación
    fun borrarDigito() {
        // Constante actual: valor inmutable que no cambia tras su asignación
        val actual = _uiState.value.numeroContacto
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (actual.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(numeroContacto = actual.dropLast(1))
        }
    }

    // ✅ Ya no depende de un reporte previo (el mural podía mostrar uno
    // "simulado" con id falso cuando aún no había ninguno real, y confirmar
    // contra ese id fallaba en silencio y nunca avisaba al dueño). Ahora
    // crea el reporte y lo confirma en un solo paso.
    // Función confirmarAvistamiento: define la lógica de esta operación
    fun confirmarAvistamiento() {
        // Constante mascotaId: valor inmutable que no cambia tras su asignación
        val mascotaId = _uiState.value.mascotaId
        // Constante contacto: valor inmutable que no cambia tras su asignación
        val contacto = _uiState.value.numeroContacto
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (mascotaId.isBlank() || contacto.length < 10) return
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(enviando = true, mostrandoDialogoContacto = false)
            // Constante ok: valor inmutable que no cambia tras su asignación
            val ok = repo.reportarAvistamiento(mascotaId, contacto)
            _uiState.value = _uiState.value.copy(
                enviando = false,
                mostrandoConfirmacionEnvio = ok,
                errorEnvio = !ok
            )
        }
    }

    // Función cerrarConfirmacionEnvio: define la lógica de esta operación
    fun cerrarConfirmacionEnvio() {
        _uiState.value = _uiState.value.copy(mostrandoConfirmacionEnvio = false, errorEnvio = false)
    }
}
```

## FASE 7: `com/lomito/seguro/tv/ui/perfil`

### Paso 7.1: `MascotaPerfilActivity.kt`

**Actividad de perfil de mascota (TV)**. Muestra el perfil completo de una mascota con foto grande y datos detallados.

```kotlin
// Paquete: com.lomito.seguro.tv.ui.perfil
package com.lomito.seguro.tv.ui.perfil

// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa la dependencia necesaria: viewModels
import androidx.activity.viewModels
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Box
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Column
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Row
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Spacer
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.aspectRatio
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.fillMaxHeight
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.fillMaxSize
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.fillMaxWidth
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.height
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.padding
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.width
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.LazyColumn
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.items
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.RoundedCornerShape
// Importa componente de Jetpack Compose
import androidx.compose.runtime.Composable
// Importa componente de Jetpack Compose
import androidx.compose.runtime.LaunchedEffect
// Importa componente de Jetpack Compose
import androidx.compose.runtime.collectAsState
// Importa componente de Jetpack Compose
import androidx.compose.runtime.getValue
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
import androidx.compose.ui.draw.clip
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa la dependencia necesaria: MaterialTheme
import androidx.tv.material3.MaterialTheme
// Importa la dependencia necesaria: Surface
import androidx.tv.material3.Surface
// Importa la dependencia necesaria: Text
import androidx.tv.material3.Text
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.tv.data.model.Mascota
// Importa la dependencia necesaria: ReporteVista
import com.lomito.seguro.tv.data.model.ReporteVista
// Importa componentes de la interfaz gráfica
import com.lomito.seguro.tv.ui.detalle.MapaView
// Importa la dependencia necesaria: LomitoAlertRed
import com.lomito.seguro.tv.ui.theme.LomitoAlertRed
// Importa la dependencia necesaria: LomitoOrange
import com.lomito.seguro.tv.ui.theme.LomitoOrange
// Importa la dependencia necesaria: LomitoSurfaceAlt
import com.lomito.seguro.tv.ui.theme.LomitoSurfaceAlt
// Importa la dependencia necesaria: LomitoTvTheme
import com.lomito.seguro.tv.ui.theme.LomitoTvTheme

/**
 * [Actividad de Perfil Completo de Mascota para Android TV]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mostrar el perfil detallado de la mascota]
 * - [Mostrar el historial completo de reportes en un mapa y línea de tiempo]
 */
// Activity MascotaPerfilActivity: pantalla principal que gestiona el ciclo de vida
class MascotaPerfilActivity : ComponentActivity() {

    companion object {
        // Constante EXTRA_MASCOTA_ID: valor fijo definido en tiempo de compilación
        const val EXTRA_MASCOTA_ID = "mascota_id"
    }

    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: MascotaPerfilViewModel by viewModels()

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)
        // Constante mascotaId: valor inmutable que no cambia tras su asignación
        val mascotaId = intent.getStringExtra(EXTRA_MASCOTA_ID).orEmpty()

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            LomitoTvTheme {
                MascotaPerfilScreen(viewModel = viewModel, mascotaId = mascotaId)
            }
        }
    }
}

/**
 * [Pantalla componible del Perfil de Mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - viewModel: [ViewModel que provee los datos del perfil]
 * - mascotaId: [Identificador de la mascota a consultar]
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MascotaPerfilScreen: define la lógica de esta operación
fun MascotaPerfilScreen(viewModel: MascotaPerfilViewModel, mascotaId: String) {
    LaunchedEffect(mascotaId) { viewModel.cargar(mascotaId) }
    // Constante state: valor inmutable que no cambia tras su asignación
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp)
    ) {
        // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
        when {
            state.cargando -> Text(
                text = "Cargando perfil…",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.mascota == null -> Text(
                text = "No se encontró información de esta mascota.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> MascotaPerfilContenido(mascota = state.mascota!!, reportes = state.reportes)
        }
    }
}

// Anotación que marca esta función como una función de composición de UI
@Composable
private fun MascotaPerfilContenido(mascota: Mascota, reportes: List<ReporteVista>) {
    // Constante perdida: valor inmutable que no cambia tras su asignación
    val perdida = mascota.estado.equals("PERDIDA", ignoreCase = true)

    // Constante ultimoReporte: valor inmutable que no cambia tras su asignación
    val ultimoReporte = reportes.maxByOrNull { it.timestamp }
    // Constante lat: valor inmutable que no cambia tras su asignación
    val lat = ultimoReporte?.latitud ?: mascota.latitud
    // Constante lng: valor inmutable que no cambia tras su asignación
    val lng = ultimoReporte?.longitud ?: mascota.longitud

    Column(modifier = Modifier.fillMaxSize()) {
        Row {
            Text(
                text = mascota.nombre,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                colors = androidx.tv.material3.SurfaceDefaults.colors(
                    containerColor = if (perdida) LomitoAlertRed else LomitoOrange
                )
            ) {
                Text(
                    text = if (perdida) "Perdida" else mascota.estado,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        Text(
            text = "${reportes.size} reportes de avistamiento registrados",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxHeight().width(380.dp)) {
                Text(
                    text = "LÍNEA DE TIEMPO",
                    color = LomitoOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (reportes.isEmpty()) {
                    Text(
                        text = "Aún no hay avistamientos reportados.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn {
                        items(reportes, key = { it.id }) { reporte ->
                            ReporteRow(reporte)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
                Text(
                    text = "📍 MAPA DE AVISTAMIENTOS",
                    color = LomitoOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (lat != null && lng != null) {
                    MapaView(
                        lat = lat,
                        lng = lng,
                        reportes = reportes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LomitoSurfaceAlt)
                    )

                    Text(
                        text = "Última ubicación: ${ultimoReporte?.direccion?.ifBlank { "Sin dirección" } ?: "Sin dirección"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LomitoSurfaceAlt)
                    ) {
                        Text(
                            text = "📍 Ubicación no disponible",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// Anotación que marca esta función como una función de composición de UI
@Composable
private fun ReporteRow(reporte: ReporteVista) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(LomitoOrange)
        )
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(
                text = reporte.direccion.ifBlank { "Ubicación sin dirección" },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = reporte.timestamp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}
```

### Paso 7.2: `MascotaPerfilViewModel.kt`

**ViewModel de perfil de mascota (TV)**. Gestiona los datos del perfil de mascota para TV.

```kotlin
// Paquete: com.lomito.seguro.tv.ui.perfil
package com.lomito.seguro.tv.ui.perfil

// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModel
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.tv.data.model.Mascota
// Importa la dependencia necesaria: ReporteVista
import com.lomito.seguro.tv.data.model.ReporteVista
// Importa la dependencia necesaria: LomitoTvRepository
import com.lomito.seguro.tv.data.repository.LomitoTvRepository
// Importa el observable de datos reactivos
import kotlinx.coroutines.flow.MutableStateFlow
// Importa el observable de datos reactivos
import kotlinx.coroutines.flow.StateFlow
// Importa el observable de datos reactivos
import kotlinx.coroutines.flow.asStateFlow
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch

// Clase de datos MascotaPerfilUiState: modelo inmutable con propiedades de dominio
data class MascotaPerfilUiState(
    // Constante cargando: valor inmutable que no cambia tras su asignación
    val cargando: Boolean = true,
    // Constante mascota: valor inmutable que no cambia tras su asignación
    val mascota: Mascota? = null,
    // Constante reportes: valor inmutable que no cambia tras su asignación
    val reportes: List<ReporteVista> = emptyList()
)

/**
 * [ViewModel del Perfil de Mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Obtener y exponer la información detallada de la mascota]
 * - [Cargar y ordenar el historial de reportes de la mascota]
 */
// ViewModel MascotaPerfilViewModel: gestiona el estado y la lógica de negocio de la pantalla
class MascotaPerfilViewModel(
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo: LomitoTvRepository = LomitoTvRepository()
) : ViewModel() {

    // Constante _uiState: valor inmutable que no cambia tras su asignación
    private val _uiState = MutableStateFlow(MascotaPerfilUiState())
    // Constante uiState: valor inmutable que no cambia tras su asignación
    val uiState: StateFlow<MascotaPerfilUiState> = _uiState.asStateFlow()

    // Función cargar: define la lógica de esta operación
    fun cargar(mascotaId: String) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true)
            // Constante mascota: valor inmutable que no cambia tras su asignación
            val mascota = repo.getMascotaById(mascotaId)
            // Constante reportes: valor inmutable que no cambia tras su asignación
            val reportes = repo.getReportesDeMascota(mascotaId).sortedByDescending { it.timestamp }
            _uiState.value = MascotaPerfilUiState(cargando = false, mascota = mascota, reportes = reportes)
        }
    }
}
```

## FASE 8: `com/lomito/seguro/tv/ui/refugio`

### Paso 8.1: `RefugioDifusionActivity.kt`

**Actividad de difusión del refugio (TV)**. Muestra la transmisión en vivo del refugio y la lista de mascotas disponibles para adopción.

```kotlin
// Paquete: com.lomito.seguro.tv.ui.refugio
package com.lomito.seguro.tv.ui.refugio

// Importa el contexto de Android
import android.content.Context
// Importa la dependencia necesaria: Uri
import android.net.Uri
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa la dependencia necesaria: viewModels
import androidx.activity.viewModels
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Box
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Column
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.fillMaxSize
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.padding
// Importa componente de Jetpack Compose
import androidx.compose.runtime.Composable
// Importa componente de Jetpack Compose
import androidx.compose.runtime.LaunchedEffect
// Importa componente de Jetpack Compose
import androidx.compose.runtime.collectAsState
// Importa componente de Jetpack Compose
import androidx.compose.runtime.getValue
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa componente de Jetpack Compose
import androidx.compose.ui.viewinterop.AndroidView
// Importa el reproductor multimedia ExoPlayer
import androidx.media3.common.MediaItem
// Importa el reproductor multimedia ExoPlayer
import androidx.media3.exoplayer.ExoPlayer
// Importa componentes de la interfaz gráfica
import androidx.media3.ui.PlayerView
// Importa la dependencia necesaria: Button
import androidx.tv.material3.Button
// Importa la dependencia necesaria: MaterialTheme
import androidx.tv.material3.MaterialTheme
// Importa la dependencia necesaria: Text
import androidx.tv.material3.Text
// Importa la dependencia necesaria: Refugio
import com.lomito.seguro.tv.data.model.Refugio
// Importa la dependencia necesaria: LomitoAlertRed
import com.lomito.seguro.tv.ui.theme.LomitoAlertRed
// Importa la dependencia necesaria: LomitoTvTheme
import com.lomito.seguro.tv.ui.theme.LomitoTvTheme

/**
 * [Actividad de Difusión de Refugio para Android TV]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Transmitir el video en vivo o grabado del refugio en la Smart TV]
 * - [Mostrar los detalles de contacto y horarios del refugio]
 */
// Activity RefugioDifusionActivity: pantalla principal que gestiona el ciclo de vida
class RefugioDifusionActivity : ComponentActivity() {

    companion object {
        // Constante EXTRA_REFUGIO_ID: valor fijo definido en tiempo de compilación
        const val EXTRA_REFUGIO_ID = "refugio_id"
    }

    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: RefugioDifusionViewModel by viewModels()

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)
        // Constante refugioId: valor inmutable que no cambia tras su asignación
        val refugioId = intent.getStringExtra(EXTRA_REFUGIO_ID).orEmpty()

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            LomitoTvTheme {
                RefugioDifusionScreen(
                    viewModel = viewModel, 
                    refugioId = refugioId,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

/**
 * [Pantalla componible de Difusión de Refugio]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - viewModel: [ViewModel que provee la información del refugio y URL del video]
 * - refugioId: [Identificador del refugio a difundir]
 * - onBackClick: [Callback para regresar a la pantalla anterior]
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función RefugioDifusionScreen: define la lógica de esta operación
fun RefugioDifusionScreen(viewModel: RefugioDifusionViewModel, refugioId: String, onBackClick: () -> Unit) {
    LaunchedEffect(refugioId) { viewModel.cargar(refugioId) }
    // Constante state: valor inmutable que no cambia tras su asignación
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
        when {
            state.cargando -> Text(
                text = "Conectando con el refugio…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
            state.refugio == null -> Text(
                text = "No se encontró información de este refugio.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
            state.refugio?.videoUrl.isNullOrBlank() -> Text(
                text = "Este refugio todavía no tiene una transmisión disponible.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
            else -> RefugioDifusionContenido(refugio = state.refugio!!, onBackClick = onBackClick)
        }
    }
}

// Anotación que marca esta función como una función de composición de UI
@Composable
private fun RefugioDifusionContenido(refugio: Refugio, onBackClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context: Context ->
                PlayerView(context).apply {
                    useController = true
                    // Configura el reproductor multimedia ExoPlayer para streaming de video
                    player = ExoPlayer.Builder(context).build().apply {
                        // Configura el reproductor multimedia ExoPlayer para streaming de video
                        setMediaItem(MediaItem.fromUri(Uri.parse(refugio.videoUrl)))
                        prepare()
                        playWhenReady = true
                        // Configura el reproductor multimedia ExoPlayer para streaming de video
                        repeatMode = ExoPlayer.REPEAT_MODE_ALL
                    }
                }
            },
            onRelease = { playerView ->
                // Libera los recursos del reproductor multimedia
                playerView.player?.release()
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .padding(32.dp)
        ) {
            Text(
                text = "● EN VIVO",
                color = LomitoAlertRed,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = refugio.nombre,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            )
            Text(
                text = "${refugio.direccion}\nTel: ${refugio.telefono}\nHorarios: ${refugio.horarios}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            
            Button(onClick = onBackClick) {
                Text(text = "VOLVER AL DASHBOARD")
            }
        }
    }
}
```

### Paso 8.2: `RefugioDifusionViewModel.kt`

**ViewModel de difusión (TV)**. Gestiona el estado del video en vivo y los datos del refugio para la pantalla de difusión.

```kotlin
// Paquete: com.lomito.seguro.tv.ui.refugio
package com.lomito.seguro.tv.ui.refugio

// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModel
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa la dependencia necesaria: Refugio
import com.lomito.seguro.tv.data.model.Refugio
// Importa la dependencia necesaria: LomitoTvRepository
import com.lomito.seguro.tv.data.repository.LomitoTvRepository
// Importa el observable de datos reactivos
import kotlinx.coroutines.flow.MutableStateFlow
// Importa el observable de datos reactivos
import kotlinx.coroutines.flow.StateFlow
// Importa el observable de datos reactivos
import kotlinx.coroutines.flow.asStateFlow
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch

// Clase de datos RefugioDifusionUiState: modelo inmutable con propiedades de dominio
data class RefugioDifusionUiState(
    // Constante cargando: valor inmutable que no cambia tras su asignación
    val cargando: Boolean = true,
    // Constante refugio: valor inmutable que no cambia tras su asignación
    val refugio: Refugio? = null
)

/**
 * [ViewModel de Difusión de Refugio]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Cargar los datos del refugio desde el repositorio]
 * - [Proveer la URL del video del refugio para su reproducción en la TV]
 */
// ViewModel RefugioDifusionViewModel: gestiona el estado y la lógica de negocio de la pantalla
class RefugioDifusionViewModel(
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo: LomitoTvRepository = LomitoTvRepository()
) : ViewModel() {

    // Constante _uiState: valor inmutable que no cambia tras su asignación
    private val _uiState = MutableStateFlow(RefugioDifusionUiState())
    // Constante uiState: valor inmutable que no cambia tras su asignación
    val uiState: StateFlow<RefugioDifusionUiState> = _uiState.asStateFlow()

    // Función cargar: define la lógica de esta operación
    fun cargar(refugioId: String) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true)
            // Constante refugio: valor inmutable que no cambia tras su asignación
            val refugio = repo.getRefugioById(refugioId)
            // ✅ AHORA USA EL videoUrl QUE VIENE DE LA BASE DE DATOS
            _uiState.value = RefugioDifusionUiState(cargando = false, refugio = refugio)
        }
    }
}
```

## FASE 9: `com/lomito/seguro/tv/ui/theme`

### Paso 9.1: `Theme.kt`

**Tema visual (TV)**. Define los colores, tipografías y estilos de la aplicación TV con Material Design para pantallas grandes.

```kotlin
// Paquete: com.lomito.seguro.tv.ui.theme
package com.lomito.seguro.tv.ui.theme

// Importa componente de Jetpack Compose
import androidx.compose.runtime.Composable
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa la dependencia necesaria: MaterialTheme
import androidx.tv.material3.MaterialTheme
// Importa la dependencia necesaria: darkColorScheme
import androidx.tv.material3.darkColorScheme

// Constante LomitoOrange: valor inmutable que no cambia tras su asignación
val LomitoOrange = Color(0xFFFF8A00)
// Constante LomitoOrangeVariant: valor inmutable que no cambia tras su asignación
val LomitoOrangeVariant = Color(0xFFFFB454)
// Constante LomitoBackground: valor inmutable que no cambia tras su asignación
val LomitoBackground = Color(0xFF121212)
// Constante LomitoSurface: valor inmutable que no cambia tras su asignación
val LomitoSurface = Color(0xFF1E1E1E)
// Constante LomitoSurfaceAlt: valor inmutable que no cambia tras su asignación
val LomitoSurfaceAlt = Color(0xFF262626)
// Constante LomitoOnSurface: valor inmutable que no cambia tras su asignación
val LomitoOnSurface = Color(0xFFF5F5F5)
// Constante LomitoOnSurfaceMuted: valor inmutable que no cambia tras su asignación
val LomitoOnSurfaceMuted = Color(0xFFA0A0A0)
// Constante LomitoAlertRed: valor inmutable que no cambia tras su asignación
val LomitoAlertRed = Color(0xFFE53935)
// Constante LomitoFoundGreen: valor inmutable que no cambia tras su asignación
val LomitoFoundGreen = Color(0xFF4CAF50)

// Constante LomitoTvColorScheme: valor inmutable que no cambia tras su asignación
private val LomitoTvColorScheme = darkColorScheme(
    primary = LomitoOrange,
    onPrimary = Color.Black,
    secondary = LomitoOrangeVariant,
    background = LomitoBackground,
    onBackground = LomitoOnSurface,
    surface = LomitoSurface,
    onSurface = LomitoOnSurface,
    surfaceVariant = LomitoSurfaceAlt,
    onSurfaceVariant = LomitoOnSurfaceMuted,
    error = LomitoAlertRed
)

/**
 * [Tema principal de la aplicación Lomito Seguro TV]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - content: [Contenido componible que será estilizado por el tema]
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función LomitoTvTheme: define la lógica de esta operación
fun LomitoTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LomitoTvColorScheme,
        content = content
    )
}
```

## FASE 10: `com/lomito/seguro/tv/util`

### Paso 10.1: `Extensions.kt`

**Funciones de extensión**. Extiende clases existentes de Android/Kotlin con utilidades adicionales: conversión de URLs, visibilidad de vistas, formato de fechas y distancias.

```kotlin
// Paquete: com.lomito.seguro.tv.util
package com.lomito.seguro.tv.util

// Importa el cliente Retrofit para peticiones HTTP
import com.lomito.seguro.tv.data.api.RetrofitClient

/**
 * [Función de extensión para convertir rutas relativas a URLs absolutas]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Concatenar la URL base del servidor a rutas relativas]
 * - [Retornar la URL original si ya es absoluta]
 */
// Función String: define la lógica de esta operación
fun String?.toAbsoluteUrl(): String? {
    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
    if (this.isNullOrEmpty()) return null
    // Retorna el valor al llamador de la función
    return if (startsWith("http://") || startsWith("https://")) this
    // Accede al cliente Retrofit singleton para realizar peticiones de red
    else RetrofitClient.SERVER_URL + (if (startsWith("/")) this else "/$this")
}
```
