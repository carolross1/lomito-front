// Paquete: com.lomito.seguro.wear.ui.mascota
package com.lomito.seguro.wear.ui.mascota
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.wear.BuildConfig

// Importa la dependencia necesaria: BroadcastReceiver
import android.content.BroadcastReceiver
// Importa el contexto de Android
import android.content.Context
// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa la clase Intent para navegación entre componentes
import android.content.IntentFilter
// Importa la dependencia necesaria: Build
import android.os.Build
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.*
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.LazyColumn
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.items
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.CircleShape
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.RoundedCornerShape
// Importa componente de Jetpack Compose
import androidx.compose.runtime.*
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
import androidx.compose.ui.draw.clip
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Brush
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
import androidx.compose.ui.layout.ContentScale
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa componente de Jetpack Compose
import coil.compose.AsyncImage
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.*
// Importa el parser JSON
import org.json.JSONArray
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
import java.net.URL

/**
 * [Modelo de datos simplificado para la lista de mascotas]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Almacenar los datos básicos necesarios para mostrar en un listado]
 */
// Clase de datos MascotaItem: modelo inmutable con propiedades de dominio
data class MascotaItem(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String,
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String,
    // Constante especie: valor inmutable que no cambia tras su asignación
    val especie: String,
    // Constante raza: valor inmutable que no cambia tras su asignación
    val raza: String = "",
    // Constante edad: valor inmutable que no cambia tras su asignación
    val edad: Int = 0,
    // Constante color: valor inmutable que no cambia tras su asignación
    val color: String = "",
    // Constante peso: valor inmutable que no cambia tras su asignación
    val peso: String = "",
    // Constante fotoUrl: valor inmutable que no cambia tras su asignación
    val fotoUrl: String? = null,
    // Constante distanciaAlerta: valor inmutable que no cambia tras su asignación
    val distanciaAlerta: Int = 50,
    // Constante estado: valor inmutable que no cambia tras su asignación
    val estado: String = "EN_CASA"
)

/**
 * [Actividad para listar todas las mascotas del usuario]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Obtener la lista de mascotas desde el backend]
 * - [Gestionar la actualización en tiempo real de su estado a través de broadcast y polling]
 */
// Activity MascotaListActivity: pantalla principal que gestiona el ciclo de vida
class MascotaListActivity : ComponentActivity() {
    // Variable pollingJob: almacena el estado mutable de este componente
    private var pollingJob: Job? = null
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    private val backendUrl = BuildConfig.BACKEND_URL
    // Constante distanciasSimuladas: valor inmutable que no cambia tras su asignación
    private val distanciasSimuladas = mutableStateMapOf<String, Int>()

    // ✅ State para mascotas con actualización inmediata
    // Variable mascotasState: almacena el estado mutable de este componente
    private var mascotasState by mutableStateOf<List<MascotaItem>>(emptyList())

    // ✅ BroadcastReceiver para actualizar el estado de una mascota
    // Constante estadoUpdateReceiver: valor inmutable que no cambia tras su asignación
    private val estadoUpdateReceiver = object : BroadcastReceiver() {
        // Sobreescribe la función onReceive de la clase padre
        override fun onReceive(context: Context, intent: Intent) {
            // Constante mascotaId: valor inmutable que no cambia tras su asignación
            val mascotaId = intent.getStringExtra("mascota_id") ?: return
            // Constante nuevoEstado: valor inmutable que no cambia tras su asignación
            val nuevoEstado = intent.getStringExtra("nuevo_estado") ?: return

            // Registro de evento en el log de Android para depuración
            android.util.Log.d("MLIST", "📢 Actualizando estado de $mascotaId a $nuevoEstado")

            // ✅ Actualizar la lista inmediatamente
            mascotasState = mascotasState.map { item ->
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (item.id == mascotaId) {
                    item.copy(estado = nuevoEstado)
                } else {
                    item
                }
            }
        }
    }

