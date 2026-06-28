package com.lomito.seguro.wear.ui.report

import android.app.RemoteInput
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.compose.material.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val KEY_TEXTO = "key_texto_input"

enum class Paso { NOMBRE, ESPECIE, RAZA, COLOR, TELEFONO, CONFIRMAR }

class AgregarMascotaPerdidaActivity : ComponentActivity() {
    private val backendUrl = "http://192.168.100.12:3000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var paso by remember { mutableStateOf(Paso.NOMBRE) }
            var nombre by remember { mutableStateOf("") }
            var especie by remember { mutableStateOf("PERRO") }
            var raza by remember { mutableStateOf("") }
            var color by remember { mutableStateOf("") }
            var telefono by remember { mutableStateOf("") }
            var isSending by remember { mutableStateOf(false) }
            var isSuccess by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf("") }
            var campoActivo by remember { mutableStateOf("") }

            val inputLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val texto = RemoteInput.getResultsFromIntent(result.data)
                    ?.getCharSequence(KEY_TEXTO)
                    ?.toString()
                    ?.trim()
                if (!texto.isNullOrEmpty()) {
                    when (campoActivo) {
                        "nombre" -> { nombre = texto; paso = Paso.ESPECIE }
                        "raza" -> { raza = texto; paso = Paso.COLOR }
                        "color" -> { color = texto; paso = Paso.TELEFONO }
                        "telefono" -> { telefono = texto.filter { it.isDigit() }; paso = Paso.CONFIRMAR }
                    }
                }
            }

            fun pedirInput(campo: String, label: String) {
                campoActivo = campo
                val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                val remoteInputs = listOf(RemoteInput.Builder(KEY_TEXTO).setLabel(label).build())
                RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                inputLauncher.launch(intent)
            }

            // ✅ Pantalla principal
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D0B1A)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isSuccess -> PantallaExito { finish() }
                    else -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // ✅ Indicador de pasos (puntos)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Paso.entries.forEach { p ->
                                Box(
                                    modifier = Modifier
                                        .size(if (p == paso) 8.dp else 5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (p.ordinal <= paso.ordinal)
                                                Color(0xFFE85D5D)
                                            else
                                                Color.White.copy(alpha = 0.15f)
                                        )
                                )
                            }
                        }

                        // ✅ Contenido del paso
                        when (paso) {
                            Paso.NOMBRE -> PasoNombre(
                                nombre = nombre,
                                onPedir = { pedirInput("nombre", "Nombre") },
                                onCancel = { finish() }
                            )
                            Paso.ESPECIE -> PasoEspecie(
                                especie = especie,
                                onSelect = {
                                    especie = it
                                    paso = Paso.RAZA
                                },
                                onAtras = { paso = Paso.NOMBRE }
                            )
                            Paso.RAZA -> PasoInput(
                                titulo = "Raza",
                                emoji = "🐾",
                                valor = raza,
                                esOpcional = true,
                                onPedir = { pedirInput("raza", "Raza") },
                                onSiguiente = { paso = Paso.COLOR },
                                onAtras = { paso = Paso.ESPECIE }
                            )
                            Paso.COLOR -> PasoInput(
                                titulo = "Color",
                                emoji = "🎨",
                                valor = color,
                                esOpcional = true,
                                onPedir = { pedirInput("color", "Color") },
                                onSiguiente = { paso = Paso.TELEFONO },
                                onAtras = { paso = Paso.RAZA }
                            )
                            Paso.TELEFONO -> PasoTelefono(
                                telefono = telefono,
                                onPedir = { pedirInput("telefono", "Teléfono") },
                                onAtras = { paso = Paso.COLOR }
                            )
                            Paso.CONFIRMAR -> PasoConfirmar(
                                nombre = nombre,
                                especie = especie,
                                raza = raza,
                                color = color,
                                telefono = telefono,
                                isSending = isSending,
                                errorMessage = errorMessage,
                                onGuardar = {
                                    if (!isSending) {
                                        isSending = true
                                        errorMessage = ""
                                        crearMascotaPerdida(
                                            nombre = nombre,
                                            especie = especie,
                                            raza = raza,
                                            color = color,
                                            telefono = telefono,
                                            onSuccess = {
                                                isSending = false
                                                isSuccess = true
                                                notificarNuevaMascotaPerdida(nombre)
                                            },
                                            onError = {
                                                isSending = false
                                                errorMessage = it
                                            }
                                        )
                                    }
                                },
                                onAtras = { paso = Paso.TELEFONO }
                            )
                        }
                    }
                }
            }
        }
    }

    // ✅ Función para crear mascota perdida
    private fun crearMascotaPerdida(
        nombre: String,
        especie: String,
        raza: String,
        color: String,
        telefono: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
                val userId = prefs.getString("user_id", "2") ?: "2"

                val url = URL("$backendUrl/api/mascotas")
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
                    put("estado", "PERDIDA")
                    put("owner_id", userId.toInt())
                }

                conn.outputStream.write(json.toString().toByteArray())
                val responseCode = conn.responseCode
                conn.disconnect()

                withContext(Dispatchers.Main) {
                    if (responseCode == 200 || responseCode == 201) {
                        onSuccess()
                    } else {
                        onError("Error (HTTP $responseCode)")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Error desconocido")
                }
            }
        }
    }

    // ✅ Función para notificar al móvil
    private fun notificarNuevaMascotaPerdida(nombre: String) {
        val context = applicationContext
        val payload = JSONObject().apply {
            put("tipo", "NUEVA_MASCOTA_PERDIDA")
            put("nombre", nombre)
        }.toString().toByteArray()

        com.google.android.gms.wearable.Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { node ->
                    com.google.android.gms.wearable.Wearable.getMessageClient(context)
                        .sendMessage(node.id, "/mascota/perdida/nueva", payload)
                        .addOnSuccessListener {
                            android.util.Log.d("MASCOTA_PERDIDA", "✅ Notificación enviada al móvil")
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("MASCOTA_PERDIDA", "❌ Error enviando: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MASCOTA_PERDIDA", "❌ Error obteniendo nodos: ${e.message}")
            }
    }
}

