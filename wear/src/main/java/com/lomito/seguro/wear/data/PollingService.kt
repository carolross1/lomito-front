// Paquete: com.lomito.seguro.wear.data
package com.lomito.seguro.wear.data
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.wear.BuildConfig

// Importa las clases para manejo de notificaciones
import android.app.NotificationChannel
// Importa las clases para manejo de notificaciones
import android.app.NotificationManager
// Importa la clase Intent para navegación entre componentes
import android.app.PendingIntent
// Importa la dependencia necesaria: Service
import android.app.Service
// Importa el contexto de Android
import android.content.Context
// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa la dependencia necesaria: Build
import android.os.Build
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: IBinder
import android.os.IBinder
// Importa la dependencia necesaria: VibrationEffect
import android.os.VibrationEffect
// Importa la dependencia necesaria: Vibrator
import android.os.Vibrator
// Importa las clases para manejo de notificaciones
import androidx.core.app.NotificationCompat
// Importa la dependencia necesaria: AlertActivity
import com.lomito.seguro.wear.ui.alert.AlertActivity
// Importa la dependencia necesaria: DashboardActivity
import com.lomito.seguro.wear.ui.dashboard.DashboardActivity
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.*
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
import java.net.URL

/**
 * [Servicio en primer plano para consultar el estado del simulador]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Ejecutar consultas periódicas al backend para obtener distancia]
 * - [Notificar al usuario si la distancia supera el umbral configurado]
 */
// Servicio PollingService: componente en background para tareas de larga duración
class PollingService : Service() {

    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    private val backendUrl = BuildConfig.BACKEND_URL
    // Variable pollingJob: almacena el estado mutable de este componente
    private var pollingJob: Job? = null

    companion object {
        // Constante CHANNEL_ID: valor fijo definido en tiempo de compilación
        const val CHANNEL_ID = "lomito_polling"
        // Constante NOTIF_ID: valor fijo definido en tiempo de compilación
        const val NOTIF_ID = 2001
        // Variable distanciaActual: almacena el estado mutable de este componente
        var distanciaActual: Int = 0
        // Variable umbralActual: almacena el estado mutable de este componente
        var umbralActual: Int = 50
        // Variable mascotaIdActual: almacena el estado mutable de este componente
        var mascotaIdActual: String = ""
        // Variable mascotaNombreActual: almacena el estado mutable de este componente
        var mascotaNombreActual: String = ""
        // Variable alertaMostrada: almacena el estado mutable de este componente
        var alertaMostrada: Boolean = false
        // Variable ultimaDistanciaAlerta: almacena el estado mutable de este componente
        var ultimaDistanciaAlerta: Int = 0
        // Constante INCREMENTO_MINIMO: valor fijo definido en tiempo de compilación
        private const val INCREMENTO_MINIMO = 20
    }

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate() {
        // Invoca la implementación del método en la clase padre
        super.onCreate()
        crearCanalNotificacion()
        startForeground(NOTIF_ID, crearNotificacion("🐾 Lomito Seguro activo"))
        iniciarPolling()
    }

