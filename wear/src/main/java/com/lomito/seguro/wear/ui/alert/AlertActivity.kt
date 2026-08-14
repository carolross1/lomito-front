// Paquete: com.lomito.seguro.wear.ui.alert
package com.lomito.seguro.wear.ui.alert

// Importa las clases para manejo de notificaciones
import android.app.NotificationChannel
// Importa las clases para manejo de notificaciones
import android.app.NotificationManager
// Importa la clase Intent para navegación entre componentes
import android.app.PendingIntent
// Importa el contexto de Android
import android.content.Context
// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa la dependencia necesaria: Build
import android.os.Build
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: WindowManager
import android.view.WindowManager
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa componente de Jetpack Compose
import androidx.compose.animation.core.*
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
import androidx.compose.foundation.border
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.*
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.CircleShape
// Importa componente de Jetpack Compose
import androidx.compose.runtime.*
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
import androidx.compose.ui.draw.clip
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
// Importa las clases para manejo de notificaciones
import androidx.core.app.NotificationCompat
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa la dependencia necesaria: Tasks
import com.google.android.gms.tasks.Tasks
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.Node
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.Wearable
// Importa la dependencia necesaria: PollingService
import com.lomito.seguro.wear.data.PollingService
// Importa la dependencia necesaria: DashboardActivity
import com.lomito.seguro.wear.ui.dashboard.DashboardActivity
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.*

/**
 * [Actividad principal para mostrar una alerta visual y táctil]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mostrar distancia actual de la mascota y notificar al móvil]
 * - [Responder al aumento crítico de la distancia con vibración y sonidos]
 */
// Activity AlertActivity: pantalla principal que gestiona el ciclo de vida
class AlertActivity : ComponentActivity() {

    companion object {
        // Constante ALERT_CHANNEL_ID: valor fijo definido en tiempo de compilación
        const val ALERT_CHANNEL_ID = "lomito_alert_channel"
        // Constante ALERT_NOTIF_ID: valor fijo definido en tiempo de compilación
        const val ALERT_NOTIF_ID = 3001
    }

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
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
        // Constante mascotaNombre: valor inmutable que no cambia tras su asignación
        val mascotaNombre = intent.getStringExtra("mascota_nombre")
            ?: PollingService.mascotaNombreActual
            ?: "Tu mascota"

        // Constante distanciaInicial: valor inmutable que no cambia tras su asignación
        val distanciaInicial = intent.getIntExtra("distancia", PollingService.distanciaActual)
        // Constante incremento: valor inmutable que no cambia tras su asignación
        val incremento = intent.getIntExtra("incremento", 0)

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            // Variable distancia: almacena el estado mutable de este componente
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
                    // Constante intent: valor inmutable que no cambia tras su asignación
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
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Constante json: valor inmutable que no cambia tras su asignación
            val json = org.json.JSONObject().apply {
                put("tipo", "ALERTA_MASCOTA")
                put("mascotaId", mascotaId)
                put("mascotaNombre", mascotaNombre)
                put("distancia", distancia)
                put("timestamp", System.currentTimeMillis())
            }

            // Constante payload: valor inmutable que no cambia tras su asignación
            val payload = json.toString().toByteArray()

