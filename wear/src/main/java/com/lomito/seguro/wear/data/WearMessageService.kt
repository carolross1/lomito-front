// Paquete: com.lomito.seguro.wear.data
package com.lomito.seguro.wear.data

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
// Importa la dependencia necesaria: VibrationEffect
import android.os.VibrationEffect
// Importa la dependencia necesaria: Vibrator
import android.os.Vibrator
// Importa las clases para manejo de notificaciones
import androidx.core.app.NotificationCompat
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.DataEvent
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.DataEventBuffer
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.DataMapItem
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.MessageEvent
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.WearableListenerService
// Importa la dependencia necesaria: AlertActivity
import com.lomito.seguro.wear.ui.alert.AlertActivity
// Importa el parser JSON
import org.json.JSONObject

/**
 * [Servicio de mensajería para comunicarse con la app móvil y backend]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Recibir eventos de conexión BLE y distancias]
 * - [Sincronizar datos de mascotas y estado entre el reloj y el dispositivo móvil]
 */
// Servicio WearMessageService: componente en background para tareas de larga duración
class WearMessageService : WearableListenerService() {

    companion object {
        // Constante CHANNEL_ID: valor fijo definido en tiempo de compilación
        const val CHANNEL_ID = "lomito_alertas"

        // Variable distancia: almacena el estado mutable de este componente
        var distancia: Int = 0
        // Variable mascotaId: almacena el estado mutable de este componente
        var mascotaId: String = ""
        // Variable umbral: almacena el estado mutable de este componente
        var umbral: Int = 50
        // Variable superaUmbral: almacena el estado mutable de este componente
        var superaUmbral: Boolean = false

        // Variable onUpdate: almacena el estado mutable de este componente
        var onUpdate: ((Int, String, Int, Boolean) -> Unit)? = null
    }