    // Sobreescribe la función onStartCommand de la clase padre
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Retorna el valor al llamador de la función
        return START_STICKY
    }

    // Sobreescribe la función onBind de la clase padre
    override fun onBind(intent: Intent?): IBinder? = null

    private fun iniciarPolling() {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        pollingJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (isActive) {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // Constante prefs: valor inmutable que no cambia tras su asignación
                    val prefs = applicationContext.getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)
                    mascotaNombreActual = prefs.getString("mascota_activa_nombre", "Tu mascota") ?: "Tu mascota"

                    // Constante url: valor inmutable que no cambia tras su asignación
                    val url = URL("$backendUrl/api/simulador/estado")
                    // Constante conn: valor inmutable que no cambia tras su asignación
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 2000
                    conn.readTimeout = 2000
                    conn.requestMethod = "GET"

                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        // Constante response: valor inmutable que no cambia tras su asignación
                        val response = conn.inputStream.bufferedReader().readText()
                        // Constante json: valor inmutable que no cambia tras su asignación
                        val json = JSONObject(response)
                        // Constante distancia: valor inmutable que no cambia tras su asignación
                        val distancia = json.optInt("distancia", 0)
                        // Constante umbral: valor inmutable que no cambia tras su asignación
                        val umbral = json.optInt("umbral", 50)
                        // Constante mascotaId: valor inmutable que no cambia tras su asignación
                        val mascotaId = json.optString("mascotaId", "")

                        distanciaActual = distancia
                        umbralActual = umbral
                        mascotaIdActual = mascotaId

                        sendBroadcast(Intent("com.lomito.seguro.wear.BLE_UPDATE").apply {
                            putExtra("distancia", distancia)
                            putExtra("mascotaId", mascotaId)
                            putExtra("umbral", umbral)
                            putExtra("superaUmbral", distancia > umbral)
                            setPackage(packageName)
                        })

                        // Registro de evento en el log de Android para depuración
                        android.util.Log.d("POLLING_SVC", "📡 distancia=$distancia umbral=$umbral mascota=$mascotaNombreActual")

                        // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                        withContext(Dispatchers.Main) {
                            // ✅ Evaluar condiciones
                            // Constante incremento: valor inmutable que no cambia tras su asignación
                            val incremento = distancia - ultimaDistanciaAlerta
                            // Constante superaUmbral: valor inmutable que no cambia tras su asignación
                            val superaUmbral = distancia > umbral
                            // Constante distanciaValida: valor inmutable que no cambia tras su asignación
                            val distanciaValida = distancia > 0
                            // Constante tieneMascota: valor inmutable que no cambia tras su asignación
                            val tieneMascota = mascotaId.isNotEmpty()
                            // Constante alertaNoActiva: valor inmutable que no cambia tras su asignación
                            val alertaNoActiva = !alertaMostrada
                            // Constante esPrimeraAlerta: valor inmutable que no cambia tras su asignación
                            val esPrimeraAlerta = ultimaDistanciaAlerta == 0
                            // Constante aumentoSignificativo: valor inmutable que no cambia tras su asignación
                            val aumentoSignificativo = incremento >= INCREMENTO_MINIMO

                            // ✅ Determinar si debe mostrar alerta (sin usar if como expresión)
                            // Variable debeMostrar: almacena el estado mutable de este componente
                            var debeMostrar = false
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (superaUmbral && distanciaValida && tieneMascota && alertaNoActiva) {
                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                if (esPrimeraAlerta || aumentoSignificativo) {
                                    debeMostrar = true
                                }
                            }

                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (debeMostrar) {
                                // ✅ Mostrar alerta
                                alertaMostrada = true
                                ultimaDistanciaAlerta = distancia
                                // Registro de evento en el log de Android para depuración
                                android.util.Log.d("POLLING_SVC", "🚨 Abriendo AlertActivity (incremento: $incremento m)")
                                vibrar()

                                // Constante intent: valor inmutable que no cambia tras su asignación
                                val intent = Intent(applicationContext, AlertActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                    putExtra("mascota_nombre", mascotaNombreActual)
                                    putExtra("distancia", distancia)
                                    putExtra("incremento", incremento)
                                }
                                startActivity(intent)
                            } else {
                                // ✅ Resetear cuando la distancia es segura
                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                if (distancia <= umbral || distancia == 0) {
                                    alertaMostrada = false
                                }
                            }

                            // ✅ Logs para debugging
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (distancia > umbral && alertaMostrada) {
                                // Registro de evento en el log de Android para depuración
                                android.util.Log.d("POLLING_SVC", "⏳ Alerta ya mostrada. Distancia actual: $distancia")
                            }
                        }
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("POLLING_SVC", "Error: ${e.message}")
                }
                delay(2000)
            }
        }
    }

    private fun vibrar() {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Constante vibrator: valor inmutable que no cambia tras su asignación
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Activa la vibración háptica del smartwatch para retroalimentación táctil
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300, 100, 600), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                // Activa la vibración háptica del smartwatch para retroalimentación táctil
                vibrator.vibrate(longArrayOf(0, 300, 100, 300, 100, 600), -1)
            }
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("POLLING_SVC", "Error vibrando: ${e.message}")
        }
    }

    private fun crearCanalNotificacion() {
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Constante mgr: valor inmutable que no cambia tras su asignación
            val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            // Crea el canal de notificación requerido en Android 8.0+
            mgr.createNotificationChannel(
                // Crea el canal de notificación requerido en Android 8.0+
                NotificationChannel(
                    CHANNEL_ID,
                    "Lomito Seguro Polling",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private fun crearNotificacion(texto: String) =
        // Construye la notificación con sus propiedades visuales y de comportamiento
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Lomito Seguro")
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    // Método del ciclo de vida: se limpia la actividad antes de destruirse
    override fun onDestroy() {
        // Invoca la implementación del método en la clase padre
        super.onDestroy()
        pollingJob?.cancel()
        alertaMostrada = false
    }
}