            // Enviar a todos los nodos conectados
            // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
            CoroutineScope(Dispatchers.IO).launch {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // ✅ Obtener nodos conectados correctamente
                    // Constante nodeClient: valor inmutable que no cambia tras su asignación
                    val nodeClient = Wearable.getNodeClient(applicationContext)
                    // Constante connectedNodes: valor inmutable que no cambia tras su asignación
                    val connectedNodes = Tasks.await(nodeClient.connectedNodes)

                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (connectedNodes.isNotEmpty()) {
                        // Itera sobre cada elemento de la colección y ejecuta el bloque
                        connectedNodes.forEach { node ->
                            // Constante messageClient: valor inmutable que no cambia tras su asignación
                            val messageClient = Wearable.getMessageClient(applicationContext)
                            Tasks.await(
                                // Envía un mensaje al dispositivo Wear OS conectado
                                messageClient.sendMessage(node.id, "/alerta/mascota", payload)
                            )
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.d("ALERT_ACTIVITY", "✅ Alerta enviada al móvil: ${node.displayName}")
                        }
                    } else {
                        // Registro de evento en el log de Android para depuración
                        android.util.Log.e("ALERT_ACTIVITY", "⚠️ No hay nodos conectados")
                        // Guardar alerta pendiente
                        guardarAlertaPendiente(mascotaId, mascotaNombre, distancia)
                    }
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("ALERT_ACTIVITY", "❌ Error enviando alerta: ${e.message}")
                    // Si hay error, guardar pendiente
                    guardarAlertaPendiente(mascotaId, mascotaNombre, distancia)
                }
            }

            // Registro de evento en el log de Android para depuración
            android.util.Log.d("ALERT_ACTIVITY", "📤 Enviando alerta: $mascotaNombre - $distancia m")
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("ALERT_ACTIVITY", "❌ Error preparando alerta: ${e.message}")
        }
    }

    /**
     * Guardar alerta pendiente para enviar cuando se conecte
     */
    private fun guardarAlertaPendiente(mascotaId: String, mascotaNombre: String, distancia: Int) {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Constante prefs: valor inmutable que no cambia tras su asignación
            val prefs = getSharedPreferences("alert_pending", MODE_PRIVATE)
            // Inicia el editor para modificar los SharedPreferences
            prefs.edit().apply {
                putString("pending_mascota_id", mascotaId)
                putString("pending_mascota_nombre", mascotaNombre)
                putInt("pending_distancia", distancia)
                putLong("pending_timestamp", System.currentTimeMillis())
                apply()
            }
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("ALERT_ACTIVITY", "💾 Alerta guardada como pendiente")
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("ALERT_ACTIVITY", "Error guardando alerta: ${e.message}")
        }
    }

    /**
     * Crear canal de notificación para la alerta
     */
    private fun crearCanalNotificacionAlerta() {
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Constante channel: valor inmutable que no cambia tras su asignación
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Alertas de Mascota",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de alerta cuando una mascota se aleja"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            // Constante manager: valor inmutable que no cambia tras su asignación
            val manager = getSystemService(NotificationManager::class.java)
            // Crea el canal de notificación requerido en Android 8.0+
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Enviar notificación local en el Wear
     */
    private fun enviarNotificacionLocal(mascotaNombre: String, distancia: Int, incremento: Int) {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Constante intent: valor inmutable que no cambia tras su asignación
            val intent = Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            // Constante pendingIntent: valor inmutable que no cambia tras su asignación
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Constante textoNotificacion: valor inmutable que no cambia tras su asignación
            val textoNotificacion = if (incremento > 0) {
                "$mascotaNombre se ha alejado a $distancia metros (+$incremento m)"
            } else {
                "$mascotaNombre se ha alejado a $distancia metros"
            }

            // Constante notification: valor inmutable que no cambia tras su asignación
            val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setContentTitle("🚨 ¡Alerta de Mascota!")
                .setContentText(textoNotificacion)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 300, 200, 300))
                .build()

            // Constante manager: valor inmutable que no cambia tras su asignación
            val manager = getSystemService(NotificationManager::class.java)
            // Muestra la notificación al usuario en la barra de estado
            manager.notify(ALERT_NOTIF_ID, notification)

            // Registro de evento en el log de Android para depuración
            android.util.Log.d("ALERT_ACTIVITY", "✅ Notificación local enviada")
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("ALERT_ACTIVITY", "Error enviando notificación local: ${e.message}")
        }
    }
}

/**
 * [Pantalla de UI que muestra el estado de alerta]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [distancia]: Distancia de la mascota
 * - [mascotaNombre]: Nombre de la mascota
 * - [incremento]: Incremento en distancia desde la última alerta
 * - [onAceptar]: Callback al confirmar la alerta
 * - [onIgnorar]: Callback al ignorar la alerta
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función AlertScreen: define la lógica de esta operación
fun AlertScreen(
    distancia: Int,
    mascotaNombre: String,
    incremento: Int,
    onAceptar: () -> Unit,
    onIgnorar: () -> Unit
) {
    // Constante borderAlpha: valor inmutable que no cambia tras su asignación
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

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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