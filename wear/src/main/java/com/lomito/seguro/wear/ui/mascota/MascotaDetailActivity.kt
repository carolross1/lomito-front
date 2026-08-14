// Paquete: com.lomito.seguro.wear.ui.mascota
package com.lomito.seguro.wear.ui.mascota
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.wear.BuildConfig

// Importa la clase Intent para navegación entre componentes
import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
import androidx.compose.ui.layout.ContentScale
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa componente de Jetpack Compose
import coil.compose.AsyncImage
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.*
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
import java.net.URL

/**
 * [Actividad para mostrar los detalles de una mascota específica]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Visualizar la información detallada de la mascota]
 * - [Permitir al usuario alternar el estado de la mascota (PERDIDA / EN CASA)]
 */
// Activity MascotaDetailActivity: pantalla principal que gestiona el ciclo de vida
class MascotaDetailActivity : ComponentActivity() {

    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    private val backendUrl = BuildConfig.BACKEND_URL

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)

        // Constante mascotaId: valor inmutable que no cambia tras su asignación
        val mascotaId = intent.getStringExtra("mascota_id") ?: ""
        // Constante nombre: valor inmutable que no cambia tras su asignación
        val nombre = intent.getStringExtra("mascota_nombre") ?: "Mascota"
        // Constante especie: valor inmutable que no cambia tras su asignación
        val especie = intent.getStringExtra("mascota_especie") ?: ""
        // Constante raza: valor inmutable que no cambia tras su asignación
        val raza = intent.getStringExtra("mascota_raza") ?: ""
        // Constante edad: valor inmutable que no cambia tras su asignación
        val edad = intent.getIntExtra("mascota_edad", 0)
        // Constante color: valor inmutable que no cambia tras su asignación
        val color = intent.getStringExtra("mascota_color") ?: ""
        // Constante peso: valor inmutable que no cambia tras su asignación
        val peso = intent.getStringExtra("mascota_peso") ?: ""
        // Constante fotoUrl: valor inmutable que no cambia tras su asignación
        val fotoUrl = intent.getStringExtra("mascota_foto")
        // Constante distanciaAlerta: valor inmutable que no cambia tras su asignación
        val distanciaAlerta = intent.getIntExtra("mascota_distancia_alerta", 50)
        // Variable estadoInicial: almacena el estado mutable de este componente
        var estadoInicial = intent.getStringExtra("mascota_estado") ?: "EN_CASA"
        // Constante distanciaSimulada: valor inmutable que no cambia tras su asignación
        val distanciaSimulada = intent.getIntExtra("mascota_distancia_simulada", 0)

        // Constante fotoUrlAbs: valor inmutable que no cambia tras su asignación
        val fotoUrlAbs = fotoUrl?.takeIf { it.isNotEmpty() }?.let {
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (it.startsWith("http")) it else "$backendUrl$it"
        }

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            // Variable estado: almacena el estado mutable de este componente
            var estado by remember { mutableStateOf(estadoInicial) }
            // Variable isUpdating: almacena el estado mutable de este componente
            var isUpdating by remember { mutableStateOf(false) }
            // Variable showToast: almacena el estado mutable de este componente
            var showToast by remember { mutableStateOf(false) }
            // Variable toastMessage: almacena el estado mutable de este componente
            var toastMessage by remember { mutableStateOf("") }

            MascotaDetailScreen(
                nombre = nombre,
                especie = especie,
                raza = raza,
                edad = edad,
                color = color,
                peso = peso,
                fotoUrl = fotoUrlAbs,
                distanciaAlerta = distanciaAlerta,
                estado = estado,
                distanciaSimulada = distanciaSimulada,
                isUpdating = isUpdating,
                onBack = { finish() },
                onCambiarEstado = {
                    isUpdating = true
                    // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                    CoroutineScope(Dispatchers.IO).launch {
                        // Bloque try-catch: maneja posibles excepciones en el código crítico
                        try {
                            // Constante nuevoEstado: valor inmutable que no cambia tras su asignación
                            val nuevoEstado = if (estado == "EN_CASA") "PERDIDA" else "EN_CASA"
                            // Constante url: valor inmutable que no cambia tras su asignación
                            val url = URL("$backendUrl/api/mascotas/$mascotaId/estado")
                            // Constante conn: valor inmutable que no cambia tras su asignación
                            val conn = url.openConnection() as HttpURLConnection
                            conn.connectTimeout = 5000
                            conn.readTimeout = 5000
                            conn.requestMethod = "PUT"
                            conn.setRequestProperty("Content-Type", "application/json")
                            conn.doOutput = true

                            // Constante json: valor inmutable que no cambia tras su asignación
                            val json = JSONObject().apply {
                                put("estado", nuevoEstado)
                            }

                            conn.outputStream.write(json.toString().toByteArray())
                            // Constante responseCode: valor inmutable que no cambia tras su asignación
                            val responseCode = conn.responseCode

                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
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
                                // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                                withContext(Dispatchers.Main) {
                                    toastMessage = "❌ Error actualizando estado"
                                    showToast = true
                                }
                            }
                            conn.disconnect()
                        } catch (e: Exception) {
                            // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                            withContext(Dispatchers.Main) {
                                toastMessage = "❌ Error: ${e.message}"
                                showToast = true
                            }
                        }
                        // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                        withContext(Dispatchers.Main) {
                            isUpdating = false
                            delay(2000)
                            showToast = false
                        }
                    }
                }
            )

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
        // Constante intent: valor inmutable que no cambia tras su asignación
        val intent = Intent("com.lomito.seguro.wear.ESTADO_ACTUALIZADO").apply {
            putExtra("mascota_id", mascotaId)
            putExtra("nuevo_estado", nuevoEstado)
            setPackage(packageName)
        }
        sendBroadcast(intent)
        // Registro de evento en el log de Android para depuración
        android.util.Log.d("DETAIL", "📢 Broadcast enviado: $mascotaId -> $nuevoEstado")
    }
}

