package com.lomito.seguro.wear.ui.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.wear.compose.material.*
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.lomito.seguro.wear.data.PollingService
import com.lomito.seguro.wear.ui.dashboard.DashboardActivity
import kotlinx.coroutines.*

class AlertActivity : ComponentActivity() {

    companion object {
        const val ALERT_CHANNEL_ID = "lomito_alert_channel"
        const val ALERT_NOTIF_ID = 3001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar para que se muestre sobre otras actividades
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        // Crear canal de notificación para la alerta
        crearCanalNotificacionAlerta()

        // Obtener datos del intent
        val mascotaNombre = intent.getStringExtra("mascota_nombre")
            ?: PollingService.mascotaNombreActual
            ?: "Tu mascota"

        val distanciaInicial = intent.getIntExtra("distancia", PollingService.distanciaActual)
        val incremento = intent.getIntExtra("incremento", 0)

        setContent {
            var distancia by remember { mutableStateOf(distanciaInicial) }

            // Actualizar distancia en tiempo real
            LaunchedEffect(Unit) {
                while (true) {
                    distancia = PollingService.distanciaActual
                    kotlinx.coroutines.delay(1000)
                }
            }

            AlertScreen(
                distancia = distancia,
                mascotaNombre = mascotaNombre,
                incremento = incremento,
                onAceptar = {
                    // ✅ Enviar alerta al móvil
                    enviarAlertaAlMovil(PollingService.mascotaIdActual, mascotaNombre, distancia)

                    // Enviar notificación local en el Wear
                    enviarNotificacionLocal(mascotaNombre, distancia, incremento)

                    // Cerrar la alerta y resetear estado
                    PollingService.alertaMostrada = false
                    PollingService.ultimaDistanciaAlerta = distancia

                    // Ir al Dashboard
                    val intent = Intent(this, DashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(intent)
                    finish()
                },
                onIgnorar = {
                    // Solo cerrar la alerta y resetear estado
                    PollingService.alertaMostrada = false
                    PollingService.ultimaDistanciaAlerta = distancia
                    finish()
                }
            )
        }
    }

    /**
     * Enviar alerta al móvil
     */
    private fun enviarAlertaAlMovil(mascotaId: String, mascotaNombre: String, distancia: Int) {
        try {
            val json = org.json.JSONObject().apply {
                put("tipo", "ALERTA_MASCOTA")
                put("mascotaId", mascotaId)
                put("mascotaNombre", mascotaNombre)
                put("distancia", distancia)
                put("timestamp", System.currentTimeMillis())
            }

            val payload = json.toString().toByteArray()

            // Enviar a todos los nodos conectados
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // ✅ Obtener nodos conectados correctamente
                    val nodeClient = Wearable.getNodeClient(applicationContext)
                    val connectedNodes = Tasks.await(nodeClient.connectedNodes)

                    if (connectedNodes.isNotEmpty()) {
                        connectedNodes.forEach { node ->
                            val messageClient = Wearable.getMessageClient(applicationContext)
                            Tasks.await(
                                messageClient.sendMessage(node.id, "/alerta/mascota", payload)
                            )
                            android.util.Log.d("ALERT_ACTIVITY", "✅ Alerta enviada al móvil: ${node.displayName}")
                        }
                    } else {
                        android.util.Log.e("ALERT_ACTIVITY", "⚠️ No hay nodos conectados")
                        // Guardar alerta pendiente
                        guardarAlertaPendiente(mascotaId, mascotaNombre, distancia)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ALERT_ACTIVITY", "❌ Error enviando alerta: ${e.message}")
                    // Si hay error, guardar pendiente
                    guardarAlertaPendiente(mascotaId, mascotaNombre, distancia)
                }
            }

            android.util.Log.d("ALERT_ACTIVITY", "📤 Enviando alerta: $mascotaNombre - $distancia m")
        } catch (e: Exception) {
            android.util.Log.e("ALERT_ACTIVITY", "❌ Error preparando alerta: ${e.message}")
        }
    }

    /**
     * Guardar alerta pendiente para enviar cuando se conecte
     */
    private fun guardarAlertaPendiente(mascotaId: String, mascotaNombre: String, distancia: Int) {
        try {
            val prefs = getSharedPreferences("alert_pending", MODE_PRIVATE)
            prefs.edit().apply {
                putString("pending_mascota_id", mascotaId)
                putString("pending_mascota_nombre", mascotaNombre)
                putInt("pending_distancia", distancia)
                putLong("pending_timestamp", System.currentTimeMillis())
                apply()
            }
            android.util.Log.d("ALERT_ACTIVITY", "💾 Alerta guardada como pendiente")
        } catch (e: Exception) {
            android.util.Log.e("ALERT_ACTIVITY", "Error guardando alerta: ${e.message}")
        }
    }

    /**
     * Crear canal de notificación para la alerta
     */
    private fun crearCanalNotificacionAlerta() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Alertas de Mascota",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de alerta cuando una mascota se aleja"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Enviar notificación local en el Wear
     */
    private fun enviarNotificacionLocal(mascotaNombre: String, distancia: Int, incremento: Int) {
        try {
            val intent = Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val textoNotificacion = if (incremento > 0) {
                "$mascotaNombre se ha alejado a $distancia metros (+$incremento m)"
            } else {
                "$mascotaNombre se ha alejado a $distancia metros"
            }

            val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setContentTitle("🚨 ¡Alerta de Mascota!")
                .setContentText(textoNotificacion)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 300, 200, 300))
                .build()

            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(ALERT_NOTIF_ID, notification)

            android.util.Log.d("ALERT_ACTIVITY", "✅ Notificación local enviada")
        } catch (e: Exception) {
            android.util.Log.e("ALERT_ACTIVITY", "Error enviando notificación local: ${e.message}")
        }
    }
}

@Composable
fun AlertScreen(
    distancia: Int,
    mascotaNombre: String,
    incremento: Int,
    onAceptar: () -> Unit,
    onIgnorar: () -> Unit
) {
    val borderAlpha by rememberInfiniteTransition(label = "border").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFF1A0000))
                .border(4.dp, Color(0xFFF44336).copy(alpha = borderAlpha), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Text("⚠️", fontSize = 28.sp)

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "¡ALERTA!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "$mascotaNombre se alejó",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "${distancia}m",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (incremento > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "⬆ +$incremento m",
                        fontSize = 12.sp,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onAceptar,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📱", fontSize = 14.sp)
                            Text("Notificar", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    Button(
                        onClick = onIgnorar,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF666666)),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✕", fontSize = 14.sp)
                            Text("Ignorar", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

            }
        }
    }
}