// ==================== COMPONENTES UI ====================

// ✅ Paso 1: Nombre
@Composable
private fun PasoNombre(
    nombre: String,
    onPedir: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("📍", fontSize = 28.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Nombre de la mascota",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
        Spacer(Modifier.height(10.dp))

        Card(
            onClick = onPedir,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C2657))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (nombre.isNotEmpty()) nombre else "Toca para dictar",
                    color = if (nombre.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = if (nombre.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        CompactChip(
            onClick = onCancel,
            label = { Text("Cancelar", fontSize = 9.sp, color = Color.White) },
            colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
            modifier = Modifier.fillMaxWidth(0.5f)
        )
    }
}

// ✅ Paso 2: Especie
@Composable
private fun PasoEspecie(
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
            text = "¿Perro o gato?",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotonEspecie(
                emoji = "🐕",
                label = "Perro",
                selected = especie == "PERRO",
                onClick = { onSelect("PERRO") }
            )
            BotonEspecie(
                emoji = "🐈",
                label = "Gato",
                selected = especie == "GATO",
                onClick = { onSelect("GATO") }
            )
        }

        Spacer(Modifier.height(12.dp))

        CompactChip(
            onClick = onAtras,
            label = { Text("Atrás", fontSize = 9.sp, color = Color.White) },
            colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
            modifier = Modifier.fillMaxWidth(0.4f)
        )
    }
}

