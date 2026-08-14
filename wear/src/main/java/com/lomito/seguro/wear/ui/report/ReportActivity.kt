// wear/ui/report/ReportActivity.kt
// Paquete: com.lomito.seguro.wear.ui.report
package com.lomito.seguro.wear.ui.report

// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.wear.BuildConfig
// Importa la dependencia necesaria: Manifest
import android.Manifest
// Importa la dependencia necesaria: PackageManager
import android.content.pm.PackageManager
// Importa la dependencia necesaria: Location
import android.location.Location
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
import androidx.compose.foundation.layout.*
// Importa componente de Jetpack Compose
import androidx.compose.runtime.*
// Importa componente de Jetpack Compose
import androidx.compose.runtime.livedata.observeAsState
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.style.TextAlign
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa la dependencia necesaria: ActivityCompat
import androidx.core.app.ActivityCompat
// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.AndroidViewModel
// Importa el observable de datos reactivos
import androidx.lifecycle.LiveData
// Importa el observable de datos reactivos
import androidx.lifecycle.MutableLiveData
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa la dependencia necesaria: LocationServices
import com.google.android.gms.location.LocationServices
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.Wearable
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.tasks.await
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: URL
import java.net.URL

// --- COLORES COHERENTES CON EL MURAL ---
// Constante ThemeBg: valor inmutable que no cambia tras su asignación
private val ThemeBg = Color(0xFF1A1A2E)
// Constante CardBg: valor inmutable que no cambia tras su asignación
private val CardBg = Color(0xFF2C2C3E)
// Constante AccentGreen: valor inmutable que no cambia tras su asignación
private val AccentGreen = Color(0xFF4CAF50)
// Constante AccentBlue: valor inmutable que no cambia tras su asignación
private val AccentBlue = Color(0xFF2196F3)

/**
 * [ViewModel para manejar la lógica de reporte]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Obtener la ubicación del dispositivo]
 * - [Enviar el reporte de avistamiento al backend y al móvil conectado]
 */
// ViewModel ReportViewModel: gestiona el estado y la lógica de negocio de la pantalla
class ReportViewModel(app: android.app.Application) : AndroidViewModel(app) {
    // Constante _estado: valor inmutable que no cambia tras su asignación
    private val _estado = MutableLiveData<String>("¿Viste a esta mascota?")
    // Constante estado: valor inmutable que no cambia tras su asignación
    val estado: LiveData<String> = _estado
    // Constante _enviado: valor inmutable que no cambia tras su asignación
    private val _enviado = MutableLiveData(false)
    // Constante enviado: valor inmutable que no cambia tras su asignación
    val enviado: LiveData<Boolean> = _enviado

    // Función reportarVista: define la lógica de esta operación
    fun reportarVista(mascotaId: String, mascotaNombre: String) {
        _estado.value = "Obteniendo ubicación..."
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante fusedClient: valor inmutable que no cambia tras su asignación
                val fusedClient = LocationServices.getFusedLocationProviderClient(getApplication())
                // Constante location: valor inmutable que no cambia tras su asignación
                val location: Location? = if (
                    ActivityCompat.checkSelfPermission(
                        getApplication(), Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedClient.lastLocation.await()
                } else null

                // Constante lat: valor inmutable que no cambia tras su asignación
                val lat = location?.latitude ?: 0.0
                // Constante lng: valor inmutable que no cambia tras su asignación
                val lng = location?.longitude ?: 0.0

                _estado.value = "Enviando reporte..."

                // Constante payload: valor inmutable que no cambia tras su asignación
                val payload = JSONObject().apply {
                    put("mascotaId", mascotaId)
                    put("latitud", lat)
                    put("longitud", lng)
                    put("accion", "reportar_vista")
                    put("mascotaNombre", mascotaNombre)
                }.toString().toByteArray()

                // Constante nodes: valor inmutable que no cambia tras su asignación
                val nodes = Wearable.getNodeClient(getApplication()).connectedNodes.await()
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                nodes.forEach { node ->
                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    Wearable.getMessageClient(getApplication())
                        // Envía un mensaje al dispositivo Wear OS conectado
                        .sendMessage(node.id, "/watch/reporte", payload).await()
                }

                // Constante url: valor inmutable que no cambia tras su asignación
                val url = URL("${BuildConfig.BACKEND_URL}/api/reportes")
                // Constante conn: valor inmutable que no cambia tras su asignación
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                // Constante json: valor inmutable que no cambia tras su asignación
                val json = JSONObject().apply {
                    put("mascotaId", mascotaId)
                    put("latitud", lat)
                    put("longitud", lng)
                    put("reportadoPorId", "usuario_watch")
                }

                conn.outputStream.write(json.toString().toByteArray())
                // Constante responseCode: valor inmutable que no cambia tras su asignación
                val responseCode = conn.responseCode
                conn.disconnect()

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (responseCode == 200 || responseCode == 201) {
                    _estado.value = "✅ ¡Enviado!"
                    _enviado.value = true
                } else {
                    _estado.value = "⚠️ Error HTTP $responseCode"
                }
            } catch (e: Exception) {
                _estado.value = "❌ Error de conexión"
            }
        }
    }
}

