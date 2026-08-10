package com.lomito.seguro.wear.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.lomito.seguro.wear.ui.alert.AlertActivity
import org.json.JSONObject

class WearMessageService : WearableListenerService() {

    companion object {
        const val CHANNEL_ID = "lomito_alertas"

        var distancia: Int = 0
        var mascotaId: String = ""
        var umbral: Int = 50
        var superaUmbral: Boolean = false

        var onUpdate: ((Int, String, Int, Boolean) -> Unit)? = null
    }

    override fun onMessageReceived(event: MessageEvent) {
        android.util.Log.d("WEAR_MSG", "📩 Mensaje recibido en path: ${event.path}")

        when (event.path) {
            "/ble/distancia" -> {
                try {
                    val json = JSONObject(String(event.data))

                    distancia = json.optInt("distancia", 0)
                    mascotaId = json.optString("mascotaId", "")
                    umbral = json.optInt("umbral", 50)
                    superaUmbral = json.optBoolean("superaUmbral", false)

                    android.util.Log.d("WEAR_MSG", "📊 Distancia: $distancia, Umbral: $umbral, Supera: $superaUmbral")

                    onUpdate?.invoke(distancia, mascotaId, umbral, superaUmbral)

                    sendBroadcast(Intent("com.lomito.seguro.wear.BLE_UPDATE").apply {
                        putExtra("distancia", distancia)
                        putExtra("mascotaId", mascotaId)
                        putExtra("umbral", umbral)
                        putExtra("superaUmbral", superaUmbral)
                        setPackage(packageName)
                    })

                    if (superaUmbral) {
                        vibrar()
                        mostrarNotificacion(distancia, mascotaId)
                        // ✅ Abrir AlertActivity automáticamente
                        startActivity(Intent(applicationContext, AlertActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        })
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WEAR_MSG", "❌ Error procesando mensaje BLE: ${e.message}")
                }
            }

            "/watch/user_id" -> {
                try {
                    val json = JSONObject(String(event.data))
                    val userId = json.getString("userId")
                    val prefs = applicationContext.getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("user_id", userId).apply()
                    android.util.Log.d("USER_ID", "✅ userId $userId guardado")
                    sendBroadcast(Intent("com.lomito.seguro.wear.USER_ID_UPDATED").apply {
                        putExtra("user_id", userId)
                        setPackage(packageName)
                    })
                } catch (e: Exception) {
                    android.util.Log.e("USER_ID", "❌ Error: ${e.message}")
                }
            }

            "/watch/reporte" -> {
                try {
                    val json = JSONObject(String(event.data))
                    val mascotaId = json.getString("mascotaId")
                    val latitud = json.getDouble("latitud")
                    val longitud = json.getDouble("longitud")
                    val direccion = json.optString("direccion", "")

                    val payload = JSONObject().apply {
                        put("tipo", "AVISTAMIENTO_REPORTADO")
                        put("mascotaId", mascotaId)
                        put("latitud", latitud)
                        put("longitud", longitud)
                        put("direccion", direccion)
                    }.toString().toByteArray()

                    com.google.android.gms.wearable.Wearable.getNodeClient(applicationContext).connectedNodes
                        .addOnSuccessListener { nodeList ->
                            nodeList.forEach { node ->
                                com.google.android.gms.wearable.Wearable.getMessageClient(applicationContext)
                                    .sendMessage(node.id, "/watch/avistamiento", payload)
                            }
                        }
                } catch (e: Exception) {
                    android.util.Log.e("WEAR_MSG", "❌ Error procesando reporte: ${e.message}")
                }
            }

            "/mascota/perdida/nueva" -> {
                try {
                    val json = JSONObject(String(event.data))
                    val nombre = json.getString("nombre")
                    android.util.Log.d("WEAR_MSG", "🐾 Nueva mascota perdida: $nombre")

                    val payload = JSONObject().apply {
                        put("tipo", "NUEVA_MASCOTA_PERDIDA")
                        put("nombre", nombre)
                    }.toString().toByteArray()

                    com.google.android.gms.wearable.Wearable.getNodeClient(applicationContext).connectedNodes
                        .addOnSuccessListener { nodeList ->
                            nodeList.forEach { node ->
                                com.google.android.gms.wearable.Wearable.getMessageClient(applicationContext)
                                    .sendMessage(node.id, "/mascota/perdida/nueva", payload)
                            }
                        }
                } catch (e: Exception) {
                    android.util.Log.e("WEAR_MSG", "❌ Error: ${e.message}")
                }
            }

            "/watch/mascotas" -> {
                try {
                    val json = JSONObject(String(event.data))
                    val mascotas = json.getJSONArray("mascotas")
                    val prefs = applicationContext.getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("mascotas_data", mascotas.toString()).apply()
                } catch (e: Exception) {
                    android.util.Log.e("WEAR_MSG", "❌ Error procesando lista de mascotas: ${e.message}")
                }
            }

            else -> android.util.Log.d("WEAR_MSG", "⚠️ Path desconocido: ${event.path}")
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        android.util.Log.d("WEAR_MSG", "📊 DataChanged recibido")

        dataEvents.forEach { event ->
            try {
                if (event.type == DataEvent.TYPE_CHANGED &&
                    event.dataItem.uri.path == "/ble/distancia") {

                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                    distancia = dataMap.getInt("distancia", 0)
                    mascotaId = dataMap.getString("mascotaId") ?: ""
                    umbral = dataMap.getInt("umbral", 50)
                    superaUmbral = dataMap.getBoolean("superaUmbral", false)

                    android.util.Log.d("WEAR_MSG", "📊 DataClient: Distancia=$distancia, Supera=$superaUmbral")

                    onUpdate?.invoke(distancia, mascotaId, umbral, superaUmbral)

                    sendBroadcast(Intent("com.lomito.seguro.wear.BLE_UPDATE").apply {
                        putExtra("distancia", distancia)
                        putExtra("mascotaId", mascotaId)
                        putExtra("umbral", umbral)
                        putExtra("superaUmbral", superaUmbral)
                        setPackage(packageName)
                    })

                    if (superaUmbral) {
                        vibrar()
                        mostrarNotificacion(distancia, mascotaId)
                        // ✅ Abrir AlertActivity automáticamente
                        startActivity(Intent(applicationContext, AlertActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        })
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WEAR_MSG", "❌ Error procesando DataChanged: ${e.message}")
            }
        }
        dataEvents.close()
    }

    private fun vibrar() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300, 100, 600), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 300, 100, 300, 100, 600), -1)
            }
        } catch (e: Exception) {
            android.util.Log.e("WEAR_MSG", "❌ Error vibrando: ${e.message}")
        }
    }

    private fun mostrarNotificacion(distancia: Int, mascotaId: String) {
        try {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mgr.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Alertas Lomito", NotificationManager.IMPORTANCE_HIGH)
                )
            }
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("🚨 ¡Lomito fuera de rango!")
                .setContentText("Distancia: ${distancia}m")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            mgr.notify(1001, notification)
        } catch (e: Exception) {
            android.util.Log.e("WEAR_MSG", "❌ Error notificación: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("WEAR_MSG", "🛑 WearMessageService destruido")
    }
}