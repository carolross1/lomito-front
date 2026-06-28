package com.lomito.seguro.wear.ui.mascota

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class MascotaParaPerder(
    val id: String,
    val nombre: String,
    val especie: String,
    val raza: String = "",
    val color: String = "",
    val fotoUrl: String? = null,
    val distanciaAlerta: Int = 50,
    val estado: String = "EN_CASA"
)

class MarcarPerdidaActivity : ComponentActivity() {
    private val backendUrl = "http://192.168.100.12:3000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
        var userId = prefs.getString("user_id", "") ?: ""

        if (userId.isEmpty() || !userId.matches(Regex("^\\d+$"))) {
            userId = "2"
            prefs.edit().putString("user_id", userId).apply()
        }

        android.util.Log.d("MARCAR_PERDIDA", "📱 userId: $userId")

        setContent {
            var mascotas by remember { mutableStateOf<List<MascotaParaPerder>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }
            var errorMessage by remember { mutableStateOf("") }
            var successMessage by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
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
                    CoroutineScope(Dispatchers.Main).launch {
                        val result = withContext(Dispatchers.IO) {
                            marcarComoPerdida(mascotaId, userId)
                        }
                        if (result.success) {
                            successMessage = "✅ Mascota marcada como PERDIDA"
                            errorMessage = ""
                            // Recargar lista
                            isLoading = true
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
                    CoroutineScope(Dispatchers.Main).launch {
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
        return try {
            android.util.Log.d("MARCAR_PERDIDA", "📱 Cargando mascotas para usuario: $userId")

            val url = URL("$backendUrl/api/mascotas?ownerId=$userId")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            val responseCode = conn.responseCode
            android.util.Log.d("MARCAR_PERDIDA", "📡 Response Code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().readText()
                android.util.Log.d("MARCAR_PERDIDA", "📥 Respuesta: $response")

                val jsonArray = JSONArray(response)
                android.util.Log.d("MARCAR_PERDIDA", "📊 JSON Array Length: ${jsonArray.length()}")

                val lista = mutableListOf<MascotaParaPerder>()
                for (i in 0 until jsonArray.length()) {
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
            android.util.Log.e("MARCAR_PERDIDA", "❌ Error: ${e.message}", e)
            CargaResult(
                mascotas = emptyList(),
                errorMessage = "Error: ${e.message}"
            )
        }
    }

    private suspend fun marcarComoPerdida(mascotaId: String, userId: String): OperacionResult {
        return try {
            android.util.Log.d("MARCAR_PERDIDA", "🔴 Marcando mascota $mascotaId como PERDIDA")

            val url = URL("$backendUrl/api/mascotas/$mascotaId")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("estado", "PERDIDA")
            }

            conn.outputStream.write(json.toString().toByteArray())
            val responseCode = conn.responseCode
            conn.disconnect()

            android.util.Log.d("MARCAR_PERDIDA", "📡 Response Code: $responseCode")

            if (responseCode == 200 || responseCode == 201) {
                OperacionResult(success = true, errorMessage = "")
            } else {
                OperacionResult(success = false, errorMessage = "Error al marcar como perdida (HTTP $responseCode)")
            }
        } catch (e: Exception) {
            android.util.Log.e("MARCAR_PERDIDA", "❌ Error: ${e.message}", e)
            OperacionResult(success = false, errorMessage = "Error: ${e.message}")
        }
    }

    data class CargaResult(
        val mascotas: List<MascotaParaPerder>,
        val errorMessage: String
    )

    data class OperacionResult(
        val success: Boolean,
        val errorMessage: String
    )
}

// 🎨 Paleta temática "mascotas perdidas" (consistente con el resto de la app)
private val BgTop = Color(0xFF1B1430)
private val BgBottom = Color(0xFF0D0B1A)
private val CardBg = Color(0xFF252044)
private val AccentRed = Color(0xFFE85D5D)
private val AccentGreen = Color(0xFF4CD97B)
private val AccentOrange = Color(0xFFFFA94D)
private val AccentBlue = Color(0xFF4D9FFF)

@Composable
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

@Composable
fun MascotaMarcarPerdidaCard(
    mascota: MascotaParaPerder,
    onMarcarPerdida: () -> Unit
) {
    val isPerdida = mascota.estado == "PERDIDA"
    val estadoColor = if (isPerdida) AccentRed else AccentGreen
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