/**
 * [Actividad para reportar que se ha visto una mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Iniciar la pantalla de reporte con el ID y nombre de la mascota]
 */
// Activity ReportActivity: pantalla principal que gestiona el ciclo de vida
class ReportActivity : ComponentActivity() {
    // Constante reportVM: valor inmutable que no cambia tras su asignación
    private val reportVM: ReportViewModel by viewModels()

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)
        // Constante mascotaId: valor inmutable que no cambia tras su asignación
        val mascotaId = intent.getStringExtra("mascotaId") ?: ""
        // Constante mascotaNombre: valor inmutable que no cambia tras su asignación
        val mascotaNombre = intent.getStringExtra("mascotaNombre") ?: "Mascota"

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            // Constante estado: valor inmutable que no cambia tras su asignación
            val estado by reportVM.estado.observeAsState("Cargando...")
            // Constante enviado: valor inmutable que no cambia tras su asignación
            val enviado by reportVM.enviado.observeAsState(false)

            ReportScreen(
                mascotaNombre = mascotaNombre,
                estado = estado,
                enviado = enviado,
                onReportar = { reportVM.reportarVista(mascotaId, mascotaNombre) },
                onDismiss = { finish() }
            )
        }
    }
}

/**
 * [Pantalla de confirmación para reportar avistamiento]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [mascotaNombre]: Nombre de la mascota vista
 * - [estado]: Estado actual del proceso de reporte
 * - [enviado]: Indica si el reporte fue enviado con éxito
 * - [onReportar]: Acción para ejecutar el envío del reporte
 * - [onDismiss]: Acción para cerrar o cancelar
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función ReportScreen: define la lógica de esta operación
fun ReportScreen(
    mascotaNombre: String,
    estado: String,
    enviado: Boolean,
    onReportar: () -> Unit,
    onDismiss: () -> Unit
) {
    // Constante listState: valor inmutable que no cambia tras su asignación
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeBg),
            contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            autoCentering = AutoCenteringParams(itemIndex = 1) // Centra el nombre de la mascota
        ) {
            // Icono y Título
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (enviado) "🎉" else "📍",
                        fontSize = 28.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (enviado) "¡Éxito!" else "Nuevo Reporte",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Cuerpo del mensaje
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = mascotaNombre,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (enviado) AccentGreen else AccentBlue,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = estado,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Acciones o Progreso
            item {
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (!enviado) {
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (estado.contains("Enviando") || estado.contains("Obteniendo")) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp).padding(4.dp),
                            strokeWidth = 3.dp,
                            indicatorColor = AccentBlue
                        )
                    } else {
                        Button(
                            onClick = onReportar,
                            colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Text("Reportar Avistamiento", fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // Botón Salir/Cancelar
            item {
                Spacer(Modifier.height(8.dp))
                CompactButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (enviado) AccentGreen else Color(0xFF424242)
                    ),
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text(
                        text = if (enviado) "Terminar" else "Cancelar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}