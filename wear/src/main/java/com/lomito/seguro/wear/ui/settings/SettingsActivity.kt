// Paquete: com.lomito.seguro.wear.ui.settings
package com.lomito.seguro.wear.ui.settings
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
import androidx.compose.foundation.clickable
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.style.TextAlign
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
 * [Modelo para representar los ajustes de una mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mantener el estado de la configuración (ej. distancia de alerta)]
 */
// Clase de datos MascotaSetting: modelo inmutable con propiedades de dominio
data class MascotaSetting(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String,
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String,
    // Constante especie: valor inmutable que no cambia tras su asignación
    val especie: String,
    // Variable distanciaAlerta: almacena el estado mutable de este componente
    var distanciaAlerta: Int = 50,
    // Constante ownerId: valor inmutable que no cambia tras su asignación
    val ownerId: Int = 0
)

/**
 * [Actividad para la configuración de preferencias en el reloj]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Cargar la lista de mascotas para cambiar su configuración individualmente]
 * - [Actualizar el umbral de distancia y la preferencia de vibración]
 */
// Activity SettingsActivity: pantalla principal que gestiona el ciclo de vida
class SettingsActivity : ComponentActivity() {
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    private val backendUrl = BuildConfig.BACKEND_URL
    // Constante userId: valor inmutable que no cambia tras su asignación
    private val userId = 2 // Usuario fijo por ahora

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)
        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            SettingsScreen()
        }
    }

    // Anotación que marca esta función como una función de composición de UI
    @Composable
    // Función SettingsScreen: define la lógica de esta operación
    fun SettingsScreen() {
        // Constante prefs: valor inmutable que no cambia tras su asignación
        val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
        // Constante listState: valor inmutable que no cambia tras su asignación
        val listState = rememberScalingLazyListState()

        // Estados
        // Variable mascotas: almacena el estado mutable de este componente
        var mascotas by remember { mutableStateOf<List<MascotaSetting>>(emptyList()) }
        // Variable isLoading: almacena el estado mutable de este componente
        var isLoading by remember { mutableStateOf(true) }
        // Variable isSaving: almacena el estado mutable de este componente
        var isSaving by remember { mutableStateOf(false) }
        // Variable errorMessage: almacena el estado mutable de este componente
        var errorMessage by remember { mutableStateOf("") }
        // Variable successMessage: almacena el estado mutable de este componente
        var successMessage by remember { mutableStateOf("") }
        // Variable mascotaSeleccionada: almacena el estado mutable de este componente
        var mascotaSeleccionada by remember {
            mutableStateOf<MascotaSetting?>(null)
        }
        // Variable umbralActual: almacena el estado mutable de este componente
        var umbralActual by remember { mutableStateOf(50) }
        // Variable vibracionActual: almacena el estado mutable de este componente
        var vibracionActual by remember { mutableStateOf(prefs.getBoolean("vibracion", true)) }

        // Cargar mascotas del usuario al inicio
        LaunchedEffect(Unit) {
            isLoading = true
            // Constante result: valor inmutable que no cambia tras su asignación
            val result = withContext(Dispatchers.IO) {
                cargarMascotasDelUsuario(userId)
            }
            mascotas = result.mascotas
            isLoading = false
            errorMessage = result.errorMessage

            // Si hay mascotas, seleccionar la primera o la guardada
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (mascotas.isNotEmpty()) {
                // Constante savedId: valor inmutable que no cambia tras su asignación
                val savedId = prefs.getString("mascota_seleccionada_id", "") ?: ""
                mascotaSeleccionada = if (savedId.isNotEmpty()) {
                    mascotas.find { it.id == savedId }
                } else {
                    mascotas.firstOrNull()
                }
                // Actualizar umbral actual
                umbralActual = mascotaSeleccionada?.distanciaAlerta ?: 50
            }
        }

        // Actualizar umbral cuando cambia la mascota seleccionada
        LaunchedEffect(mascotaSeleccionada) {
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (mascotaSeleccionada != null) {
                umbralActual = mascotaSeleccionada!!.distanciaAlerta
            }
        }

        Scaffold(
            timeText = { TimeText() },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
        ) {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A2E)),
                contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp, start = 6.dp, end = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ✅ Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚙️ Configuración",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ✅ Botón Guardar
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (!isLoading && mascotas.isNotEmpty()) {
                                CompactButton(
                                    onClick = {
                                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                        if (mascotaSeleccionada != null) {
                                            guardarCambios(
                                                mascotaId = mascotaSeleccionada!!.id,
                                                nuevaDistancia = umbralActual,
                                                vibracion = vibracionActual,
                                                onSuccess = {
                                                    successMessage = "✅ Configuración guardada"
                                                    errorMessage = ""
                                                    // Actualizar la distancia en la lista local
                                                    mascotas = mascotas.map {
                                                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                                        if (it.id == mascotaSeleccionada!!.id) {
                                                            it.copy(distanciaAlerta = umbralActual)
                                                        } else {
                                                            it
                                                        }
                                                    }
                                                    mascotaSeleccionada = mascotaSeleccionada?.copy(distanciaAlerta = umbralActual)
                                                },
                                                onError = { msg ->
                                                    errorMessage = msg
                                                    successMessage = ""
                                                }
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = if (isSaving) Color(0xFF666666) else Color(0xFF4CAF50)
                                    ),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape),
                                    enabled = !isSaving
                                ) {
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    if (isSaving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            indicatorColor = Color.White
                                        )
                                    } else {
                                        Text("💾", fontSize = 14.sp)
                                    }
                                }
                            }
                            // ✅ Botón cerrar
                            CompactButton(
                                onClick = { finish() },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF666666)),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("✕", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }

                // ✅ Mensajes de estado
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (successMessage.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                                .clip(RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = successMessage,
                                color = Color(0xFF4CAF50),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (errorMessage.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE85D5D).copy(alpha = 0.15f))
                                .clip(RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = "⚠️ $errorMessage",
                                color = Color(0xFFE85D5D),
                                fontSize = 8.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ✅ Estado de carga
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    indicatorColor = Color(0xFF4CAF50)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Cargando...",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
                // ✅ Error de carga
                else if (errorMessage.isNotEmpty() && mascotas.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚠️ $errorMessage",
                                color = Color(0xFFFF9800),
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                // ✅ Sin mascotas
                else if (mascotas.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "📭 Sin mascotas",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Agrega una mascota primero",
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }
                // ✅ Contenido
                else {
                    // ✅ Selector de mascota
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2C2C3E))
                                .clip(RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Seleccionar mascota",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 9.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            // Grid de mascotas en 2 columnas
                            // Itera sobre cada elemento de la colección y ejecuta el bloque
                            mascotas.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Itera sobre cada elemento de la colección y ejecuta el bloque
                                    pair.forEach { mascota ->
                                        // Constante isSelected: valor inmutable que no cambia tras su asignación
                                        val isSelected = mascotaSeleccionada?.id == mascota.id
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            onClick = {
                                                mascotaSeleccionada = mascota
                                                // Inicia el editor para modificar los SharedPreferences
                                                prefs.edit().putString("mascota_seleccionada_id", mascota.id).apply()
                                                // Limpiar mensajes al cambiar de mascota
                                                successMessage = ""
                                                errorMessage = ""
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                                        if (isSelected) Color(0xFF4CAF50).copy(alpha = 0.3f)
                                                        else Color(0xFF3D3D5C)
                                                    )
                                                    .padding(horizontal = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = if (mascota.especie == "PERRO") "🐕" else "🐈",
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = mascota.nombre.take(6),
                                                    color = if (isSelected) Color(0xFF4CAF50) else Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1
                                                )
                                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                                if (isSelected) {
                                                    Text(
                                                        text = "✓",
                                                        color = Color(0xFF4CAF50),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }

                    // ✅ Umbral de alerta
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (mascotaSeleccionada != null) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2C2C3E))
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📏 Distancia alerta",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 9.sp
                                    )
                                    Text(
                                        text = "$umbralActual m",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CompactButton(
                                        onClick = {
                                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                            if (umbralActual > 10) {
                                                umbralActual -= 10
                                                successMessage = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF666666)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("−10", fontSize = 11.sp, color = Color.White)
                                    }
                                    CompactButton(
                                        onClick = {
                                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                            if (umbralActual < 100) {
                                                umbralActual += 10
                                                successMessage = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF666666)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("+10", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // ✅ Vibración
                    item {
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2C2C3E))
                                .clip(RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "📳 Vibración",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (vibracionActual) "Activada" else "Desactivada",
                                    color = if (vibracionActual) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    fontSize = 8.sp
                                )
                            }
                            CompactButton(
                                onClick = {
                                    vibracionActual = !vibracionActual
                                    successMessage = ""
                                },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (vibracionActual) Color(0xFF4CAF50) else Color(0xFF666666)
                                ),
                                modifier = Modifier.size(width = 50.dp, height = 28.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    if (vibracionActual) "ON" else "OFF",
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Espacio al final
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    private fun guardarCambios(
        mascotaId: String,
        nuevaDistancia: Int,
        vibracion: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        CoroutineScope(Dispatchers.Main).launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // 1. Guardar vibración en SharedPreferences
                // Constante prefs: valor inmutable que no cambia tras su asignación
                val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
                // Inicia el editor para modificar los SharedPreferences
                prefs.edit().putBoolean("vibracion", vibracion).apply()
                // Inicia el editor para modificar los SharedPreferences
                prefs.edit().putInt("umbral_$mascotaId", nuevaDistancia).apply()

                // 2. Actualizar la distancia en el backend
                // Constante result: valor inmutable que no cambia tras su asignación
                val result = withContext(Dispatchers.IO) {
                    actualizarDistanciaMascota(mascotaId, nuevaDistancia)
                }

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (result.success) {
                    onSuccess()
                } else {
                    onError(result.errorMessage ?: "Error al guardar")
                }
            } catch (e: Exception) {
                onError("Error: ${e.message}")
            }
        }
    }

    private suspend fun actualizarDistanciaMascota(
        mascotaId: String,
        nuevaDistancia: Int
    ): OperacionResult {
        // Retorna el valor al llamador de la función
        return try {
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("SETTINGS", "Actualizando distancia de mascota $mascotaId a $nuevaDistancia")

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
                put("distancia_alerta", nuevaDistancia)
            }

            conn.outputStream.write(json.toString().toByteArray())
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            val responseCode = conn.responseCode
            // Constante responseBody: valor inmutable que no cambia tras su asignación
            val responseBody = if (responseCode == 200 || responseCode == 201) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText()
            }
            conn.disconnect()

            // Registro de evento en el log de Android para depuración
            android.util.Log.d("SETTINGS", "Response Code: $responseCode")
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("SETTINGS", "Response: $responseBody")

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (responseCode == 200 || responseCode == 201) {
                OperacionResult(true, "")
            } else {
                OperacionResult(false, "Error al actualizar (HTTP $responseCode)")
            }
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("SETTINGS", "Error: ${e.message}", e)
            OperacionResult(false, "Error: ${e.message}")
        }
    }

    private suspend fun cargarMascotasDelUsuario(ownerId: Int): CargaMascotasResult {
        // Retorna el valor al llamador de la función
        return try {
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("SETTINGS", "Cargando mascotas del usuario $ownerId")

            // Constante url: valor inmutable que no cambia tras su asignación
            val url = URL("$backendUrl/api/mascotas?ownerId=$ownerId")
            // Constante conn: valor inmutable que no cambia tras su asignación
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            val responseCode = conn.responseCode

            // Registro de evento en el log de Android para depuración
            android.util.Log.d("SETTINGS", "Response Code: $responseCode")

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Constante response: valor inmutable que no cambia tras su asignación
                val response = conn.inputStream.bufferedReader().readText()
                // Registro de evento en el log de Android para depuración
                android.util.Log.d("SETTINGS", "Respuesta: $response")

                // Constante jsonArray: valor inmutable que no cambia tras su asignación
                val jsonArray = JSONArray(response)
                // Constante lista: valor inmutable que no cambia tras su asignación
                val lista = mutableListOf<MascotaSetting>()

                // Itera sobre la colección para procesar cada elemento
                for (i in 0 until jsonArray.length()) {
                    // Constante obj: valor inmutable que no cambia tras su asignación
                    val obj = jsonArray.getJSONObject(i)
                    lista.add(
                        MascotaSetting(
                            id = obj.getString("id"),
                            nombre = obj.getString("nombre"),
                            especie = obj.getString("especie"),
                            distanciaAlerta = obj.optInt("distancia_alerta", 50),
                            ownerId = obj.optInt("owner_id", 0)
                        )
                    )
                }
                conn.disconnect()
                // Registro de evento en el log de Android para depuración
                android.util.Log.d("SETTINGS", "${lista.size} mascotas cargadas")
                CargaMascotasResult(lista, "")
            } else {
                // Constante errorBody: valor inmutable que no cambia tras su asignación
                val errorBody = conn.errorStream?.bufferedReader()?.readText()
                conn.disconnect()
                // Registro de evento en el log de Android para depuración
                android.util.Log.e("SETTINGS", "Error HTTP $responseCode: $errorBody")
                CargaMascotasResult(emptyList(), "Error al cargar (HTTP $responseCode)")
            }
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("SETTINGS", "Error: ${e.message}", e)
            CargaMascotasResult(emptyList(), "Error: ${e.message}")
        }
    }

    // Clase de datos CargaMascotasResult: modelo inmutable con propiedades de dominio
    data class CargaMascotasResult(
        // Constante mascotas: valor inmutable que no cambia tras su asignación
        val mascotas: List<MascotaSetting>,
        // Constante errorMessage: valor inmutable que no cambia tras su asignación
        val errorMessage: String
    )

    // Clase de datos OperacionResult: modelo inmutable con propiedades de dominio
    data class OperacionResult(
        // Constante success: valor inmutable que no cambia tras su asignación
        val success: Boolean,
        // Constante errorMessage: valor inmutable que no cambia tras su asignación
        val errorMessage: String?
    )
}