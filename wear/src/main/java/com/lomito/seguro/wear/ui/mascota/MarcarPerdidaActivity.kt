// Paquete: com.lomito.seguro.wear.ui.mascota
package com.lomito.seguro.wear.ui.mascota
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.wear.BuildConfig

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
import androidx.compose.ui.graphics.Brush
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
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
 * [Modelo simplificado para representar a una mascota en la lista de mascotas a perder]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mantener el estado e información básica de la mascota]
 */
// Clase de datos MascotaParaPerder: modelo inmutable con propiedades de dominio
data class MascotaParaPerder(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String,
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String,
    // Constante especie: valor inmutable que no cambia tras su asignación
    val especie: String,
    // Constante raza: valor inmutable que no cambia tras su asignación
    val raza: String = "",
    // Constante color: valor inmutable que no cambia tras su asignación
    val color: String = "",
    // Constante fotoUrl: valor inmutable que no cambia tras su asignación
    val fotoUrl: String? = null,
    // Constante distanciaAlerta: valor inmutable que no cambia tras su asignación
    val distanciaAlerta: Int = 50,
    // Constante estado: valor inmutable que no cambia tras su asignación
    val estado: String = "EN_CASA"
)

/**
 * [Actividad para listar mascotas y marcarlas como perdidas]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Obtener la lista de mascotas del usuario actual]
 * - [Permitir al usuario cambiar el estado de una mascota a "PERDIDA"]
 */