// ✅ Paso 3/4: Input opcional
@Composable
private fun PasoInput(
    titulo: String,
    emoji: String,
    valor: String,
    esOpcional: Boolean,
    onPedir: () -> Unit,
    onSiguiente: () -> Unit,
    onAtras: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(emoji, fontSize = 28.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (esOpcional) "$titulo (opcional)" else titulo,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
        Spacer(Modifier.height(10.dp))

        Card(
            onClick = onPedir,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C2657))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (valor.isNotEmpty()) valor else "Toca para dictar",
                    color = if (valor.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = if (valor.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactChip(
                onClick = onAtras,
                label = { Text("Atrás", fontSize = 9.sp, color = Color.White) },
                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
                modifier = Modifier.width(70.dp)
            )
            CompactChip(
                onClick = onSiguiente,
                label = {
                    Text(
                        if (valor.isEmpty()) "Omitir" else "Siguiente",
                        fontSize = 9.sp,
                        color = Color.White
                    )
                },
                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF4D9FFF)),
                modifier = Modifier.width(85.dp)
            )
        }
    }
}

// ✅ Paso 5: Teléfono
@Composable
private fun PasoTelefono(
    telefono: String,
    onPedir: () -> Unit,
    onAtras: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("📞", fontSize = 28.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tu número de teléfono",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
        Spacer(Modifier.height(10.dp))

        Card(
            onClick = onPedir,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C2657))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (telefono.isNotEmpty()) telefono else "Ej: 123456789",
                    color = if (telefono.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = if (telefono.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        CompactChip(
            onClick = onAtras,
            label = { Text("Atrás", fontSize = 9.sp, color = Color.White) },
            colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
            modifier = Modifier.fillMaxWidth(0.4f)
        )
    }
}

// ✅ Paso final: Confirmar
@Composable
private fun PasoConfirmar(
    nombre: String,
    especie: String,
    raza: String,
    color: String,
    telefono: String,
    isSending: Boolean,
    errorMessage: String,
    onGuardar: () -> Unit,
    onAtras: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(if (especie == "PERRO") "🐕" else "🐈", fontSize = 30.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            text = nombre,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))

        val detalles = listOfNotNull(
            raza.ifEmpty { null },
            color.ifEmpty { null },
            telefono.ifEmpty { null }
        )
        if (detalles.isNotEmpty()) {
            Text(
                text = detalles.joinToString(" • "),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 8.sp,
                maxLines = 1
            )
        }

        Spacer(Modifier.height(10.dp))

        if (errorMessage.isNotEmpty()) {
            Text(
                text = "⚠️ $errorMessage",
                color = Color(0xFFFFA94D),
                fontSize = 8.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        val puedeGuardar = nombre.isNotBlank() && telefono.isNotBlank() && !isSending

        Button(
            onClick = { if (puedeGuardar) onGuardar() },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(38.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (puedeGuardar) Color(0xFFE85D5D) else Color(0xFF555555)
            ),
            enabled = puedeGuardar
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    indicatorColor = Color.White
                )
            } else {
                Text("Publicar", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(6.dp))

        CompactChip(
            onClick = onAtras,
            label = { Text("Atrás", fontSize = 9.sp, color = Color.White) },
            colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
            modifier = Modifier.fillMaxWidth(0.4f)
        )
    }
}

// ✅ Botón de especie
@Composable
private fun BotonEspecie(
    emoji: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (selected) Color(0xFFE85D5D).copy(alpha = 0.3f)
                    else Color(0xFF2C2657)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(emoji, fontSize = 26.sp)
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = if (selected) Color(0xFFE85D5D) else Color.White.copy(alpha = 0.7f),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ✅ Pantalla de éxito
@Composable
private fun PantallaExito(onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("✅", fontSize = 40.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "¡Reportada!",
            color = Color(0xFF4CD97B),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Se publicará en el mural",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 9.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        CompactChip(
            onClick = onBack,
            label = { Text("Volver", fontSize = 10.sp, color = Color.White) },
            colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF4D9FFF)),
            modifier = Modifier.fillMaxWidth(0.5f)
        )
    }
}