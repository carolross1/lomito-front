package com.lomito.seguro.wear.ui.report

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class MascotaPerdida(
    val id: String,
    val nombre: String,
    val especie: String,
    val raza: String = "",
    val color: String = "",
    val fotoUrl: String? = null,
    val distanciaAlerta: Int = 50,
    val estado: String = "PERDIDA",
    val ownerId: String = "",
    val duenoNombre: String = "",
    val duenoTelefono: String = "",
    val ultimaUbicacionLat: Double? = null,
    val ultimaUbicacionLng: Double? = null
)

class ReportarAvistamientoActivity : ComponentActivity() {
    private val backendUrl = "http://192.168.100.12:3000"
    private var ubicacionLat = 0.0
    private var ubicacionLng = 0.0
    private var ubicacionTexto = "Obteniendo ubicacion..."
    private var ubicacionValida = false

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("AVISTAMIENTO", "Permiso de ubicacion concedido")
            obtenerUbicacion()
        } else {
            android.util.Log.d("AVISTAMIENTO", "Permiso de ubicacion denegado")
            ubicacionValida = false
            ubicacionTexto = "Sin permiso de ubicacion"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("AVISTAMIENTO", "INICIANDO")

        val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
        var userId = prefs.getString("user_id", "") ?: ""
        if (userId.isEmpty() || !userId.matches(Regex("^\\d+$"))) {
            userId = "2"
            prefs.edit().putString("user_id", userId).apply()
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            obtenerUbicacion()
        }

        setContent {
            var mascotasPerdidas by remember { mutableStateOf<List<MascotaPerdida>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }
            var errorMessage by remember { mutableStateOf("") }
            var successMessage by remember { mutableStateOf("") }
            var mostrarFormulario by remember { mutableStateOf(false) }
            var mostrarDetalles by remember { mutableStateOf(false) }
            var mascotaSeleccionada by remember { mutableStateOf<MascotaPerdida?>(null) }
            var isSendingReport by remember { mutableStateOf(false) }
            var isCreating by remember { mutableStateOf(false) }

            var paso by remember { mutableStateOf(0) }
            var nombre by remember { mutableStateOf("") }
            var especie by remember { mutableStateOf("PERRO") }
            var raza by remember { mutableStateOf("") }
            var color by remember { mutableStateOf("") }
            var telefono by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                val result = withContext(Dispatchers.IO) {
                    cargarMascotasPerdidas()
                }
                mascotasPerdidas = result.mascotas
                isLoading = false
                errorMessage = result.errorMessage
                android.util.Log.d("AVISTAMIENTO", "Mascotas cargadas: ${result.mascotas.size}")
            }

            when {
                mostrarDetalles && mascotaSeleccionada != null -> {
                    MascotaPerdidaDetailScreen(
                        mascota = mascotaSeleccionada!!,
                        onBack = {
                            mostrarDetalles = false
                            mascotaSeleccionada = null
                        },
                        onReportar = {
                            if (ubicacionValida) {
                                isSendingReport = true
                                CoroutineScope(Dispatchers.Main).launch {
                                    val result = withContext(Dispatchers.IO) {
                                        reportarAvistamiento(
                                            mascotaId = mascotaSeleccionada!!.id,
                                            lat = ubicacionLat,
                                            lng = ubicacionLng,
                                            direccion = ubicacionTexto
                                        )
                                    }
                                    isSendingReport = false
                                    if (result.success) {
                                        successMessage = "Reporte enviado"
                                        errorMessage = ""
                                        mostrarDetalles = false
                                        mascotaSeleccionada = null
                                        isLoading = true
                                        val newResult = withContext(Dispatchers.IO) {
                                            cargarMascotasPerdidas()
                                        }
                                        mascotasPerdidas = newResult.mascotas
                                        isLoading = false
                                        errorMessage = newResult.errorMessage
                                    } else {
                                        errorMessage = result.errorMessage
                                    }
                                }
                            } else {
                                errorMessage = "Sin ubicacion. Activa el GPS."
                            }
                        }
                    )
                }
                mostrarFormulario -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        FormularioScreenSimplificado(
                            paso = paso,
                            nombre = nombre,
                            especie = especie,
                            raza = raza,
                            color = color,
                            telefono = telefono,
                            isCreating = isCreating,
                            onNombreChange = { nombre = it },
                            onEspecieChange = { especie = it },
                            onRazaChange = { raza = it },
                            onColorChange = { color = it },
                            onTelefonoChange = { telefono = it },
                            onPasoChange = { paso = it },
                            onSave = {
                                if (nombre.isNotEmpty() && telefono.isNotEmpty()) {
                                    isCreating = true
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val result = withContext(Dispatchers.IO) {
                                            crearMascotaPerdida(nombre, especie, raza, color, telefono)
                                        }
                                        isCreating = false
                                        if (result.success) {
                                            successMessage = "Mascota publicada en el mural"
                                            errorMessage = ""
                                            mostrarFormulario = false
                                            paso = 0
                                            nombre = ""
                                            raza = ""
                                            color = ""
                                            telefono = ""
                                            isLoading = true
                                            val newResult = withContext(Dispatchers.IO) {
                                                cargarMascotasPerdidas()
                                            }
                                            mascotasPerdidas = newResult.mascotas
                                            isLoading = false
                                            errorMessage = newResult.errorMessage
                                            notificarNuevaMascotaPerdida(nombre)
                                        } else {
                                            errorMessage = result.errorMessage
                                        }
                                    }
                                } else {
                                    errorMessage = "Nombre y telefono son obligatorios"
                                }
                            },
                            onBack = {
                                mostrarFormulario = false
                                paso = 0
                                errorMessage = ""
                            }
                        )
                    }
                }
                else -> {
                    MainScreenGrid(
                        mascotas = mascotasPerdidas,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        successMessage = successMessage,
                        isSendingReport = isSendingReport,
                        ubicacionTexto = ubicacionTexto,
                        ubicacionValida = ubicacionValida,
                        onMascotaClick = { mascota ->
                            mascotaSeleccionada = mascota
                            mostrarDetalles = true
                        },
                        onAgregarClick = {
                            android.util.Log.d("AVISTAMIENTO", "Boton + presionado")
                            mostrarFormulario = true
                            paso = 0
                            errorMessage = ""
                            nombre = ""
                            raza = ""
                            color = ""
                            telefono = ""
                        },
                        onRetry = {
                            isLoading = true
                            errorMessage = ""
                            successMessage = ""
                            CoroutineScope(Dispatchers.Main).launch {
                                val result = withContext(Dispatchers.IO) {
                                    cargarMascotasPerdidas()
                                }
                                mascotasPerdidas = result.mascotas
                                isLoading = false
                                errorMessage = result.errorMessage
                            }
                        }
                    )
                }
            }
        }
    }

    private fun obtenerUbicacion() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ubicacionValida = false
            ubicacionTexto = "Sin permiso de ubicacion"
            return
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                ubicacionLat = location.latitude
                ubicacionLng = location.longitude
                ubicacionTexto = "Lat: ${String.format("%.4f", location.latitude)}, Lng: ${String.format("%.4f", location.longitude)}"
                ubicacionValida = true
                android.util.Log.d("AVISTAMIENTO", "Ubicacion obtenida: $ubicacionTexto")
            } else {
                ubicacionTexto = "Ubicacion no disponible"
                ubicacionValida = false
            }
        }.addOnFailureListener {
            ubicacionTexto = "Error obteniendo ubicacion"
            ubicacionValida = false
        }
    }

    private suspend fun cargarMascotasPerdidas(): CargaResult {
        return try {
            android.util.Log.d("AVISTAMIENTO", "Cargando mascotas del mural...")

            val url = URL("$backendUrl/api/mascotas/mural")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            val responseCode = conn.responseCode

            android.util.Log.d("AVISTAMIENTO", "Response Code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().readText()
                android.util.Log.d("AVISTAMIENTO", "Respuesta: $response")

                val jsonArray = JSONArray(response)
                val lista = mutableListOf<MascotaPerdida>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val ownerId = obj.optString("owner_id", "")
                    lista.add(
                        MascotaPerdida(
                            id = obj.getString("id"),
                            nombre = obj.getString("nombre"),
                            especie = obj.getString("especie"),
                            raza = obj.optString("raza", ""),
                            color = obj.optString("color", ""),
                            fotoUrl = obj.optString("foto_url", null),
                            distanciaAlerta = obj.getInt("distancia_alerta"),
                            estado = obj.getString("estado"),
                            ownerId = ownerId,
                            duenoNombre = if (ownerId == "0") "Reportado por comunidad" else obj.optString("dueno_nombre", "Dueño"),
                            duenoTelefono = obj.optString("telefono", "")
                        )
                    )
                }
                conn.disconnect()
                android.util.Log.d("AVISTAMIENTO", "${lista.size} mascotas cargadas")
                CargaResult(lista, if (lista.isEmpty()) "No hay mascotas perdidas en el mural" else "")
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.readText()
                conn.disconnect()
                android.util.Log.e("AVISTAMIENTO", "Error HTTP $responseCode: $errorBody")
                CargaResult(emptyList(), "Error al cargar (HTTP $responseCode)")
            }
        } catch (e: Exception) {
            android.util.Log.e("AVISTAMIENTO", "Error: ${e.message}", e)
            CargaResult(emptyList(), "Error: ${e.message}")
        }
    }

    private suspend fun reportarAvistamiento(
        mascotaId: String,
        lat: Double,
        lng: Double,
        direccion: String
    ): OperacionResult {
        return try {
            val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
            val userIdStr = prefs.getString("user_id", "2") ?: "2"
            val reportadoPorId = userIdStr.toIntOrNull() ?: 2

            android.util.Log.d("AVISTAMIENTO", "Reportando: $mascotaId en $lat, $lng")

            val url = URL("$backendUrl/api/reportes")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("mascota_id", mascotaId)
                put("latitud", lat)
                put("longitud", lng)
                put("reportado_por_id", reportadoPorId)
                put("direccion", direccion)
            }

            conn.outputStream.write(json.toString().toByteArray())
            val responseCode = conn.responseCode
            conn.disconnect()

            if (responseCode == 200 || responseCode == 201) {
                enviarNotificacionAlMovil(mascotaId, lat, lng, direccion)
                OperacionResult(true, "")
            } else {
                OperacionResult(false, "Error al reportar (HTTP $responseCode)")
            }
        } catch (e: Exception) {
            OperacionResult(false, "Error: ${e.message}")
        }
    }

    private suspend fun crearMascotaPerdida(
        nombre: String,
        especie: String,
        raza: String,
        color: String,
        telefono: String
    ): OperacionResult {
        return try {
            android.util.Log.d("AVISTAMIENTO", "Creando mascota perdida para el mural: $nombre")

            val url = URL("$backendUrl/api/mascotas/mural")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("nombre", nombre)
                put("especie", especie)
                put("raza", raza)
                put("color", color)
                put("telefono", telefono)
            }

            android.util.Log.d("AVISTAMIENTO", "JSON enviado: $json")

            conn.outputStream.write(json.toString().toByteArray())
            val responseCode = conn.responseCode
            val responseBody = if (responseCode == 200 || responseCode == 201) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText()
            }
            conn.disconnect()

            android.util.Log.d("AVISTAMIENTO", "Response Code: $responseCode")
            android.util.Log.d("AVISTAMIENTO", "Response: $responseBody")

            if (responseCode == 200 || responseCode == 201) {
                OperacionResult(true, "")
            } else {
                OperacionResult(false, "Error al crear (HTTP $responseCode): $responseBody")
            }
        } catch (e: Exception) {
            android.util.Log.e("AVISTAMIENTO", "Error: ${e.message}", e)
            OperacionResult(false, "Error: ${e.message}")
        }
    }

    private fun enviarNotificacionAlMovil(mascotaId: String, lat: Double, lng: Double, direccion: String) {
        val context = applicationContext
        val payload = JSONObject().apply {
            put("tipo", "AVISTAMIENTO_REPORTADO")
            put("mascota_id", mascotaId)
            put("latitud", lat)
            put("longitud", lng)
            put("direccion", direccion)
        }.toString().toByteArray()

        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { node ->
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, "/watch/avistamiento", payload)
                        .addOnSuccessListener {
                            android.util.Log.d("AVISTAMIENTO", "Notificacion enviada al movil")
                        }
                }
            }
    }

    private fun notificarNuevaMascotaPerdida(nombre: String) {
        val context = applicationContext
        val payload = JSONObject().apply {
            put("tipo", "NUEVA_MASCOTA_PERDIDA")
            put("nombre", nombre)
        }.toString().toByteArray()

        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { node ->
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, "/mascota/perdida/nueva", payload)
                        .addOnSuccessListener {
                            android.util.Log.d("AVISTAMIENTO", "Notificacion de nueva mascota enviada")
                        }
                }
            }
    }

    data class CargaResult(
        val mascotas: List<MascotaPerdida>,
        val errorMessage: String
    )

    data class OperacionResult(
        val success: Boolean,
        val errorMessage: String
    )
}

