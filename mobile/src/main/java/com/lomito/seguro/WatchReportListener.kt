package com.lomito.seguro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.lomito.seguro.data.api.RetrofitClient
import com.lomito.seguro.data.model.ReporteRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class WatchReportListener : WearableListenerService() {

    companion object {
        private const val CHANNEL_ID = "wear_alert_channel"
    }

    override fun onMessageReceived(event: MessageEvent) {
        android.util.Log.d("WATCH_LISTENER", "📩 Mensaje recibido en path: ${event.path}")

        when (event.path) {
            // ✅ Nuevo path para recibir alertas del Wear
            "/alerta/mascota" -> {
                try {
                    val json = JSONObject(String(event.data))
                    val tipo = json.optString("tipo", "")

                    if (tipo == "ALERTA_MASCOTA") {
                        val mascotaId = json.optString("mascotaId", "")
                        val mascotaNombre = json.optString("mascotaNombre", "")
                        val distancia = json.optInt("distancia", 0)
                        val timestamp = json.optLong("timestamp", System.currentTimeMillis())

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
                    android.util.Log.e("WATCH_LISTENER", "❌ Error procesando alerta: ${e.message}")
                }
            }

            "/watch/reporte" -> {
                val json = runCatching { JSONObject(String(event.data)) }.getOrNull() ?: return
                val mascotaId = json.optString("mascotaId", "")
                val lat = json.optDouble("latitud", 20.9167)
                val lng = json.optDouble("longitud", -101.1500)
                if (mascotaId.isEmpty()) return
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching {
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
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Crear canal de notificación para Android 8+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Alertas del Wear",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones de alerta enviadas desde el reloj"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Crear intent para abrir la app
            val intent = Intent(this, com.lomito.seguro.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_alerta", true)
                putExtra("mascota_nombre", mascotaNombre)
                putExtra("distancia", distancia)
            }

            val pendingIntent = android.app.PendingIntent.getActivity(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🚨 ¡Alerta de Mascota!")
                .setContentText("$mascotaNombre se ha alejado a $distancia metros")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)

            android.util.Log.d("WATCH_LISTENER", "✅ Notificación mostrada en móvil")
        } catch (e: Exception) {
            android.util.Log.e("WATCH_LISTENER", "Error mostrando notificación: ${e.message}")
        }
    }

    /**
     * Guardar alerta en base de datos local
     */
    private fun guardarAlerta(mascotaId: String, mascotaNombre: String, distancia: Int, timestamp: Long) {
        try {
            val prefs = getSharedPreferences("alertas_wear", MODE_PRIVATE)
            val count = prefs.getInt("alert_count", 0)

            prefs.edit().apply {
                putString("alert_${count}_id", mascotaId)
                putString("alert_${count}_nombre", mascotaNombre)
                putInt("alert_${count}_distancia", distancia)
                putLong("alert_${count}_timestamp", timestamp)
                putInt("alert_count", count + 1)
                apply()
            }

            android.util.Log.d("WATCH_LISTENER", "💾 Alerta guardada en SharedPreferences")
        } catch (e: Exception) {
            android.util.Log.e("WATCH_LISTENER", "Error guardando alerta: ${e.message}")
        }
    }
}