    // Constante bleReceiver: valor inmutable que no cambia tras su asignación
    private val bleReceiver = object : BroadcastReceiver() {
        // Sobreescribe la función onReceive de la clase padre
        override fun onReceive(context: Context, intent: Intent) {
            // Constante mascotaId: valor inmutable que no cambia tras su asignación
            val mascotaId = intent.getStringExtra("mascotaId") ?: return
            // Constante distancia: valor inmutable que no cambia tras su asignación
            val distancia = intent.getIntExtra("distancia", 0)
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (mascotaId.isNotEmpty()) {
                distanciasSimuladas[mascotaId] = distancia
            }
        }
    }

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)

        // ✅ Registrar receiver para actualizaciones de estado
        // Constante filter: valor inmutable que no cambia tras su asignación
        val filter = IntentFilter("com.lomito.seguro.wear.ESTADO_ACTUALIZADO")
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(estadoUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(estadoUpdateReceiver, filter)
        }

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            // Variable isLoading: almacena el estado mutable de este componente
            var isLoading by remember { mutableStateOf(true) }
            // Variable errorMessage: almacena el estado mutable de este componente
            var errorMessage by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                // Constante result: valor inmutable que no cambia tras su asignación
                val result = withContext(Dispatchers.IO) { cargarMascotas() }
                mascotasState = result.mascotas
                isLoading = false
                errorMessage = result.errorMessage
            }

            MascotaListScreen(
                mascotas = mascotasState,
                distanciasSimuladas = distanciasSimuladas,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onSelect = { mascota ->
                    // Constante intent: valor inmutable que no cambia tras su asignación
                    val intent = Intent(this@MascotaListActivity, MascotaDetailActivity::class.java).apply {
                        putExtra("mascota_id", mascota.id)
                        putExtra("mascota_nombre", mascota.nombre)
                        putExtra("mascota_especie", mascota.especie)
                        putExtra("mascota_raza", mascota.raza)
                        putExtra("mascota_edad", mascota.edad)
                        putExtra("mascota_color", mascota.color)
                        putExtra("mascota_peso", mascota.peso)
                        putExtra("mascota_foto", mascota.fotoUrl)
                        putExtra("mascota_distancia_alerta", mascota.distanciaAlerta)
                        putExtra("mascota_estado", mascota.estado)
                        putExtra("mascota_distancia_simulada", distanciasSimuladas[mascota.id] ?: 0)
                    }
                    startActivity(intent)
                },
                onBack = { finish() },
                onRetry = {
                    isLoading = true
                    errorMessage = ""
                    // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                    CoroutineScope(Dispatchers.Main).launch {
                        // Constante result: valor inmutable que no cambia tras su asignación
                        val result = withContext(Dispatchers.IO) { cargarMascotas() }
                        mascotasState = result.mascotas
                        isLoading = false
                        errorMessage = result.errorMessage
                    }
                }
            )
        }
    }

    // Método del ciclo de vida: la actividad se vuelve visible
    override fun onStart() {
        // Invoca la implementación del método en la clase padre
        super.onStart()
        // Constante filter: valor inmutable que no cambia tras su asignación
        val filter = IntentFilter("com.lomito.seguro.wear.BLE_UPDATE")
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bleReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(bleReceiver, filter)
        }

        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        pollingJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (isActive) {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // Constante url: valor inmutable que no cambia tras su asignación
                    val url = URL("$backendUrl/api/simulador/estado")
                    // Constante conn: valor inmutable que no cambia tras su asignación
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 2000
                    conn.readTimeout = 2000
                    conn.requestMethod = "GET"
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        // Constante response: valor inmutable que no cambia tras su asignación
                        val response = conn.inputStream.bufferedReader().readText()
                        // Constante json: valor inmutable que no cambia tras su asignación
                        val json = JSONObject(response)
                        // Constante distancia: valor inmutable que no cambia tras su asignación
                        val distancia = json.optInt("distancia", 0)
                        // Constante mascotaId: valor inmutable que no cambia tras su asignación
                        val mascotaId = json.optString("mascotaId", "")
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (mascotaId.isNotEmpty()) {
                            // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                            withContext(Dispatchers.Main) {
                                distanciasSimuladas[mascotaId] = distancia
                            }
                        }
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("MLIST_POLL", "Error: ${e.message}")
                }
                delay(2000)
            }
        }
    }

    // Método del ciclo de vida: la actividad ya no es visible
    override fun onStop() {
        // Invoca la implementación del método en la clase padre
        super.onStop()
        unregisterReceiver(bleReceiver)
        pollingJob?.cancel()
    }

    // Método del ciclo de vida: se limpia la actividad antes de destruirse
    override fun onDestroy() {
        // Invoca la implementación del método en la clase padre
        super.onDestroy()
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            unregisterReceiver(estadoUpdateReceiver)
        } catch (e: Exception) {
            // Receiver ya fue desregistrado
        }
    }

    private suspend fun cargarMascotas(): CargaResult {
        // Retorna el valor al llamador de la función
        return try {
            // Constante prefs: valor inmutable que no cambia tras su asignación
            val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
            // Constante userId: valor inmutable que no cambia tras su asignación
            val userId = prefs.getString("user_id", "2") ?: "2"
            // Constante url: valor inmutable que no cambia tras su asignación
            val url = URL("$backendUrl/api/mascotas?ownerId=$userId")
            // Constante conn: valor inmutable que no cambia tras su asignación
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            val responseCode = conn.responseCode
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Constante response: valor inmutable que no cambia tras su asignación
                val response = conn.inputStream.bufferedReader().readText()
                // Constante jsonArray: valor inmutable que no cambia tras su asignación
                val jsonArray = JSONArray(response)
                // Constante lista: valor inmutable que no cambia tras su asignación
                val lista = mutableListOf<MascotaItem>()
                // Itera sobre la colección para procesar cada elemento
                for (i in 0 until jsonArray.length()) {
                    // Constante obj: valor inmutable que no cambia tras su asignación
                    val obj = jsonArray.getJSONObject(i)
                    // Constante fotoRelativa: valor inmutable que no cambia tras su asignación
                    val fotoRelativa = obj.optString("foto_url", null)
                    // Constante fotoAbsoluta: valor inmutable que no cambia tras su asignación
                    val fotoAbsoluta = fotoRelativa?.takeIf { it.isNotEmpty() }?.let {
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (it.startsWith("http")) it else "$backendUrl$it"
                    }
                    lista.add(
                        MascotaItem(
                            id = obj.getString("id"),
                            nombre = obj.getString("nombre"),
                            especie = obj.getString("especie"),
                            raza = obj.optString("raza", ""),
                            edad = obj.optInt("edad", 0),
                            color = obj.optString("color", ""),
                            peso = obj.optString("peso", ""),
                            fotoUrl = fotoAbsoluta,
                            distanciaAlerta = obj.getInt("distancia_alerta"),
                            estado = obj.getString("estado")
                        )
                    )
                }
                conn.disconnect()
                CargaResult(lista, if (lista.isEmpty()) "No hay mascotas" else "")
            } else {
                conn.disconnect()
                CargaResult(emptyList(), "Error HTTP $responseCode")
            }
        } catch (e: Exception) {
            CargaResult(emptyList(), e.message ?: "Error desconocido")
        }
    }

    // Clase de datos CargaResult: modelo inmutable con propiedades de dominio
    data class CargaResult(val mascotas: List<MascotaItem>, val errorMessage: String)
}

