package com.lomito.seguro.wear.ui.mascota

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AddMascotaActivity : ComponentActivity() {
    private var isSending = false
    private var isSuccess = false
    private var errorMessage = ""
    private val backendUrl = "http://192.168.100.12:3000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AddMascotaScreen(
                isSending = isSending,
                isSuccess = isSuccess,
                errorMessage = errorMessage,
                onSave = { nombre, especie, umbral ->
                    crearMascota(nombre, especie, umbral)
                },
                onBack = { finish() }
            )
        }
    }

    private fun crearMascota(nombre: String, especie: String, umbral: Int) {
        if (isSending) return
        isSending = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
                val userId = prefs.getString("user_id", "2") ?: "2"

                val url = URL("$backendUrl/api/mascotas")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val json = JSONObject().apply {
                    put("nombre", nombre)
                    put("especie", especie)
                    put("distancia_alerta", umbral)
                    put("owner_id", userId.toInt())
                }

                conn.outputStream.write(json.toString().toByteArray())
                val responseCode = conn.responseCode
                conn.disconnect()

                withContext(Dispatchers.Main) {
                    isSending = false
                    if (responseCode == 200 || responseCode == 201) {
                        isSuccess = true
                    } else {
                        errorMessage = "Error al crear"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSending = false
                    errorMessage = e.message ?: "Error desconocido"
                }
            }
        }
    }
}

@Composable
fun AddMascotaScreen(
    isSending: Boolean,
    isSuccess: Boolean,
    errorMessage: String,
    onSave: (String, String, Int) -> Unit,
    onBack: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var especie by remember { mutableStateOf("PERRO") }
    var umbral by remember { mutableStateOf(50) }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2E))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header compacto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "➕ Agregar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                CompactButton(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF666666)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("✕", fontSize = 12.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (isSuccess) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 32.sp)
                        Text(
                            text = "¡Creada!",
                            color = Color(0xFF4CAF50),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CompactButton(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2196F3)),
                            modifier = Modifier.size(width = 80.dp, height = 32.dp)
                        ) {
                            Text("Volver", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            } else {
                // Campo Nombre
                Text(
                    text = "Nombre",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = if (nombre.isEmpty()) "Escribe..." else nombre,
                    color = if (nombre.isEmpty()) Color.White.copy(alpha = 0.3f) else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2C2C3E))
                        .padding(6.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Especie
                Text(
                    text = "Especie",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 9.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CompactButton(
                        onClick = { especie = "PERRO" },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (especie == "PERRO") Color(0xFF4CAF50) else Color(0xFF2C2C3E)
                        ),
                        modifier = Modifier.weight(1f).height(28.dp)
                    ) {
                        Text("🐕", fontSize = 12.sp)
                    }
                    CompactButton(
                        onClick = { especie = "GATO" },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (especie == "GATO") Color(0xFF4CAF50) else Color(0xFF2C2C3E)
                        ),
                        modifier = Modifier.weight(1f).height(28.dp)
                    ) {
                        Text("🐈", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Umbral
                Text(
                    text = "Umbral: $umbral m",
                    color = Color.White,
                    fontSize = 11.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(0.7f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactButton(
                        onClick = { if (umbral > 10) umbral -= 10 },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C2C3E)),
                        modifier = Modifier.weight(1f).height(28.dp)
                    ) {
                        Text("−", fontSize = 14.sp, color = Color.White)
                    }
                    CompactButton(
                        onClick = { if (umbral < 100) umbral += 10 },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C2C3E)),
                        modifier = Modifier.weight(1f).height(28.dp)
                    ) {
                        Text("+", fontSize = 14.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFF44336),
                        fontSize = 8.sp
                    )
                }

                CompactButton(
                    onClick = {
                        if (nombre.isNotEmpty()) {
                            onSave(nombre, especie, umbral)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (nombre.isNotEmpty()) Color(0xFF4CAF50) else Color(0xFF666666)
                    ),
                    modifier = Modifier.fillMaxWidth(0.8f).height(32.dp)
                ) {
                    Text(
                        if (isSending) "Creando..." else "Crear",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}