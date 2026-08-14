// Paquete: com.lomito.seguro.wear.ui.report
package com.lomito.seguro.wear.ui.report
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.wear.BuildConfig

// Importa la dependencia necesaria: Manifest
import android.Manifest
// Importa la dependencia necesaria: PackageManager
import android.content.pm.PackageManager
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa la dependencia necesaria: ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
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
// Importa el contexto de Android
import androidx.core.content.ContextCompat
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa la dependencia necesaria: LocationServices
import com.google.android.gms.location.LocationServices
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.Wearable
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.*
// Importa el parser JSON
import org.json.JSONArray
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
import java.net.URL

/**
 * [Modelo para representar una mascota en el mural de perdidas]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Almacenar los datos de la mascota a buscar, incluyendo dueño e ubicación]
 */
// Clase de datos MascotaPerdida: modelo inmutable con propiedades de dominio
data class MascotaPerdida(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String,
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String,
    // Constante especie: valor inmutable que no cambia tras su asignación
    val especie: String,
    // Constante raza: valor inmutable que no cambia tras su asignación
    val raza: String = "",
    // Constante color: valor inmutable que no cambia tras su asignación
    val color: String = "",
    // Constante fotoUrl: valor inmutable que no cambia tras su asignación
    val fotoUrl: String? = null,
    // Constante distanciaAlerta: valor inmutable que no cambia tras su asignación
    val distanciaAlerta: Int = 50,
    // Constante estado: valor inmutable que no cambia tras su asignación
    val estado: String = "PERDIDA",
    // Constante ownerId: valor inmutable que no cambia tras su asignación
    val ownerId: String = "",
    // Constante duenoNombre: valor inmutable que no cambia tras su asignación
    val duenoNombre: String = "",
    // Constante duenoTelefono: valor inmutable que no cambia tras su asignación
    val duenoTelefono: String = "",
    // Constante ultimaUbicacionLat: valor inmutable que no cambia tras su asignación
    val ultimaUbicacionLat: Double? = null,
    // Constante ultimaUbicacionLng: valor inmutable que no cambia tras su asignación
    val ultimaUbicacionLng: Double? = null
)

/**
 * [Actividad principal para el mural de mascotas perdidas y reportar avistamientos]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mostrar la lista de mascotas reportadas como perdidas por la comunidad]
 * - [Permitir agregar nuevas mascotas al mural de perdidas]
 * - [Permitir reportar el avistamiento de una mascota con la ubicación GPS]
 */
// Activity ReportarAvistamientoActivity: pantalla principal que gestiona el ciclo de vida
class ReportarAvistamientoActivity : ComponentActivity() {
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    private val backendUrl = BuildConfig.BACKEND_URL
    // Variable ubicacionLat: almacena el estado mutable de este componente
    private var ubicacionLat = 0.0
    // Variable ubicacionLng: almacena el estado mutable de este componente
    private var ubicacionLng = 0.0
    // Variable ubicacionTexto: almacena el estado mutable de este componente
    private var ubicacionTexto = "Obteniendo ubicacion..."
    // Variable ubicacionValida: almacena el estado mutable de este componente
    private var ubicacionValida = false

