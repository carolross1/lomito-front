package com.lomito.seguro.wear.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class MascotaSetting(
    val id: String,
    val nombre: String,
    val especie: String,
    var distanciaAlerta: Int = 50,
    val ownerId: Int = 0
)

class SettingsActivity : ComponentActivity() {
    private val backendUrl = "http://192.168.100.12:3000"
    private val userId = 2 // Usuario fijo por ahora

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsScreen()
        }
    }

    @Composable
    fun SettingsScreen() {
        val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
        val listState = rememberScalingLazyListState()

        // Estados
        var mascotas by remember { mutableStateOf<List<MascotaSetting>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var isSaving by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        var successMessage by remember { mutableStateOf("") }
        var mascotaSeleccionada by remember {
            mutableStateOf<MascotaSetting?>(null)
        }
        var umbralActual by remember { mutableStateOf(50) }
        var vibracionActual by remember { mutableStateOf(prefs.getBoolean("vibracion", true)) }

        // Cargar mascotas del usuario al inicio
        LaunchedEffect(Unit) {
            isLoading = true
            val result = withContext(Dispatchers.IO) {
                cargarMascotasDelUsuario(userId)
            }
            mascotas = result.mascotas
            isLoading = false
            errorMessage = result.errorMessage

            // Si hay mascotas, seleccionar la primera o la guardada
            if (mascotas.isNotEmpty()) {
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
                            if (!isLoading && mascotas.isNotEmpty()) {
                                CompactButton(
                                    onClick = {
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
                            mascotas.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    pair.forEach { mascota ->
                                        val isSelected = mascotaSeleccionada?.id == mascota.id
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            onClick = {
                                                mascotaSeleccionada = mascota
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
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }

                    // ✅ Umbral de alerta
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
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 1. Guardar vibración en SharedPreferences
                val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
                prefs.edit().putBoolean("vibracion", vibracion).apply()
                prefs.edit().putInt("umbral_$mascotaId", nuevaDistancia).apply()

                // 2. Actualizar la distancia en el backend
                val result = withContext(Dispatchers.IO) {
                    actualizarDistanciaMascota(mascotaId, nuevaDistancia)
                }

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
        return try {
            android.util.Log.d("SETTINGS", "Actualizando distancia de mascota $mascotaId a $nuevaDistancia")

            val url = URL("$backendUrl/api/mascotas/$mascotaId")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("distancia_alerta", nuevaDistancia)
            }

            conn.outputStream.write(json.toString().toByteArray())
            val responseCode = conn.responseCode
            val responseBody = if (responseCode == 200 || responseCode == 201) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText()
            }
            conn.disconnect()

            android.util.Log.d("SETTINGS", "Response Code: $responseCode")
            android.util.Log.d("SETTINGS", "Response: $responseBody")

            if (responseCode == 200 || responseCode == 201) {
                OperacionResult(true, "")
            } else {
                OperacionResult(false, "Error al actualizar (HTTP $responseCode)")
            }
        } catch (e: Exception) {
            android.util.Log.e("SETTINGS", "Error: ${e.message}", e)
            OperacionResult(false, "Error: ${e.message}")
        }
    }

    private suspend fun cargarMascotasDelUsuario(ownerId: Int): CargaMascotasResult {
        return try {
            android.util.Log.d("SETTINGS", "Cargando mascotas del usuario $ownerId")

            val url = URL("$backendUrl/api/mascotas?ownerId=$ownerId")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            val responseCode = conn.responseCode

            android.util.Log.d("SETTINGS", "Response Code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().readText()
                android.util.Log.d("SETTINGS", "Respuesta: $response")

                val jsonArray = JSONArray(response)
                val lista = mutableListOf<MascotaSetting>()

                for (i in 0 until jsonArray.length()) {
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
                android.util.Log.d("SETTINGS", "${lista.size} mascotas cargadas")
                CargaMascotasResult(lista, "")
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.readText()
                conn.disconnect()
                android.util.Log.e("SETTINGS", "Error HTTP $responseCode: $errorBody")
                CargaMascotasResult(emptyList(), "Error al cargar (HTTP $responseCode)")
            }
        } catch (e: Exception) {
            android.util.Log.e("SETTINGS", "Error: ${e.message}", e)
            CargaMascotasResult(emptyList(), "Error: ${e.message}")
        }
    }

    data class CargaMascotasResult(
        val mascotas: List<MascotaSetting>,
        val errorMessage: String
    )

    data class OperacionResult(
        val success: Boolean,
        val errorMessage: String?
    )
}