// 🎨 Paleta temática "mascotas perdidas" (misma del Dashboard)
// Constante BgTop: valor inmutable que no cambia tras su asignación
private val BgTop = Color(0xFF1B1430)
// Constante BgBottom: valor inmutable que no cambia tras su asignación
private val BgBottom = Color(0xFF0D0B1A)
// Constante CardBg: valor inmutable que no cambia tras su asignación
private val CardBg = Color(0xFF252044)
// Constante AccentRed: valor inmutable que no cambia tras su asignación
private val AccentRed = Color(0xFFE85D5D)
// Constante AccentGreen: valor inmutable que no cambia tras su asignación
private val AccentGreen = Color(0xFF4CD97B)
// Constante AccentOrange: valor inmutable que no cambia tras su asignación
private val AccentOrange = Color(0xFFFFA94D)
// Constante AccentBlue: valor inmutable que no cambia tras su asignación
private val AccentBlue = Color(0xFF4D9FFF)

/**
 * [Pantalla con la lista de mascotas del usuario]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [mascotas]: Lista de las mascotas
 * - [distanciasSimuladas]: Mapa de distancias actuales por ID de mascota
 * - [isLoading]: Estado de carga de la lista
 * - [errorMessage]: Mensaje de error general
 * - [onSelect]: Callback al seleccionar una mascota
 * - [onBack]: Callback para regresar
 * - [onRetry]: Callback para reintentar la conexión
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MascotaListScreen: define la lógica de esta operación
fun MascotaListScreen(
    mascotas: List<MascotaItem>,
    distanciasSimuladas: Map<String, Int>,
    isLoading: Boolean,
    errorMessage: String,
    onSelect: (MascotaItem) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                // ✅ Header centrado, con botón de cerrar pequeño y discreto
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🐾", fontSize = 13.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Mis Mascotas",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                Box(modifier = Modifier.align(Alignment.End)) {
                    // espacio reservado, el botón real va abajo flotando
                }

                // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
                when {
                    isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(26.dp),
                                    strokeWidth = 2.dp,
                                    indicatorColor = AccentGreen
                                )
                                Spacer(Modifier.height(6.dp))
                                Text("Cargando...", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                            }
                        }
                    }
                    mascotas.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🐾", fontSize = 22.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("No hay mascotas", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                if (errorMessage.isNotEmpty()) {
                                    Text(
                                        errorMessage,
                                        color = AccentOrange,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                CompactButton(
                                    onClick = onRetry,
                                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
                                    modifier = Modifier.size(width = 84.dp, height = 32.dp)
                                ) {
                                    Text("Reintentar", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(0.94f).align(Alignment.CenterHorizontally)
                        ) {
                            items(mascotas) { mascota ->
                                MascotaItemCard(
                                    mascota = mascota,
                                    distanciaSimulada = distanciasSimuladas[mascota.id],
                                    onClick = { onSelect(mascota) }
                                )
                            }
                            item {
                                Spacer(Modifier.height(4.dp))
                                CompactChip(
                                    onClick = onBack,
                                    label = { Text("Cerrar", fontSize = 10.sp) },
                                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
                                    modifier = Modifier.fillMaxWidth()
                                )
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
// Función MascotaItemCard: define la lógica de esta operación
fun MascotaItemCard(
    mascota: MascotaItem,
    distanciaSimulada: Int?,
    onClick: () -> Unit
) {
    // Constante estadoColor: valor inmutable que no cambia tras su asignación
    val estadoColor = when (mascota.estado) {
        "PERDIDA" -> AccentRed
        "ENCONTRADA" -> AccentGreen
        else -> AccentOrange
    }

    // Constante estadoTexto: valor inmutable que no cambia tras su asignación
    val estadoTexto = when (mascota.estado) {
        "PERDIDA" -> "Perdida"
        "ENCONTRADA" -> "Encontrada"
        else -> "En Casa"
    }

    // Constante distanciaColor: valor inmutable que no cambia tras su asignación
    val distanciaColor = when {
        distanciaSimulada == null -> Color.White.copy(alpha = 0.4f)
        distanciaSimulada > mascota.distanciaAlerta -> AccentRed
        distanciaSimulada > mascota.distanciaAlerta * 0.8 -> AccentOrange
        else -> AccentGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(CardBg, CardBg.copy(alpha = 0.7f))
                    )
                )
        ) {
            // ✅ Barra lateral de color según estado (más legible que solo un punto)
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(estadoColor)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (!mascota.fotoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = mascota.fotoUrl,
                                contentDescription = mascota.nombre,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = if (mascota.especie == "PERRO") "🐕" else "🐈",
                                fontSize = 18.sp
                            )
                        }
                        Column {
                            Text(
                                text = mascota.nombre,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (distanciaSimulada != null) "📍${distanciaSimulada}m" else "📍--",
                                    color = distanciaColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "/ ${mascota.distanciaAlerta}m",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 9.sp
                                )
                            }
                            Text(
                                text = estadoTexto,
                                color = estadoColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(estadoColor, CircleShape)
                    )
                }
            }
        }
    }
}