// 🎨 Paleta temática "mascotas perdidas"
private val BgTop = Color(0xFF1B1430)
private val BgBottom = Color(0xFF0D0B1A)
private val CardBg = Color(0xFF2C2C3E)
private val FieldBg = Color(0xFF2C2657)
private val AccentRed = Color(0xFFE85D5D)
private val AccentGreen = Color(0xFF4CD97B)
private val AccentBlue = Color(0xFF4D9FFF)

// ============================================================
// ✅ PANTALLA PRINCIPAL DEL MURAL - CON SCROLL VERTICAL
// ============================================================

@Composable
fun MainScreenGrid(
    mascotas: List<MascotaPerdida>,
    isLoading: Boolean,
    errorMessage: String,
    successMessage: String,
    isSendingReport: Boolean,
    ubicacionTexto: String,
    ubicacionValida: Boolean,
    onMascotaClick: (MascotaPerdida) -> Unit,
    onAgregarClick: () -> Unit,
    onRetry: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp, start = 6.dp, end = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ✅ HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🐾 Mural",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (ubicacionValida) "●" else "○",
                            fontSize = 8.sp,
                            color = if (ubicacionValida) AccentGreen else Color(0xFFFF9800)
                        )
                        Button(
                            onClick = onAgregarClick,
                            colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("+", fontSize = 14.sp, color = Color.White)
                        }
                    }
                }
            }

            // ✅ Mensaje de éxito
            if (successMessage.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentGreen.copy(alpha = 0.15f))
                            .clip(RoundedCornerShape(6.dp))
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "✅ $successMessage",
                            color = AccentGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ✅ Mensaje de error
            if (errorMessage.isNotEmpty() && !isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentRed.copy(alpha = 0.15f))
                            .clip(RoundedCornerShape(6.dp))
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "⚠️ ${errorMessage.take(25)}",
                            color = AccentRed,
                            fontSize = 8.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ✅ Estado de envío
            if (isSendingReport) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 1.5.dp,
                            indicatorColor = AccentBlue
                        )
                        Text(
                            text = "Enviando...",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 8.sp
                        )
                    }
                }
            }

            // ✅ CONTENIDO PRINCIPAL
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                indicatorColor = AccentGreen
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
            } else if (mascotas.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "📭 Sin mascotas",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
                                modifier = Modifier.size(width = 70.dp, height = 28.dp)
                            ) {
                                Text("↻", fontSize = 14.sp, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                // ✅ Grid de 2 columnas con items
                items(mascotas.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        pair.forEach { mascota ->
                            MascotaGridItemCompacto(
                                mascota = mascota,
                                onClick = { onMascotaClick(mascota) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Si es impar, agregar un espacio vacío
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
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

// ✅ ITEM DE MASCOTA MÁS COMPACTO
@Composable
fun MascotaGridItemCompacto(
    mascota: MascotaPerdida,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .padding(vertical = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CardBg)
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF3D3D5C)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (mascota.especie == "PERRO") "🐕" else "🐈",
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = mascota.nombre.take(8),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1
            )

            Text(
                text = "⚠️",
                fontSize = 6.sp
            )
        }
    }
}

// ============================================================
// ✅ PANTALLA DE DETALLES - COMPLETA Y SIN CORTES
// ============================================================

@Composable
fun MascotaPerdidaDetailScreen(
    mascota: MascotaPerdida,
    onBack: () -> Unit,
    onReportar: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
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
                        text = "🔍 Detalles",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    CompactButton(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF666666)),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Text("✕", fontSize = 10.sp, color = Color.White)
                    }
                }
            }

            // ✅ Avatar
            item {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(CardBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (mascota.especie == "PERRO") "🐕" else "🐈",
                        fontSize = 22.sp
                    )
                }
            }

            // ✅ Nombre y estado
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = mascota.nombre.take(10),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "⚠️",
                        fontSize = 10.sp,
                        color = AccentRed
                    )
                }
            }

            // ✅ Información en tarjeta
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    onClick = { }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardBg)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InfoRowCompacta("Especie", mascota.especie)
                            InfoRowCompacta("Raza", if (mascota.raza.isNotEmpty()) mascota.raza.take(6) else "-")
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InfoRowCompacta("Color", if (mascota.color.isNotEmpty()) mascota.color.take(6) else "-")
                            InfoRowCompacta("Dueño", mascota.duenoNombre.take(8))
                        }
                        if (mascota.duenoTelefono.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                InfoRowCompacta("Teléfono", mascota.duenoTelefono, AccentGreen)
                                Text("", fontSize = 8.sp)
                            }
                        }
                    }
                }
            }

            // ✅ Botón de reporte - SIMPLIFICADO Y LIMPIO
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onReportar,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AccentRed,
                        contentColor = Color.White
                    ),
                    // fillMaxWidth() hace que el botón tome el ancho disponible
                    // Usamos un height mayor para que el texto de varias líneas no se corte
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 4.dp), // Un pequeño margen para que no toque los bordes
                    shape = RoundedCornerShape(12.dp) // Los bordes redondeados hacen que el texto se ajuste mejor
                ) {
                    Text(
                        text = "Reportar ", // Tu texto largo
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, // Vital para textos largos
                        maxLines = 2 // Permite que el texto salte de línea en lugar de cortarse
                    )
                }
            }

            // Espacio al final
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// ✅ InfoRow más compacta
@Composable
fun InfoRowCompacta(label: String, value: String, color: Color = Color.White) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 7.sp
        )
        Text(
            text = value,
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

// ============================================================
// ✅ FORMULARIO - MANTENIDO INTACTO
// ============================================================

@Composable
fun FormularioScreenSimplificado(
    paso: Int,
    nombre: String,
    especie: String,
    raza: String,
    color: String,
    telefono: String,
    isCreating: Boolean,
    onNombreChange: (String) -> Unit,
    onEspecieChange: (String) -> Unit,
    onRazaChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onPasoChange: (Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    var mostrarTeclado by remember { mutableStateOf(false) }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0B1A))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                (0..5).forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == paso) 6.dp else 4.dp)
                            .clip(CircleShape)
                            .background(
                                if (i <= paso) AccentRed
                                else Color.White.copy(alpha = 0.15f)
                            )
                    )
                }
            }

            when (paso) {
                0 -> PasoNombreForm(
                    nombre = nombre,
                    onNombreChange = onNombreChange,
                    onSiguiente = { if (nombre.isNotEmpty()) onPasoChange(1) },
                    onAtras = onBack,
                    onMostrarTeclado = { mostrarTeclado = true }
                )
                1 -> PasoEspecieForm(
                    especie = especie,
                    onSelect = {
                        onEspecieChange(it)
                        onPasoChange(2)
                    },
                    onAtras = { onPasoChange(0) }
                )
                2 -> PasoOpcionalForm(
                    titulo = "Raza",
                    valor = raza,
                    placeholder = "Toca para escribir",
                    onValorChange = onRazaChange,
                    onSiguiente = { onPasoChange(3) },
                    onAtras = { onPasoChange(1) },
                    onMostrarTeclado = { mostrarTeclado = true }
                )
                3 -> PasoOpcionalForm(
                    titulo = "Color",
                    valor = color,
                    placeholder = "Toca para escribir",
                    onValorChange = onColorChange,
                    onSiguiente = { onPasoChange(4) },
                    onAtras = { onPasoChange(2) },
                    onMostrarTeclado = { mostrarTeclado = true }
                )
                4 -> PasoTelefonoForm(
                    telefono = telefono,
                    onTelefonoChange = onTelefonoChange,
                    onSiguiente = { if (telefono.isNotEmpty()) onPasoChange(5) },
                    onAtras = { onPasoChange(3) },
                    onMostrarTeclado = { mostrarTeclado = true }
                )
                5 -> PasoConfirmarForm(
                    nombre = nombre,
                    especie = especie,
                    raza = raza,
                    color = color,
                    telefono = telefono,
                    isCreating = isCreating,
                    onGuardar = onSave,
                    onAtras = { onPasoChange(4) }
                )
            }
        }
    }

    if (mostrarTeclado) {
        val valorActual = when (paso) {
            0 -> nombre
            2 -> raza
            3 -> color
            4 -> telefono
            else -> ""
        }

        val esNumerico = paso == 4

        val onLetraClick: (String) -> Unit = { letra ->
            when (paso) {
                0 -> onNombreChange(nombre + letra)
                2 -> onRazaChange(raza + letra)
                3 -> onColorChange(color + letra)
                4 -> onTelefonoChange(telefono + letra)
            }
        }

        val onBorrarClick: () -> Unit = {
            when (paso) {
                0 -> { if (nombre.isNotEmpty()) onNombreChange(nombre.dropLast(1)) }
                2 -> { if (raza.isNotEmpty()) onRazaChange(raza.dropLast(1)) }
                3 -> { if (color.isNotEmpty()) onColorChange(color.dropLast(1)) }
                4 -> { if (telefono.isNotEmpty()) onTelefonoChange(telefono.dropLast(1)) }
            }
        }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { mostrarTeclado = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                TecladoSimple(
                    valor = valorActual,
                    esNumerico = esNumerico,
                    onLetraClick = onLetraClick,
                    onBorrarClick = onBorrarClick,
                    onCerrar = { mostrarTeclado = false }
                )
            }
        }
    }
}