// Activity MarcarPerdidaActivity: pantalla principal que gestiona el ciclo de vida
class MarcarPerdidaActivity : ComponentActivity() {
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    private val backendUrl = BuildConfig.BACKEND_URL

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)

        // Constante prefs: valor inmutable que no cambia tras su asignación
        val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
        // Variable userId: almacena el estado mutable de este componente
        var userId = prefs.getString("user_id", "") ?: ""

        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (userId.isEmpty() || !userId.matches(Regex("^\\d+$"))) {
            userId = "2"
            // Inicia el editor para modificar los SharedPreferences
            prefs.edit().putString("user_id", userId).apply()
        }

        // Registro de evento en el log de Android para depuración
        android.util.Log.d("MARCAR_PERDIDA", "📱 userId: $userId")

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            // Variable mascotas: almacena el estado mutable de este componente
            var mascotas by remember { mutableStateOf<List<MascotaParaPerder>>(emptyList()) }
            // Variable isLoading: almacena el estado mutable de este componente
            var isLoading by remember { mutableStateOf(true) }
            // Variable errorMessage: almacena el estado mutable de este componente
            var errorMessage by remember { mutableStateOf("") }
            // Variable successMessage: almacena el estado mutable de este componente
            var successMessage by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                // Constante result: valor inmutable que no cambia tras su asignación
                val result = withContext(Dispatchers.IO) {
                    cargarMascotas(userId)
                }
                mascotas = result.mascotas
                isLoading = false
                errorMessage = result.errorMessage
            }

            MarcarPerdidaScreen(
                mascotas = mascotas,
                isLoading = isLoading,
                errorMessage = errorMessage,
                successMessage = successMessage,
                onMarcarPerdida = { mascotaId ->
                    // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                    CoroutineScope(Dispatchers.Main).launch {
                        // Constante result: valor inmutable que no cambia tras su asignación
                        val result = withContext(Dispatchers.IO) {
                            marcarComoPerdida(mascotaId, userId)
                        }
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (result.success) {
                            successMessage = "✅ Mascota marcada como PERDIDA"
                            errorMessage = ""
                            // Recargar lista
                            isLoading = true
                            // Constante newResult: valor inmutable que no cambia tras su asignación
                            val newResult = withContext(Dispatchers.IO) {
                                cargarMascotas(userId)
                            }
                            mascotas = newResult.mascotas
                            isLoading = false
                            errorMessage = newResult.errorMessage
                        } else {
                            errorMessage = result.errorMessage
                            successMessage = ""
                        }
                    }
                },
                onBack = { finish() },
                onRetry = {
                    isLoading = true
                    errorMessage = ""
                    successMessage = ""
                    // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                    CoroutineScope(Dispatchers.Main).launch {
                        // Constante result: valor inmutable que no cambia tras su asignación
                        val result = withContext(Dispatchers.IO) {
                            cargarMascotas(userId)
                        }
                        mascotas = result.mascotas
                        isLoading = false
                        errorMessage = result.errorMessage
                    }
                }
            )
        }
    }

    private suspend fun cargarMascotas(userId: String): CargaResult {
        // Retorna el valor al llamador de la función
        return try {
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("MARCAR_PERDIDA", "📱 Cargando mascotas para usuario: $userId")

            // Constante url: valor inmutable que no cambia tras su asignación
            val url = URL("$backendUrl/api/mascotas?ownerId=$userId")
            // Constante conn: valor inmutable que no cambia tras su asignación
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            // Constante responseCode: valor inmutable que no cambia tras su asignación
            val responseCode = conn.responseCode
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("MARCAR_PERDIDA", "📡 Response Code: $responseCode")

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Constante response: valor inmutable que no cambia tras su asignación
                val response = conn.inputStream.bufferedReader().readText()
                // Registro de evento en el log de Android para depuración
                android.util.Log.d("MARCAR_PERDIDA", "📥 Respuesta: $response")

                // Constante jsonArray: valor inmutable que no cambia tras su asignación
                val jsonArray = JSONArray(response)
                // Registro de evento en el log de Android para depuración
                android.util.Log.d("MARCAR_PERDIDA", "📊 JSON Array Length: ${jsonArray.length()}")

                // Constante lista: valor inmutable que no cambia tras su asignación
                val lista = mutableListOf<MascotaParaPerder>()
                // Itera sobre la colección para procesar cada elemento
                for (i in 0 until jsonArray.length()) {
                    // Constante obj: valor inmutable que no cambia tras su asignación
                    val obj = jsonArray.getJSONObject(i)
                    lista.add(
                        MascotaParaPerder(
                            id = obj.getString("id"),
                            nombre = obj.getString("nombre"),
                            especie = obj.getString("especie"),
                            raza = obj.optString("raza", ""),
                            color = obj.optString("color", ""),
                            fotoUrl = obj.optString("foto_url", null),
                            distanciaAlerta = obj.getInt("distancia_alerta"),
                            estado = obj.getString("estado")
                        )
                    )
                }
                conn.disconnect()

                CargaResult(
                    mascotas = lista,
                    errorMessage = if (lista.isEmpty()) "No tienes mascotas registradas" else ""
                )
            } else {
                conn.disconnect()
                CargaResult(
                    mascotas = emptyList(),
                    errorMessage = "Error al cargar mascotas (HTTP $responseCode)"
                )
            }
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("MARCAR_PERDIDA", "❌ Error: ${e.message}", e)
            CargaResult(
                mascotas = emptyList(),
                errorMessage = "Error: ${e.message}"
            )
        }
    }

    private suspend fun marcarComoPerdida(mascotaId: String, userId: String): OperacionResult {
        // Retorna el valor al llamador de la función
        return try {
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("MARCAR_PERDIDA", "🔴 Marcando mascota $mascotaId como PERDIDA")

            // Constante url: valor inmutable que no cambia tras su asignación
            val url = URL("$backendUrl/api/mascotas/$mascotaId")
            // Constante conn: valor inmutable que no cambia tras su asignación
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            // Constante json: valor inmutable que no cambia tras su asignación
            val json = JSONObject().apply {
                put("estado", "PERDIDA")
            }

            conn.outputStream.write(json.toString().toByteArray())
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            val responseCode = conn.responseCode
            conn.disconnect()

            // Registro de evento en el log de Android para depuración
            android.util.Log.d("MARCAR_PERDIDA", "📡 Response Code: $responseCode")

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (responseCode == 200 || responseCode == 201) {
                OperacionResult(success = true, errorMessage = "")
            } else {
                OperacionResult(success = false, errorMessage = "Error al marcar como perdida (HTTP $responseCode)")
            }
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("MARCAR_PERDIDA", "❌ Error: ${e.message}", e)
            OperacionResult(success = false, errorMessage = "Error: ${e.message}")
        }
    }

    // Clase de datos CargaResult: modelo inmutable con propiedades de dominio
    data class CargaResult(
        // Constante mascotas: valor inmutable que no cambia tras su asignación
        val mascotas: List<MascotaParaPerder>,
        // Constante errorMessage: valor inmutable que no cambia tras su asignación
        val errorMessage: String
    )

    // Clase de datos OperacionResult: modelo inmutable con propiedades de dominio
    data class OperacionResult(
        // Constante success: valor inmutable que no cambia tras su asignación
        val success: Boolean,
        // Constante errorMessage: valor inmutable que no cambia tras su asignación
        val errorMessage: String
    )
}

