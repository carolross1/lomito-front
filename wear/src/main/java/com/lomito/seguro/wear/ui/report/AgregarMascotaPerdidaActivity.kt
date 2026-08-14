// Paquete: com.lomito.seguro.wear.ui.report
package com.lomito.seguro.wear.ui.report
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.wear.BuildConfig

// Importa la dependencia necesaria: RemoteInput
import android.app.RemoteInput
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa componente de Jetpack Compose
import androidx.activity.compose.rememberLauncherForActivityResult
// Importa la dependencia necesaria: ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
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
// Importa la clase Intent para navegación entre componentes
import androidx.wear.input.RemoteInputIntentHelper
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.*
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
import java.net.URL

// Constante KEY_TEXTO: valor fijo definido en tiempo de compilación
private const val KEY_TEXTO = "key_texto_input"

// Declaración de la clase Paso
enum class Paso { NOMBRE, ESPECIE, RAZA, COLOR, TELEFONO, CONFIRMAR }

/**
 * [Actividad para registrar una mascota que ha sido encontrada o perdida por otra persona]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Recopilar los datos de una mascota ajena mediante pasos (wizard)]
 * - [Publicar el registro de la mascota como perdida en el backend]
 */
// Activity AgregarMascotaPerdidaActivity: pantalla principal que gestiona el ciclo de vida
class AgregarMascotaPerdidaActivity : ComponentActivity() {
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    private val backendUrl = BuildConfig.BACKEND_URL

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            // Variable paso: almacena el estado mutable de este componente
            var paso by remember { mutableStateOf(Paso.NOMBRE) }
            // Variable nombre: almacena el estado mutable de este componente
            var nombre by remember { mutableStateOf("") }
            // Variable especie: almacena el estado mutable de este componente
            var especie by remember { mutableStateOf("PERRO") }
            // Variable raza: almacena el estado mutable de este componente
            var raza by remember { mutableStateOf("") }
            // Variable color: almacena el estado mutable de este componente
            var color by remember { mutableStateOf("") }
            // Variable telefono: almacena el estado mutable de este componente
            var telefono by remember { mutableStateOf("") }
            // Variable isSending: almacena el estado mutable de este componente
            var isSending by remember { mutableStateOf(false) }
            // Variable isSuccess: almacena el estado mutable de este componente
            var isSuccess by remember { mutableStateOf(false) }
            // Variable errorMessage: almacena el estado mutable de este componente
            var errorMessage by remember { mutableStateOf("") }
            // Variable campoActivo: almacena el estado mutable de este componente
            var campoActivo by remember { mutableStateOf("") }

            // Constante inputLauncher: valor inmutable que no cambia tras su asignación
            val inputLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                // Constante texto: valor inmutable que no cambia tras su asignación
                val texto = RemoteInput.getResultsFromIntent(result.data)
                    ?.getCharSequence(KEY_TEXTO)
                    ?.toString()
                    ?.trim()
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (!texto.isNullOrEmpty()) {
                    // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
                    when (campoActivo) {
                        "nombre" -> { nombre = texto; paso = Paso.ESPECIE }
                        "raza" -> { raza = texto; paso = Paso.COLOR }
                        "color" -> { color = texto; paso = Paso.TELEFONO }
                        "telefono" -> { telefono = texto.filter { it.isDigit() }; paso = Paso.CONFIRMAR }
                    }
                }
            }

            // Función pedirInput: define la lógica de esta operación
            fun pedirInput(campo: String, label: String) {
                campoActivo = campo
                // Constante intent: valor inmutable que no cambia tras su asignación
                val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                // Constante remoteInputs: valor inmutable que no cambia tras su asignación
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
                // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
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
                            // Itera sobre cada elemento de la colección y ejecuta el bloque
                            Paso.entries.forEach { p ->
                                Box(
                                    modifier = Modifier
                                        .size(if (p == paso) 8.dp else 5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                            if (p.ordinal <= paso.ordinal)
                                                Color(0xFFE85D5D)
                                            else
                                                Color.White.copy(alpha = 0.15f)
                                        )
                                )
                            }
                        }

                        // ✅ Contenido del paso
                        // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
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
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        CoroutineScope(Dispatchers.IO).launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante prefs: valor inmutable que no cambia tras su asignación
                val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
                // Constante userId: valor inmutable que no cambia tras su asignación
                val userId = prefs.getString("user_id", "2") ?: "2"

                // Constante url: valor inmutable que no cambia tras su asignación
                val url = URL("$backendUrl/api/mascotas")
                // Constante conn: valor inmutable que no cambia tras su asignación
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                // Constante json: valor inmutable que no cambia tras su asignación
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
                // Constante responseCode: valor inmutable que no cambia tras su asignación
                val responseCode = conn.responseCode
                conn.disconnect()

                // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                withContext(Dispatchers.Main) {
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (responseCode == 200 || responseCode == 201) {
                        onSuccess()
                    } else {
                        onError("Error (HTTP $responseCode)")
                    }
                }
            } catch (e: Exception) {
                // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Error desconocido")
                }
            }
        }
    }

    // ✅ Función para notificar al móvil
    private fun notificarNuevaMascotaPerdida(nombre: String) {
        // Constante context: valor inmutable que no cambia tras su asignación
        val context = applicationContext
        // Constante payload: valor inmutable que no cambia tras su asignación
        val payload = JSONObject().apply {
            put("tipo", "NUEVA_MASCOTA_PERDIDA")
            put("nombre", nombre)
        }.toString().toByteArray()

        // Usa la API de Wearable para comunicación con dispositivos Wear OS
        com.google.android.gms.wearable.Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                nodes.forEach { node ->
                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    com.google.android.gms.wearable.Wearable.getMessageClient(context)
                        // Envía un mensaje al dispositivo Wear OS conectado
                        .sendMessage(node.id, "/mascota/perdida/nueva", payload)
                        .addOnSuccessListener {
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.d("MASCOTA_PERDIDA", "✅ Notificación enviada al móvil")
                        }
                        .addOnFailureListener { e ->
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.e("MASCOTA_PERDIDA", "❌ Error enviando: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                // Registro de evento en el log de Android para depuración
                android.util.Log.e("MASCOTA_PERDIDA", "❌ Error obteniendo nodos: ${e.message}")
            }
    }
}

// ==================== COMPONENTES UI ====================

// ✅ Paso 1: Nombre
// Anotación que marca esta función como una función de composición de UI
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
// Anotación que marca esta función como una función de composición de UI
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
// Anotación que marca esta función como una función de composición de UI
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
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
// Anotación que marca esta función como una función de composición de UI
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
// Anotación que marca esta función como una función de composición de UI
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

        // Constante detalles: valor inmutable que no cambia tras su asignación
        val detalles = listOfNotNull(
            raza.ifEmpty { null },
            color.ifEmpty { null },
            telefono.ifEmpty { null }
        )
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (detalles.isNotEmpty()) {
            Text(
                text = detalles.joinToString(" • "),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 8.sp,
                maxLines = 1
            )
        }

        Spacer(Modifier.height(10.dp))

        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (errorMessage.isNotEmpty()) {
            Text(
                text = "⚠️ $errorMessage",
                color = Color(0xFFFFA94D),
                fontSize = 8.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Constante puedeGuardar: valor inmutable que no cambia tras su asignación
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
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
// Anotación que marca esta función como una función de composición de UI
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
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
// Anotación que marca esta función como una función de composición de UI
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