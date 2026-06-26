package com.lomito.seguro.wear.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.lomito.seguro.wear.ui.home.WearMainActivity
import org.json.JSONObject

class WearMessageService : WearableListenerService() {

    companion object {
        const val CHANNEL_ID = "lomito_alertas"
        // Estado compartido estático para actualizar la UI
        var distancia: Int = 0
        var mascotaId: String = ""
        var umbral: Int = 50
        var superaUmbral: Boolean = false
        var onUpdate: ((Int, String, Int, Boolean) -> Unit)? = null
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != "/ble/distancia") return

        val json = runCatching { JSONObject(String(event.data)) }.getOrNull() ?: return

        distancia = json.optInt("distancia", 0)
        mascotaId = json.optString("mascotaId", "")
        umbral = json.optInt("umbral", 50)
        superaUmbral = json.optBoolean("superaUmbral", false)

        // Actualizar UI via callback si la Activity está activa
        onUpdate?.invoke(distancia, mascotaId, umbral, superaUmbral)

        // También mandar broadcast local
        sendBroadcast(Intent("com.lomito.seguro.wear.BLE_UPDATE").apply {
            putExtra("distancia", distancia)
            putExtra("mascotaId", mascotaId)
            putExtra("umbral", umbral)
            putExtra("superaUmbral", superaUmbral)
            setPackage(packageName)
        })

        if (superaUmbral) {
            vibrar()
            mostrarNotificacion(distancia)
        }
    }

    private fun vibrar() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300, 100, 600), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 300, 100, 300, 100, 600), -1)
        }
    }

    private fun mostrarNotificacion(distancia: Int) {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Alertas Lomito", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        mgr.notify(1001, NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 ¡Lomito fuera de rango!")
            .setContentText("Distancia: ${distancia}m")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build())
    }

override fun onDataChanged(dataEvents: com.google.android.gms.wearable.DataEventBuffer) {
    dataEvents.forEach { event ->
        if (event.type == com.google.android.gms.wearable.DataEvent.TYPE_CHANGED &&
            event.dataItem.uri.path == "/ble/distancia") {

            val dataMap = com.google.android.gms.wearable.DataMapItem
                .fromDataItem(event.dataItem).dataMap

            distancia = dataMap.getInt("distancia", 0)
            mascotaId = dataMap.getString("mascotaId") ?: ""
            umbral = dataMap.getInt("umbral", 50)
            superaUmbral = dataMap.getBoolean("superaUmbral", false)

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
                mostrarNotificacion(distancia)
            }
        }
    }
}}