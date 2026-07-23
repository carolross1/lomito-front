package com.lomito.seguro.wear.ui.selection

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import kotlinx.coroutines.*
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class MascotaSeleccion(
    val id: String,
    val nombre: String,
    val especie: String,
    val fotoUrl: String = "",
    val distanciaAlerta: Int = 50
)

class SelectionActivity : ComponentActivity() {
    // Estado de la UI
    private var mascotasList = mutableStateListOf<MascotaSeleccion>()
    private var isLoading = mutableStateOf(true)
    private var errorMsg = mutableStateOf("")
    private var debugMsg = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Limpiar preferencias para prueba
        getSharedPreferences("watch_prefs", MODE_PRIVATE).edit().clear().apply()

        // Cargar datos
        cargarMascotas()

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
                    if (debugMsg.value.isNotEmpty()) {
                        Text(
                            text = debugMsg.value,
                            color = Color(0xFF2196F3),
                            fontSize = 9.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Error
                    if (errorMsg.value.isNotEmpty() && !isLoading.value) {
                        Text(
                            text = errorMsg.value,
                            color = Color(0xFFFF9800),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Contenido
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
                                        getSharedPreferences("watch_prefs", MODE_PRIVATE)
                                            .edit()
                                            .putString("mascota_activa_id", mascota.id)
                                            .putString("mascota_activa_nombre", mascota.nombre)
                                            .putInt("mascota_umbral", mascota.distanciaAlerta)
                                            .apply()

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
            try {
                runOnUiThread {
                    isLoading.value = true
                    debugMsg.value = "Conectando a $backendUrl..."
                }

                val userId = "2"  // ✅ Usuario con mascotas
                val url = URL("$backendUrl/api/mascotas?ownerId=$userId")

                runOnUiThread { debugMsg.value = "Conectando a $url..." }

                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/json")

                val responseCode = conn.responseCode

                runOnUiThread { debugMsg.value = "Response Code: $responseCode" }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    val responseText = response.toString()
                    runOnUiThread { debugMsg.value = "Respuesta: ${responseText.take(100)}..." }

                    // Parsear JSON
                    val jsonArray = JSONArray(responseText)
                    val lista = mutableListOf<MascotaSeleccion>()

                    for (i in 0 until jsonArray.length()) {
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
                        if (lista.isEmpty()) {
                            errorMsg.value = "No hay mascotas para este usuario"
                        } else {
                            errorMsg.value = ""
                        }
                    }
                } else {
                    val errorReader = BufferedReader(InputStreamReader(conn.errorStream))
                    val errorResponse = StringBuilder()
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
        private const val backendUrl = "http://192.168.100.12:3000"
    }
}