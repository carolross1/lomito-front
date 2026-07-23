package com.lomito.seguro.wear.ui.mascota

import android.content.Intent
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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MascotaDetailActivity : ComponentActivity() {

    private val backendUrl = "http://192.168.100.12:3000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mascotaId = intent.getStringExtra("mascota_id") ?: ""
        val nombre = intent.getStringExtra("mascota_nombre") ?: "Mascota"
        val especie = intent.getStringExtra("mascota_especie") ?: ""
        val raza = intent.getStringExtra("mascota_raza") ?: ""
        val edad = intent.getIntExtra("mascota_edad", 0)
        val color = intent.getStringExtra("mascota_color") ?: ""
        val peso = intent.getStringExtra("mascota_peso") ?: ""
        val distanciaAlerta = intent.getIntExtra("mascota_distancia_alerta", 50)
        var estadoInicial = intent.getStringExtra("mascota_estado") ?: "EN_CASA"
        val distanciaSimulada = intent.getIntExtra("mascota_distancia_simulada", 0)

        setContent {
            var estado by remember { mutableStateOf(estadoInicial) }
            var isUpdating by remember { mutableStateOf(false) }
            var showToast by remember { mutableStateOf(false) }
            var toastMessage by remember { mutableStateOf("") }

            MascotaDetailScreen(
                nombre = nombre,
                especie = especie,
                raza = raza,
                edad = edad,
                color = color,
                peso = peso,
                distanciaAlerta = distanciaAlerta,
                estado = estado,
                distanciaSimulada = distanciaSimulada,
                isUpdating = isUpdating,
                onBack = { finish() },
                onCambiarEstado = {
                    isUpdating = true
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val nuevoEstado = if (estado == "EN_CASA") "PERDIDA" else "EN_CASA"
                            val url = URL("$backendUrl/api/mascotas/$mascotaId/estado")
                            val conn = url.openConnection() as HttpURLConnection
                            conn.connectTimeout = 5000
                            conn.readTimeout = 5000
                            conn.requestMethod = "PUT"
                            conn.setRequestProperty("Content-Type", "application/json")
                            conn.doOutput = true

                            val json = JSONObject().apply {
                                put("estado", nuevoEstado)
                            }

                            conn.outputStream.write(json.toString().toByteArray())
                            val responseCode = conn.responseCode

                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                withContext(Dispatchers.Main) {
                                    estado = nuevoEstado
                                    toastMessage = if (nuevoEstado == "PERDIDA") {
                                        "🔴 Marcada como Perdida"
                                    } else {
                                        "🏠 Marcada como En Casa"
                                    }
                                    showToast = true

                                    // ✅ Enviar broadcast para actualizar la lista
                                    enviarBroadcastEstado(mascotaId, nuevoEstado)
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    toastMessage = "❌ Error actualizando estado"
                                    showToast = true
                                }
                            }
                            conn.disconnect()
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                toastMessage = "❌ Error: ${e.message}"
                                showToast = true
                            }
                        }
                        withContext(Dispatchers.Main) {
                            isUpdating = false
                            delay(2000)
                            showToast = false
                        }
                    }
                }
            )

            if (showToast) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 20.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .height(34.dp),
                        onClick = { },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF2E2A52)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = toastMessage,
                                color = Color.White,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * ✅ Enviar broadcast para actualizar la lista
     */
    private fun enviarBroadcastEstado(mascotaId: String, nuevoEstado: String) {
        val intent = Intent("com.lomito.seguro.wear.ESTADO_ACTUALIZADO").apply {
            putExtra("mascota_id", mascotaId)
            putExtra("nuevo_estado", nuevoEstado)
            setPackage(packageName)
        }
        sendBroadcast(intent)
        android.util.Log.d("DETAIL", "📢 Broadcast enviado: $mascotaId -> $nuevoEstado")
    }
}

// 🎨 Paleta temática "mascotas perdidas" (consistente con Dashboard y Lista)
private val BgTop = Color(0xFF1B1430)
private val BgBottom = Color(0xFF0D0B1A)
private val CardBg = Color(0xFF252044)
private val AccentRed = Color(0xFFE85D5D)
private val AccentGreen = Color(0xFF4CD97B)
private val AccentOrange = Color(0xFFFFA94D)

@Composable
fun MascotaDetailScreen(
    nombre: String,
    especie: String,
    raza: String,
    edad: Int,
    color: String,
    peso: String,
    distanciaAlerta: Int,
    estado: String,
    distanciaSimulada: Int,
    isUpdating: Boolean,
    onBack: () -> Unit,
    onCambiarEstado: () -> Unit
) {
    val estadoColor = when (estado) {
        "PERDIDA" -> AccentRed
        "ENCONTRADA" -> AccentGreen
        else -> AccentOrange
    }

    val estadoTexto = when (estado) {
        "PERDIDA" -> "Perdida"
        "ENCONTRADA" -> "Encontrada"
        else -> "En Casa"
    }

    val distanciaColor = when {
        distanciaSimulada > distanciaAlerta -> AccentRed
        distanciaSimulada > distanciaAlerta * 0.8 -> AccentOrange
        else -> AccentGreen
    }

    val btnColor = if (estado == "EN_CASA") AccentRed else AccentGreen
    val btnText = if (estado == "EN_CASA") "Marcar Perdida" else "Marcar En Casa"
    val btnIcon = if (estado == "EN_CASA") "🆘" else "🏠"

    Scaffold(
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
        ) {
            // ✅ LazyColumn en vez de Column fijo: si el contenido no
            // cabe completo en el círculo, se puede scrollear en vez
            // de cortarse o aplastarse.
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(top = 22.dp, bottom = 22.dp)
            ) {
                // Header
                item {
                    Text(
                        text = if (especie == "PERRO") "🐕" else "🐈",
                        fontSize = 30.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = nombre,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(6.dp))
                }

                // ✅ Tarjeta de estado destacada
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        onClick = { },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(estadoColor.copy(alpha = 0.18f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(estadoColor, CircleShape)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = estadoTexto,
                                color = estadoColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ✅ Tarjeta de distancia / alerta
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        onClick = { },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardBg)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📍 Distancia",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = "${distanciaSimulada}m",
                                    color = distanciaColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️ Alerta a",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = "${distanciaAlerta}m",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ✅ Tarjeta de datos (raza, edad, color, peso)
                val datos = buildList {
                    if (raza.isNotEmpty()) add("Raza" to raza)
                    if (edad > 0) add("Edad" to "$edad años")
                    if (color.isNotEmpty()) add("Color" to color)
                    if (peso.isNotEmpty()) add("Peso" to peso)
                }
                if (datos.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            onClick = { },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardBg)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                datos.forEachIndexed { index, (label, value) ->
                                    DetailRow(label, value)
                                    if (index != datos.lastIndex) Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                // ✅ Botón principal de acción
                item {
                    Button(
                        onClick = onCambiarEstado,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = btnColor),
                        enabled = !isUpdating
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                indicatorColor = Color.White
                            )
                        } else {
                            Text(
                                text = "$btnIcon $btnText",
                                fontSize = 11.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                // ✅ Botón de cerrar, discreto, al final (en vez de
                // flotando arriba pegado al borde curvo)
                item {
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

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 9.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}