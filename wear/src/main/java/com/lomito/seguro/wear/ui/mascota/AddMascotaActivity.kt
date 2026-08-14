// Paquete: com.lomito.seguro.wear.ui.mascota
package com.lomito.seguro.wear.ui.mascota
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
import org.json.JSONObject
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
import java.net.URL

/**
 * [Actividad para agregar una nueva mascota desde el reloj]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Capturar nombre, especie y umbral]
 * - [Enviar la solicitud al backend para registrar la nueva mascota]
 */
// Activity AddMascotaActivity: pantalla principal que gestiona el ciclo de vida
class AddMascotaActivity : ComponentActivity() {
    // Variable isSending: almacena el estado mutable de este componente
    private var isSending = false
    // Variable isSuccess: almacena el estado mutable de este componente
    private var isSuccess = false
    // Variable errorMessage: almacena el estado mutable de este componente
    private var errorMessage = ""
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    private val backendUrl = BuildConfig.BACKEND_URL

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
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
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (isSending) return
        isSending = true

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
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                // Constante json: valor inmutable que no cambia tras su asignación
                val json = JSONObject().apply {
                    put("nombre", nombre)
                    put("especie", especie)
                    put("distancia_alerta", umbral)
                    put("owner_id", userId.toInt())
                }

                conn.outputStream.write(json.toString().toByteArray())
                // Constante responseCode: valor inmutable que no cambia tras su asignación
                val responseCode = conn.responseCode
                conn.disconnect()

                // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                withContext(Dispatchers.Main) {
                    isSending = false
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (responseCode == 200 || responseCode == 201) {
                        isSuccess = true
                    } else {
                        errorMessage = "Error al crear"
                    }
                }
            } catch (e: Exception) {
                // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                withContext(Dispatchers.Main) {
                    isSending = false
                    errorMessage = e.message ?: "Error desconocido"
                }
            }
        }
    }
}

/**
 * [Pantalla de formulario para agregar mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [isSending]: Estado de envío de la solicitud
 * - [isSuccess]: Estado de éxito tras guardar
 * - [errorMessage]: Mensaje de error a mostrar si falla
 * - [onSave]: Acción al guardar los datos
 * - [onBack]: Acción para volver atrás
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función AddMascotaScreen: define la lógica de esta operación
fun AddMascotaScreen(
    isSending: Boolean,
    isSuccess: Boolean,
    errorMessage: String,
    onSave: (String, String, Int) -> Unit,
    onBack: () -> Unit
) {
    // Variable nombre: almacena el estado mutable de este componente
    var nombre by remember { mutableStateOf("") }
    // Variable especie: almacena el estado mutable de este componente
    var especie by remember { mutableStateOf("PERRO") }
    // Variable umbral: almacena el estado mutable de este componente
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

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFF44336),
                        fontSize = 8.sp
                    )
                }

                CompactButton(
                    onClick = {
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (isSending) "Creando..." else "Crear",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}