@Composable
fun PasoNombreForm(
    nombre: String,
    onNombreChange: (String) -> Unit,
    onSiguiente: () -> Unit,
    onAtras: () -> Unit,
    onMostrarTeclado: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("🐾", fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Nombre",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))

        Card(
            onClick = onMostrarTeclado,
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldBg)
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (nombre.isNotEmpty()) nombre else "Toca",
                    color = if (nombre.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = if (nombre.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactChip(
                onClick = onAtras,
                label = { Text("Cancelar", fontSize = 8.sp, color = Color.White) },
                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
                modifier = Modifier.width(60.dp)
            )
            CompactChip(
                onClick = onSiguiente,
                label = { Text("Sig", fontSize = 9.sp, color = Color.White) },
                colors = ChipDefaults.chipColors(
                    backgroundColor = if (nombre.isNotEmpty()) AccentBlue else Color(0xFF555555)
                ),
                modifier = Modifier.width(60.dp),
                enabled = nombre.isNotEmpty()
            )
        }
    }
}

@Composable
fun PasoOpcionalForm(
    titulo: String,
    valor: String,
    placeholder: String,
    onValorChange: (String) -> Unit,
    onSiguiente: () -> Unit,
    onAtras: () -> Unit,
    onMostrarTeclado: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("✏️", fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = titulo,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))

        Card(
            onClick = onMostrarTeclado,
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldBg)
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (valor.isNotEmpty()) valor else placeholder,
                    color = if (valor.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = if (valor.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactChip(
                onClick = onAtras,
                label = { Text("Atrás", fontSize = 9.sp, color = Color.White) },
                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
                modifier = Modifier.width(60.dp)
            )
            CompactChip(
                onClick = onSiguiente,
                label = { Text("Omitir", fontSize = 9.sp, color = Color.White) },
                colors = ChipDefaults.chipColors(backgroundColor = AccentBlue),
                modifier = Modifier.width(60.dp)
            )
        }
    }
}

