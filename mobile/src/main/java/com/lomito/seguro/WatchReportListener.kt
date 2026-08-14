// Paquete: com.lomito.seguro
package com.lomito.seguro

// Importa las clases para manejo de notificaciones
import android.app.NotificationChannel
// Importa las clases para manejo de notificaciones
import android.app.NotificationManager
// Importa el contexto de Android
import android.content.Context
// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa la dependencia necesaria: Build
import android.os.Build
// Importa las clases para manejo de notificaciones
import androidx.core.app.NotificationCompat
// Importa la dependencia necesaria: LocalBroadcastManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.MessageEvent
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.WearableListenerService
// Importa el cliente Retrofit para peticiones HTTP
import com.lomito.seguro.data.api.RetrofitClient
// Importa la dependencia necesaria: ReporteRequest
import com.lomito.seguro.data.model.ReporteRequest
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.CoroutineScope
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.Dispatchers
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch
// Importa el parser JSON
import org.json.JSONObject

// Servicio WatchReportListener: componente en background para tareas de larga duración
class WatchReportListener : WearableListenerService() {

    companion object {
        // Constante CHANNEL_ID: valor fijo definido en tiempo de compilación
        private const val CHANNEL_ID = "wear_alert_channel"
    }

    // Callback que se ejecuta cuando llega un mensaje desde el dispositivo Wear
    override fun onMessageReceived(event: MessageEvent) {
        // Registro de evento en el log de Android para depuración
        android.util.Log.d("WATCH_LISTENER", "📩 Mensaje recibido en path: ${event.path}")

        // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
        when (event.path) {
            // ✅ Nuevo path para recibir alertas del Wear
            "/alerta/mascota" -> {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // Constante json: valor inmutable que no cambia tras su asignación
                    val json = JSONObject(String(event.data))
                    // Constante tipo: valor inmutable que no cambia tras su asignación
                    val tipo = json.optString("tipo", "")

                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (tipo == "ALERTA_MASCOTA") {
                        // Constante mascotaId: valor inmutable que no cambia tras su asignación
                        val mascotaId = json.optString("mascotaId", "")
                        // Constante mascotaNombre: valor inmutable que no cambia tras su asignación
                        val mascotaNombre = json.optString("mascotaNombre", "")
                        // Constante distancia: valor inmutable que no cambia tras su asignación
                        val distancia = json.optInt("distancia", 0)
                        // Constante timestamp: valor inmutable que no cambia tras su asignación
                        val timestamp = json.optLong("timestamp", System.currentTimeMillis())

                        // Registro de evento en el log de Android para depuración
                        android.util.Log.d("WATCH_LISTENER", "📱 Alerta recibida: $mascotaNombre - $distancia m")

                        // Mostrar notificación en el móvil
                        mostrarNotificacionMovil(mascotaNombre, distancia)

                        // Guardar en base de datos local (opcional)
                        guardarAlerta(mascotaId, mascotaNombre, distancia, timestamp)

                        // Enviar broadcast para actualizar UI
                        LocalBroadcastManager.getInstance(applicationContext)
                            .sendBroadcast(Intent("com.lomito.seguro.ALERTA_RECIBIDA").apply {
                                putExtra("mascotaId", mascotaId)
                                putExtra("mascotaNombre", mascotaNombre)
                                putExtra("distancia", distancia)
                            })
                    }
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("WATCH_LISTENER", "❌ Error procesando alerta: ${e.message}")
                }
            }

            "/watch/reporte" -> {
                // Constante json: valor inmutable que no cambia tras su asignación
                val json = runCatching { JSONObject(String(event.data)) }.getOrNull() ?: return
                // Constante mascotaId: valor inmutable que no cambia tras su asignación
                val mascotaId = json.optString("mascotaId", "")
                // Constante lat: valor inmutable que no cambia tras su asignación
                val lat = json.optDouble("latitud", 20.9167)
                // Constante lng: valor inmutable que no cambia tras su asignación
                val lng = json.optDouble("longitud", -101.1500)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (mascotaId.isEmpty()) return
                // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching {
                        // Accede al cliente Retrofit singleton para realizar peticiones de red
                        RetrofitClient.api.reportarVista(
                            ReporteRequest(mascotaId, lat, lng, "Reportado desde Watch")
                        )
                    }
                }
            }

            "/mascota/perdida/nueva" -> {
                LocalBroadcastManager.getInstance(applicationContext)
                    .sendBroadcast(Intent("com.lomito.seguro.MASCOTA_PERDIDA_NUEVA"))
            }
        }
    }

    /**
     * Mostrar notificación en el móvil
     */
    private fun mostrarNotificacionMovil(mascotaNombre: String, distancia: Int) {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Constante notificationManager: valor inmutable que no cambia tras su asignación
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Crear canal de notificación para Android 8+
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Constante channel: valor inmutable que no cambia tras su asignación
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Alertas del Wear",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones de alerta enviadas desde el reloj"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                }
                // Crea el canal de notificación requerido en Android 8.0+
                notificationManager.createNotificationChannel(channel)
            }

            // Crear intent para abrir la app
            // Constante intent: valor inmutable que no cambia tras su asignación
            val intent = Intent(this, com.lomito.seguro.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_alerta", true)
                putExtra("mascota_nombre", mascotaNombre)
                putExtra("distancia", distancia)
            }

            // Constante pendingIntent: valor inmutable que no cambia tras su asignación
            val pendingIntent = android.app.PendingIntent.getActivity(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            // Constante notification: valor inmutable que no cambia tras su asignación
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🚨 ¡Alerta de Mascota!")
                .setContentText("$mascotaNombre se ha alejado a $distancia metros")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .build()

            // Muestra la notificación al usuario en la barra de estado
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)

            // Registro de evento en el log de Android para depuración
            android.util.Log.d("WATCH_LISTENER", "✅ Notificación mostrada en móvil")
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("WATCH_LISTENER", "Error mostrando notificación: ${e.message}")
        }
    }

    /**
     * Guardar alerta en base de datos local
     */
    private fun guardarAlerta(mascotaId: String, mascotaNombre: String, distancia: Int, timestamp: Long) {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Constante prefs: valor inmutable que no cambia tras su asignación
            val prefs = getSharedPreferences("alertas_wear", MODE_PRIVATE)
            // Constante count: valor inmutable que no cambia tras su asignación
            val count = prefs.getInt("alert_count", 0)

            // Inicia el editor para modificar los SharedPreferences
            prefs.edit().apply {
                putString("alert_${count}_id", mascotaId)
                putString("alert_${count}_nombre", mascotaNombre)
                putInt("alert_${count}_distancia", distancia)
                putLong("alert_${count}_timestamp", timestamp)
                putInt("alert_count", count + 1)
                apply()
            }

            // Registro de evento en el log de Android para depuración
            android.util.Log.d("WATCH_LISTENER", "💾 Alerta guardada en SharedPreferences")
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("WATCH_LISTENER", "Error guardando alerta: ${e.message}")
        }
    }
}