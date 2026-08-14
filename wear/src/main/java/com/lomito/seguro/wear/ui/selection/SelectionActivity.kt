// Paquete: com.lomito.seguro.wear.ui.selection
package com.lomito.seguro.wear.ui.selection

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
import androidx.compose.runtime.*
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
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
// Importa la dependencia necesaria: BufferedReader
import java.io.BufferedReader
// Importa la dependencia necesaria: InputStreamReader
import java.io.InputStreamReader
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
import java.net.URL

/**
 * [Modelo de datos mínimo para la pantalla de selección]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Proveer la información necesaria para mostrar la lista inicial de selección]
 */
// Clase de datos MascotaSeleccion: modelo inmutable con propiedades de dominio
data class MascotaSeleccion(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String,
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String,
    // Constante especie: valor inmutable que no cambia tras su asignación
    val especie: String,
    // Constante fotoUrl: valor inmutable que no cambia tras su asignación
    val fotoUrl: String = "",
    // Constante distanciaAlerta: valor inmutable que no cambia tras su asignación
    val distanciaAlerta: Int = 50
)

/**
 * [Actividad inicial para seleccionar la mascota a monitorear]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Cargar la lista de mascotas desde el backend]
 * - [Guardar en SharedPreferences la mascota elegida y navegar a la pantalla principal]
 */
// Activity SelectionActivity: pantalla principal que gestiona el ciclo de vida
class SelectionActivity : ComponentActivity() {
    // Estado de la UI
    // Variable mascotasList: almacena el estado mutable de este componente
    private var mascotasList = mutableStateListOf<MascotaSeleccion>()
    // Variable isLoading: almacena el estado mutable de este componente
    private var isLoading = mutableStateOf(true)
    // Variable errorMsg: almacena el estado mutable de este componente
    private var errorMsg = mutableStateOf("")
    // Variable debugMsg: almacena el estado mutable de este componente
    private var debugMsg = mutableStateOf("")

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)

        // Limpiar preferencias para prueba
        // Accede al almacenamiento clave-valor persistente de la aplicación
        getSharedPreferences("watch_prefs", MODE_PRIVATE).edit().clear().apply()

        // Cargar datos
        cargarMascotas()

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            Scaffold(
                timeText = { TimeText() },
                vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A2E))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🐾 Selecciona tu mascota",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Debug info
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (debugMsg.value.isNotEmpty()) {
                        Text(
                            text = debugMsg.value,
                            color = Color(0xFF2196F3),
                            fontSize = 9.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Error
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (errorMsg.value.isNotEmpty() && !isLoading.value) {
                        Text(
                            text = errorMsg.value,
                            color = Color(0xFFFF9800),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Contenido
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (isLoading.value) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp,
                                    indicatorColor = Color(0xFF4CAF50)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Cargando mascotas...",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else if (mascotasList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No hay mascotas",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                CompactButton(
                                    onClick = {
                                        isLoading.value = true
                                        errorMsg.value = ""
                                        debugMsg.value = ""
                                        cargarMascotas()
                                    },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2196F3))
                                ) {
                                    Text("🔄 Reintentar", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        LazyColumn {
                            items(mascotasList) { mascota ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        // Guardar selección
                                        // Accede al almacenamiento clave-valor persistente de la aplicación
                                        getSharedPreferences("watch_prefs", MODE_PRIVATE)
                                            .edit()
                                            .putString("mascota_activa_id", mascota.id)
                                            .putString("mascota_activa_nombre", mascota.nombre)
                                            .putInt("mascota_umbral", mascota.distanciaAlerta)
                                            // Aplica los cambios de forma asíncrona en el hilo principal
                                            .apply()

                                        // Constante intent: valor inmutable que no cambia tras su asignación
                                        val intent = android.content.Intent(
                                            this@SelectionActivity,
                                            com.lomito.seguro.wear.ui.home.WearMainActivity::class.java
                                        )
                                        intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                        startActivity(intent)
                                        finish()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${mascota.especie} ${mascota.nombre}",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Umbral: ${mascota.distanciaAlerta}m",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 11.sp
                                            )
                                        }
                                        Text(
                                            text = "→",
                                            color = Color(0xFF4CAF50),
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun cargarMascotas() {
        // Ejecutar en un hilo separado
        Thread {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                runOnUiThread {
                    isLoading.value = true
                    debugMsg.value = "Conectando a $backendUrl..."
                }

                // Constante userId: valor inmutable que no cambia tras su asignación
                val userId = "2"  // ✅ Usuario con mascotas
                // Constante url: valor inmutable que no cambia tras su asignación
                val url = URL("$backendUrl/api/mascotas?ownerId=$userId")

                runOnUiThread { debugMsg.value = "Conectando a $url..." }

                // Constante conn: valor inmutable que no cambia tras su asignación
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/json")

                // Constante responseCode: valor inmutable que no cambia tras su asignación
                val responseCode = conn.responseCode

                runOnUiThread { debugMsg.value = "Response Code: $responseCode" }

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Constante reader: valor inmutable que no cambia tras su asignación
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    // Constante response: valor inmutable que no cambia tras su asignación
                    val response = StringBuilder()
                    // Variable line: almacena el estado mutable de este componente
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // Constante responseText: valor inmutable que no cambia tras su asignación
                    val responseText = response.toString()
                    runOnUiThread { debugMsg.value = "Respuesta: ${responseText.take(100)}..." }

                    // Parsear JSON
                    // Constante jsonArray: valor inmutable que no cambia tras su asignación
                    val jsonArray = JSONArray(responseText)
                    // Constante lista: valor inmutable que no cambia tras su asignación
                    val lista = mutableListOf<MascotaSeleccion>()

                    // Itera sobre la colección para procesar cada elemento
                    for (i in 0 until jsonArray.length()) {
                        // Constante obj: valor inmutable que no cambia tras su asignación
                        val obj = jsonArray.getJSONObject(i)
                        lista.add(
                            MascotaSeleccion(
                                id = obj.getString("id"),
                                nombre = obj.getString("nombre"),
                                especie = obj.getString("especie"),
                                distanciaAlerta = obj.getInt("distancia_alerta")
                            )
                        )
                    }

                    conn.disconnect()

                    runOnUiThread {
                        mascotasList.clear()
                        mascotasList.addAll(lista)
                        isLoading.value = false
                        debugMsg.value = "✅ ${lista.size} mascotas cargadas"
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (lista.isEmpty()) {
                            errorMsg.value = "No hay mascotas para este usuario"
                        } else {
                            errorMsg.value = ""
                        }
                    }
                } else {
                    // Constante errorReader: valor inmutable que no cambia tras su asignación
                    val errorReader = BufferedReader(InputStreamReader(conn.errorStream))
                    // Constante errorResponse: valor inmutable que no cambia tras su asignación
                    val errorResponse = StringBuilder()
                    // Variable line: almacena el estado mutable de este componente
                    var line: String?
                    while (errorReader.readLine().also { line = it } != null) {
                        errorResponse.append(line)
                    }
                    errorReader.close()
                    conn.disconnect()

                    runOnUiThread {
                        isLoading.value = false
                        errorMsg.value = "Error HTTP $responseCode"
                        debugMsg.value = errorResponse.toString().take(100)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    isLoading.value = false
                    errorMsg.value = "Error: ${e.message}"
                    debugMsg.value = e.stackTraceToString().take(200)
                }
                e.printStackTrace()
            }
        }.start()
    }

    companion object {
        // Constante backendUrl: valor inmutable que no cambia tras su asignación
        private val backendUrl = BuildConfig.BACKEND_URL
    }
}