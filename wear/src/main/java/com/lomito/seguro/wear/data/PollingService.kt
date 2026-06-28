package com.lomito.seguro.wear.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.lomito.seguro.wear.ui.alert.AlertActivity
import com.lomito.seguro.wear.ui.dashboard.DashboardActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class PollingService : Service() {

    private val backendUrl = "http://192.168.100.12:3000"
    private var pollingJob: Job? = null

    companion object {
        const val CHANNEL_ID = "lomito_polling"
        const val NOTIF_ID = 2001
        var distanciaActual: Int = 0
        var umbralActual: Int = 50
        var mascotaIdActual: String = ""
        var mascotaNombreActual: String = ""
        var alertaMostrada: Boolean = false
        var ultimaDistanciaAlerta: Int = 0
        private const val INCREMENTO_MINIMO = 20
    }

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificacion()
        startForeground(NOTIF_ID, crearNotificacion("🐾 Lomito Seguro activo"))
        iniciarPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun iniciarPolling() {
        pollingJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (isActive) {
                try {
                    val prefs = applicationContext.getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)
                    mascotaNombreActual = prefs.getString("mascota_activa_nombre", "Tu mascota") ?: "Tu mascota"

                    val url = URL("$backendUrl/api/simulador/estado")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 2000
                    conn.readTimeout = 2000
                    conn.requestMethod = "GET"

                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        val response = conn.inputStream.bufferedReader().readText()
                        val json = JSONObject(response)
                        val distancia = json.optInt("distancia", 0)
                        val umbral = json.optInt("umbral", 50)
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

                        android.util.Log.d("POLLING_SVC", "📡 distancia=$distancia umbral=$umbral mascota=$mascotaNombreActual")

                        withContext(Dispatchers.Main) {
                            // ✅ Evaluar condiciones
                            val incremento = distancia - ultimaDistanciaAlerta
                            val superaUmbral = distancia > umbral
                            val distanciaValida = distancia > 0
                            val tieneMascota = mascotaId.isNotEmpty()
                            val alertaNoActiva = !alertaMostrada
                            val esPrimeraAlerta = ultimaDistanciaAlerta == 0
                            val aumentoSignificativo = incremento >= INCREMENTO_MINIMO

                            // ✅ Determinar si debe mostrar alerta (sin usar if como expresión)
                            var debeMostrar = false
                            if (superaUmbral && distanciaValida && tieneMascota && alertaNoActiva) {
                                if (esPrimeraAlerta || aumentoSignificativo) {
                                    debeMostrar = true
                                }
                            }

                            if (debeMostrar) {
                                // ✅ Mostrar alerta
                                alertaMostrada = true
                                ultimaDistanciaAlerta = distancia
                                android.util.Log.d("POLLING_SVC", "🚨 Abriendo AlertActivity (incremento: $incremento m)")
                                vibrar()

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
                                if (distancia <= umbral || distancia == 0) {
                                    alertaMostrada = false
                                }
                            }

                            // ✅ Logs para debugging
                            if (distancia > umbral && alertaMostrada) {
                                android.util.Log.d("POLLING_SVC", "⏳ Alerta ya mostrada. Distancia actual: $distancia")
                            }
                        }
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    android.util.Log.e("POLLING_SVC", "Error: ${e.message}")
                }
                delay(2000)
            }
        }
    }

    private fun vibrar() {
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300, 100, 600), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 300, 100, 300, 100, 600), -1)
            }
        } catch (e: Exception) {
            android.util.Log.e("POLLING_SVC", "Error vibrando: ${e.message}")
        }
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Lomito Seguro Polling",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private fun crearNotificacion(texto: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Lomito Seguro")
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
        alertaMostrada = false
    }
}