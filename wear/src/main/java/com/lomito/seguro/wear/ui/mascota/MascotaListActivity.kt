package com.lomito.seguro.wear.ui.mascota

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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

data class MascotaItem(
    val id: String,
    val nombre: String,
    val especie: String,
    val raza: String = "",
    val edad: Int = 0,
    val color: String = "",
    val peso: String = "",
    val fotoUrl: String? = null,
    val distanciaAlerta: Int = 50,
    val estado: String = "EN_CASA"
)

class MascotaListActivity : ComponentActivity() {
    private var pollingJob: Job? = null
    private val backendUrl = "http://192.168.100.12:3000"
    private val distanciasSimuladas = mutableStateMapOf<String, Int>()

    // ✅ State para mascotas con actualización inmediata
    private var mascotasState by mutableStateOf<List<MascotaItem>>(emptyList())

    // ✅ BroadcastReceiver para actualizar el estado de una mascota
    private val estadoUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val mascotaId = intent.getStringExtra("mascota_id") ?: return
            val nuevoEstado = intent.getStringExtra("nuevo_estado") ?: return

            android.util.Log.d("MLIST", "📢 Actualizando estado de $mascotaId a $nuevoEstado")

            // ✅ Actualizar la lista inmediatamente
            mascotasState = mascotasState.map { item ->
                if (item.id == mascotaId) {
                    item.copy(estado = nuevoEstado)
                } else {
                    item
                }
            }
        }
    }

    private val bleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val mascotaId = intent.getStringExtra("mascotaId") ?: return
            val distancia = intent.getIntExtra("distancia", 0)
            if (mascotaId.isNotEmpty()) {
                distanciasSimuladas[mascotaId] = distancia
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Registrar receiver para actualizaciones de estado
        val filter = IntentFilter("com.lomito.seguro.wear.ESTADO_ACTUALIZADO")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(estadoUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(estadoUpdateReceiver, filter)
        }

        setContent {
            var isLoading by remember { mutableStateOf(true) }
            var errorMessage by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
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
                    CoroutineScope(Dispatchers.Main).launch {
                        val result = withContext(Dispatchers.IO) { cargarMascotas() }
                        mascotasState = result.mascotas
                        isLoading = false
                        errorMessage = result.errorMessage
                    }
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter("com.lomito.seguro.wear.BLE_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bleReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(bleReceiver, filter)
        }

        pollingJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (isActive) {
                try {
                    val url = URL("$backendUrl/api/simulador/estado")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 2000
                    conn.readTimeout = 2000
                    conn.requestMethod = "GET"
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        val response = conn.inputStream.bufferedReader().readText()
                        val json = JSONObject(response)
                        val distancia = json.optInt("distancia", 0)
                        val mascotaId = json.optString("mascotaId", "")
                        if (mascotaId.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                distanciasSimuladas[mascotaId] = distancia
                            }
                        }
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    android.util.Log.e("MLIST_POLL", "Error: ${e.message}")
                }
                delay(2000)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(bleReceiver)
        pollingJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(estadoUpdateReceiver)
        } catch (e: Exception) {
            // Receiver ya fue desregistrado
        }
    }

    private suspend fun cargarMascotas(): CargaResult {
        return try {
            val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
            val userId = prefs.getString("user_id", "2") ?: "2"
            val url = URL("$backendUrl/api/mascotas?ownerId=$userId")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                val lista = mutableListOf<MascotaItem>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    lista.add(
                        MascotaItem(
                            id = obj.getString("id"),
                            nombre = obj.getString("nombre"),
                            especie = obj.getString("especie"),
                            raza = obj.optString("raza", ""),
                            edad = obj.optInt("edad", 0),
                            color = obj.optString("color", ""),
                            peso = obj.optString("peso", ""),
                            fotoUrl = obj.optString("foto_url", null),
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

    data class CargaResult(val mascotas: List<MascotaItem>, val errorMessage: String)
}

// 🎨 Paleta temática "mascotas perdidas" (misma del Dashboard)
private val BgTop = Color(0xFF1B1430)
private val BgBottom = Color(0xFF0D0B1A)
private val CardBg = Color(0xFF252044)
private val AccentRed = Color(0xFFE85D5D)
private val AccentGreen = Color(0xFF4CD97B)
private val AccentOrange = Color(0xFFFFA94D)
private val AccentBlue = Color(0xFF4D9FFF)

@Composable
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

@Composable
fun MascotaItemCard(
    mascota: MascotaItem,
    distanciaSimulada: Int?,
    onClick: () -> Unit
) {
    val estadoColor = when (mascota.estado) {
        "PERDIDA" -> AccentRed
        "ENCONTRADA" -> AccentGreen
        else -> AccentOrange
    }

    val estadoTexto = when (mascota.estado) {
        "PERDIDA" -> "Perdida"
        "ENCONTRADA" -> "Encontrada"
        else -> "En Casa"
    }

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
                        Text(
                            text = if (mascota.especie == "PERRO") "🐕" else "🐈",
                            fontSize = 18.sp
                        )
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