// 🎨 Paleta temática "mascotas perdidas" (consistente con Dashboard y Lista)
// Constante BgTop: valor inmutable que no cambia tras su asignación
private val BgTop = Color(0xFF1B1430)
// Constante BgBottom: valor inmutable que no cambia tras su asignación
private val BgBottom = Color(0xFF0D0B1A)
// Constante CardBg: valor inmutable que no cambia tras su asignación
private val CardBg = Color(0xFF252044)
// Constante AccentRed: valor inmutable que no cambia tras su asignación
private val AccentRed = Color(0xFFE85D5D)
// Constante AccentGreen: valor inmutable que no cambia tras su asignación
private val AccentGreen = Color(0xFF4CD97B)
// Constante AccentOrange: valor inmutable que no cambia tras su asignación
private val AccentOrange = Color(0xFFFFA94D)

/**
 * [Pantalla de UI con el detalle de la mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [nombre]: Nombre de la mascota
 * - [especie]: Especie de la mascota
 * - [raza]: Raza de la mascota
 * - [edad]: Edad de la mascota
 * - [color]: Color de la mascota
 * - [peso]: Peso de la mascota
 * - [fotoUrl]: URL de la foto de la mascota
 * - [distanciaAlerta]: Distancia máxima permitida
 * - [estado]: Estado actual de la mascota
 * - [distanciaSimulada]: Distancia simulada o actual
 * - [isUpdating]: Estado de actualización
 * - [onBack]: Callback para volver atrás
 * - [onCambiarEstado]: Callback para alternar el estado
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MascotaDetailScreen: define la lógica de esta operación
fun MascotaDetailScreen(
    nombre: String,
    especie: String,
    raza: String,
    edad: Int,
    color: String,
    peso: String,
    fotoUrl: String?,
    distanciaAlerta: Int,
    estado: String,
    distanciaSimulada: Int,
    isUpdating: Boolean,
    onBack: () -> Unit,
    onCambiarEstado: () -> Unit
) {
    // Constante estadoColor: valor inmutable que no cambia tras su asignación
    val estadoColor = when (estado) {
        "PERDIDA" -> AccentRed
        "ENCONTRADA" -> AccentGreen
        else -> AccentOrange
    }

    // Constante estadoTexto: valor inmutable que no cambia tras su asignación
    val estadoTexto = when (estado) {
        "PERDIDA" -> "Perdida"
        "ENCONTRADA" -> "Encontrada"
        else -> "En Casa"
    }

    // Constante distanciaColor: valor inmutable que no cambia tras su asignación
    val distanciaColor = when {
        distanciaSimulada > distanciaAlerta -> AccentRed
        distanciaSimulada > distanciaAlerta * 0.8 -> AccentOrange
        else -> AccentGreen
    }

    // Constante btnColor: valor inmutable que no cambia tras su asignación
    val btnColor = if (estado == "EN_CASA") AccentRed else AccentGreen
    // Constante btnText: valor inmutable que no cambia tras su asignación
    val btnText = if (estado == "EN_CASA") "Marcar Perdida" else "Marcar En Casa"
    // Constante btnIcon: valor inmutable que no cambia tras su asignación
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
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (!fotoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = fotoUrl,
                            contentDescription = nombre,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(CardBg)
                        )
                    } else {
                        Text(
                            text = if (especie == "PERRO") "🐕" else "🐈",
                            fontSize = 30.sp
                        )
                    }
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
                // Constante datos: valor inmutable que no cambia tras su asignación
                val datos = buildList {
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (raza.isNotEmpty()) add("Raza" to raza)
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (edad > 0) add("Edad" to "$edad años")
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (color.isNotEmpty()) add("Color" to color)
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (peso.isNotEmpty()) add("Peso" to peso)
                }
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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

// Anotación que marca esta función como una función de composición de UI
@Composable
// Función DetailRow: define la lógica de esta operación
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