    // Constante locationPermissionRequest: valor inmutable que no cambia tras su asignación
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (isGranted) {
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("AVISTAMIENTO", "Permiso de ubicacion concedido")
            obtenerUbicacion()
        } else {
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("AVISTAMIENTO", "Permiso de ubicacion denegado")
            ubicacionValida = false
            ubicacionTexto = "Sin permiso de ubicacion"
        }
    }

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)
        // Registro de evento en el log de Android para depuración
        android.util.Log.d("AVISTAMIENTO", "INICIANDO")

        // Constante prefs: valor inmutable que no cambia tras su asignación
        val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
        // Variable userId: almacena el estado mutable de este componente
        var userId = prefs.getString("user_id", "") ?: ""
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (userId.isEmpty() || !userId.matches(Regex("^\\d+$"))) {
            userId = "2"
            // Inicia el editor para modificar los SharedPreferences
            prefs.edit().putString("user_id", userId).apply()
        }

        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            obtenerUbicacion()
        }

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            // Variable mascotasPerdidas: almacena el estado mutable de este componente
            var mascotasPerdidas by remember { mutableStateOf<List<MascotaPerdida>>(emptyList()) }
            // Variable isLoading: almacena el estado mutable de este componente
            var isLoading by remember { mutableStateOf(true) }
            // Variable errorMessage: almacena el estado mutable de este componente
            var errorMessage by remember { mutableStateOf("") }
            // Variable successMessage: almacena el estado mutable de este componente
            var successMessage by remember { mutableStateOf("") }
            // Variable mostrarFormulario: almacena el estado mutable de este componente
            var mostrarFormulario by remember { mutableStateOf(false) }
            // Variable mostrarDetalles: almacena el estado mutable de este componente
            var mostrarDetalles by remember { mutableStateOf(false) }
            // Variable mascotaSeleccionada: almacena el estado mutable de este componente
            var mascotaSeleccionada by remember { mutableStateOf<MascotaPerdida?>(null) }
            // Variable isSendingReport: almacena el estado mutable de este componente
            var isSendingReport by remember { mutableStateOf(false) }
            // Variable isCreating: almacena el estado mutable de este componente
            var isCreating by remember { mutableStateOf(false) }

            // Variable paso: almacena el estado mutable de este componente
            var paso by remember { mutableStateOf(0) }
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

            LaunchedEffect(Unit) {
                // Constante result: valor inmutable que no cambia tras su asignación
                val result = withContext(Dispatchers.IO) {
                    cargarMascotasPerdidas()
                }
                mascotasPerdidas = result.mascotas
                isLoading = false
                errorMessage = result.errorMessage
                // Registro de evento en el log de Android para depuración
                android.util.Log.d("AVISTAMIENTO", "Mascotas cargadas: ${result.mascotas.size}")
            }

            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
            when {
                mostrarDetalles && mascotaSeleccionada != null -> {
                    MascotaPerdidaDetailScreen(
                        mascota = mascotaSeleccionada!!,
                        onBack = {
                            mostrarDetalles = false
                            mascotaSeleccionada = null
                        },
                        onReportar = {
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (ubicacionValida) {
                                isSendingReport = true
                                // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                                CoroutineScope(Dispatchers.Main).launch {
                                    // Constante result: valor inmutable que no cambia tras su asignación
                                    val result = withContext(Dispatchers.IO) {
                                        reportarAvistamiento(
                                            mascotaId = mascotaSeleccionada!!.id,
                                            lat = ubicacionLat,
                                            lng = ubicacionLng,
                                            direccion = ubicacionTexto
                                        )
                                    }
                                    isSendingReport = false
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    if (result.success) {
                                        successMessage = "Reporte enviado"
                                        errorMessage = ""
                                        mostrarDetalles = false
                                        mascotaSeleccionada = null
                                        isLoading = true
                                        // Constante newResult: valor inmutable que no cambia tras su asignación
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
                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                if (nombre.isNotEmpty() && telefono.isNotEmpty()) {
                                    isCreating = true
                                    // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                                    CoroutineScope(Dispatchers.Main).launch {
                                        // Constante result: valor inmutable que no cambia tras su asignación
                                        val result = withContext(Dispatchers.IO) {
                                            crearMascotaPerdida(nombre, especie, raza, color, telefono)
                                        }
                                        isCreating = false
                                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
                                            // Constante newResult: valor inmutable que no cambia tras su asignación
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
                            // Registro de evento en el log de Android para depuración
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
                            // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                            CoroutineScope(Dispatchers.Main).launch {
                                // Constante result: valor inmutable que no cambia tras su asignación
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
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ubicacionValida = false
            ubicacionTexto = "Sin permiso de ubicacion"
            // Retorna el valor al llamador de la función
            return
        }

        // Constante fusedClient: valor inmutable que no cambia tras su asignación
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        fusedClient.lastLocation.addOnSuccessListener { location ->
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (location != null) {
                ubicacionLat = location.latitude
                ubicacionLng = location.longitude
                ubicacionTexto = "Lat: ${String.format("%.4f", location.latitude)}, Lng: ${String.format("%.4f", location.longitude)}"
                ubicacionValida = true
                // Registro de evento en el log de Android para depuración
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
        // Retorna el valor al llamador de la función
        return try {
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("AVISTAMIENTO", "Cargando mascotas del mural...")

            // Constante url: valor inmutable que no cambia tras su asignación
            val url = URL("$backendUrl/api/mascotas/mural")
            // Constante conn: valor inmutable que no cambia tras su asignación
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            val responseCode = conn.responseCode

            // Registro de evento en el log de Android para depuración
            android.util.Log.d("AVISTAMIENTO", "Response Code: $responseCode")

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Constante response: valor inmutable que no cambia tras su asignación
                val response = conn.inputStream.bufferedReader().readText()
                // Registro de evento en el log de Android para depuración
                android.util.Log.d("AVISTAMIENTO", "Respuesta: $response")

                // Constante jsonArray: valor inmutable que no cambia tras su asignación
                val jsonArray = JSONArray(response)
                // Constante lista: valor inmutable que no cambia tras su asignación
                val lista = mutableListOf<MascotaPerdida>()

                // Itera sobre la colección para procesar cada elemento
                for (i in 0 until jsonArray.length()) {
                    // Constante obj: valor inmutable que no cambia tras su asignación
                    val obj = jsonArray.getJSONObject(i)
                    // Constante ownerId: valor inmutable que no cambia tras su asignación
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
                // Registro de evento en el log de Android para depuración
                android.util.Log.d("AVISTAMIENTO", "${lista.size} mascotas cargadas")
                CargaResult(lista, if (lista.isEmpty()) "No hay mascotas perdidas en el mural" else "")
            } else {
                // Constante errorBody: valor inmutable que no cambia tras su asignación
                val errorBody = conn.errorStream?.bufferedReader()?.readText()
                conn.disconnect()
                // Registro de evento en el log de Android para depuración
                android.util.Log.e("AVISTAMIENTO", "Error HTTP $responseCode: $errorBody")
                CargaResult(emptyList(), "Error al cargar (HTTP $responseCode)")
            }
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
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
        // Retorna el valor al llamador de la función
        return try {
            // Constante prefs: valor inmutable que no cambia tras su asignación
            val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
            // Constante userIdStr: valor inmutable que no cambia tras su asignación
            val userIdStr = prefs.getString("user_id", "2") ?: "2"
            // Constante reportadoPorId: valor inmutable que no cambia tras su asignación
            val reportadoPorId = userIdStr.toIntOrNull() ?: 2

            // Registro de evento en el log de Android para depuración
            android.util.Log.d("AVISTAMIENTO", "Reportando: $mascotaId en $lat, $lng")

            // Constante url: valor inmutable que no cambia tras su asignación
            val url = URL("$backendUrl/api/reportes")
            // Constante conn: valor inmutable que no cambia tras su asignación
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            // Constante json: valor inmutable que no cambia tras su asignación
            val json = JSONObject().apply {
                put("mascota_id", mascotaId)
                put("latitud", lat)
                put("longitud", lng)
                put("reportado_por_id", reportadoPorId)
                put("direccion", direccion)
            }

            conn.outputStream.write(json.toString().toByteArray())
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            val responseCode = conn.responseCode
            conn.disconnect()

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
        // Retorna el valor al llamador de la función
        return try {
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("AVISTAMIENTO", "Creando mascota perdida para el mural: $nombre")

            // Constante url: valor inmutable que no cambia tras su asignación
            val url = URL("$backendUrl/api/mascotas/mural")
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
            }

            // Registro de evento en el log de Android para depuración
            android.util.Log.d("AVISTAMIENTO", "JSON enviado: $json")

            conn.outputStream.write(json.toString().toByteArray())
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            val responseCode = conn.responseCode
            // Constante responseBody: valor inmutable que no cambia tras su asignación
            val responseBody = if (responseCode == 200 || responseCode == 201) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText()
            }
            conn.disconnect()

            // Registro de evento en el log de Android para depuración
            android.util.Log.d("AVISTAMIENTO", "Response Code: $responseCode")
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("AVISTAMIENTO", "Response: $responseBody")

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (responseCode == 200 || responseCode == 201) {
                OperacionResult(true, "")
            } else {
                OperacionResult(false, "Error al crear (HTTP $responseCode): $responseBody")
            }
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("AVISTAMIENTO", "Error: ${e.message}", e)
            OperacionResult(false, "Error: ${e.message}")
        }
    }

    private fun enviarNotificacionAlMovil(mascotaId: String, lat: Double, lng: Double, direccion: String) {
        // Constante context: valor inmutable que no cambia tras su asignación
        val context = applicationContext
        // Constante payload: valor inmutable que no cambia tras su asignación
        val payload = JSONObject().apply {
            put("tipo", "AVISTAMIENTO_REPORTADO")
            put("mascota_id", mascotaId)
            put("latitud", lat)
            put("longitud", lng)
            put("direccion", direccion)
        }.toString().toByteArray()

        // Usa la API de Wearable para comunicación con dispositivos Wear OS
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                nodes.forEach { node ->
                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    Wearable.getMessageClient(context)
                        // Envía un mensaje al dispositivo Wear OS conectado
                        .sendMessage(node.id, "/watch/avistamiento", payload)
                        .addOnSuccessListener {
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.d("AVISTAMIENTO", "Notificacion enviada al movil")
                        }
                }
            }
    }

    private fun notificarNuevaMascotaPerdida(nombre: String) {
        // Constante context: valor inmutable que no cambia tras su asignación
        val context = applicationContext
        // Constante payload: valor inmutable que no cambia tras su asignación
        val payload = JSONObject().apply {
            put("tipo", "NUEVA_MASCOTA_PERDIDA")
            put("nombre", nombre)
        }.toString().toByteArray()

        // Usa la API de Wearable para comunicación con dispositivos Wear OS
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                nodes.forEach { node ->
                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    Wearable.getMessageClient(context)
                        // Envía un mensaje al dispositivo Wear OS conectado
                        .sendMessage(node.id, "/mascota/perdida/nueva", payload)
                        .addOnSuccessListener {
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.d("AVISTAMIENTO", "Notificacion de nueva mascota enviada")
                        }
                }
            }
    }

    // Clase de datos CargaResult: modelo inmutable con propiedades de dominio
    data class CargaResult(
        // Constante mascotas: valor inmutable que no cambia tras su asignación
        val mascotas: List<MascotaPerdida>,
        // Constante errorMessage: valor inmutable que no cambia tras su asignación
        val errorMessage: String
    )

    // Clase de datos OperacionResult: modelo inmutable con propiedades de dominio
    data class OperacionResult(
        // Constante success: valor inmutable que no cambia tras su asignación
        val success: Boolean,
        // Constante errorMessage: valor inmutable que no cambia tras su asignación
        val errorMessage: String
    )
}

// 🎨 Paleta temática "mascotas perdidas"
// Constante BgTop: valor inmutable que no cambia tras su asignación
private val BgTop = Color(0xFF1B1430)
// Constante BgBottom: valor inmutable que no cambia tras su asignación
private val BgBottom = Color(0xFF0D0B1A)
// Constante CardBg: valor inmutable que no cambia tras su asignación
private val CardBg = Color(0xFF2C2C3E)
// Constante FieldBg: valor inmutable que no cambia tras su asignación
private val FieldBg = Color(0xFF2C2657)
// Constante AccentRed: valor inmutable que no cambia tras su asignación
private val AccentRed = Color(0xFFE85D5D)
// Constante AccentGreen: valor inmutable que no cambia tras su asignación
private val AccentGreen = Color(0xFF4CD97B)
// Constante AccentBlue: valor inmutable que no cambia tras su asignación
private val AccentBlue = Color(0xFF4D9FFF)

// ============================================================
// ✅ PANTALLA PRINCIPAL DEL MURAL - CON SCROLL VERTICAL
// ============================================================

/**
 * [Pantalla con la cuadrícula del mural de mascotas perdidas]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [mascotas]: Lista de mascotas del mural
 * - [isLoading]: Estado de carga del mural
 * - [errorMessage]: Mensaje de error a mostrar
 * - [successMessage]: Mensaje de éxito de la última operación
 * - [isSendingReport]: Indica si hay un reporte en proceso de envío
 * - [ubicacionTexto]: Texto descriptivo de la ubicación actual
 * - [ubicacionValida]: Indica si se tiene una ubicación GPS válida
 * - [onMascotaClick]: Acción al presionar una mascota del mural
 * - [onAgregarClick]: Acción al presionar el botón de agregar
 * - [onRetry]: Acción para recargar el mural
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MainScreenGrid: define la lógica de esta operación
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
    // Constante listState: valor inmutable que no cambia tras su asignación
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
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
                        // Itera sobre cada elemento de la colección y ejecuta el bloque
                        pair.forEach { mascota ->
                            MascotaGridItemCompacto(
                                mascota = mascota,
                                onClick = { onMascotaClick(mascota) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Si es impar, agregar un espacio vacío
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MascotaGridItemCompacto: define la lógica de esta operación
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

// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MascotaPerdidaDetailScreen: define la lógica de esta operación
fun MascotaPerdidaDetailScreen(
    mascota: MascotaPerdida,
    onBack: () -> Unit,
    onReportar: () -> Unit
) {
    // Constante listState: valor inmutable que no cambia tras su asignación
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
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función InfoRowCompacta: define la lógica de esta operación
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

// Anotación que marca esta función como una función de composición de UI
@Composable
// Función FormularioScreenSimplificado: define la lógica de esta operación
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
    // Variable mostrarTeclado: almacena el estado mutable de este componente
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
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                (0..5).forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == paso) 6.dp else 4.dp)
                            .clip(CircleShape)
                            .background(
                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                if (i <= paso) AccentRed
                                else Color.White.copy(alpha = 0.15f)
                            )
                    )
                }
            }

            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
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

    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
    if (mostrarTeclado) {
        // Constante valorActual: valor inmutable que no cambia tras su asignación
        val valorActual = when (paso) {
            0 -> nombre
            2 -> raza
            3 -> color
            4 -> telefono
            else -> ""
        }

        // Constante esNumerico: valor inmutable que no cambia tras su asignación
        val esNumerico = paso == 4

        // Constante onLetraClick: valor inmutable que no cambia tras su asignación
        val onLetraClick: (String) -> Unit = { letra ->
            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
            when (paso) {
                0 -> onNombreChange(nombre + letra)
                2 -> onRazaChange(raza + letra)
                3 -> onColorChange(color + letra)
                4 -> onTelefonoChange(telefono + letra)
            }
        }

        // Constante onBorrarClick: valor inmutable que no cambia tras su asignación
        val onBorrarClick: () -> Unit = {
            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
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

// Anotación que marca esta función como una función de composición de UI
@Composable
// Función PasoNombreForm: define la lógica de esta operación
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

// Anotación que marca esta función como una función de composición de UI
@Composable
// Función PasoOpcionalForm: define la lógica de esta operación
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

// Anotación que marca esta función como una función de composición de UI
@Composable
// Función PasoTelefonoForm: define la lógica de esta operación
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

// Anotación que marca esta función como una función de composición de UI
@Composable
// Función PasoEspecieForm: define la lógica de esta operación
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
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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

// Anotación que marca esta función como una función de composición de UI
@Composable
// Función PasoConfirmarForm: define la lógica de esta operación
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

        // Constante detalles: valor inmutable que no cambia tras su asignación
        val detalles = listOfNotNull(
            raza.ifEmpty { null },
            color.ifEmpty { null },
            telefono.ifEmpty { null }
        )
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (detalles.isNotEmpty()) {
            Text(
                text = detalles.joinToString(" - "),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 8.sp,
                maxLines = 1
            )
        }

        Spacer(Modifier.height(10.dp))

        // Constante puedeGuardar: valor inmutable que no cambia tras su asignación
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
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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

// Anotación que marca esta función como una función de composición de UI
@Composable
// Función TecladoSimple: define la lógica de esta operación
fun TecladoSimple(
    valor: String,
    esNumerico: Boolean,
    onLetraClick: (String) -> Unit,
    onBorrarClick: () -> Unit,
    onCerrar: () -> Unit
) {
    // Constante keyBg: valor inmutable que no cambia tras su asignación
    val keyBg = Color(0xFF3A3360)
    // Constante displayBg: valor inmutable que no cambia tras su asignación
    val displayBg = Color(0xFF252044)
    // Constante accentRed: valor inmutable que no cambia tras su asignación
    val accentRed = Color(0xFFE85D5D)
    // Constante accentGreen: valor inmutable que no cambia tras su asignación
    val accentGreen = Color(0xFF4CD97B)
    // Constante accentBlue: valor inmutable que no cambia tras su asignación
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

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (esNumerico) {
                // Constante filas: valor inmutable que no cambia tras su asignación
                val filas = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                filas.forEach { fila ->
                    Row(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // Itera sobre cada elemento de la colección y ejecuta el bloque
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
                // Constante filaQ: valor inmutable que no cambia tras su asignación
                val filaQ = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
                // Constante filaA: valor inmutable que no cambia tras su asignación
                val filaA = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
                // Constante filaZ: valor inmutable que no cambia tras su asignación
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
                    // Itera sobre cada elemento de la colección y ejecuta el bloque
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

// Anotación que marca esta función como una función de composición de UI
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
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (indentFraction > 0f) Box(modifier = Modifier.weight(indentFraction))
        // Itera sobre cada elemento de la colección y ejecuta el bloque
        letras.forEach { letra ->
            TeclaPequeñaCompacta(letra, keyBg, modifier = Modifier.weight(1f)) { onLetraClick(letra) }
        }
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (indentFraction > 0f) Box(modifier = Modifier.weight(indentFraction))
    }
}

// Anotación que marca esta función como una función de composición de UI
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

// Anotación que marca esta función como una función de composición de UI
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

// Anotación que marca esta función como una función de composición de UI
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