// 🎨 Paleta temática "mascotas perdidas" (consistente con el resto de la app)
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
 * [Pantalla para listar y marcar mascotas como perdidas]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [mascotas]: Lista de mascotas del usuario
 * - [isLoading]: Estado de carga
 * - [errorMessage]: Mensaje de error general
 * - [successMessage]: Mensaje de éxito al realizar la operación
 * - [onMarcarPerdida]: Acción al intentar marcar como perdida
 * - [onBack]: Acción para volver atrás
 * - [onRetry]: Acción para reintentar la carga
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MarcarPerdidaScreen: define la lógica de esta operación
fun MarcarPerdidaScreen(
    mascotas: List<MascotaParaPerder>,
    isLoading: Boolean,
    errorMessage: String,
    successMessage: String,
    onMarcarPerdida: (String) -> Unit,
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
                            Text(
                                text = "Cargando mascotas...",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                mascotas.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🐾", fontSize = 22.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "No hay mascotas",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (errorMessage.isNotEmpty()) {
                                Text(
                                    text = errorMessage,
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
                    // ✅ Todo el contenido (header, mensajes, lista,
                    // botón cerrar) va dentro del LazyColumn, así nada
                    // se corta contra el bisel circular y todo es
                    // scrolleable si no alcanza el espacio.
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(top = 22.dp, bottom = 16.dp)
                    ) {
                        item {
                            Text(text = "🆘", fontSize = 20.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Marcar como Perdida",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(6.dp))
                        }

                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (successMessage.isNotEmpty()) {
                            item {
                                Text(
                                    text = successMessage,
                                    color = AccentGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }

                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (errorMessage.isNotEmpty()) {
                            item {
                                Text(
                                    text = errorMessage,
                                    color = AccentOrange,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        item {
                            Text(
                                text = "Selecciona la mascota perdida",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 9.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        items(mascotas) { mascota ->
                            MascotaMarcarPerdidaCard(
                                mascota = mascota,
                                onMarcarPerdida = {
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    if (mascota.estado != "PERDIDA") {
                                        onMarcarPerdida(mascota.id)
                                    }
                                }
                            )
                            Spacer(Modifier.height(6.dp))
                        }

                        item {
                            Spacer(Modifier.height(2.dp))
                            CompactChip(
                                onClick = onBack,
                                label = { Text("Cerrar", fontSize = 10.sp) },
                                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
                                modifier = Modifier.fillMaxWidth(0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MascotaMarcarPerdidaCard: define la lógica de esta operación
fun MascotaMarcarPerdidaCard(
    mascota: MascotaParaPerder,
    onMarcarPerdida: () -> Unit
) {
    // Constante isPerdida: valor inmutable que no cambia tras su asignación
    val isPerdida = mascota.estado == "PERDIDA"
    // Constante estadoColor: valor inmutable que no cambia tras su asignación
    val estadoColor = if (isPerdida) AccentRed else AccentGreen
    // Constante estadoTexto: valor inmutable que no cambia tras su asignación
    val estadoTexto = if (isPerdida) "PERDIDA" else "EN CASA"

    Card(
        modifier = Modifier.fillMaxWidth(0.92f),
        onClick = { if (!isPerdida) onMarcarPerdida() },
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(CardBg, CardBg.copy(alpha = 0.7f))))
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(estadoColor)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (mascota.especie == "PERRO") "🐕" else "🐈",
                                fontSize = 16.sp
                            )
                            Column {
                                Text(
                                    text = mascota.nombre,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = listOf(mascota.raza, mascota.color)
                                        .filter { it.isNotEmpty() }
                                        .joinToString(" • "),
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(estadoColor, CircleShape)
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (isPerdida) {
                        Text(
                            text = "⚠️ Ya está perdida",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 9.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        CompactButton(
                            onClick = onMarcarPerdida,
                            colors = ButtonDefaults.buttonColors(backgroundColor = AccentRed),
                            modifier = Modifier.fillMaxWidth().height(28.dp)
                        ) {
                            Text("🆘 Marcar como Perdida", fontSize = 9.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}