@Composable
fun PasoTelefonoForm(
    telefono: String,
    onTelefonoChange: (String) -> Unit,
    onSiguiente: () -> Unit,
    onAtras: () -> Unit,
    onMostrarTeclado: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("📞", fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Teléfono",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))

        Card(
            onClick = onMostrarTeclado,
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldBg)
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (telefono.isNotEmpty()) telefono else "Ej: 123456",
                    color = if (telefono.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = if (telefono.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactChip(
                onClick = onAtras,
                label = { Text("Atrás", fontSize = 9.sp, color = Color.White) },
                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
                modifier = Modifier.width(60.dp)
            )
            CompactChip(
                onClick = onSiguiente,
                label = { Text("Sig", fontSize = 9.sp, color = Color.White) },
                colors = ChipDefaults.chipColors(
                    backgroundColor = if (telefono.isNotEmpty()) AccentBlue else Color(0xFF555555)
                ),
                modifier = Modifier.width(60.dp),
                enabled = telefono.isNotEmpty()
            )
        }
    }
}

@Composable
fun PasoEspecieForm(
    especie: String,
    onSelect: (String) -> Unit,
    onAtras: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Perro o gato?",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                onClick = { onSelect("PERRO") },
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (especie == "PERRO") AccentRed.copy(alpha = 0.3f)
                            else FieldBg
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🐕", fontSize = 22.sp)
                        Text(
                            text = "Perro",
                            fontSize = 9.sp,
                            color = if (especie == "PERRO") AccentRed else Color.White.copy(alpha = 0.7f),
                            fontWeight = if (especie == "PERRO") FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Card(
                onClick = { onSelect("GATO") },
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (especie == "GATO") AccentRed.copy(alpha = 0.3f)
                            else FieldBg
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🐈", fontSize = 22.sp)
                        Text(
                            text = "Gato",
                            fontSize = 9.sp,
                            color = if (especie == "GATO") AccentRed else Color.White.copy(alpha = 0.7f),
                            fontWeight = if (especie == "GATO") FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        CompactChip(
            onClick = onAtras,
            label = { Text("Atrás", fontSize = 9.sp, color = Color.White) },
            colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
            modifier = Modifier.fillMaxWidth(0.35f)
        )
    }
}

@Composable
fun PasoConfirmarForm(
    nombre: String,
    especie: String,
    raza: String,
    color: String,
    telefono: String,
    isCreating: Boolean,
    onGuardar: () -> Unit,
    onAtras: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(if (especie == "PERRO") "🐕" else "🐈", fontSize = 26.sp)
        Spacer(Modifier.height(2.dp))

        Text(
            text = nombre,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))

        val detalles = listOfNotNull(
            raza.ifEmpty { null },
            color.ifEmpty { null },
            telefono.ifEmpty { null }
        )
        if (detalles.isNotEmpty()) {
            Text(
                text = detalles.joinToString(" - "),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 8.sp,
                maxLines = 1
            )
        }

        Spacer(Modifier.height(10.dp))

        val puedeGuardar = nombre.isNotBlank() && telefono.isNotBlank() && !isCreating

        Button(
            onClick = { if (puedeGuardar) onGuardar() },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(36.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (puedeGuardar) AccentRed else Color(0xFF555555)
            ),
            enabled = puedeGuardar
        ) {
            if (isCreating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    indicatorColor = Color.White
                )
            } else {
                Text(
                    text = "Publicar",
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        CompactChip(
            onClick = onAtras,
            label = { Text("Atrás", fontSize = 9.sp, color = Color.White) },
            colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
            modifier = Modifier.fillMaxWidth(0.35f)
        )
    }
}

// ============================================================
// ✅ TECLADO REDISEÑADO - COMPLETO Y SIN CORTES
// ============================================================

@Composable
fun TecladoSimple(
    valor: String,
    esNumerico: Boolean,
    onLetraClick: (String) -> Unit,
    onBorrarClick: () -> Unit,
    onCerrar: () -> Unit
) {
    val keyBg = Color(0xFF3A3360)
    val displayBg = Color(0xFF252044)
    val accentRed = Color(0xFFE85D5D)
    val accentGreen = Color(0xFF4CD97B)
    val accentBlue = Color(0xFF4D9FFF)

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(14.dp),
        onClick = { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
                .padding(horizontal = 3.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Display
            Card(
                modifier = Modifier.fillMaxWidth(0.92f),
                shape = RoundedCornerShape(8.dp),
                onClick = { }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(displayBg)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = (if (valor.isEmpty()) "" else valor.takeLast(10)) + "▏",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (esNumerico) accentGreen.copy(alpha = 0.2f) else accentBlue.copy(alpha = 0.2f))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (esNumerico) "123" else "ABC",
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (esNumerico) accentGreen else accentBlue
                        )
                    }
                }
            }

            Spacer(Modifier.height(3.dp))

            if (esNumerico) {
                val filas = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                filas.forEach { fila ->
                    Row(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        fila.forEach { num ->
                            TeclaRedondaCompacta(num, keyBg, modifier = Modifier.weight(1f)) { onLetraClick(num) }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    TeclaAccionCompacta("⌫", accentRed, modifier = Modifier.weight(1f), onClick = onBorrarClick)
                    TeclaRedondaCompacta("0", keyBg, modifier = Modifier.weight(1f)) { onLetraClick("0") }
                    TeclaAccionCompacta("✓", accentGreen, modifier = Modifier.weight(1f), onClick = onCerrar)
                }
            } else {
                val filaQ = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
                val filaA = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
                val filaZ = listOf("Z", "X", "C", "V", "B", "N", "M")

                TecladoFilaCompacta(filaQ, keyBg, onLetraClick)
                Spacer(Modifier.height(2.dp))
                TecladoFilaCompacta(filaA, keyBg, onLetraClick, indentFraction = 0.5f)
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(modifier = Modifier.weight(1f))
                    filaZ.forEach { letra ->
                        TeclaPequeñaCompacta(letra, keyBg, modifier = Modifier.weight(1f)) { onLetraClick(letra) }
                    }
                    Box(modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    TeclaAccionCompacta("⌫", accentRed, modifier = Modifier.weight(1f), onClick = onBorrarClick)
                    TeclaAccionCompacta("␣", keyBg, modifier = Modifier.weight(1f)) { onLetraClick(" ") }
                    TeclaAccionCompacta("Listo", accentGreen, modifier = Modifier.weight(2f), onClick = onCerrar)
                }
            }
        }
    }
}

@Composable
private fun TecladoFilaCompacta(
    letras: List<String>,
    keyBg: Color,
    onLetraClick: (String) -> Unit,
    indentFraction: Float = 0f
) {
    Row(
        modifier = Modifier.fillMaxWidth(0.92f),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (indentFraction > 0f) Box(modifier = Modifier.weight(indentFraction))
        letras.forEach { letra ->
            TeclaPequeñaCompacta(letra, keyBg, modifier = Modifier.weight(1f)) { onLetraClick(letra) }
        }
        if (indentFraction > 0f) Box(modifier = Modifier.weight(indentFraction))
    }
}

@Composable
private fun TeclaPequeñaCompacta(
    letra: String,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(letra, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TeclaRedondaCompacta(
    numero: String,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(numero, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TeclaAccionCompacta(
    label: String,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}