    // Callback que se ejecuta cuando llega un mensaje desde el dispositivo Wear
    override fun onMessageReceived(event: MessageEvent) {
        // Registro de evento en el log de Android para depuración
        android.util.Log.d("WEAR_MSG", "📩 Mensaje recibido en path: ${event.path}")

        // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
        when (event.path) {
            "/ble/distancia" -> {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // Constante json: valor inmutable que no cambia tras su asignación
                    val json = JSONObject(String(event.data))

                    distancia = json.optInt("distancia", 0)
                    mascotaId = json.optString("mascotaId", "")
                    umbral = json.optInt("umbral", 50)
                    superaUmbral = json.optBoolean("superaUmbral", false)

                    // Registro de evento en el log de Android para depuración
                    android.util.Log.d("WEAR_MSG", "📊 Distancia: $distancia, Umbral: $umbral, Supera: $superaUmbral")

                    onUpdate?.invoke(distancia, mascotaId, umbral, superaUmbral)

                    sendBroadcast(Intent("com.lomito.seguro.wear.BLE_UPDATE").apply {
                        putExtra("distancia", distancia)
                        putExtra("mascotaId", mascotaId)
                        putExtra("umbral", umbral)
                        putExtra("superaUmbral", superaUmbral)
                        setPackage(packageName)
                    })

                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (superaUmbral) {
                        vibrar()
                        mostrarNotificacion(distancia, mascotaId)
                        // ✅ Abrir AlertActivity automáticamente
                        startActivity(Intent(applicationContext, AlertActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        })
                    }
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("WEAR_MSG", "❌ Error procesando mensaje BLE: ${e.message}")
                }
            }

            "/watch/user_id" -> {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // Constante json: valor inmutable que no cambia tras su asignación
                    val json = JSONObject(String(event.data))
                    // Constante userId: valor inmutable que no cambia tras su asignación
                    val userId = json.getString("userId")
                    // Constante prefs: valor inmutable que no cambia tras su asignación
                    val prefs = applicationContext.getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)
                    // Inicia el editor para modificar los SharedPreferences
                    prefs.edit().putString("user_id", userId).apply()
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.d("USER_ID", "✅ userId $userId guardado")
                    sendBroadcast(Intent("com.lomito.seguro.wear.USER_ID_UPDATED").apply {
                        putExtra("user_id", userId)
                        setPackage(packageName)
                    })
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("USER_ID", "❌ Error: ${e.message}")
                }
            }

            "/watch/reporte" -> {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // Constante json: valor inmutable que no cambia tras su asignación
                    val json = JSONObject(String(event.data))
                    // Constante mascotaId: valor inmutable que no cambia tras su asignación
                    val mascotaId = json.getString("mascotaId")
                    // Constante latitud: valor inmutable que no cambia tras su asignación
                    val latitud = json.getDouble("latitud")
                    // Constante longitud: valor inmutable que no cambia tras su asignación
                    val longitud = json.getDouble("longitud")
                    // Constante direccion: valor inmutable que no cambia tras su asignación
                    val direccion = json.optString("direccion", "")

                    // Constante payload: valor inmutable que no cambia tras su asignación
                    val payload = JSONObject().apply {
                        put("tipo", "AVISTAMIENTO_REPORTADO")
                        put("mascotaId", mascotaId)
                        put("latitud", latitud)
                        put("longitud", longitud)
                        put("direccion", direccion)
                    }.toString().toByteArray()

                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    com.google.android.gms.wearable.Wearable.getNodeClient(applicationContext).connectedNodes
                        .addOnSuccessListener { nodeList ->
                            // Itera sobre cada elemento de la colección y ejecuta el bloque
                            nodeList.forEach { node ->
                                // Usa la API de Wearable para comunicación con dispositivos Wear OS
                                com.google.android.gms.wearable.Wearable.getMessageClient(applicationContext)
                                    // Envía un mensaje al dispositivo Wear OS conectado
                                    .sendMessage(node.id, "/watch/avistamiento", payload)
                            }
                        }
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("WEAR_MSG", "❌ Error procesando reporte: ${e.message}")
                }
            }

            // ✅ Alguien confirmó en la TV que vio a la mascota: vibra y
            // muestra notificación en el watch (llega reenviado por el móvil,
            // que es quien recibe la alerta del backend).
            "/watch/avistamiento_confirmado" -> {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // Constante json: valor inmutable que no cambia tras su asignación
                    val json = JSONObject(String(event.data))
                    // Constante nombre: valor inmutable que no cambia tras su asignación
                    val nombre = json.optString("mascotaNombre", "tu mascota")
                    // Constante mensaje: valor inmutable que no cambia tras su asignación
                    val mensaje = json.optString("mensaje", "Alguien confirmó un avistamiento")

                    vibrar()
                    mostrarNotificacionAvistamiento(nombre, mensaje)
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("WEAR_MSG", "❌ Error procesando avistamiento confirmado: ${e.message}")
                }
            }

            "/mascota/perdida/nueva" -> {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // Constante json: valor inmutable que no cambia tras su asignación
                    val json = JSONObject(String(event.data))
                    // Constante nombre: valor inmutable que no cambia tras su asignación
                    val nombre = json.getString("nombre")
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.d("WEAR_MSG", "🐾 Nueva mascota perdida: $nombre")

                    // Constante payload: valor inmutable que no cambia tras su asignación
                    val payload = JSONObject().apply {
                        put("tipo", "NUEVA_MASCOTA_PERDIDA")
                        put("nombre", nombre)
                    }.toString().toByteArray()

                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    com.google.android.gms.wearable.Wearable.getNodeClient(applicationContext).connectedNodes
                        .addOnSuccessListener { nodeList ->
                            // Itera sobre cada elemento de la colección y ejecuta el bloque
                            nodeList.forEach { node ->
                                // Usa la API de Wearable para comunicación con dispositivos Wear OS
                                com.google.android.gms.wearable.Wearable.getMessageClient(applicationContext)
                                    // Envía un mensaje al dispositivo Wear OS conectado
                                    .sendMessage(node.id, "/mascota/perdida/nueva", payload)
                            }
                        }
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("WEAR_MSG", "❌ Error: ${e.message}")
                }
            }

            "/watch/mascotas" -> {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // Constante json: valor inmutable que no cambia tras su asignación
                    val json = JSONObject(String(event.data))
                    // Constante mascotas: valor inmutable que no cambia tras su asignación
                    val mascotas = json.getJSONArray("mascotas")
                    // Constante prefs: valor inmutable que no cambia tras su asignación
                    val prefs = applicationContext.getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)
                    // Inicia el editor para modificar los SharedPreferences
                    prefs.edit().putString("mascotas_data", mascotas.toString()).apply()
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("WEAR_MSG", "❌ Error procesando lista de mascotas: ${e.message}")
                }
            }

            // Registro de evento en el log de Android para depuración
            else -> android.util.Log.d("WEAR_MSG", "⚠️ Path desconocido: ${event.path}")
        }
    }

    // Sobreescribe la función onDataChanged de la clase padre
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // Registro de evento en el log de Android para depuración
        android.util.Log.d("WEAR_MSG", "📊 DataChanged recibido")

        // Itera sobre cada elemento de la colección y ejecuta el bloque
        dataEvents.forEach { event ->
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (event.type == DataEvent.TYPE_CHANGED &&
                    event.dataItem.uri.path == "/ble/distancia") {

                    // Constante dataMap: valor inmutable que no cambia tras su asignación
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                    distancia = dataMap.getInt("distancia", 0)
                    mascotaId = dataMap.getString("mascotaId") ?: ""
                    umbral = dataMap.getInt("umbral", 50)
                    superaUmbral = dataMap.getBoolean("superaUmbral", false)

                    // Registro de evento en el log de Android para depuración
                    android.util.Log.d("WEAR_MSG", "📊 DataClient: Distancia=$distancia, Supera=$superaUmbral")

                    onUpdate?.invoke(distancia, mascotaId, umbral, superaUmbral)

                    sendBroadcast(Intent("com.lomito.seguro.wear.BLE_UPDATE").apply {
                        putExtra("distancia", distancia)
                        putExtra("mascotaId", mascotaId)
                        putExtra("umbral", umbral)
                        putExtra("superaUmbral", superaUmbral)
                        setPackage(packageName)
                    })

                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
                // Registro de evento en el log de Android para depuración
                android.util.Log.e("WEAR_MSG", "❌ Error procesando DataChanged: ${e.message}")
            }
        }
        dataEvents.close()
    }

    private fun vibrar() {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Constante vibrator: valor inmutable que no cambia tras su asignación
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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
            android.util.Log.e("WEAR_MSG", "❌ Error vibrando: ${e.message}")
        }
    }

    private fun mostrarNotificacionAvistamiento(nombre: String, mensaje: String) {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Constante mgr: valor inmutable que no cambia tras su asignación
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Crea el canal de notificación requerido en Android 8.0+
                mgr.createNotificationChannel(
                    // Crea el canal de notificación requerido en Android 8.0+
                    NotificationChannel(CHANNEL_ID, "Alertas Lomito", NotificationManager.IMPORTANCE_HIGH)
                )
            }
            // Constante notification: valor inmutable que no cambia tras su asignación
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("🐾 ¡Avistamiento de $nombre!")
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            // Muestra la notificación al usuario en la barra de estado
            mgr.notify(1002, notification)
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("WEAR_MSG", "❌ Error notificación de avistamiento: ${e.message}")
        }
    }

    private fun mostrarNotificacion(distancia: Int, mascotaId: String) {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Constante mgr: valor inmutable que no cambia tras su asignación
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Crea el canal de notificación requerido en Android 8.0+
                mgr.createNotificationChannel(
                    // Crea el canal de notificación requerido en Android 8.0+
                    NotificationChannel(CHANNEL_ID, "Alertas Lomito", NotificationManager.IMPORTANCE_HIGH)
                )
            }
            // Constante notification: valor inmutable que no cambia tras su asignación
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("🚨 ¡Lomito fuera de rango!")
                .setContentText("Distancia: ${distancia}m")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            // Muestra la notificación al usuario en la barra de estado
            mgr.notify(1001, notification)
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("WEAR_MSG", "❌ Error notificación: ${e.message}")
        }
    }

    // Método del ciclo de vida: se limpia la actividad antes de destruirse
    override fun onDestroy() {
        // Invoca la implementación del método en la clase padre
        super.onDestroy()
        // Registro de evento en el log de Android para depuración
        android.util.Log.d("WEAR_MSG", "🛑 WearMessageService destruido")
    }
}