# Guia Paso a Paso: Construyendo el Modulo Movil (Android Smartphone) de Lomito Seguro

Esta guia documenta y desglosa paso a paso la arquitectura, configuracion y construccion completa del modulo **Movil (Android Smartphone)** de **Lomito Seguro**, explicando las decisiones tecnicas, patrones de diseno y bloques de codigo esenciales para un proyecto profesional en **Kotlin** y **Jetpack Compose**.

---

## Objetivo de Esta Guia

Al estudiar y seguir esta guia, comprenderas:

1. Como estructurar un proyecto Android profesional con **Kotlin** y **Jetpack Compose** bajo los principios de **Clean Architecture** y **MVVM**.
2. Como implementar comunicacion bidireccional con un smartwatch **Wear OS** usando la **Wearable Data Layer API**.
3. Como conectar la aplicacion con un backend **Spring Boot** usando **Retrofit** y **OkHttp**.
4. Como implementar un sistema de notificaciones en tiempo real con **polling** y **NotificationCompat**.
5. Como gestionar sesiones de usuario con **SharedPreferences** y manejar el flujo de autenticacion.
6. Como integrar **Google Maps** para mostrar refugios y reportar avistamientos geolocalizados.
7. Como implementar la funcionalidad de fotos con subida al servidor y visualizacion con **Glide/Coil**.

## Arquitectura del Módulo Móvil

El módulo móvil sigue la arquitectura **MVVM (Model-View-ViewModel)** con separación de responsabilidades en capas:

```text
app/
├── config/        # Configuración global (URLs, constantes)
├── data/
│   ├── api/       # Interfaces Retrofit + Cliente HTTP
│   ├── model/     # Data classes (entidades de dominio)
│   └── repos/     # Repositorios (acceso a datos)
├── network/       # Interceptores OkHttp
├── ui/            # Fragments + ViewModels + Adapters
│   ├── auth/      # Login y Registro
│   ├── home/      # Pantalla principal y lista de mascotas
│   ├── mascota/   # CRUD de mascotas
│   ├── alertas/   # Notificaciones de avistamientos
│   ├── mural/     # Mural comunitario de mascotas perdidas
│   ├── refugios/  # Mapa de refugios
│   └── simulator/ # Simulador BLE de distancia
└── util/          # Funciones de extensión y utilidades
```

## FASE 1: `com/lomito/seguro`

### Paso 1.1: `MainActivity.kt`

**Actividad principal** de la aplicación móvil. Gestiona la navegación central mediante Navigation Component, configura el toolbar, el menú de logout y coordina el polling de notificaciones de avistamientos. También maneja la comunicación bidireccional con el smartwatch a través de la API de Wearable.

```kotlin
// mobile/MainActivity.kt
// Paquete: com.lomito.seguro
package com.lomito.seguro

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
// Importa la clase de logging de Android
import android.util.Log
// Importa la dependencia necesaria: Menu
import android.view.Menu
// Importa la dependencia necesaria: MenuInflater
import android.view.MenuInflater
// Importa la dependencia necesaria: MenuItem
import android.view.MenuItem
// Importa la dependencia necesaria: AppCompatActivity
import androidx.appcompat.app.AppCompatActivity
// Importa las clases para manejo de notificaciones
import androidx.core.app.NotificationCompat
// Importa la dependencia necesaria: MenuProvider
import androidx.core.view.MenuProvider
// Importa la dependencia necesaria: Lifecycle
import androidx.lifecycle.Lifecycle
// Importa la dependencia necesaria: lifecycleScope
import androidx.lifecycle.lifecycleScope
// Importa la dependencia necesaria: repeatOnLifecycle
import androidx.lifecycle.repeatOnLifecycle
// Importa componente de navegación
import androidx.navigation.NavController
// Importa componente de navegación
import androidx.navigation.fragment.NavHostFragment
// Importa componente de navegación
import androidx.navigation.ui.AppBarConfiguration
// Importa componente de navegación
import androidx.navigation.ui.setupActionBarWithNavController
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.Wearable
// Importa la dependencia necesaria: AlertasRepository
import com.lomito.seguro.repository.AlertasRepository
// Importa el sistema de View Binding
import com.lomito.seguro.databinding.ActivityMainBinding
// Importa la dependencia necesaria: SessionManager
import com.lomito.seguro.util.SessionManager
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.delay
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch
// Importa el parser JSON
import org.json.JSONObject

/**
 * [Actividad principal de la aplicación]
 *
 * Responsabilidades:
 * - [Gestionar la navegación principal de la app]
 * - [Configurar el polling de notificaciones de avistamientos]
 * - [Sincronizar información básica con el smartwatch]
 */
// Activity MainActivity: pantalla principal que gestiona el ciclo de vida
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    // Constante alertasRepository: valor inmutable que no cambia tras su asignación
    private val alertasRepository = AlertasRepository()
    // Constante avistamientosNotificados: valor inmutable que no cambia tras su asignación
    private val avistamientosNotificados = mutableSetOf<Int>()
    // Controlador de navegación para moverse entre fragments
    private lateinit var navController: NavController
    private lateinit var sessionManager: SessionManager

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)
        // Infla el layout de la Activity usando View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        // Accede a un componente de UI a través del View Binding type-safe
        setContentView(binding.root)
        // Accede a un componente de UI a través del View Binding type-safe
        setSupportActionBar(binding.toolbar)

        sessionManager = SessionManager(this)

        // Constante navHost: valor inmutable que no cambia tras su asignación
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        // Controlador de navegación para moverse entre fragments
        navController = navHost.navController

        // Constante appBarConfig: valor inmutable que no cambia tras su asignación
        val appBarConfig = AppBarConfiguration(setOf(R.id.loginFragment, R.id.homeFragment))
        // Controlador de navegación para moverse entre fragments
        setupActionBarWithNavController(navController, appBarConfig)

        addMenuProvider(object : MenuProvider {
            // Sobreescribe la función onCreateMenu de la clase padre
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.home_menu, menu)
            }

            // Sobreescribe la función onPrepareMenu de la clase padre
            override fun onPrepareMenu(menu: Menu) {
                // Constante destinoActual: valor inmutable que no cambia tras su asignación
                val destinoActual = navController.currentDestination?.id
                // Constante ocultarEn: valor inmutable que no cambia tras su asignación
                val ocultarEn = setOf(R.id.loginFragment, R.id.registerFragment)
                menu.findItem(R.id.action_logout)?.isVisible = destinoActual !in ocultarEn
            }

            // Sobreescribe la función onMenuItemSelected de la clase padre
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Retorna el valor al llamador de la función
                return when (menuItem.itemId) {
                    R.id.action_logout -> {
                        sessionManager.logout()
                        // Navega hacia el destino especificado en el grafo de navegación
                        navController.navigate(R.id.action_global_logout)
                        true
                    }
                    else -> false
                }
            }
        })

        // Controlador de navegación para moverse entre fragments
        navController.addOnDestinationChangedListener { _, _, _ -> invalidateMenu() }

        enviarUserIdAlWatchSiExiste()
        iniciarPollingDeAvistamientos()
        manejarIntentDeNotificacion(intent)
    }

    // Sobreescribe la función onNewIntent de la clase padre
    override fun onNewIntent(intent: Intent) {
        // Invoca la implementación del método en la clase padre
        super.onNewIntent(intent)
        setIntent(intent)
        manejarIntentDeNotificacion(intent)
    }

    private fun manejarIntentDeNotificacion(intent: Intent?) {
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (intent?.getBooleanExtra("open_alerta", false) == true) {
            // Navega hacia el destino especificado en el grafo de navegación
            navController.navigate(R.id.alertasFragment)
        }
    }

    private fun iniciarPollingDeAvistamientos() {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    // Bloque try-catch: maneja posibles excepciones en el código crítico
                    try {
                        // Registro de evento en el log de Android para depuración
                        Log.d("POLLING", "========== INICIO POLLING ==========")
                        // Constante ownerId: valor inmutable que no cambia tras su asignación
                        val ownerId = sessionManager.getUserId().toIntOrNull()
                        // Registro de evento en el log de Android para depuración
                        Log.d("POLLING", "OwnerId: $ownerId")

                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (ownerId != null && ownerId != 0) {
                            // Registro de evento en el log de Android para depuración
                            Log.d("POLLING", "Llamando a getAlertasNoLeidas...")
                            // Constante resultado: valor inmutable que no cambia tras su asignación
                            val resultado = alertasRepository.getAlertasNoLeidas(ownerId)
                            // Registro de evento en el log de Android para depuración
                            Log.d("POLLING", "Resultado success: ${resultado.success}")
                            // Registro de evento en el log de Android para depuración
                            Log.d("POLLING", "Cantidad de alertas: ${resultado.alertas.size}")

                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (resultado.success) {
                                // Mostrar todas las alertas que llegaron
                                // Itera sobre cada elemento de la colección y ejecuta el bloque
                                resultado.alertas.forEach { alerta ->
                                    // Registro de evento en el log de Android para depuración
                                    Log.d("POLLING", "Alerta: id=${alerta.id}, tipo=${alerta.tipo}, leida=${alerta.leida}, mensaje=${alerta.mensaje}")
                                }

                                // ✅ Filtrar SOLO alertas NO leídas y que contengan "AVISTAMIENTO"
                                // Constante avistamientos: valor inmutable que no cambia tras su asignación
                                val avistamientos = resultado.alertas
                                    .filter {
                                        // Registro de evento en el log de Android para depuración
                                        Log.d("POLLING", "Filtrando: tipo=${it.tipo}, leida=${it.leida}, contiene AVISTAMIENTO=${it.tipo.contains("AVISTAMIENTO")}")
                                        it.tipo.contains("AVISTAMIENTO") &&
                                                !it.leida &&  // ✅ SOLO NO LEÍDAS
                                                it.id !in avistamientosNotificados
                                    }

                                // Registro de evento en el log de Android para depuración
                                Log.d("POLLING", "Avistamientos encontrados: ${avistamientos.size}")
                                // Registro de evento en el log de Android para depuración
                                Log.d("POLLING", "IDs ya notificados: ${avistamientosNotificados}")

                                // Itera sobre cada elemento de la colección y ejecuta el bloque
                                avistamientos.forEach { alerta ->
                                    // Registro de evento en el log de Android para depuración
                                    Log.d("POLLING", "📢 PROCESANDO AVISTAMIENTO: ${alerta.id}")
                                    avistamientosNotificados.add(alerta.id)
                                    mostrarNotificacionAvistamiento(
                                        alerta.mascotaNombre,
                                        alerta.mensaje,
                                        alerta.mascotaId,
                                        alerta.tipo
                                    )
                                    enviarAvistamientoAlWatch(alerta.mascotaNombre, alerta.mensaje, alerta.mascotaId)
                                }
                            } else {
                                // Registro de evento en el log de Android para depuración
                                Log.e("POLLING", "Error en resultado: ${resultado.error}")
                            }
                        } else {
                            // Registro de evento en el log de Android para depuración
                            Log.d("POLLING", "OwnerId no válido: $ownerId")
                        }
                    } catch (e: Exception) {
                        // Registro de evento en el log de Android para depuración
                        Log.e("POLLING", "Error en polling: ${e.message}", e)
                    }
                    delay(8000)
                }
            }
        }
    }

    private fun mostrarNotificacionAvistamiento(
        mascotaNombre: String?,
        mensaje: String,
        mascotaId: String?,
        tipo: String = "AVISTAMIENTO"
    ) {
        // Constante channelId: valor inmutable que no cambia tras su asignación
        val channelId = "lomito_avistamientos"
        // Constante notificationManager: valor inmutable que no cambia tras su asignación
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Crea el canal de notificación requerido en Android 8.0+
            notificationManager.createNotificationChannel(
                // Crea el canal de notificación requerido en Android 8.0+
                NotificationChannel(channelId, "Avistamientos confirmados", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        // Título dinámico según el tipo
        // Constante titulo: valor inmutable que no cambia tras su asignación
        val titulo = when {
            tipo.contains("CONFIRMADO") -> "✅ ¡Avistamiento confirmado!"
            tipo.contains("REPORTADO") -> "👀 ¡Nuevo avistamiento reportado!"
            else -> "🐾 ¡Avistamiento!"
        }

        // Constante intent: valor inmutable que no cambia tras su asignación
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_alerta", true)
            putExtra("mascota_nombre", mascotaNombre)
        }
        // Constante pendingIntent: valor inmutable que no cambia tras su asignación
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Constante notification: valor inmutable que no cambia tras su asignación
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Muestra la notificación al usuario en la barra de estado
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        // Registro de evento en el log de Android para depuración
        Log.d("POLLING", "✅ Notificación enviada: $titulo - $mensaje")
    }

    private fun enviarAvistamientoAlWatch(mascotaNombre: String?, mensaje: String, mascotaId: String?) {
        // Constante context: valor inmutable que no cambia tras su asignación
        val context = applicationContext
        // Constante payload: valor inmutable que no cambia tras su asignación
        val payload = JSONObject().apply {
            put("tipo", "AVISTAMIENTO_CONFIRMADO")
            put("mascotaId", mascotaId ?: "")
            put("mascotaNombre", mascotaNombre ?: "")
            put("mensaje", mensaje)
        }.toString().toByteArray()

        // Usa la API de Wearable para comunicación con dispositivos Wear OS
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                nodes.forEach { node ->
                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    Wearable.getMessageClient(context)
                        // Envía un mensaje al dispositivo Wear OS conectado
                        .sendMessage(node.id, "/watch/avistamiento_confirmado", payload)
                }
            }
    }

    // Sobreescribe la función onSupportNavigateUp de la clase padre
    override fun onSupportNavigateUp(): Boolean {
        // Controlador de navegación para moverse entre fragments
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun enviarUserIdAlWatchSiExiste() {
        // Constante userId: valor inmutable que no cambia tras su asignación
        val userId = sessionManager.getUserId()
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (userId.isNotEmpty() && userId != "null") {
            enviarUserIdAlWatch(userId)
        }
    }

    private fun enviarUserIdAlWatch(userId: String) {
        // Constante context: valor inmutable que no cambia tras su asignación
        val context = applicationContext
        // Constante payload: valor inmutable que no cambia tras su asignación
        val payload = JSONObject().apply {
            put("tipo", "USER_ID")
            put("userId", userId)
        }.toString().toByteArray()

        // Registro de evento en el log de Android para depuración
        Log.d("USER_ID", "📤 Enviando userId $userId al watch")

        // Usa la API de Wearable para comunicación con dispositivos Wear OS
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (nodes.isEmpty()) {
                    // Registro de evento en el log de Android para depuración
                    Log.d("USER_ID", "⚠️ No hay nodos (watch) conectados")
                    return@addOnSuccessListener
                }

                // Itera sobre cada elemento de la colección y ejecuta el bloque
                nodes.forEach { node ->
                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    Wearable.getMessageClient(context)
                        // Envía un mensaje al dispositivo Wear OS conectado
                        .sendMessage(node.id, "/watch/user_id", payload)
                        .addOnSuccessListener {
                            // Registro de evento en el log de Android para depuración
                            Log.d("USER_ID", "✅ userId $userId enviado al watch: ${node.displayName}")
                        }
                        .addOnFailureListener {
                            // Registro de evento en el log de Android para depuración
                            Log.e("USER_ID", "❌ Error enviando userId: ${it.message}")
                        }
                }
            }
            .addOnFailureListener {
                // Registro de evento en el log de Android para depuración
                Log.e("USER_ID", "❌ Error obteniendo nodos: ${it.message}")
            }
    }

    // Función actualizarUserIdEnWatch: define la lógica de esta operación
    fun actualizarUserIdEnWatch(userId: String) {
        enviarUserIdAlWatch(userId)
    }
}
```

### Paso 1.2: `WatchReportListener.kt`

**Servicio de escucha Wearable** que se ejecuta en background. Recibe mensajes desde el smartwatch (alertas de proximidad, reportes de avistamiento y notificaciones de mascotas perdidas) y los procesa mostrando notificaciones locales en el teléfono.

```kotlin
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
```

## FASE 2: `com/lomito/seguro/config`

### Paso 2.1: `AppConfig.kt`

**Configuración centralizada** de la aplicación. Contiene las constantes globales como la URL base del backend y los timeouts de red.

```kotlin
package com.lomito.seguro.config

object AppConfig {
    const val BASE_URL = "https://api.lomito.com/"
    const val TIMEOUT = 30L
}
```

## FASE 3: `com/lomito/seguro/data`

### Paso 3.1: `Constants.kt`

**Constantes globales** del módulo de datos. Define los valores fijos utilizados en toda la capa de datos como códigos de error, rutas de API y claves de configuración.

```kotlin
package com.lomito.seguro.data

object Constants {
    const val SHARED_PREFS_NAME = "lomito_prefs"
    const val KEY_USER_ID = "user_id"
}
```

## FASE 4: `com/lomito/seguro/data/api`

### Paso 4.1: `LomitoApi.kt`

**Interfaz de la API REST** definida con Retrofit. Declara todos los endpoints del backend de Lomito Seguro con sus métodos HTTP, rutas, parámetros y tipos de respuesta.

```kotlin
// Paquete: com.lomito.seguro.data.api
package com.lomito.seguro.data.api

// Importa la dependencia necesaria: *
import com.lomito.seguro.data.model.*
// Importa la dependencia necesaria: MultipartBody
import okhttp3.MultipartBody
// Importa la dependencia necesaria: Response
import retrofit2.Response
// Importa la dependencia necesaria: *
import retrofit2.http.*

/**
 * [Interfaz de red para comunicarse con el backend de Lomito]
 *
 * Responsabilidades:
 * - [Definir los endpoints de la API REST]
 * - [Gestionar peticiones de autenticación, mascotas, reportes y alertas]
 */
// Interfaz LomitoApi: contrato que deben cumplir las implementaciones
interface LomitoApi {

    // FOTOS
    @Multipart
    @POST("upload")
    suspend fun uploadFoto(@Part foto: MultipartBody.Part): Response<UploadResponse>

    // AUTH
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<Usuario>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Usuario>

    @GET("auth/profile/{id}")
    suspend fun getProfile(@Path("id") id: String): Response<Usuario>

    @PUT("auth/profile/{id}")
    suspend fun updateProfile(@Path("id") id: String, @Body data: Map<String, @JvmSuppressWildcards String>): Response<Usuario>

    // MASCOTAS — rutas estáticas primero, luego las de {id}
    @GET("mascotas")
    suspend fun getMascotas(@Query("ownerId") ownerId: String): Response<List<Mascota>>

    @GET("mascotas/estado")
    suspend fun getMascotasByEstado(@Query("estado") estado: String): Response<List<Mascota>>

    @POST("mascotas")
    suspend fun createMascota(@Body mascota: CreateMascotaRequest): Response<Mascota>

    @GET("mascotas/{id}")
    suspend fun getMascotaById(@Path("id") id: String): Response<Mascota>

    @PUT("mascotas/{id}")
    suspend fun updateMascota(@Path("id") id: String, @Body data: Map<String, @JvmSuppressWildcards Any>): Response<Mascota>

    @DELETE("mascotas/{id}")
    suspend fun deleteMascota(@Path("id") id: String): Response<Map<String, String>>

    @PUT("mascotas/{id}/ubicacion")
    suspend fun updateUbicacion(@Path("id") id: String, @Body data: UbicacionRequest): Response<Mascota>

    @GET("mascotas/{id}/ultimo-reporte")
    suspend fun getUltimoReporte(@Path("id") id: String): Response<ReporteVista>

    @GET("mascotas/{id}/reportes")
    suspend fun getReportesMascota(@Path("id") id: String): Response<List<ReporteVista>>

    // ALERTAS
    @GET("alertas")
    suspend fun getAlertas(@Query("ownerId") ownerId: String): Response<List<Alerta>>

    @GET("alertas/no-leidas")
    suspend fun getAlertasNoLeidas(@Query("ownerId") ownerId: String): Response<List<Alerta>>

    @PUT("alertas/{id}/leida")
    suspend fun marcarLeida(@Path("id") id: String): Response<Map<String, Any>>

    @PUT("alertas.leidas/{ownerId}")
    suspend fun marcarTodasLeidas(@Path("ownerId") ownerId: String): Response<Map<String, Any>>

    // REPORTES
    @POST("reportes")
    suspend fun reportarVista(@Body reporte: ReporteRequest): Response<ReporteVista>

    @GET("reportes/mascota/{mascotaId}")
    suspend fun getReportesByMascota(@Path("mascotaId") mascotaId: String): Response<List<ReporteVista>>

    @PUT("reportes/{id}/confirmar")
    suspend fun confirmarReporte(@Path("id") id: String): Response<Map<String, Any>>

    // REFUGIOS
    @GET("refugios")
    suspend fun getRefugios(): Response<List<Refugio>>

    @GET("refugios/{id}")
    suspend fun getRefugioById(@Path("id") id: String): Response<Refugio>
}
```

### Paso 4.2: `RetrofitClient.kt`

**Cliente Retrofit singleton** para la capa de red. Configura el cliente OkHttp con interceptores, establece la URL base del servidor y construye la instancia de Retrofit con el conversor Gson para serialización JSON.

```kotlin
// Paquete: com.lomito.seguro.data.api
package com.lomito.seguro.data.api

// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.BuildConfig
// Importa el cliente HTTP OkHttp
import okhttp3.OkHttpClient
// Importa la clase de logging de Android
import okhttp3.logging.HttpLoggingInterceptor
// Importa el cliente Retrofit para peticiones HTTP
import retrofit2.Retrofit
// Importa el parser JSON
import retrofit2.converter.gson.GsonConverterFactory
// Importa la dependencia necesaria: TimeUnit
import java.util.concurrent.TimeUnit

/**
 * [Cliente Retrofit configurado para la app]
 *
 * Responsabilidades:
 * - [Construir y proveer la instancia central de Retrofit]
 * - [Configurar interceptores y tiempos de espera de las peticiones]
 */
// Singleton RetrofitClient: instancia única compartida en toda la aplicación
object RetrofitClient {
    // ✅ La IP/URL real vive en /gradle.properties (LOMITO_BACKEND_URL),
    // se inyecta aquí vía BuildConfig. No hardcodees la IP en más archivos.
    // Constante SERVER_URL: valor inmutable que no cambia tras su asignación
    val SERVER_URL: String = BuildConfig.BACKEND_URL
    // Constante BASE_URL: valor inmutable que no cambia tras su asignación
    private val BASE_URL = "$SERVER_URL/api/"
    // Constante loggingInterceptor: valor inmutable que no cambia tras su asignación
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Constante okHttpClient: valor inmutable que no cambia tras su asignación
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Constante api: valor inmutable que no cambia tras su asignación
    val api: LomitoApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LomitoApi::class.java)
    }
}
```

## FASE 5: `com/lomito/seguro/data/model`

### Paso 5.1: `Alerta.kt`

**Modelo de datos Alerta**. Clase de datos (data class) que representa una notificación de avistamiento o evento relacionado con una mascota.

```kotlin
// Paquete: com.lomito.seguro.models
package com.lomito.seguro.models

// Importa la dependencia necesaria: Date
import java.util.Date

// Clase de datos Alerta: modelo inmutable con propiedades de dominio
data class Alerta(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: Int,
    // Constante titulo: valor inmutable que no cambia tras su asignación
    val titulo: String = "",
    // Constante mensaje: valor inmutable que no cambia tras su asignación
    val mensaje: String = "",
    // Constante tipo: valor inmutable que no cambia tras su asignación
    val tipo: String = "GENERAL",
    // Constante fecha: valor inmutable que no cambia tras su asignación
    val fecha: Date = Date(),
    // Constante leida: valor inmutable que no cambia tras su asignación
    val leida: Boolean = false,
    // Constante mascotaId: valor inmutable que no cambia tras su asignación
    val mascotaId: String? = null,
    // Constante mascotaNombre: valor inmutable que no cambia tras su asignación
    val mascotaNombre: String? = null
)
```

### Paso 5.2: `Models.kt`

**Modelos de datos** del dominio. Define las data classes que representan las entidades principales: Mascota, Usuario, Reporte, Refugio, etc.

```kotlin
// Paquete: com.lomito.seguro.data.model
package com.lomito.seguro.data.model

// Importa la dependencia necesaria: SerializedName
import com.google.gson.annotations.SerializedName

// Clase de datos Usuario: modelo inmutable con propiedades de dominio
data class Usuario(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String = "",
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String = "",
    // Constante correo: valor inmutable que no cambia tras su asignación
    val correo: String = "",
    // Constante telefono: valor inmutable que no cambia tras su asignación
    val telefono: String = "",
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

// Clase de datos Mascota: modelo inmutable con propiedades de dominio
data class Mascota(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String = "",
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String = "",
    // Constante especie: valor inmutable que no cambia tras su asignación
    val especie: String = "",
    // Constante raza: valor inmutable que no cambia tras su asignación
    val raza: String = "",
    // Constante edad: valor inmutable que no cambia tras su asignación
    val edad: Int = 0,
    // Constante color: valor inmutable que no cambia tras su asignación
    val color: String = "",
    // Constante peso: valor inmutable que no cambia tras su asignación
    val peso: Double = 0.0,
    @SerializedName("foto_url") val fotoUrl: String? = null,
    @SerializedName("distancia_alerta") val distanciaAlerta: Int = 50,
    // Constante estado: valor inmutable que no cambia tras su asignación
    val estado: String = "EN_CASA",
    // Constante activa: valor inmutable que no cambia tras su asignación
    val activa: Boolean = true,
    @SerializedName("owner_id") val ownerId: String = "",
    // Constante latitud: valor inmutable que no cambia tras su asignación
    val latitud: Double? = null,
    // Constante longitud: valor inmutable que no cambia tras su asignación
    val longitud: Double? = null
)
// Clase de datos CreateMascotaRequest: modelo inmutable con propiedades de dominio
data class CreateMascotaRequest(
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String,
    // Constante especie: valor inmutable que no cambia tras su asignación
    val especie: String,
    @SerializedName("owner_id") val ownerId: String,
    // Constante raza: valor inmutable que no cambia tras su asignación
    val raza: String = "",
    // Constante color: valor inmutable que no cambia tras su asignación
    val color: String = "",
    // Constante edad: valor inmutable que no cambia tras su asignación
    val edad: Int = 0,
    // Constante peso: valor inmutable que no cambia tras su asignación
    val peso: Double = 0.0,
    @SerializedName("distancia_alerta") val distanciaAlerta: Int = 50,
    @SerializedName("foto_url") val fotoUrl: String? = null
)

// Clase de datos UploadResponse: modelo inmutable con propiedades de dominio
data class UploadResponse(
    @SerializedName("foto_url") val fotoUrl: String
)

// Clase de datos Alerta: modelo inmutable con propiedades de dominio
data class Alerta(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String = "",
    @SerializedName("mascota_id") val mascotaId: String = "",
    @SerializedName("owner_id") val ownerId: String = "",
    // Constante tipo: valor inmutable que no cambia tras su asignación
    val tipo: String = "",
    // Constante mensaje: valor inmutable que no cambia tras su asignación
    val mensaje: String = "",
    // Constante distancia: valor inmutable que no cambia tras su asignación
    val distancia: Int = 0,
    // Constante leida: valor inmutable que no cambia tras su asignación
    val leida: Boolean = false,
    // Constante timestamp: valor inmutable que no cambia tras su asignación
    val timestamp: String = ""
)

// Clase de datos ReporteVista: modelo inmutable con propiedades de dominio
data class ReporteVista(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String = "",
    @SerializedName("mascota_id") val mascotaId: String = "",
    @SerializedName("reportado_por_id") val reportadoPorId: String? = null,
    // Constante latitud: valor inmutable que no cambia tras su asignación
    val latitud: Double = 0.0,
    // Constante longitud: valor inmutable que no cambia tras su asignación
    val longitud: Double = 0.0,
    // Constante direccion: valor inmutable que no cambia tras su asignación
    val direccion: String = "",
    // Constante timestamp: valor inmutable que no cambia tras su asignación
    val timestamp: String = ""
)

// Clase de datos Refugio: modelo inmutable con propiedades de dominio
data class Refugio(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String = "",
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String = "",
    // Constante direccion: valor inmutable que no cambia tras su asignación
    val direccion: String = "",
    // Constante telefono: valor inmutable que no cambia tras su asignación
    val telefono: String = "",
    // Constante horarios: valor inmutable que no cambia tras su asignación
    val horarios: String = "",
    @SerializedName("video_url") val videoUrl: String? = null,
    @SerializedName("logo_url") val logoUrl: String? = null
)

// Clase de datos LoginRequest: modelo inmutable con propiedades de dominio
data class LoginRequest(val correo: String, val contrasena: String)
// Clase de datos RegisterRequest: modelo inmutable con propiedades de dominio
data class RegisterRequest(val nombre: String, val correo: String, val telefono: String, val contrasena: String)
// Clase de datos UbicacionRequest: modelo inmutable con propiedades de dominio
data class UbicacionRequest(val lat: Double, val lng: Double)
// Clase de datos ReporteRequest: modelo inmutable con propiedades de dominio
data class ReporteRequest(
    @SerializedName("mascota_id") val mascotaId: String,
    // Constante latitud: valor inmutable que no cambia tras su asignación
    val latitud: Double,
    // Constante longitud: valor inmutable que no cambia tras su asignación
    val longitud: Double,
    // Constante direccion: valor inmutable que no cambia tras su asignación
    val direccion: String = ""
)
```

## FASE 6: `com/lomito/seguro/data/repository`

### Paso 6.1: `AlertasRepository.kt`

**Repositorio de alertas**. Capa de abstracción que centraliza el acceso a los datos de alertas, coordinando las peticiones al backend y el manejo de errores.

```kotlin
// Paquete: com.lomito.seguro.repository
package com.lomito.seguro.repository

// Importa la clase de logging de Android
import android.util.Log
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.BuildConfig
// Importa la dependencia necesaria: Alerta
import com.lomito.seguro.models.Alerta
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.Dispatchers
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.withContext
// Importa el parser JSON
import org.json.JSONArray
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
import java.net.URL
// Importa la dependencia necesaria: SimpleDateFormat
import java.text.SimpleDateFormat
// Importa la dependencia necesaria: *
import java.util.*

// Repositorio AlertasRepository: capa de datos que abstrae las fuentes de información
class AlertasRepository {
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    private val backendUrl = BuildConfig.BACKEND_URL
    // Constante TAG: valor inmutable que no cambia tras su asignación
    private val TAG = "AlertasRepository"

    suspend fun getAlertas(ownerId: Int): AlertasResult {
        // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
        return withContext(Dispatchers.IO) {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Registro de evento en el log de Android para depuración
                Log.d(TAG, "Obteniendo alertas para ownerId: $ownerId")

                // Constante url: valor inmutable que no cambia tras su asignación
                val url = URL("$backendUrl/api/alertas?ownerId=$ownerId")
                // Constante conn: valor inmutable que no cambia tras su asignación
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                // Constante responseCode: valor inmutable que no cambia tras su asignación
                val responseCode = conn.responseCode

                // Registro de evento en el log de Android para depuración
                Log.d(TAG, "Response Code: $responseCode")

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Constante response: valor inmutable que no cambia tras su asignación
                    val response = conn.inputStream.bufferedReader().readText()
                    // Registro de evento en el log de Android para depuración
                    Log.d(TAG, "Respuesta: $response")

                    // Constante jsonArray: valor inmutable que no cambia tras su asignación
                    val jsonArray = JSONArray(response)
                    // Constante alertas: valor inmutable que no cambia tras su asignación
                    val alertas = mutableListOf<Alerta>()

                    // Itera sobre la colección para procesar cada elemento
                    for (i in 0 until jsonArray.length()) {
                        // Constante obj: valor inmutable que no cambia tras su asignación
                        val obj = jsonArray.getJSONObject(i)

                        // Parsear fecha de manera segura
                        // Constante fechaStr: valor inmutable que no cambia tras su asignación
                        val fechaStr = obj.optString("timestamp", null) ?: obj.optString("fecha", null)
                        // Constante fecha: valor inmutable que no cambia tras su asignación
                        val fecha = try {
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (fechaStr != null) {
                                // Constante formatos: valor inmutable que no cambia tras su asignación
                                val formatos = listOf(
                                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                    "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                                    "yyyy-MM-dd HH:mm:ss",
                                    "yyyy-MM-dd'T'HH:mm:ss"
                                )
                                // Variable parsedDate: almacena el estado mutable de este componente
                                var parsedDate: Date? = null
                                // Itera sobre la colección para procesar cada elemento
                                for (formato in formatos) {
                                    // Bloque try-catch: maneja posibles excepciones en el código crítico
                                    try {
                                        // Constante sdf: valor inmutable que no cambia tras su asignación
                                        val sdf = SimpleDateFormat(formato, Locale.getDefault())
                                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                                        parsedDate = sdf.parse(fechaStr)
                                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                        if (parsedDate != null) break
                                    } catch (e: Exception) {
                                        // Intentar con el siguiente formato
                                    }
                                }
                                parsedDate ?: Date()
                            } else {
                                Date()
                            }
                        } catch (e: Exception) {
                            // Registro de evento en el log de Android para depuración
                            Log.e(TAG, "Error parseando fecha: ${e.message}")
                            Date()
                        }

                        // Constante alerta: valor inmutable que no cambia tras su asignación
                        val alerta = Alerta(
                            id = obj.getInt("id"),
                            titulo = obj.optString("tipo", "Notificación"),
                            mensaje = obj.optString("mensaje", ""),
                            tipo = obj.optString("tipo", "GENERAL"),
                            fecha = fecha,
                            leida = obj.optBoolean("leida", false),
                            mascotaId = obj.optString("mascota_id", null),
                            mascotaNombre = obj.optString("mascota_nombre", null)
                        )
                        alertas.add(alerta)
                    }
                    conn.disconnect()
                    // Registro de evento en el log de Android para depuración
                    Log.d(TAG, "Alertas cargadas: ${alertas.size}")
                    AlertasResult(alertas, true, null)
                } else {
                    // Constante errorBody: valor inmutable que no cambia tras su asignación
                    val errorBody = conn.errorStream?.bufferedReader()?.readText()
                    conn.disconnect()
                    // Registro de evento en el log de Android para depuración
                    Log.e(TAG, "Error HTTP $responseCode: $errorBody")
                    AlertasResult(emptyList(), false, "Error al cargar alertas (HTTP $responseCode): $errorBody")
                }
            } catch (e: Exception) {
                // Registro de evento en el log de Android para depuración
                Log.e(TAG, "Error: ${e.message}", e)
                AlertasResult(emptyList(), false, "Error: ${e.message}")
            }
        }
    }

    // ✅ MODIFICADO: Usa el endpoint /avistamientos
    suspend fun getAlertasNoLeidas(ownerId: Int): AlertasResult {
        // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
        return withContext(Dispatchers.IO) {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Registro de evento en el log de Android para depuración
                Log.d(TAG, "Obteniendo avistamientos para ownerId: $ownerId")

                // ✅ Usar /api/alertas/avistamientos
                // Constante url: valor inmutable que no cambia tras su asignación
                val url = URL("$backendUrl/api/alertas/avistamientos?ownerId=$ownerId")
                // Constante conn: valor inmutable que no cambia tras su asignación
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                // Constante responseCode: valor inmutable que no cambia tras su asignación
                val responseCode = conn.responseCode

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Constante response: valor inmutable que no cambia tras su asignación
                    val response = conn.inputStream.bufferedReader().readText()
                    // Registro de evento en el log de Android para depuración
                    Log.d(TAG, "Respuesta avistamientos: $response")

                    // Constante jsonArray: valor inmutable que no cambia tras su asignación
                    val jsonArray = JSONArray(response)
                    // Constante alertas: valor inmutable que no cambia tras su asignación
                    val alertas = mutableListOf<Alerta>()

                    // Itera sobre la colección para procesar cada elemento
                    for (i in 0 until jsonArray.length()) {
                        // Constante obj: valor inmutable que no cambia tras su asignación
                        val obj = jsonArray.getJSONObject(i)
                        alertas.add(
                            Alerta(
                                id = obj.getInt("id"),
                                titulo = obj.optString("tipo", "Notificación"),
                                mensaje = obj.optString("mensaje", ""),
                                tipo = obj.optString("tipo", "GENERAL"),
                                fecha = Date(),
                                leida = obj.optBoolean("leida", false),
                                mascotaId = obj.optString("mascota_id", null),
                                mascotaNombre = obj.optString("mascota_nombre", null)
                            )
                        )
                    }
                    conn.disconnect()
                    // Registro de evento en el log de Android para depuración
                    Log.d(TAG, "Avistamientos encontrados: ${alertas.size}")
                    AlertasResult(alertas, true, null)
                } else {
                    conn.disconnect()
                    AlertasResult(emptyList(), false, "Error HTTP $responseCode")
                }
            } catch (e: Exception) {
                // Registro de evento en el log de Android para depuración
                Log.e(TAG, "Error en getAlertasNoLeidas: ${e.message}", e)
                AlertasResult(emptyList(), false, "Error: ${e.message}")
            }
        }
    }

    suspend fun marcarComoLeida(alertaId: Int): OperacionResult {
        // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
        return withContext(Dispatchers.IO) {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Registro de evento en el log de Android para depuración
                Log.d(TAG, "Marcando alerta $alertaId como leída")

                // Constante url: valor inmutable que no cambia tras su asignación
                val url = URL("$backendUrl/api/alertas/$alertaId/leida")
                // Constante conn: valor inmutable que no cambia tras su asignación
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                // Constante json: valor inmutable que no cambia tras su asignación
                val json = JSONObject().apply {
                    put("leida", true)
                }

                conn.outputStream.write(json.toString().toByteArray())
                // Constante responseCode: valor inmutable que no cambia tras su asignación
                val responseCode = conn.responseCode
                conn.disconnect()

                // Registro de evento en el log de Android para depuración
                Log.d(TAG, "Response Code: $responseCode")

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (responseCode == 200 || responseCode == 201) {
                    OperacionResult(true, null)
                } else {
                    OperacionResult(false, "Error al marcar como leída (HTTP $responseCode)")
                }
            } catch (e: Exception) {
                // Registro de evento en el log de Android para depuración
                Log.e(TAG, "Error: ${e.message}", e)
                OperacionResult(false, "Error: ${e.message}")
            }
        }
    }

    suspend fun marcarTodasComoLeidas(ownerId: Int): OperacionResult {
        // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
        return withContext(Dispatchers.IO) {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Registro de evento en el log de Android para depuración
                Log.d(TAG, "Marcando todas las alertas como leídas para ownerId: $ownerId")

                // Constante url: valor inmutable que no cambia tras su asignación
                val url = URL("$backendUrl/api/alertas/leidas/$ownerId")
                // Constante conn: valor inmutable que no cambia tras su asignación
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                // Constante json: valor inmutable que no cambia tras su asignación
                val json = JSONObject().apply {
                    put("ownerId", ownerId)
                }

                conn.outputStream.write(json.toString().toByteArray())
                // Constante responseCode: valor inmutable que no cambia tras su asignación
                val responseCode = conn.responseCode
                conn.disconnect()

                // Registro de evento en el log de Android para depuración
                Log.d(TAG, "Response Code: $responseCode")

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (responseCode == 200 || responseCode == 201) {
                    OperacionResult(true, null)
                } else {
                    OperacionResult(false, "Error al marcar todas como leídas (HTTP $responseCode)")
                }
            } catch (e: Exception) {
                // Registro de evento en el log de Android para depuración
                Log.e(TAG, "Error: ${e.message}", e)
                OperacionResult(false, "Error: ${e.message}")
            }
        }
    }

    // Clase de datos AlertasResult: modelo inmutable con propiedades de dominio
    data class AlertasResult(
        // Constante alertas: valor inmutable que no cambia tras su asignación
        val alertas: List<Alerta>,
        // Constante success: valor inmutable que no cambia tras su asignación
        val success: Boolean,
        // Constante error: valor inmutable que no cambia tras su asignación
        val error: String?
    )

    // Clase de datos OperacionResult: modelo inmutable con propiedades de dominio
    data class OperacionResult(
        // Constante success: valor inmutable que no cambia tras su asignación
        val success: Boolean,
        // Constante error: valor inmutable que no cambia tras su asignación
        val error: String?
    )
}
```

### Paso 6.2: `LomitoRepository.kt`

**Repositorio principal**. Capa de abstracción que centraliza el acceso a todos los datos de la aplicación, coordinando las peticiones al backend REST.

```kotlin
// Paquete: com.lomito.seguro.data.repository
package com.lomito.seguro.data.repository

// Importa el cliente Retrofit para peticiones HTTP
import com.lomito.seguro.data.api.RetrofitClient
// Importa la dependencia necesaria: *
import com.lomito.seguro.data.model.*
// Importa la dependencia necesaria: MultipartBody
import okhttp3.MultipartBody

/**
 * [Repositorio de datos para abstraer las llamadas de red]
 *
 * Responsabilidades:
 * - [Servir como intermediario entre los ViewModels y la API]
 * - [Gestionar los métodos de login, mascotas, alertas y refugios]
 */
// Repositorio LomitoRepository: capa de datos que abstrae las fuentes de información
class LomitoRepository {
    // Constante api: valor inmutable que no cambia tras su asignación
    private val api = RetrofitClient.api

    suspend fun login(correo: String, contrasena: String) =
        api.login(LoginRequest(correo, contrasena))

    suspend fun uploadFoto(foto: MultipartBody.Part) = api.uploadFoto(foto)

    suspend fun register(nombre: String, correo: String, telefono: String, contrasena: String) =
        api.register(RegisterRequest(nombre, correo, telefono, contrasena))

    suspend fun getMascotas(ownerId: String) = api.getMascotas(ownerId)

    suspend fun getMascotaById(id: String) = api.getMascotaById(id)

    suspend fun createMascota(request: CreateMascotaRequest) = api.createMascota(request)

    suspend fun updateMascota(id: String, data: Map<String, Any>) = api.updateMascota(id, data)

    suspend fun deleteMascota(id: String) = api.deleteMascota(id)

    suspend fun updateUbicacion(id: String, lat: Double, lng: Double) =
        api.updateUbicacion(id, UbicacionRequest(lat, lng))

    suspend fun getUltimoReporte(mascotaId: String) = api.getUltimoReporte(mascotaId)

    suspend fun getAlertas(ownerId: String) = api.getAlertas(ownerId)

    suspend fun getAlertasNoLeidas(ownerId: String) = api.getAlertasNoLeidas(ownerId)

    suspend fun marcarLeida(id: String) = api.marcarLeida(id)

    suspend fun marcarTodasLeidas(ownerId: String) = api.marcarTodasLeidas(ownerId)

    suspend fun reportarVista(mascotaId: String, lat: Double, lng: Double, direccion: String = "") =
        api.reportarVista(ReporteRequest(mascotaId, lat, lng, direccion))

    suspend fun confirmarReporte(id: String) = api.confirmarReporte(id)

    suspend fun getRefugios() = api.getRefugios()

    suspend fun getMascotasByEstado(estado: String) = api.getMascotasByEstado(estado)
}
```

## FASE 7: `com/lomito/seguro/network`

### Paso 7.1: `NetworkInterceptor.kt`

**Interceptor de red OkHttp**. Añade cabeceras de autenticación, maneja errores de conectividad y registra las peticiones/respuestas HTTP en el log.

```kotlin
package com.lomito.seguro.network

class NetworkInterceptor {
    fun logRequest() { /* Implementar */ }
}
```

## FASE 8: `com/lomito/seguro/repository`

### Paso 8.1: `UserRepository.kt`

**Repositorio de usuario**. Gestiona las operaciones de autenticación y perfil de usuario, coordinando entre el backend y el almacenamiento local.

```kotlin
package com.lomito.seguro.repository

class UserRepository {
    fun getUser(id: String) { /* Implementar */ }
}
```

## FASE 9: `com/lomito/seguro/ui`

### Paso 9.1: `BaseActivity.kt`

**Actividad base abstracta**. Clase padre de todas las Activities del módulo, proporciona funcionalidades comunes como manejo de loading, errores y navegación.

```kotlin
package com.lomito.seguro.ui

// Fix: Manejar correctamente el ciclo de vida
open class BaseActivity {
    fun showLoading() { /* Fix: Implementar */ }
}
```

## FASE 10: `com/lomito/seguro/ui/alertas`

### Paso 10.1: `AlertasAdapter.kt`

**Adaptador del RecyclerView de alertas**. Conecta la lista de alertas con la vista, inflando los layouts de cada ítem y vinculando los datos.

```kotlin
// Paquete: com.lomito.seguro.ui.alertas
package com.lomito.seguro.ui.alertas

// Importa la dependencia necesaria: LayoutInflater
import android.view.LayoutInflater
// Importa componentes de la interfaz gráfica
import android.view.View
// Importa componentes de la interfaz gráfica
import android.view.ViewGroup
// Importa la dependencia necesaria: DiffUtil
import androidx.recyclerview.widget.DiffUtil
// Importa la dependencia necesaria: ListAdapter
import androidx.recyclerview.widget.ListAdapter
// Importa componentes de la interfaz gráfica
import androidx.recyclerview.widget.RecyclerView
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa el sistema de View Binding
import com.lomito.seguro.databinding.ItemAlertaBinding
// Importa la dependencia necesaria: Alerta
import com.lomito.seguro.models.Alerta
// Importa la dependencia necesaria: SimpleDateFormat
import java.text.SimpleDateFormat
// Importa la dependencia necesaria: *
import java.util.*

// Adaptador AlertasAdapter: conecta los datos con la vista del RecyclerView
class AlertasAdapter(
    // Constante onItemClick: valor inmutable que no cambia tras su asignación
    private val onItemClick: (Alerta) -> Unit,
    // Constante onMarcarLeida: valor inmutable que no cambia tras su asignación
    private val onMarcarLeida: (Int) -> Unit
) : ListAdapter<Alerta, AlertasAdapter.AlertaViewHolder>(AlertaDiffCallback()) {

    // Infla el layout y crea el ViewHolder para el RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertaViewHolder {
        // Constante binding: valor inmutable que no cambia tras su asignación
        val binding = ItemAlertaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Retorna el valor al llamador de la función
        return AlertaViewHolder(binding)
    }

    // Vincula los datos del modelo con las vistas del ViewHolder
    override fun onBindViewHolder(holder: AlertaViewHolder, position: Int) {
        // Constante alerta: valor inmutable que no cambia tras su asignación
        val alerta = getItem(position)
        holder.bind(alerta)
    }

    inner class AlertaViewHolder(
        // Constante binding: valor inmutable que no cambia tras su asignación
        private val binding: ItemAlertaBinding
    // Accede a un componente de UI a través del View Binding type-safe
    ) : RecyclerView.ViewHolder(binding.root) {

        // Función bind: define la lógica de esta operación
        fun bind(alerta: Alerta) {
            // ✅ Icono según tipo
            // Constante icono: valor inmutable que no cambia tras su asignación
            val icono = when (alerta.tipo) {
                "AVISTAMIENTO" -> "👁️"
                "PERDIDA" -> "🐾"
                "ENCONTRADA" -> "✅"
                "UBICACION" -> "📍"
                else -> "📢"
            }
            // Actualiza el componente de UI a través del View Binding
            binding.tvIcono.text = icono

            // ✅ Título y mensaje
            // Actualiza el componente de UI a través del View Binding
            binding.tvTitulo.text = when (alerta.tipo) {
                "AVISTAMIENTO" -> "👁️ Avistamiento"
                "PERDIDA" -> "🐾 Mascota perdida"
                "ENCONTRADA" -> "✅ Mascota encontrada"
                "UBICACION" -> "📍 Nueva ubicación"
                else -> alerta.tipo
            }
            // Actualiza el componente de UI a través del View Binding
            binding.tvMensaje.text = alerta.mensaje

            // ✅ Estado de leída
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (alerta.leida) {
                // Actualiza el componente de UI a través del View Binding
                binding.tvLeida.text = "✓ Leída"
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvLeida.setTextColor(android.graphics.Color.parseColor("#4CD97B"))
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvTitulo.setTextColor(android.graphics.Color.parseColor("#8888AA"))
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvMensaje.setTextColor(android.graphics.Color.parseColor("#8888AA"))
                // Actualiza el componente de UI a través del View Binding
                binding.btnMarcarLeida.visibility = View.GONE
            } else {
                // Actualiza el componente de UI a través del View Binding
                binding.tvLeida.text = "● No leída"
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvLeida.setTextColor(android.graphics.Color.parseColor("#E85D5D"))
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvTitulo.setTextColor(android.graphics.Color.parseColor("#1A1A2E"))
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvMensaje.setTextColor(android.graphics.Color.parseColor("#555577"))
                // Actualiza el componente de UI a través del View Binding
                binding.btnMarcarLeida.visibility = View.VISIBLE
            }

            // ✅ Fecha formateada
            // Constante dateFormat: valor inmutable que no cambia tras su asignación
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            // Actualiza el componente de UI a través del View Binding
            binding.tvFecha.text = dateFormat.format(alerta.fecha)

            // ✅ Click en el item
            // Accede a un componente de UI a través del View Binding type-safe
            binding.root.setOnClickListener {
                onItemClick(alerta)
            }

            // ✅ Botón marcar como leída
            // Accede a un componente de UI a través del View Binding type-safe
            binding.btnMarcarLeida.setOnClickListener {
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (!alerta.leida) {
                    onMarcarLeida(alerta.id)
                }
            }
        }
    }
}

// Declaración de la clase AlertaDiffCallback
class AlertaDiffCallback : DiffUtil.ItemCallback<Alerta>() {
    // Sobreescribe la función areItemsTheSame de la clase padre
    override fun areItemsTheSame(oldItem: Alerta, newItem: Alerta): Boolean {
        // Retorna el valor al llamador de la función
        return oldItem.id == newItem.id
    }

    // Sobreescribe la función areContentsTheSame de la clase padre
    override fun areContentsTheSame(oldItem: Alerta, newItem: Alerta): Boolean {
        // Retorna el valor al llamador de la función
        return oldItem == newItem
    }
}
```

### Paso 10.2: `AlertasFragment.kt`

**Fragment de alertas**. Muestra la lista de notificaciones de avistamientos recibidas. Observa el ViewModel y actualiza la UI de forma reactiva.

```kotlin
// Paquete: com.lomito.seguro.ui.alertas
package com.lomito.seguro.ui.alertas

// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la clase de logging de Android
import android.util.Log
// Importa la dependencia necesaria: *
import android.view.*
// Importa la dependencia necesaria: Toast
import android.widget.Toast
// Importa la clase Fragment de AndroidX
import androidx.fragment.app.Fragment
// Importa la dependencia necesaria: viewModels
import androidx.fragment.app.viewModels
// Importa la dependencia necesaria: lifecycleScope
import androidx.lifecycle.lifecycleScope
// Importa componente de navegación
import androidx.navigation.fragment.findNavController
// Importa la dependencia necesaria: LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
// Importa la dependencia necesaria: MaterialToolbar
import com.google.android.material.appbar.MaterialToolbar
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa la clase Fragment de AndroidX
import com.lomito.seguro.databinding.FragmentAlertasBinding
// Importa la dependencia necesaria: Alerta
import com.lomito.seguro.models.Alerta
// Importa la dependencia necesaria: SessionManager
import com.lomito.seguro.util.SessionManager
// Importa la dependencia necesaria: gone
import com.lomito.seguro.util.gone
// Importa la dependencia necesaria: visible
import com.lomito.seguro.util.visible
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch

/**
 * [Fragmento para listar las notificaciones y alertas del usuario]
 *
 * Responsabilidades:
 * - [Cargar las alertas asociadas al usuario actual]
 * - [Permitir marcar alertas como leídas individual o globalmente]
 */
// Fragment AlertasFragment: componente de UI que representa una sección de la pantalla
class AlertasFragment : Fragment() {
    // Variable _binding: almacena el estado mutable de este componente
    private var _binding: FragmentAlertasBinding? = null
    // Constante binding: valor inmutable que no cambia tras su asignación
    private val binding get() = _binding!!
    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: AlertasViewModel by viewModels()
    private lateinit var session: SessionManager
    private lateinit var adapter: AlertasAdapter
    // Constante TAG: valor inmutable que no cambia tras su asignación
    private val TAG = "AlertasFragment"

    // Infla el layout del Fragment y retorna la vista raíz
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAlertasBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        // Accede a un componente de UI a través del View Binding type-safe
        return binding.root
    }

    // Se llama cuando la vista del Fragment está lista; se inicializa la UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        // ✅ Configurar toolbar
        // Constante toolbar: valor inmutable que no cambia tras su asignación
        val toolbar = binding.toolbar as MaterialToolbar
        toolbar.title = "🔔 Notificaciones"
        toolbar.setTitleTextColor(resources.getColor(R.color.white, null))
        toolbar.inflateMenu(R.menu.alertas_menu)
        toolbar.setOnMenuItemClickListener { item ->
            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
            when (item.itemId) {
                R.id.action_marcar_todas -> {
                    marcarTodasComoLeidas()
                    true
                }
                else -> false
            }
        }

        // ✅ Configurar RecyclerView
        // Asigna el adaptador al RecyclerView para mostrar la lista de datos
        adapter = AlertasAdapter(
            onItemClick = { alerta ->
                // Registro de evento en el log de Android para depuración
                Log.d(TAG, "Click en alerta: ${alerta.id}, leída: ${alerta.leida}")
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (!alerta.leida) {
                    marcarComoLeida(alerta.id)
                }
                navegarADetalle(alerta)
            },
            onMarcarLeida = { alertaId ->
                // Registro de evento en el log de Android para depuración
                Log.d(TAG, "Marcar como leída: $alertaId")
                marcarComoLeida(alertaId)
            }
        )
        // Define cómo se organizan visualmente los elementos del RecyclerView
        binding.rvAlertas.layoutManager = LinearLayoutManager(requireContext())
        // Asigna el adaptador al RecyclerView para mostrar la lista de datos
        binding.rvAlertas.adapter = adapter

        // ✅ Observar alertas
        viewModel.alertas.observe(viewLifecycleOwner) { alertas ->
            // Registro de evento en el log de Android para depuración
            Log.d(TAG, "Alertas observadas: ${alertas.size}")
            adapter.submitList(alertas)
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (alertas.isEmpty()) {
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvEmpty.visible()
                // Accede a un componente de UI a través del View Binding type-safe
                binding.rvAlertas.gone()
            } else {
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvEmpty.gone()
                // Accede a un componente de UI a través del View Binding type-safe
                binding.rvAlertas.visible()
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            // Actualiza el componente de UI a través del View Binding
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (error.isNotEmpty()) {
                // Registro de evento en el log de Android para depuración
                Log.e(TAG, "Error: $error")
                // Muestra un mensaje emergente breve al usuario
                Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_LONG).show()
            }
        }

        // ✅ Cargar alertas al iniciar
        cargarAlertas()
    }

    // ✅ Remover onResume para evitar llamadas redundantes
    // override fun onResume() {
    //     super.onResume()
    //     cargarAlertas()
    // }

    private fun cargarAlertas() {
        // Constante ownerId: valor inmutable que no cambia tras su asignación
        val ownerId = session.getUserId().toIntOrNull() ?: 0
        // Registro de evento en el log de Android para depuración
        Log.d(TAG, "Cargando alertas para ownerId: $ownerId")
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (ownerId == 0) {
            // Muestra un mensaje emergente breve al usuario
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            // Retorna el valor al llamador de la función
            return
        }
        viewModel.cargarAlertas(ownerId)
    }

    private fun marcarComoLeida(alertaId: Int) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewLifecycleOwner.lifecycleScope.launch {
            // Constante success: valor inmutable que no cambia tras su asignación
            val success = viewModel.marcarComoLeida(alertaId)
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (success) {
                // Muestra un mensaje emergente breve al usuario
                Toast.makeText(requireContext(), "Marcada como leída", Toast.LENGTH_SHORT).show()
            } else {
                // Muestra un mensaje emergente breve al usuario
                Toast.makeText(requireContext(), "Error al marcar como leída", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun marcarTodasComoLeidas() {
        // Constante ownerId: valor inmutable que no cambia tras su asignación
        val ownerId = session.getUserId().toIntOrNull() ?: 0
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (ownerId == 0) {
            // Muestra un mensaje emergente breve al usuario
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            // Retorna el valor al llamador de la función
            return
        }

        // Constante alertasNoLeidas: valor inmutable que no cambia tras su asignación
        val alertasNoLeidas = viewModel.alertas.value?.filter { !it.leida } ?: emptyList()
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (alertasNoLeidas.isEmpty()) {
            // Muestra un mensaje emergente breve al usuario
            Toast.makeText(requireContext(), "No hay notificaciones sin leer", Toast.LENGTH_SHORT).show()
            // Retorna el valor al llamador de la función
            return
        }

        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewLifecycleOwner.lifecycleScope.launch {
            // Constante success: valor inmutable que no cambia tras su asignación
            val success = viewModel.marcarTodasComoLeidas(ownerId)
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (success) {
                // Muestra un mensaje emergente breve al usuario
                Toast.makeText(requireContext(), "Todas las notificaciones marcadas como leídas", Toast.LENGTH_SHORT).show()
            } else {
                // Muestra un mensaje emergente breve al usuario
                Toast.makeText(requireContext(), "Error al marcar todas como leídas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navegarADetalle(alerta: Alerta) {
        // Registro de evento en el log de Android para depuración
        Log.d(TAG, "Navegando a detalle: tipo=${alerta.tipo}, mascotaId=${alerta.mascotaId}")

        // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
        when (alerta.tipo) {
            "AVISTAMIENTO", "PERDIDA", "ENCONTRADA" -> {
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (alerta.mascotaId != null) {
                    // Constante bundle: valor inmutable que no cambia tras su asignación
                    val bundle = Bundle().apply {
                        putString("mascotaId", alerta.mascotaId)
                    }
                    // Bloque try-catch: maneja posibles excepciones en el código crítico
                    try {
                        // Navega hacia el destino especificado en el grafo de navegación
                        findNavController().navigate(
                            R.id.action_alertas_to_mascota_detail,
                            bundle
                        )
                    } catch (e: Exception) {
                        // Registro de evento en el log de Android para depuración
                        Log.e(TAG, "Error navegando: ${e.message}")
                        // Muestra un mensaje emergente breve al usuario
                        Toast.makeText(requireContext(), "Detalle: ${alerta.mensaje}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Muestra un mensaje emergente breve al usuario
                    Toast.makeText(requireContext(), "Detalle: ${alerta.mensaje}", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                // Muestra un mensaje emergente breve al usuario
                Toast.makeText(requireContext(), "Detalle: ${alerta.mensaje}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Sobreescribe la función onCreateOptionsMenu de la clase padre
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.alertas_menu, menu)
    }

    // Sobreescribe la función onDestroyView de la clase padre
    override fun onDestroyView() {
        // Invoca la implementación del método en la clase padre
        super.onDestroyView()
        _binding = null
    }
}
```

### Paso 10.3: `AlertasViewModel.kt`

**ViewModel de alertas**. Gestiona el estado de la pantalla de alertas, expone los datos con StateFlow/LiveData y coordina las llamadas al repositorio.

```kotlin
// Paquete: com.lomito.seguro.ui.alertas
package com.lomito.seguro.ui.alertas

// Importa el observable de datos reactivos
import androidx.lifecycle.LiveData
// Importa el observable de datos reactivos
import androidx.lifecycle.MutableLiveData
// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModel
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa la dependencia necesaria: Alerta
import com.lomito.seguro.models.Alerta
// Importa la dependencia necesaria: AlertasRepository
import com.lomito.seguro.repository.AlertasRepository
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.Dispatchers
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.withContext

// ViewModel AlertasViewModel: gestiona el estado y la lógica de negocio de la pantalla
class AlertasViewModel : ViewModel() {
    // Constante repository: valor inmutable que no cambia tras su asignación
    private val repository = AlertasRepository()

    // Constante _alertas: valor inmutable que no cambia tras su asignación
    private val _alertas = MutableLiveData<List<Alerta>>(emptyList())
    // Constante alertas: valor inmutable que no cambia tras su asignación
    val alertas: LiveData<List<Alerta>> = _alertas

    // Constante _loading: valor inmutable que no cambia tras su asignación
    private val _loading = MutableLiveData(false)
    // Constante loading: valor inmutable que no cambia tras su asignación
    val loading: LiveData<Boolean> = _loading

    // Constante _errorMessage: valor inmutable que no cambia tras su asignación
    private val _errorMessage = MutableLiveData("")
    // Constante errorMessage: valor inmutable que no cambia tras su asignación
    val errorMessage: LiveData<String> = _errorMessage

    // Función cargarAlertas: define la lógica de esta operación
    fun cargarAlertas(ownerId: Int) {
        _loading.value = true
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // ✅ Forzar la ejecución en un hilo de IO
                // Constante result: valor inmutable que no cambia tras su asignación
                val result = withContext(Dispatchers.IO) {
                    repository.getAlertas(ownerId)
                }
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (result.success) {
                    _alertas.value = result.alertas
                    _errorMessage.value = ""
                } else {
                    _errorMessage.value = result.error ?: "Error al cargar alertas"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    suspend fun marcarComoLeida(alertaId: Int): Boolean {
        // Retorna el valor al llamador de la función
        return try {
            // Constante result: valor inmutable que no cambia tras su asignación
            val result = withContext(Dispatchers.IO) {
                repository.marcarComoLeida(alertaId)
            }
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (result.success) {
                // Actualizar la lista localmente
                // Constante alertasActualizadas: valor inmutable que no cambia tras su asignación
                val alertasActualizadas = _alertas.value?.map { alerta ->
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (alerta.id == alertaId) {
                        alerta.copy(leida = true)
                    } else {
                        alerta
                    }
                }
                _alertas.value = alertasActualizadas ?: emptyList()
                true
            } else {
                _errorMessage.value = result.error ?: "Error al marcar como leída"
                false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error: ${e.message}"
            false
        }
    }

    suspend fun marcarTodasComoLeidas(ownerId: Int): Boolean {
        // Retorna el valor al llamador de la función
        return try {
            // Constante result: valor inmutable que no cambia tras su asignación
            val result = withContext(Dispatchers.IO) {
                repository.marcarTodasComoLeidas(ownerId)
            }
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (result.success) {
                // Actualizar la lista localmente
                // Constante alertasActualizadas: valor inmutable que no cambia tras su asignación
                val alertasActualizadas = _alertas.value?.map { alerta ->
                    alerta.copy(leida = true)
                }
                _alertas.value = alertasActualizadas ?: emptyList()
                true
            } else {
                _errorMessage.value = result.error ?: "Error al marcar todas como leídas"
                false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error: ${e.message}"
            false
        }
    }
}
```

## FASE 11: `com/lomito/seguro/ui/auth`

### Paso 11.1: `AuthViewModel.kt`

**ViewModel de autenticación**. Maneja la lógica de login y registro, valida las credenciales, realiza las peticiones al backend y gestiona el estado de la sesión.

```kotlin
// Paquete: com.lomito.seguro.ui.auth
package com.lomito.seguro.ui.auth

// Importa el observable de datos reactivos
import androidx.lifecycle.LiveData
// Importa el observable de datos reactivos
import androidx.lifecycle.MutableLiveData
// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModel
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa la dependencia necesaria: Usuario
import com.lomito.seguro.data.model.Usuario
// Importa la dependencia necesaria: LomitoRepository
import com.lomito.seguro.data.repository.LomitoRepository
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch

// Declaración de la clase AuthState
sealed class AuthState {
    // Singleton Loading: instancia única compartida en toda la aplicación
    object Loading : AuthState()
    // Clase de datos Success: modelo inmutable con propiedades de dominio
    data class Success(val usuario: Usuario) : AuthState()
    // Clase de datos Error: modelo inmutable con propiedades de dominio
    data class Error(val message: String) : AuthState()
}

// ViewModel AuthViewModel: gestiona el estado y la lógica de negocio de la pantalla
class AuthViewModel : ViewModel() {
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo = LomitoRepository()
    // Constante _authState: valor inmutable que no cambia tras su asignación
    private val _authState = MutableLiveData<AuthState>()
    // Constante authState: valor inmutable que no cambia tras su asignación
    val authState: LiveData<AuthState> = _authState

    // Función login: define la lógica de esta operación
    fun login(correo: String, contrasena: String) {
        _authState.value = AuthState.Loading
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante response: valor inmutable que no cambia tras su asignación
                val response = repo.login(correo, contrasena)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (response.isSuccessful && response.body() != null) {
                    _authState.value = AuthState.Success(response.body()!!)
                } else {
                    _authState.value = AuthState.Error("Credenciales inválidas")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error de conexión: ${e.message}")
            }
        }
    }

    // Función register: define la lógica de esta operación
    fun register(nombre: String, correo: String, telefono: String, contrasena: String) {
        _authState.value = AuthState.Loading
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante response: valor inmutable que no cambia tras su asignación
                val response = repo.register(nombre, correo, telefono, contrasena)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (response.isSuccessful && response.body() != null) {
                    _authState.value = AuthState.Success(response.body()!!)
                } else {
                    _authState.value = AuthState.Error("Error al registrarse")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error de conexión: ${e.message}")
            }
        }
    }
}
```

### Paso 11.2: `LoginFragment.kt`

**Fragment de inicio de sesión**. Presenta el formulario de login, valida las entradas del usuario y delega la autenticación al AuthViewModel.

```kotlin
// Paquete: com.lomito.seguro.ui.auth
package com.lomito.seguro.ui.auth

// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: LayoutInflater
import android.view.LayoutInflater
// Importa componentes de la interfaz gráfica
import android.view.View
// Importa componentes de la interfaz gráfica
import android.view.ViewGroup
// Importa la dependencia necesaria: Toast
import android.widget.Toast
// Importa la clase Fragment de AndroidX
import androidx.fragment.app.Fragment
// Importa la dependencia necesaria: viewModels
import androidx.fragment.app.viewModels
// Importa componente de navegación
import androidx.navigation.fragment.findNavController
// Importa la dependencia necesaria: MainActivity
import com.lomito.seguro.MainActivity
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa la clase Fragment de AndroidX
import com.lomito.seguro.databinding.FragmentLoginBinding
// Importa la dependencia necesaria: SessionManager
import com.lomito.seguro.util.SessionManager

/**
 * [Fragmento de inicio de sesión]
 *
 * Responsabilidades:
 * - [Capturar las credenciales del usuario]
 * - [Realizar la petición de login y guardar la sesión si es exitosa]
 */
// Fragment LoginFragment: componente de UI que representa una sección de la pantalla
class LoginFragment : Fragment() {
    // Variable _binding: almacena el estado mutable de este componente
    private var _binding: FragmentLoginBinding? = null
    // Constante binding: valor inmutable que no cambia tras su asignación
    private val binding get() = _binding!!
    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    // Infla el layout del Fragment y retorna la vista raíz
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        // Accede a un componente de UI a través del View Binding type-safe
        return binding.root
    }

    // Se llama cuando la vista del Fragment está lista; se inicializa la UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        // Accede a un componente de UI a través del View Binding type-safe
        binding.btnLogin.setOnClickListener {
            // Constante correo: valor inmutable que no cambia tras su asignación
            val correo = binding.etCorreo.text.toString()
            // Constante contrasena: valor inmutable que no cambia tras su asignación
            val contrasena = binding.etContrasena.text.toString()

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (correo.isEmpty() || contrasena.isEmpty()) {
                // Muestra un mensaje emergente breve al usuario
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Actualiza el componente de UI a través del View Binding
            binding.progressBar.visibility = View.VISIBLE
            // Actualiza el componente de UI a través del View Binding
            binding.btnLogin.isEnabled = false
            // Actualiza el componente de UI a través del View Binding
            binding.btnLogin.text = ""

            viewModel.login(correo, contrasena)
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
            when (state) {
                is AuthState.Loading -> {
                    // Ya mostramos loading
                }
                is AuthState.Success -> {
                    // Actualiza el componente de UI a través del View Binding
                    binding.progressBar.visibility = View.GONE
                    // Actualiza el componente de UI a través del View Binding
                    binding.btnLogin.isEnabled = true
                    // Actualiza el componente de UI a través del View Binding
                    binding.btnLogin.text = "Iniciar sesión"

                    // Constante usuario: valor inmutable que no cambia tras su asignación
                    val usuario = state.usuario

                    // ✅ Guardar en SessionManager
                    sessionManager.saveUser(
                        id = usuario.id,
                        nombre = usuario.nombre,
                        correo = usuario.correo
                    )

                    // ✅ Enviar userId al watch
                    // Constante activity: valor inmutable que no cambia tras su asignación
                    val activity = requireActivity() as? MainActivity
                    activity?.actualizarUserIdEnWatch(usuario.id)

                    // Muestra un mensaje emergente breve al usuario
                    Toast.makeText(requireContext(), "Bienvenido ${usuario.nombre}", Toast.LENGTH_SHORT).show()

                    // ✅ Usar el ID correcto del nav_graph
                    // Navega hacia el destino especificado en el grafo de navegación
                    findNavController().navigate(R.id.action_login_to_home)
                }
                is AuthState.Error -> {
                    // Actualiza el componente de UI a través del View Binding
                    binding.progressBar.visibility = View.GONE
                    // Actualiza el componente de UI a través del View Binding
                    binding.btnLogin.isEnabled = true
                    // Actualiza el componente de UI a través del View Binding
                    binding.btnLogin.text = "Iniciar sesión"
                    // Muestra un mensaje emergente breve al usuario
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Accede a un componente de UI a través del View Binding type-safe
        binding.tvRegistro.setOnClickListener {
            // ✅ Usar el ID correcto del nav_graph
            // Navega hacia el destino especificado en el grafo de navegación
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    // Sobreescribe la función onDestroyView de la clase padre
    override fun onDestroyView() {
        // Invoca la implementación del método en la clase padre
        super.onDestroyView()
        _binding = null
    }
}
```

### Paso 11.3: `RegisterFragment.kt`

**Fragment de registro**. Muestra el formulario de creación de cuenta nueva, valida los datos y coordina el registro a través del AuthViewModel.

```kotlin
// Paquete: com.lomito.seguro.ui.auth
package com.lomito.seguro.ui.auth

// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: LayoutInflater
import android.view.LayoutInflater
// Importa componentes de la interfaz gráfica
import android.view.View
// Importa componentes de la interfaz gráfica
import android.view.ViewGroup
// Importa la dependencia necesaria: Toast
import android.widget.Toast
// Importa la clase Fragment de AndroidX
import androidx.fragment.app.Fragment
// Importa la dependencia necesaria: viewModels
import androidx.fragment.app.viewModels
// Importa componente de navegación
import androidx.navigation.fragment.findNavController
// Importa la dependencia necesaria: MainActivity
import com.lomito.seguro.MainActivity
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa la clase Fragment de AndroidX
import com.lomito.seguro.databinding.FragmentRegisterBinding
// Importa la dependencia necesaria: SessionManager
import com.lomito.seguro.util.SessionManager

// Fragment RegisterFragment: componente de UI que representa una sección de la pantalla
class RegisterFragment : Fragment() {
    // Variable _binding: almacena el estado mutable de este componente
    private var _binding: FragmentRegisterBinding? = null
    // Constante binding: valor inmutable que no cambia tras su asignación
    private val binding get() = _binding!!
    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    // Infla el layout del Fragment y retorna la vista raíz
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        // Accede a un componente de UI a través del View Binding type-safe
        return binding.root
    }

    // Se llama cuando la vista del Fragment está lista; se inicializa la UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        // Accede a un componente de UI a través del View Binding type-safe
        binding.btnRegistrar.setOnClickListener {
            // Constante nombre: valor inmutable que no cambia tras su asignación
            val nombre = binding.etNombre.text.toString()
            // Constante correo: valor inmutable que no cambia tras su asignación
            val correo = binding.etCorreo.text.toString()
            // Constante telefono: valor inmutable que no cambia tras su asignación
            val telefono = binding.etTelefono.text.toString()
            // Constante contrasena: valor inmutable que no cambia tras su asignación
            val contrasena = binding.etContrasena.text.toString()

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (nombre.isEmpty() || correo.isEmpty() || contrasena.isEmpty()) {
                // Muestra un mensaje emergente breve al usuario
                Toast.makeText(requireContext(), "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Actualiza el componente de UI a través del View Binding
            binding.progressBar.visibility = View.VISIBLE
            // Actualiza el componente de UI a través del View Binding
            binding.btnRegistrar.isEnabled = false
            // Actualiza el componente de UI a través del View Binding
            binding.btnRegistrar.text = ""

            viewModel.register(nombre, correo, telefono, contrasena)
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
            when (state) {
                is AuthState.Loading -> {
                    // Ya mostramos loading
                }
                is AuthState.Success -> {
                    // Actualiza el componente de UI a través del View Binding
                    binding.progressBar.visibility = View.GONE
                    // Actualiza el componente de UI a través del View Binding
                    binding.btnRegistrar.isEnabled = true
                    // Actualiza el componente de UI a través del View Binding
                    binding.btnRegistrar.text = "Crear cuenta"

                    // Constante usuario: valor inmutable que no cambia tras su asignación
                    val usuario = state.usuario

                    // ✅ Guardar en SessionManager
                    sessionManager.saveUser(
                        id = usuario.id,
                        nombre = usuario.nombre,
                        correo = usuario.correo
                    )

                    // ✅ Enviar userId al watch
                    // Constante activity: valor inmutable que no cambia tras su asignación
                    val activity = requireActivity() as? MainActivity
                    activity?.actualizarUserIdEnWatch(usuario.id)

                    // Muestra un mensaje emergente breve al usuario
                    Toast.makeText(requireContext(), "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()

                    // ✅ Usar el ID correcto del nav_graph (ir al home directamente)
                    // Navega hacia el destino especificado en el grafo de navegación
                    findNavController().navigate(R.id.action_register_to_home)
                }
                is AuthState.Error -> {
                    // Actualiza el componente de UI a través del View Binding
                    binding.progressBar.visibility = View.GONE
                    // Actualiza el componente de UI a través del View Binding
                    binding.btnRegistrar.isEnabled = true
                    // Actualiza el componente de UI a través del View Binding
                    binding.btnRegistrar.text = "Crear cuenta"
                    // Muestra un mensaje emergente breve al usuario
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Accede a un componente de UI a través del View Binding type-safe
        binding.tvLogin.setOnClickListener {
            // ✅ Ir al login
            // Navega hacia el destino especificado en el grafo de navegación
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    // Sobreescribe la función onDestroyView de la clase padre
    override fun onDestroyView() {
        // Invoca la implementación del método en la clase padre
        super.onDestroyView()
        _binding = null
    }
}
```

## FASE 12: `com/lomito/seguro/ui/home`

### Paso 12.1: `HomeFragment.kt`

**Fragment principal (Home)**. Pantalla principal tras el login. Muestra las mascotas registradas del usuario, accesos rápidos y el estado general de la app.

```kotlin
// Paquete: com.lomito.seguro.ui.home
package com.lomito.seguro.ui.home

// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: *
import android.view.*
// Importa la clase Fragment de AndroidX
import androidx.fragment.app.Fragment
// Importa la dependencia necesaria: viewModels
import androidx.fragment.app.viewModels
// Importa componente de navegación
import androidx.navigation.fragment.findNavController
// Importa la dependencia necesaria: LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa la clase Fragment de AndroidX
import com.lomito.seguro.databinding.FragmentHomeBinding
// Importa la dependencia necesaria: SessionManager
import com.lomito.seguro.util.SessionManager
// Importa la dependencia necesaria: gone
import com.lomito.seguro.util.gone
// Importa la dependencia necesaria: visible
import com.lomito.seguro.util.visible

/**
 * [Fragmento principal (Home) de la aplicación]
 *
 * Responsabilidades:
 * - [Mostrar la lista de mascotas del usuario logueado]
 * - [Ofrecer navegación a las diferentes secciones de la app]
 */
// Fragment HomeFragment: componente de UI que representa una sección de la pantalla
class HomeFragment : Fragment() {
    // Variable _binding: almacena el estado mutable de este componente
    private var _binding: FragmentHomeBinding? = null
    // Constante binding: valor inmutable que no cambia tras su asignación
    private val binding get() = _binding!!
    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var session: SessionManager
    private lateinit var adapter: MascotaCardAdapter

    // Infla el layout del Fragment y retorna la vista raíz
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        // Accede a un componente de UI a través del View Binding type-safe
        return binding.root
    }

    // Se llama cuando la vista del Fragment está lista; se inicializa la UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        // ✅ Configurar RecyclerView
        // Asigna el adaptador al RecyclerView para mostrar la lista de datos
        adapter = MascotaCardAdapter { mascota ->
            // Constante bundle: valor inmutable que no cambia tras su asignación
            val bundle = android.os.Bundle().apply { putString("mascotaId", mascota.id) }
            // Navega hacia el destino especificado en el grafo de navegación
            findNavController().navigate(R.id.action_home_to_mascota_detail, bundle)
        }
        // Define cómo se organizan visualmente los elementos del RecyclerView
        binding.rvMascotas.layoutManager = LinearLayoutManager(requireContext())
        // Asigna el adaptador al RecyclerView para mostrar la lista de datos
        binding.rvMascotas.adapter = adapter

        // ✅ Configurar FAB
        // Accede a un componente de UI a través del View Binding type-safe
        binding.fabAgregarMascota.setOnClickListener {
            // Navega hacia el destino especificado en el grafo de navegación
            findNavController().navigate(R.id.action_home_to_crear_mascota)
        }

        // ✅ Configurar botones de acciones rápidas
        // Accede a un componente de UI a través del View Binding type-safe
        binding.btnAlertas.setOnClickListener {
            // Navega hacia el destino especificado en el grafo de navegación
            findNavController().navigate(R.id.action_home_to_alertas)
        }

        // Accede a un componente de UI a través del View Binding type-safe
        binding.btnRefugios.setOnClickListener {
            // Navega hacia el destino especificado en el grafo de navegación
            findNavController().navigate(R.id.action_home_to_refugios)
        }

        // Accede a un componente de UI a través del View Binding type-safe
        binding.btnSimulador.setOnClickListener {
            // Navega hacia el destino especificado en el grafo de navegación
            findNavController().navigate(R.id.action_home_to_simulator)
        }

        // Accede a un componente de UI a través del View Binding type-safe
        binding.btnMural.setOnClickListener {
            // Navega hacia el destino especificado en el grafo de navegación
            findNavController().navigate(R.id.action_home_to_mural)
        }

        // ✅ Observar datos
        viewModel.mascotas.observe(viewLifecycleOwner) { mascotas ->
            adapter.submitList(mascotas)
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (mascotas.isEmpty()) {
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvEmpty.visible()
                // Accede a un componente de UI a través del View Binding type-safe
                binding.ivEmpty.visible()
            } else {
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvEmpty.gone()
                // Accede a un componente de UI a través del View Binding type-safe
                binding.ivEmpty.gone()
            }
        }

        viewModel.alertasNoLeidas.observe(viewLifecycleOwner) { count ->
            // Actualiza el componente de UI a través del View Binding
            binding.badgeAlertas.text = if (count > 0) count.toString() else ""
            // Actualiza el componente de UI a través del View Binding
            binding.badgeAlertas.visibility = if (count > 0) View.VISIBLE else View.GONE
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            // Actualiza el componente de UI a través del View Binding
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.cargar(session.getUserId())
    }

    // Sobreescribe la función onDestroyView de la clase padre
    override fun onDestroyView() {
        // Invoca la implementación del método en la clase padre
        super.onDestroyView()
        _binding = null
    }
}
```

### Paso 12.2: `HomeViewModel.kt`

**ViewModel del Home**. Gestiona el estado de la pantalla principal: lista de mascotas, estadísticas y coordina las peticiones al repositorio.

```kotlin
// Paquete: com.lomito.seguro.ui.home
package com.lomito.seguro.ui.home

// Importa el observable de datos reactivos
import androidx.lifecycle.LiveData
// Importa el observable de datos reactivos
import androidx.lifecycle.MutableLiveData
// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModel
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa la dependencia necesaria: Alerta
import com.lomito.seguro.data.model.Alerta
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.data.model.Mascota
// Importa la dependencia necesaria: LomitoRepository
import com.lomito.seguro.data.repository.LomitoRepository
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch

// ViewModel HomeViewModel: gestiona el estado y la lógica de negocio de la pantalla
class HomeViewModel : ViewModel() {
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo = LomitoRepository()

    // Constante _mascotas: valor inmutable que no cambia tras su asignación
    private val _mascotas = MutableLiveData<List<Mascota>>()
    // Constante mascotas: valor inmutable que no cambia tras su asignación
    val mascotas: LiveData<List<Mascota>> = _mascotas

    // Constante _alertasNoLeidas: valor inmutable que no cambia tras su asignación
    private val _alertasNoLeidas = MutableLiveData<Int>()
    // Constante alertasNoLeidas: valor inmutable que no cambia tras su asignación
    val alertasNoLeidas: LiveData<Int> = _alertasNoLeidas

    // Constante _loading: valor inmutable que no cambia tras su asignación
    private val _loading = MutableLiveData<Boolean>()
    // Constante loading: valor inmutable que no cambia tras su asignación
    val loading: LiveData<Boolean> = _loading

    // Función cargar: define la lógica de esta operación
    fun cargar(ownerId: String) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _loading.value = true
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante mResp: valor inmutable que no cambia tras su asignación
                val mResp = repo.getMascotas(ownerId)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (mResp.isSuccessful) _mascotas.value = mResp.body() ?: emptyList()

                // Constante aResp: valor inmutable que no cambia tras su asignación
                val aResp = repo.getAlertasNoLeidas(ownerId)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (aResp.isSuccessful) _alertasNoLeidas.value = aResp.body()?.size ?: 0
            } catch (e: Exception) {
                _mascotas.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}
```

### Paso 12.3: `MascotaCardAdapter.kt`

**Adaptador de tarjetas de mascotas**. Muestra cada mascota como una tarjeta en la cuadrícula del Home, con foto, nombre y estado.

```kotlin
// Paquete: com.lomito.seguro.ui.home
package com.lomito.seguro.ui.home

// Importa la dependencia necesaria: LayoutInflater
import android.view.LayoutInflater
// Importa componentes de la interfaz gráfica
import android.view.ViewGroup
// Importa el contexto de Android
import androidx.core.content.ContextCompat
// Importa la dependencia necesaria: DiffUtil
import androidx.recyclerview.widget.DiffUtil
// Importa la dependencia necesaria: ListAdapter
import androidx.recyclerview.widget.ListAdapter
// Importa componentes de la interfaz gráfica
import androidx.recyclerview.widget.RecyclerView
// Importa la librería de carga de imágenes
import com.bumptech.glide.Glide
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.data.model.Mascota
// Importa el sistema de View Binding
import com.lomito.seguro.databinding.ItemMascotaCardBinding
// Importa la dependencia necesaria: toAbsoluteUrl
import com.lomito.seguro.util.toAbsoluteUrl

// Adaptador MascotaCardAdapter: conecta los datos con la vista del RecyclerView
class MascotaCardAdapter(
    // Constante onClick: valor inmutable que no cambia tras su asignación
    private val onClick: (Mascota) -> Unit
) : ListAdapter<Mascota, MascotaCardAdapter.VH>(DiffCallback()) {

    // Accede a un componente de UI a través del View Binding type-safe
    inner class VH(val binding: ItemMascotaCardBinding) : RecyclerView.ViewHolder(binding.root) {
        // Función bind: define la lógica de esta operación
        fun bind(mascota: Mascota) {
            // Actualiza el componente de UI a través del View Binding
            binding.tvNombre.text = mascota.nombre
            // Actualiza el componente de UI a través del View Binding
            binding.tvEspecie.text = "${mascota.especie} • ${mascota.raza}"
            // Actualiza el componente de UI a través del View Binding
            binding.tvEstado.text = when (mascota.estado) {
                "EN_CASA" -> "✅ En casa"
                "PERDIDA" -> "🚨 Perdida"
                "ENCONTRADA" -> "✅ Encontrada"
                else -> mascota.estado
            }
            // Constante estadoColor: valor inmutable que no cambia tras su asignación
            val estadoColor = when (mascota.estado) {
                "PERDIDA" -> R.color.alerta_rojo
                "ENCONTRADA" -> R.color.verde_ok
                else -> R.color.primary
            }
            // Accede a un componente de UI a través del View Binding type-safe
            binding.tvEstado.setTextColor(ContextCompat.getColor(binding.root.context, estadoColor))

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (!mascota.fotoUrl.isNullOrEmpty()) {
                // Constante iv: valor inmutable que no cambia tras su asignación
                val iv = binding.ivMascota
                iv.imageTintList = null
                iv.setPadding(0, 0, 0, 0)
                // Accede a un componente de UI a través del View Binding type-safe
                Glide.with(binding.root).load(mascota.fotoUrl.toAbsoluteUrl())
                    .placeholder(R.drawable.ic_pet_placeholder)
                    .circleCrop()
                    .into(iv)
            } else {
                // Constante iv: valor inmutable que no cambia tras su asignación
                val iv = binding.ivMascota
                // Constante pad: valor inmutable que no cambia tras su asignación
                val pad = (8 * binding.root.resources.displayMetrics.density).toInt()
                iv.setPadding(pad, pad, pad, pad)
                // Actualiza el componente de UI a través del View Binding
                iv.imageTintList = ContextCompat.getColorStateList(binding.root.context, R.color.white)
                iv.setImageResource(
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (mascota.especie == "GATO") R.drawable.ic_cat else R.drawable.ic_dog
                )
            }
            // Accede a un componente de UI a través del View Binding type-safe
            binding.root.setOnClickListener { onClick(mascota) }
        }
    }

    // Infla el layout y crea el ViewHolder para el RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemMascotaCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    // Vincula los datos del modelo con las vistas del ViewHolder
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    // Declaración de la clase DiffCallback
    class DiffCallback : DiffUtil.ItemCallback<Mascota>() {
        // Sobreescribe la función areItemsTheSame de la clase padre
        override fun areItemsTheSame(a: Mascota, b: Mascota) = a.id == b.id
        // Sobreescribe la función areContentsTheSame de la clase padre
        override fun areContentsTheSame(a: Mascota, b: Mascota) = a == b
    }
}
```

## FASE 13: `com/lomito/seguro/ui/mascota`

### Paso 13.1: `CrearMascotaFragment.kt`

**Fragment de creación de mascota**. Formulario para registrar una nueva mascota con nombre, especie, foto y otros detalles.

```kotlin
// Paquete: com.lomito.seguro.ui.mascota
package com.lomito.seguro.ui.mascota

// Importa la dependencia necesaria: Uri
import android.net.Uri
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: *
import android.view.*
// Importa la dependencia necesaria: ArrayAdapter
import android.widget.ArrayAdapter
// Importa la dependencia necesaria: ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts
// Importa la clase Fragment de AndroidX
import androidx.fragment.app.Fragment
// Importa la dependencia necesaria: viewModels
import androidx.fragment.app.viewModels
// Importa componente de navegación
import androidx.navigation.fragment.findNavController
// Importa la librería de carga de imágenes
import com.bumptech.glide.Glide
// Importa la dependencia necesaria: CreateMascotaRequest
import com.lomito.seguro.data.model.CreateMascotaRequest
// Importa la clase Fragment de AndroidX
import com.lomito.seguro.databinding.FragmentCrearMascotaBinding
// Importa la dependencia necesaria: SessionManager
import com.lomito.seguro.util.SessionManager
// Importa la dependencia necesaria: gone
import com.lomito.seguro.util.gone
// Importa la dependencia necesaria: toAbsoluteUrl
import com.lomito.seguro.util.toAbsoluteUrl
// Importa la dependencia necesaria: toast
import com.lomito.seguro.util.toast
// Importa la dependencia necesaria: visible
import com.lomito.seguro.util.visible

// Fragment CrearMascotaFragment: componente de UI que representa una sección de la pantalla
class CrearMascotaFragment : Fragment() {
    // Variable _binding: almacena el estado mutable de este componente
    private var _binding: FragmentCrearMascotaBinding? = null
    // Constante binding: valor inmutable que no cambia tras su asignación
    private val binding get() = _binding!!
    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: MascotaViewModel by viewModels()
    private lateinit var session: SessionManager
    // Variable fotoUri: almacena el estado mutable de este componente
    private var fotoUri: Uri? = null
    // Variable datosPrecargados: almacena el estado mutable de este componente
    private var datosPrecargados = false

    // Si viene un mascotaId, estamos editando; si no, estamos creando
    // Constante mascotaId: valor inmutable que no cambia tras su asignación
    private val mascotaId: String? by lazy { arguments?.getString("mascotaId") }
    // Constante esEdicion: valor inmutable que no cambia tras su asignación
    private val esEdicion get() = mascotaId != null

    // Constante pickMedia: valor inmutable que no cambia tras su asignación
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (uri != null) {
            fotoUri = uri
            // Accede a un componente de UI a través del View Binding type-safe
            Glide.with(this).load(uri).into(binding.ivFoto)
        }
    }

    // Infla el layout del Fragment y retorna la vista raíz
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCrearMascotaBinding.inflate(inflater, container, false)
        // Accede a un componente de UI a través del View Binding type-safe
        return binding.root
    }

    // Se llama cuando la vista del Fragment está lista; se inicializa la UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        // Constante especies: valor inmutable que no cambia tras su asignación
        val especies = arrayOf("PERRO", "GATO")
        // Constante especieAdapter: valor inmutable que no cambia tras su asignación
        val especieAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, especies)
        especieAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Asigna el adaptador al RecyclerView para mostrar la lista de datos
        binding.spinnerEspecie.adapter = especieAdapter

        // Constante elegirFoto: valor inmutable que no cambia tras su asignación
        val elegirFoto = {
            pickMedia.launch(androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            ))
        }
        // Accede a un componente de UI a través del View Binding type-safe
        binding.ivFoto.setOnClickListener { elegirFoto() }
        // Accede a un componente de UI a través del View Binding type-safe
        binding.btnElegirFoto.setOnClickListener { elegirFoto() }

        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (esEdicion) {
            // Actualiza el componente de UI a través del View Binding
            binding.tvTitulo.text = "Editar mascota"
            // Actualiza el componente de UI a través del View Binding
            binding.btnGuardar.text = "Guardar cambios"
            (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = "Editar mascota"
            viewModel.cargarMascota(mascotaId!!)

            viewModel.mascota.observe(viewLifecycleOwner) { mascota ->
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (mascota == null || datosPrecargados) return@observe
                datosPrecargados = true

                // Accede a un componente de UI a través del View Binding type-safe
                binding.etNombre.setText(mascota.nombre)
                // Accede a un componente de UI a través del View Binding type-safe
                binding.etRaza.setText(mascota.raza)
                // Accede a un componente de UI a través del View Binding type-safe
                binding.etColor.setText(mascota.color)
                // Accede a un componente de UI a través del View Binding type-safe
                binding.etEdad.setText(mascota.edad.toString())
                // Accede a un componente de UI a través del View Binding type-safe
                binding.etPeso.setText(mascota.peso.toString())
                // Accede a un componente de UI a través del View Binding type-safe
                binding.etUmbral.setText(mascota.distanciaAlerta.toString())
                // Constante especieIndex: valor inmutable que no cambia tras su asignación
                val especieIndex = especies.indexOf(mascota.especie)
                // Actualiza el componente de UI a través del View Binding
                if (especieIndex >= 0) binding.spinnerEspecie.setSelection(especieIndex)

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (!mascota.fotoUrl.isNullOrEmpty()) {
                    // Accede a un componente de UI a través del View Binding type-safe
                    Glide.with(this).load(mascota.fotoUrl.toAbsoluteUrl()).into(binding.ivFoto)
                }
            }
        }

        // Accede a un componente de UI a través del View Binding type-safe
        binding.btnGuardar.setOnClickListener {
            // Constante nombre: valor inmutable que no cambia tras su asignación
            val nombre = binding.etNombre.text.toString().trim()
            // Constante raza: valor inmutable que no cambia tras su asignación
            val raza = binding.etRaza.text.toString().trim()
            // Constante color: valor inmutable que no cambia tras su asignación
            val color = binding.etColor.text.toString().trim()
            // Constante edadStr: valor inmutable que no cambia tras su asignación
            val edadStr = binding.etEdad.text.toString().trim()
            // Constante pesoStr: valor inmutable que no cambia tras su asignación
            val pesoStr = binding.etPeso.text.toString().trim()
            // Constante umbralStr: valor inmutable que no cambia tras su asignación
            val umbralStr = binding.etUmbral.text.toString().trim()

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (nombre.isEmpty()) {
                toast("El nombre es requerido")
                return@setOnClickListener
            }

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (esEdicion) {
                // Constante datos: valor inmutable que no cambia tras su asignación
                val datos = mapOf(
                    "nombre" to nombre,
                    // Accede a un componente de UI a través del View Binding type-safe
                    "especie" to binding.spinnerEspecie.selectedItem.toString(),
                    "raza" to raza,
                    "color" to color,
                    "edad" to (edadStr.toIntOrNull() ?: 0),
                    "peso" to (pesoStr.toDoubleOrNull() ?: 0.0),
                    "distancia_alerta" to (umbralStr.toIntOrNull() ?: 50)
                )
                viewModel.actualizarMascotaConFoto(requireContext(), mascotaId!!, datos, fotoUri) {
                    findNavController().navigateUp()
                }
            } else {
                viewModel.crearMascotaConFoto(
                    requireContext(),
                    CreateMascotaRequest(
                        nombre = nombre,
                        // Actualiza el componente de UI a través del View Binding
                        especie = binding.spinnerEspecie.selectedItem.toString(),
                        ownerId = session.getUserId(),
                        raza = raza,
                        color = color,
                        edad = edadStr.toIntOrNull() ?: 0,
                        peso = pesoStr.toDoubleOrNull() ?: 0.0,
                        distanciaAlerta = umbralStr.toIntOrNull() ?: 50
                    ),
                    fotoUri
                ) {
                    findNavController().navigateUp()
                }
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            // Accede a un componente de UI a través del View Binding type-safe
            if (loading) binding.progressBar.visible() else binding.progressBar.gone()
        }

        viewModel.message.observe(viewLifecycleOwner) { msg ->
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (msg.isNotEmpty()) toast(msg)
        }
    }

    // Sobreescribe la función onDestroyView de la clase padre
    override fun onDestroyView() {
        // Invoca la implementación del método en la clase padre
        super.onDestroyView()
        _binding = null
    }
}
```

### Paso 13.2: `MascotaDetailFragment.kt`

**Fragment de detalle de mascota**. Muestra la información completa de una mascota: fotos, historial de avistamientos, estado y acciones disponibles.

```kotlin
// Paquete: com.lomito.seguro.ui.mascota
package com.lomito.seguro.ui.mascota

// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: *
import android.view.*
// Importa la dependencia necesaria: AlertDialog
import androidx.appcompat.app.AlertDialog
// Importa la dependencia necesaria: MenuProvider
import androidx.core.view.MenuProvider
// Importa la clase Fragment de AndroidX
import androidx.fragment.app.Fragment
// Importa la dependencia necesaria: viewModels
import androidx.fragment.app.viewModels
// Importa la dependencia necesaria: Lifecycle
import androidx.lifecycle.Lifecycle
// Importa componente de navegación
import androidx.navigation.fragment.findNavController
// Importa la librería de carga de imágenes
import com.bumptech.glide.Glide
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa la clase Fragment de AndroidX
import com.lomito.seguro.databinding.FragmentMascotaDetailBinding
// Importa la dependencia necesaria: gone
import com.lomito.seguro.util.gone
// Importa la dependencia necesaria: toAbsoluteUrl
import com.lomito.seguro.util.toAbsoluteUrl
// Importa la dependencia necesaria: toast
import com.lomito.seguro.util.toast
// Importa la dependencia necesaria: visible
import com.lomito.seguro.util.visible

/**
 * [Fragmento que muestra el detalle de una mascota]
 *
 * Responsabilidades:
 * - [Cargar y mostrar los datos específicos de la mascota]
 * - [Permitir editar o eliminar la mascota desde el menú superior]
 */
// Fragment MascotaDetailFragment: componente de UI que representa una sección de la pantalla
class MascotaDetailFragment : Fragment() {
    // Variable _binding: almacena el estado mutable de este componente
    private var _binding: FragmentMascotaDetailBinding? = null
    // Constante binding: valor inmutable que no cambia tras su asignación
    private val binding get() = _binding!!
    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: MascotaViewModel by viewModels()
    // Constante mascotaId: valor inmutable que no cambia tras su asignación
    private val mascotaId: String by lazy {
        arguments?.getString("mascotaId") ?: ""
    }
    // Infla el layout del Fragment y retorna la vista raíz
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMascotaDetailBinding.inflate(inflater, container, false)
        // Accede a un componente de UI a través del View Binding type-safe
        return binding.root
    }

    // Se llama cuando la vista del Fragment está lista; se inicializa la UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onViewCreated(view, savedInstanceState)

        // ✅ Editar / Eliminar en la barra superior única de la app (MenuProvider,
        // se agrega y quita automáticamente según el ciclo de vida de este fragmento)
        requireActivity().addMenuProvider(object : MenuProvider {
            // Sobreescribe la función onCreateMenu de la clase padre
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.mascota_menu, menu)
            }

            // Sobreescribe la función onMenuItemSelected de la clase padre
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Retorna el valor al llamador de la función
                return when (menuItem.itemId) {
                    R.id.action_edit -> {
                        // Constante bundle: valor inmutable que no cambia tras su asignación
                        val bundle = Bundle().apply { putString("mascotaId", mascotaId) }
                        // Navega hacia el destino especificado en el grafo de navegación
                        findNavController().navigate(R.id.action_mascota_detail_to_editar, bundle)
                        true
                    }
                    R.id.action_delete -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Eliminar mascota")
                            .setMessage("¿Seguro que deseas eliminar esta mascota?")
                            .setPositiveButton("Eliminar") { _, _ ->
                                viewModel.eliminarMascota(mascotaId) {
                                    findNavController().navigateUp()
                                }
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        viewModel.cargarMascota(mascotaId)

        viewModel.mascota.observe(viewLifecycleOwner) { mascota ->
            mascota ?: return@observe
            // Actualiza el componente de UI a través del View Binding
            binding.tvNombre.text = mascota.nombre
            // Actualiza el componente de UI a través del View Binding
            binding.tvEspecie.text = "${mascota.especie} • ${mascota.raza}"
            // Actualiza el componente de UI a través del View Binding
            binding.tvEdad.text = "${mascota.edad} años"
            // Actualiza el componente de UI a través del View Binding
            binding.tvColor.text = "Color: ${mascota.color}"
            // Actualiza el componente de UI a través del View Binding
            binding.tvPeso.text = "Peso: ${mascota.peso} kg"
            // Actualiza el componente de UI a través del View Binding
            binding.tvEstado.text = when (mascota.estado) {
                "EN_CASA" -> "✅ En casa"
                "PERDIDA" -> "🚨 ¡Perdida!"
                "ENCONTRADA" -> "✅ Encontrada"
                else -> mascota.estado
            }
            // Actualiza el componente de UI a través del View Binding
            binding.tvUmbral.text = "Umbral BLE: ${mascota.distanciaAlerta}m"

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (!mascota.fotoUrl.isNullOrEmpty()) {
                Glide.with(this).load(mascota.fotoUrl.toAbsoluteUrl())
                    // Accede a un componente de UI a través del View Binding type-safe
                    .placeholder(R.drawable.ic_pet_placeholder).into(binding.ivMascota)
            }

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (mascota.latitud != null && mascota.longitud != null) {
                // Actualiza el componente de UI a través del View Binding
                binding.tvUbicacion.text = "Última ubicación: ${String.format("%.4f", mascota.latitud)}, ${String.format("%.4f", mascota.longitud)}"
            } else {
                // Actualiza el componente de UI a través del View Binding
                binding.tvUbicacion.text = "Sin ubicación registrada"
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            // Accede a un componente de UI a través del View Binding type-safe
            if (loading) binding.progressBar.visible() else binding.progressBar.gone()
        }

        viewModel.message.observe(viewLifecycleOwner) { msg ->
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (msg.isNotEmpty()) toast(msg)
        }
    }

    // Sobreescribe la función onDestroyView de la clase padre
    override fun onDestroyView() {
        // Invoca la implementación del método en la clase padre
        super.onDestroyView()
        _binding = null
    }
}
```

### Paso 13.3: `MascotaViewModel.kt`

**ViewModel de mascota**. Gestiona el estado CRUD de mascotas: creación, edición, eliminación y carga de datos desde el repositorio.

```kotlin
// Paquete: com.lomito.seguro.ui.mascota
package com.lomito.seguro.ui.mascota

// Importa el contexto de Android
import android.content.Context
// Importa la dependencia necesaria: Uri
import android.net.Uri
// Importa la dependencia necesaria: *
import androidx.lifecycle.*
// Importa la dependencia necesaria: CreateMascotaRequest
import com.lomito.seguro.data.model.CreateMascotaRequest
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.data.model.Mascota
// Importa la dependencia necesaria: ReporteVista
import com.lomito.seguro.data.model.ReporteVista
// Importa la dependencia necesaria: LomitoRepository
import com.lomito.seguro.data.repository.LomitoRepository
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.Dispatchers
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.withContext
// Importa la dependencia necesaria: toMediaTypeOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
// Importa la dependencia necesaria: MultipartBody
import okhttp3.MultipartBody
// Importa la dependencia necesaria: asRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
// Importa la dependencia necesaria: File
import java.io.File

// ViewModel MascotaViewModel: gestiona el estado y la lógica de negocio de la pantalla
class MascotaViewModel : ViewModel() {
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo = LomitoRepository()

    // Constante _mascota: valor inmutable que no cambia tras su asignación
    private val _mascota = MutableLiveData<Mascota?>()
    // Constante mascota: valor inmutable que no cambia tras su asignación
    val mascota: LiveData<Mascota?> = _mascota

    // Constante _reportes: valor inmutable que no cambia tras su asignación
    private val _reportes = MutableLiveData<List<ReporteVista>>()
    // Constante reportes: valor inmutable que no cambia tras su asignación
    val reportes: LiveData<List<ReporteVista>> = _reportes

    // Constante _loading: valor inmutable que no cambia tras su asignación
    private val _loading = MutableLiveData<Boolean>()
    // Constante loading: valor inmutable que no cambia tras su asignación
    val loading: LiveData<Boolean> = _loading

    // Constante _message: valor inmutable que no cambia tras su asignación
    private val _message = MutableLiveData<String>()
    // Constante message: valor inmutable que no cambia tras su asignación
    val message: LiveData<String> = _message

    // Función cargarMascota: define la lógica de esta operación
    fun cargarMascota(id: String) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _loading.value = true
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante resp: valor inmutable que no cambia tras su asignación
                val resp = repo.getMascotaById(id)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (resp.isSuccessful) _mascota.value = resp.body()
                // Constante rResp: valor inmutable que no cambia tras su asignación
                val rResp = repo.getUltimoReporte(id)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (rResp.isSuccessful) _reportes.value = listOfNotNull(rResp.body())
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // Función reportarVista: define la lógica de esta operación
    fun reportarVista(mascotaId: String, lat: Double, lng: Double) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante resp: valor inmutable que no cambia tras su asignación
                val resp = repo.reportarVista(mascotaId, lat, lng, "Ubicación reportada desde Watch")
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (resp.isSuccessful) _message.value = "✅ Vista reportada exitosamente"
                else _message.value = "Error al reportar vista"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }

    // Función crearMascota: define la lógica de esta operación
    fun crearMascota(request: CreateMascotaRequest, onSuccess: () -> Unit) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _loading.value = true
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante resp: valor inmutable que no cambia tras su asignación
                val resp = repo.createMascota(request)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (resp.isSuccessful) {
                    _message.value = "✅ Mascota registrada"
                    onSuccess()
                } else {
                    _message.value = "Error al crear mascota"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // Sube la foto elegida (si hay) y luego crea la mascota con el foto_url resultante
    // Función crearMascotaConFoto: define la lógica de esta operación
    fun crearMascotaConFoto(context: Context, request: CreateMascotaRequest, fotoUri: Uri?, onSuccess: () -> Unit) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _loading.value = true
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Variable fotoUrl: almacena el estado mutable de este componente
                var fotoUrl: String? = null

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (fotoUri != null) {
                    // Constante tempFile: valor inmutable que no cambia tras su asignación
                    val tempFile = withContext(Dispatchers.IO) {
                        // Constante stream: valor inmutable que no cambia tras su asignación
                        val stream = context.contentResolver.openInputStream(fotoUri)
                        // Constante file: valor inmutable que no cambia tras su asignación
                        val file = File.createTempFile("foto_", ".jpg", context.cacheDir)
                        stream?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                        file
                    }
                    // Constante body: valor inmutable que no cambia tras su asignación
                    val body = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                    // Constante part: valor inmutable que no cambia tras su asignación
                    val part = MultipartBody.Part.createFormData("foto", tempFile.name, body)
                    // Constante uploadResp: valor inmutable que no cambia tras su asignación
                    val uploadResp = repo.uploadFoto(part)
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (uploadResp.isSuccessful) {
                        fotoUrl = uploadResp.body()?.fotoUrl
                    } else {
                        _message.value = "No se pudo subir la foto, se guardará sin ella"
                    }
                }

                // Constante resp: valor inmutable que no cambia tras su asignación
                val resp = repo.createMascota(request.copy(fotoUrl = fotoUrl))
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (resp.isSuccessful) {
                    _message.value = "✅ Mascota registrada"
                    onSuccess()
                } else {
                    _message.value = "Error al crear mascota"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // Sube foto nueva (si se eligió una) y actualiza los datos de la mascota existente
    // Función actualizarMascotaConFoto: define la lógica de esta operación
    fun actualizarMascotaConFoto(
        context: Context,
        id: String,
        datos: Map<String, Any>,
        fotoUri: Uri?,
        onSuccess: () -> Unit
    ) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _loading.value = true
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante datosFinales: valor inmutable que no cambia tras su asignación
                val datosFinales = datos.toMutableMap()

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (fotoUri != null) {
                    // Constante tempFile: valor inmutable que no cambia tras su asignación
                    val tempFile = withContext(Dispatchers.IO) {
                        // Constante stream: valor inmutable que no cambia tras su asignación
                        val stream = context.contentResolver.openInputStream(fotoUri)
                        // Constante file: valor inmutable que no cambia tras su asignación
                        val file = File.createTempFile("foto_", ".jpg", context.cacheDir)
                        stream?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                        file
                    }
                    // Constante body: valor inmutable que no cambia tras su asignación
                    val body = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                    // Constante part: valor inmutable que no cambia tras su asignación
                    val part = MultipartBody.Part.createFormData("foto", tempFile.name, body)
                    // Constante uploadResp: valor inmutable que no cambia tras su asignación
                    val uploadResp = repo.uploadFoto(part)
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (uploadResp.isSuccessful) {
                        uploadResp.body()?.fotoUrl?.let { datosFinales["foto_url"] = it }
                    } else {
                        _message.value = "No se pudo subir la foto, se guardarán los demás cambios"
                    }
                }

                // Constante resp: valor inmutable que no cambia tras su asignación
                val resp = repo.updateMascota(id, datosFinales)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (resp.isSuccessful) {
                    _message.value = "✅ Cambios guardados"
                    onSuccess()
                } else {
                    _message.value = "Error al actualizar mascota"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // Función eliminarMascota: define la lógica de esta operación
    fun eliminarMascota(id: String, onSuccess: () -> Unit) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante resp: valor inmutable que no cambia tras su asignación
                val resp = repo.deleteMascota(id)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (resp.isSuccessful) onSuccess()
                else _message.value = "Error al eliminar"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }
}
```

## FASE 14: `com/lomito/seguro/ui/mural`

### Paso 14.1: `MascotaPerdidaAdapter.kt`

**Adaptador del mural de mascotas perdidas**. Muestra cada reporte de mascota perdida como una tarjeta con foto, descripción y botón para reportar avistamiento.

```kotlin
// mobile/ui/mural/MascotaPerdidaAdapter.kt
// Paquete: com.lomito.seguro.ui.mural
package com.lomito.seguro.ui.mural

// Importa la dependencia necesaria: LayoutInflater
import android.view.LayoutInflater
// Importa componentes de la interfaz gráfica
import android.view.View
// Importa componentes de la interfaz gráfica
import android.view.ViewGroup
// Importa componentes de la interfaz gráfica
import android.widget.ImageView
// Importa componentes de la interfaz gráfica
import android.widget.TextView
// Importa componentes de la interfaz gráfica
import androidx.recyclerview.widget.RecyclerView
// Importa la librería de carga de imágenes
import com.bumptech.glide.Glide
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.data.model.Mascota
// Importa la dependencia necesaria: toAbsoluteUrl
import com.lomito.seguro.util.toAbsoluteUrl

// Adaptador MascotaPerdidaAdapter: conecta los datos con la vista del RecyclerView
class MascotaPerdidaAdapter(
    // Constante onItemClick: valor inmutable que no cambia tras su asignación
    private val onItemClick: (Mascota) -> Unit
) : RecyclerView.Adapter<MascotaPerdidaAdapter.ViewHolder>() {

    // Variable items: almacena el estado mutable de este componente
    private var items: List<Mascota> = emptyList()

    // Función submitList: define la lógica de esta operación
    fun submitList(list: List<Mascota>) {
        items = list
        notifyDataSetChanged()
    }

    // Infla el layout y crea el ViewHolder para el RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Constante view: valor inmutable que no cambia tras su asignación
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mascota_mural, parent, false)
        // Retorna el valor al llamador de la función
        return ViewHolder(view)
    }

    // Vincula los datos del modelo con las vistas del ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Constante mascota: valor inmutable que no cambia tras su asignación
        val mascota = items[position]
        holder.bind(mascota)
        holder.itemView.setOnClickListener { onItemClick(mascota) }
    }

    // Retorna el número total de elementos en la lista
    override fun getItemCount(): Int = items.size

    // Declaración de la clase ViewHolder
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Constante ivFoto: valor inmutable que no cambia tras su asignación
        private val ivFoto: ImageView = itemView.findViewById(R.id.ivFotoMascota)
        // Constante tvNombre: valor inmutable que no cambia tras su asignación
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreMascota)
        // Constante tvRaza: valor inmutable que no cambia tras su asignación
        private val tvRaza: TextView = itemView.findViewById(R.id.tvRazaMascota)
        // Constante tvEstado: valor inmutable que no cambia tras su asignación
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstadoMascota)

        // Función bind: define la lógica de esta operación
        fun bind(mascota: Mascota) {
            tvNombre.text = mascota.nombre
            tvRaza.text = mascota.raza
            tvEstado.text = "🔴 PERDIDA"

            // Constante placeholder: valor inmutable que no cambia tras su asignación
            val placeholder = if (mascota.especie == "PERRO") R.drawable.ic_dog else R.drawable.ic_cat
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (!mascota.fotoUrl.isNullOrEmpty()) {
                Glide.with(ivFoto).load(mascota.fotoUrl.toAbsoluteUrl())
                    .placeholder(placeholder).into(ivFoto)
            } else {
                ivFoto.setImageResource(placeholder)
            }
        }
    }
}
```

### Paso 14.2: `MuralFragment.kt`

**Fragment del mural comunitario**. Muestra todos los reportes de mascotas perdidas de la comunidad, permite ver detalles y reportar avistamientos.

```kotlin
// Paquete: com.lomito.seguro.ui.mural
package com.lomito.seguro.ui.mural

// Importa la dependencia necesaria: BroadcastReceiver
import android.content.BroadcastReceiver
// Importa el contexto de Android
import android.content.Context
// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa la clase Intent para navegación entre componentes
import android.content.IntentFilter
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: LayoutInflater
import android.view.LayoutInflater
// Importa componentes de la interfaz gráfica
import android.view.View
// Importa componentes de la interfaz gráfica
import android.view.ViewGroup
// Importa la dependencia necesaria: Toast
import android.widget.Toast
// Importa la clase Fragment de AndroidX
import androidx.fragment.app.Fragment
// Importa la dependencia necesaria: viewModels
import androidx.fragment.app.viewModels
// Importa el observable de datos reactivos
import androidx.lifecycle.LiveData
// Importa el observable de datos reactivos
import androidx.lifecycle.MutableLiveData
// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModel
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa la dependencia necesaria: LocalBroadcastManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
// Importa componente de navegación
import androidx.navigation.fragment.findNavController
// Importa la dependencia necesaria: GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.data.model.Mascota
// Importa la dependencia necesaria: LomitoRepository
import com.lomito.seguro.data.repository.LomitoRepository
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch

// ViewModel MuralViewModel: gestiona el estado y la lógica de negocio de la pantalla
class MuralViewModel : ViewModel() {
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo = LomitoRepository()
    // Constante _mascotasPerdidas: valor inmutable que no cambia tras su asignación
    private val _mascotasPerdidas = MutableLiveData<List<Mascota>>()
    // Constante mascotasPerdidas: valor inmutable que no cambia tras su asignación
    val mascotasPerdidas: LiveData<List<Mascota>> = _mascotasPerdidas

    // Función cargarMascotasPerdidas: define la lógica de esta operación
    fun cargarMascotasPerdidas() {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante response: valor inmutable que no cambia tras su asignación
                val response = repo.getMascotasByEstado("PERDIDA")
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (response.isSuccessful) {
                    _mascotasPerdidas.value = response.body() ?: emptyList()
                } else {
                    _mascotasPerdidas.value = emptyList()
                }
            } catch (e: Exception) {
                _mascotasPerdidas.value = emptyList()
            }
        }
    }
}

/**
 * [Fragmento del mural de mascotas perdidas]
 *
 * Responsabilidades:
 * - [Mostrar una cuadrícula con todas las mascotas en estado PERDIDA]
 * - [Reaccionar a notificaciones de nuevas mascotas perdidas para actualizar la lista]
 */
// Fragment MuralFragment: componente de UI que representa una sección de la pantalla
class MuralFragment : Fragment() {
    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: MuralViewModel by viewModels()
    private lateinit var adapter: MascotaPerdidaAdapter

    // Constante mascotaPerdidaReceiver: valor inmutable que no cambia tras su asignación
    private val mascotaPerdidaReceiver = object : BroadcastReceiver() {
        // Sobreescribe la función onReceive de la clase padre
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.cargarMascotasPerdidas()
            // Muestra un mensaje emergente breve al usuario
            Toast.makeText(requireContext(), "🐾 Nueva mascota perdida en el mural", Toast.LENGTH_SHORT).show()
        }
    }

    // Infla el layout del Fragment y retorna la vista raíz
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_mural, container, false)

    // Se llama cuando la vista del Fragment está lista; se inicializa la UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onViewCreated(view, savedInstanceState)

        // Constante recyclerView: valor inmutable que no cambia tras su asignación
        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewMural)
        // Constante tvSinMascotas: valor inmutable que no cambia tras su asignación
        val tvSinMascotas = view.findViewById<android.widget.TextView>(R.id.tvSinMascotas)

        // Asigna el adaptador al RecyclerView para mostrar la lista de datos
        adapter = MascotaPerdidaAdapter { mascota ->
            // Constante bundle: valor inmutable que no cambia tras su asignación
            val bundle = Bundle().apply { putString("mascotaId", mascota.id) }
            // Navega hacia el destino especificado en el grafo de navegación
            findNavController().navigate(R.id.action_mural_to_mascota_detail, bundle)
        }

        // Define cómo se organizan visualmente los elementos del RecyclerView
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        // Asigna el adaptador al RecyclerView para mostrar la lista de datos
        recyclerView.adapter = adapter

        viewModel.mascotasPerdidas.observe(viewLifecycleOwner) { mascotas ->
            adapter.submitList(mascotas)
            tvSinMascotas.visibility = if (mascotas.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (mascotas.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.cargarMascotasPerdidas()
    }

    // Método del ciclo de vida: la actividad se vuelve visible
    override fun onStart() {
        // Invoca la implementación del método en la clase padre
        super.onStart()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            mascotaPerdidaReceiver,
            IntentFilter("com.lomito.seguro.MASCOTA_PERDIDA_NUEVA")
        )
    }

    // Método del ciclo de vida: la actividad ya no es visible
    override fun onStop() {
        // Invoca la implementación del método en la clase padre
        super.onStop()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mascotaPerdidaReceiver)
    }
}
```

## FASE 15: `com/lomito/seguro/ui/refugios`

### Paso 15.1: `RefugiosFragment.kt`

**Fragment de refugios**. Muestra el mapa con los refugios de animales cercanos usando Google Maps, con marcadores y información de cada refugio.

```kotlin
// Paquete: com.lomito.seguro.ui.refugios
package com.lomito.seguro.ui.refugios

// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa la dependencia necesaria: Uri
import android.net.Uri
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: *
import android.view.*
// Importa la clase Fragment de AndroidX
import androidx.fragment.app.Fragment
// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModel
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa la dependencia necesaria: DiffUtil
import androidx.recyclerview.widget.DiffUtil
// Importa la dependencia necesaria: LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
// Importa la dependencia necesaria: ListAdapter
import androidx.recyclerview.widget.ListAdapter
// Importa componentes de la interfaz gráfica
import androidx.recyclerview.widget.RecyclerView
// Importa la dependencia necesaria: Refugio
import com.lomito.seguro.data.model.Refugio
// Importa la dependencia necesaria: LomitoRepository
import com.lomito.seguro.data.repository.LomitoRepository
// Importa la clase Fragment de AndroidX
import com.lomito.seguro.databinding.FragmentRefugiosBinding
// Importa el sistema de View Binding
import com.lomito.seguro.databinding.ItemRefugioBinding
// Importa la dependencia necesaria: gone
import com.lomito.seguro.util.gone
// Importa la dependencia necesaria: visible
import com.lomito.seguro.util.visible
// Importa la dependencia necesaria: viewModels
import androidx.fragment.app.viewModels
// Importa el observable de datos reactivos
import androidx.lifecycle.LiveData
// Importa el observable de datos reactivos
import androidx.lifecycle.MutableLiveData
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch

// ViewModel RefugiosViewModel: gestiona el estado y la lógica de negocio de la pantalla
class RefugiosViewModel : ViewModel() {
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo = LomitoRepository()
    // Constante _refugios: valor inmutable que no cambia tras su asignación
    private val _refugios = MutableLiveData<List<Refugio>>()
    // Constante refugios: valor inmutable que no cambia tras su asignación
    val refugios: LiveData<List<Refugio>> = _refugios

    // Función cargar: define la lógica de esta operación
    fun cargar() {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante resp: valor inmutable que no cambia tras su asignación
                val resp = repo.getRefugios()
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (resp.isSuccessful) _refugios.value = resp.body() ?: emptyList()
            } catch (e: Exception) { _refugios.value = emptyList() }
        }
    }
}

// Adaptador RefugioAdapter: conecta los datos con la vista del RecyclerView
class RefugioAdapter(private val onCall: (String) -> Unit) :
    ListAdapter<Refugio, RefugioAdapter.VH>(DiffCB()) {
    inner class VH(val b: ItemRefugioBinding) : RecyclerView.ViewHolder(b.root) {
        // Función bind: define la lógica de esta operación
        fun bind(r: Refugio) {
            b.tvNombre.text = r.nombre
            b.tvDireccion.text = r.direccion
            b.tvTelefono.text = r.telefono
            b.tvHorarios.text = r.horarios
            b.btnLlamar.setOnClickListener { onCall(r.telefono) }
        }
    }
    // Infla el layout y crea el ViewHolder para el RecyclerView
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(
        ItemRefugioBinding.inflate(LayoutInflater.from(p.context), p, false)
    )
    // Vincula los datos del modelo con las vistas del ViewHolder
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
    // Declaración de la clase DiffCB
    class DiffCB : DiffUtil.ItemCallback<Refugio>() {
        // Sobreescribe la función areItemsTheSame de la clase padre
        override fun areItemsTheSame(a: Refugio, b: Refugio) = a.id == b.id
        // Sobreescribe la función areContentsTheSame de la clase padre
        override fun areContentsTheSame(a: Refugio, b: Refugio) = a == b
    }
}

// Fragment RefugiosFragment: componente de UI que representa una sección de la pantalla
class RefugiosFragment : Fragment() {
    // Variable _binding: almacena el estado mutable de este componente
    private var _binding: FragmentRefugiosBinding? = null
    // Constante binding: valor inmutable que no cambia tras su asignación
    private val binding get() = _binding!!
    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: RefugiosViewModel by viewModels()

    // Infla el layout del Fragment y retorna la vista raíz
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRefugiosBinding.inflate(inflater, container, false)
        // Accede a un componente de UI a través del View Binding type-safe
        return binding.root
    }

    // Se llama cuando la vista del Fragment está lista; se inicializa la UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onViewCreated(view, savedInstanceState)
        // Constante adapter: valor inmutable que no cambia tras su asignación
        val adapter = RefugioAdapter { tel ->
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel")))
        }
        // Define cómo se organizan visualmente los elementos del RecyclerView
        binding.rvRefugios.layoutManager = LinearLayoutManager(requireContext())
        // Asigna el adaptador al RecyclerView para mostrar la lista de datos
        binding.rvRefugios.adapter = adapter

        viewModel.refugios.observe(viewLifecycleOwner) { refugios ->
            adapter.submitList(refugios)
            // Accede a un componente de UI a través del View Binding type-safe
            if (refugios.isEmpty()) binding.tvEmpty.visible() else binding.tvEmpty.gone()
        }

        viewModel.cargar()
    }

    // Sobreescribe la función onDestroyView de la clase padre
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
```

## FASE 16: `com/lomito/seguro/ui/simulator`

### Paso 16.1: `SimulatorFragment.kt`

**Fragment del simulador BLE**. Herramienta de desarrollo que simula la distancia entre el teléfono y el collar BLE de la mascota, enviando los datos al smartwatch.

```kotlin
// Paquete: com.lomito.seguro.ui.simulator
package com.lomito.seguro.ui.simulator

// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: LayoutInflater
import android.view.LayoutInflater
// Importa componentes de la interfaz gráfica
import android.view.View
// Importa componentes de la interfaz gráfica
import android.view.ViewGroup
// Importa la clase Fragment de AndroidX
import androidx.fragment.app.Fragment
// Importa la dependencia necesaria: viewModels
import androidx.fragment.app.viewModels
// Importa el observable de datos reactivos
import androidx.lifecycle.LiveData
// Importa el observable de datos reactivos
import androidx.lifecycle.MutableLiveData
// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModel
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa la dependencia necesaria: lifecycleScope
import androidx.lifecycle.lifecycleScope
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.Wearable
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.BuildConfig
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.data.model.Mascota
// Importa la dependencia necesaria: LomitoRepository
import com.lomito.seguro.data.repository.LomitoRepository
// Importa la clase Fragment de AndroidX
import com.lomito.seguro.databinding.FragmentSimulatorBinding
// Importa la dependencia necesaria: SessionManager
import com.lomito.seguro.util.SessionManager
// Importa la dependencia necesaria: gone
import com.lomito.seguro.util.gone
// Importa la dependencia necesaria: toast
import com.lomito.seguro.util.toast
// Importa la dependencia necesaria: visible
import com.lomito.seguro.util.visible
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.*
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
import java.net.URL

// ViewModel SimulatorViewModel: gestiona el estado y la lógica de negocio de la pantalla
class SimulatorViewModel : ViewModel() {
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo = LomitoRepository()

    // Constante _mascotas: valor inmutable que no cambia tras su asignación
    private val _mascotas = MutableLiveData<List<Mascota>>()
    // Constante mascotas: valor inmutable que no cambia tras su asignación
    val mascotas: LiveData<List<Mascota>> = _mascotas

    // Constante _distanciaSimulada: valor inmutable que no cambia tras su asignación
    private val _distanciaSimulada = MutableLiveData(0)
    // Constante distanciaSimulada: valor inmutable que no cambia tras su asignación
    val distanciaSimulada: LiveData<Int> = _distanciaSimulada

    // Constante _mensaje: valor inmutable que no cambia tras su asignación
    private val _mensaje = MutableLiveData<String>()
    // Constante mensaje: valor inmutable que no cambia tras su asignación
    val mensaje: LiveData<String> = _mensaje

    // Variable mascotaSeleccionadaId: almacena el estado mutable de este componente
    private var mascotaSeleccionadaId = ""
    // Variable umbralActual: almacena el estado mutable de este componente
    private var umbralActual = 50

    // Función cargarMascotas: define la lógica de esta operación
    fun cargarMascotas(ownerId: String) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante resp: valor inmutable que no cambia tras su asignación
                val resp = repo.getMascotas(ownerId)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (resp.isSuccessful) _mascotas.value = resp.body() ?: emptyList()
            } catch (e: Exception) { _mascotas.value = emptyList() }
        }
    }

    // Función seleccionarMascota: define la lógica de esta operación
    fun seleccionarMascota(mascota: Mascota) {
        mascotaSeleccionadaId = mascota.id
        umbralActual = mascota.distanciaAlerta
    }

    // Función setDistancia: define la lógica de esta operación
    fun setDistancia(distancia: Int) {
        _distanciaSimulada.value = distancia
        // Constante superaUmbral: valor inmutable que no cambia tras su asignación
        val superaUmbral = distancia > umbralActual
        _mensaje.value = if (superaUmbral)
            "🚨 ¡Umbral superado! (${distancia}m > ${umbralActual}m)"
        else
            "✅ Dentro del rango (${distancia}m / umbral ${umbralActual}m)"
    }

    // Función getMascotaId: define la lógica de esta operación
    fun getMascotaId() = mascotaSeleccionadaId
    // Función getUmbral: define la lógica de esta operación
    fun getUmbral() = umbralActual
}

// Fragment SimulatorFragment: componente de UI que representa una sección de la pantalla
class SimulatorFragment : Fragment() {
    // Variable _binding: almacena el estado mutable de este componente
    private var _binding: FragmentSimulatorBinding? = null
    // Constante binding: valor inmutable que no cambia tras su asignación
    private val binding get() = _binding!!
    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: SimulatorViewModel by viewModels()
    private lateinit var session: SessionManager
    // Variable debounceJob: almacena el estado mutable de este componente
    private var debounceJob: Job? = null

    // Infla el layout del Fragment y retorna la vista raíz
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSimulatorBinding.inflate(inflater, container, false)
        // Accede a un componente de UI a través del View Binding type-safe
        return binding.root
    }

    // Se llama cuando la vista del Fragment está lista; se inicializa la UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        // Actualiza el componente de UI a través del View Binding
        binding.sliderDistancia.valueFrom = 0f
        // Actualiza el componente de UI a través del View Binding
        binding.sliderDistancia.valueTo = 200f
        // Actualiza el componente de UI a través del View Binding
        binding.sliderDistancia.value = 0f
        // Actualiza el componente de UI a través del View Binding
        binding.sliderDistancia.stepSize = 1f

        // Accede a un componente de UI a través del View Binding type-safe
        binding.sliderDistancia.addOnChangeListener { _, value, _ ->
            // Constante dist: valor inmutable que no cambia tras su asignación
            val dist = value.toInt()
            // Actualiza el componente de UI a través del View Binding
            binding.tvDistanciaActual.text = "${dist}m"
            viewModel.setDistancia(dist)

            // ✅ forceAlert automático si supera el umbral
            // Constante superaUmbral: valor inmutable que no cambia tras su asignación
            val superaUmbral = dist > viewModel.getUmbral()
            enviarAlWear(dist, forceAlert = superaUmbral)

            // ✅ Debounce 500ms para no spamear el backend
            debounceJob?.cancel()
            // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
            debounceJob = lifecycleScope.launch {
                delay(500)
                enviarAlBackend(dist)
            }
        }

        viewModel.mascotas.observe(viewLifecycleOwner) { mascotas ->
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (mascotas.isEmpty()) {
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvSinMascotas.visible()
                // Accede a un componente de UI a través del View Binding type-safe
                binding.layoutSimulator.gone()
                return@observe
            }
            // Accede a un componente de UI a través del View Binding type-safe
            binding.tvSinMascotas.gone()
            // Accede a un componente de UI a través del View Binding type-safe
            binding.layoutSimulator.visible()

            // Constante nombres: valor inmutable que no cambia tras su asignación
            val nombres = mascotas.map { "${it.nombre} (umbral: ${it.distanciaAlerta}m)" }
            // Constante adapter: valor inmutable que no cambia tras su asignación
            val adapter = android.widget.ArrayAdapter(
                requireContext(), android.R.layout.simple_spinner_item, nombres
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Asigna el adaptador al RecyclerView para mostrar la lista de datos
            binding.spinnerMascota.adapter = adapter

            // Actualiza el componente de UI a través del View Binding
            binding.spinnerMascota.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                // Sobreescribe la función onItemSelected de la clase padre
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    viewModel.seleccionarMascota(mascotas[pos])
                    // Actualiza el componente de UI a través del View Binding
                    binding.tvUmbral.text = "Umbral de alerta: ${mascotas[pos].distanciaAlerta}m"
                    // Actualiza el componente de UI a través del View Binding
                    binding.sliderDistancia.value = 0f
                }
                // Sobreescribe la función onNothingSelected de la clase padre
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

            viewModel.seleccionarMascota(mascotas[0])
            // Actualiza el componente de UI a través del View Binding
            binding.tvUmbral.text = "Umbral de alerta: ${mascotas[0].distanciaAlerta}m"
        }

        viewModel.mensaje.observe(viewLifecycleOwner) { msg ->
            // Actualiza el componente de UI a través del View Binding
            binding.tvEstadoSimulacion.text = msg
            // Constante esAlerta: valor inmutable que no cambia tras su asignación
            val esAlerta = msg.contains("🚨")
            // Accede a un componente de UI a través del View Binding type-safe
            binding.cardEstado.setCardBackgroundColor(
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (esAlerta) requireContext().getColor(R.color.alerta_rojo_light)
                else requireContext().getColor(R.color.verde_light)
            )
        }

        // Accede a un componente de UI a través del View Binding type-safe
        binding.btnEnviarAlerta.setOnClickListener {
            // Constante dist: valor inmutable que no cambia tras su asignación
            val dist = binding.sliderDistancia.value.toInt()
            enviarAlWear(dist, forceAlert = true)
            enviarAlBackend(dist)
            toast("📡 Señal enviada al Watch")
        }

        viewModel.cargarMascotas(session.getUserId())
    }

    private fun enviarAlWear(distancia: Int, forceAlert: Boolean = false) {
        // Constante context: valor inmutable que no cambia tras su asignación
        val context = requireContext().applicationContext
        // Constante mascotaId: valor inmutable que no cambia tras su asignación
        val mascotaId = viewModel.getMascotaId()
        // Constante umbral: valor inmutable que no cambia tras su asignación
        val umbral = viewModel.getUmbral()
        // Constante superaUmbral: valor inmutable que no cambia tras su asignación
        val superaUmbral = distancia > umbral || forceAlert

        // Constante payload: valor inmutable que no cambia tras su asignación
        val payload = JSONObject().apply {
            put("distancia", distancia)
            put("mascotaId", mascotaId)
            put("umbral", umbral)
            put("superaUmbral", superaUmbral)
        }.toString().toByteArray()

        // Usa la API de Wearable para comunicación con dispositivos Wear OS
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                nodes.forEach { node ->
                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    Wearable.getMessageClient(context)
                        // Envía un mensaje al dispositivo Wear OS conectado
                        .sendMessage(node.id, "/ble/distancia", payload)
                        .addOnSuccessListener {
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.d("SIMULATOR", "✅ Mensaje enviado a ${node.displayName}")
                        }
                        .addOnFailureListener {
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.e("SIMULATOR", "❌ Error: ${it.message}")
                        }
                }
            }

        // Constante putDataRequest: valor inmutable que no cambia tras su asignación
        val putDataRequest = com.google.android.gms.wearable.PutDataMapRequest.create("/ble/distancia").apply {
            dataMap.putInt("distancia", distancia)
            dataMap.putString("mascotaId", mascotaId)
            dataMap.putInt("umbral", umbral)
            dataMap.putBoolean("superaUmbral", superaUmbral)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        // Usa la API de Wearable para comunicación con dispositivos Wear OS
        Wearable.getDataClient(context).putDataItem(putDataRequest)
    }

    private fun enviarAlBackend(distancia: Int) {
        // Constante mascotaId: valor inmutable que no cambia tras su asignación
        val mascotaId = viewModel.getMascotaId()
        // Constante umbral: valor inmutable que no cambia tras su asignación
        val umbral = viewModel.getUmbral()

        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        CoroutineScope(Dispatchers.IO).launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante url: valor inmutable que no cambia tras su asignación
                val url = URL("${BuildConfig.BACKEND_URL}/api/simulador/distancia")
                // Constante conn: valor inmutable que no cambia tras su asignación
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                // Constante json: valor inmutable que no cambia tras su asignación
                val json = JSONObject().apply {
                    put("distancia", distancia)
                    put("umbral", umbral)
                    put("mascotaId", mascotaId)
                }

                conn.outputStream.write(json.toString().toByteArray())
                conn.outputStream.flush()
                conn.outputStream.close()

                // Constante responseCode: valor inmutable que no cambia tras su asignación
                val responseCode = conn.responseCode
                conn.disconnect()
                // Registro de evento en el log de Android para depuración
                android.util.Log.d("SIMULATOR", "📡 Backend actualizado: ${distancia}m (HTTP $responseCode)")
            } catch (e: Exception) {
                // Registro de evento en el log de Android para depuración
                android.util.Log.e("SIMULATOR", "❌ Error backend: ${e.message}")
            }
        }
    }

    // Sobreescribe la función onDestroyView de la clase padre
    override fun onDestroyView() {
        // Invoca la implementación del método en la clase padre
        super.onDestroyView()
        debounceJob?.cancel()
        _binding = null
    }
}
```

## FASE 17: `com/lomito/seguro/util`

### Paso 17.1: `Extensions.kt`

**Funciones de extensión**. Extiende clases existentes de Android/Kotlin con utilidades adicionales: conversión de URLs, visibilidad de vistas, formato de fechas y distancias.

```kotlin
// Paquete: com.lomito.seguro.util
package com.lomito.seguro.util

// Importa componentes de la interfaz gráfica
import android.view.View
// Importa la dependencia necesaria: Toast
import android.widget.Toast
// Importa la clase Fragment de AndroidX
import androidx.fragment.app.Fragment
// Importa el cliente Retrofit para peticiones HTTP
import com.lomito.seguro.data.api.RetrofitClient
// Importa la dependencia necesaria: SimpleDateFormat
import java.text.SimpleDateFormat
// Importa la dependencia necesaria: *
import java.util.*

/**
 * El backend guarda foto_url como ruta relativa (ej: "/uploads/123.jpg").
 * Glide/Coil necesitan una URL absoluta para poder cargarla, así que la
 * resolvemos contra el host del servidor. Si ya viene absoluta (http/https)
 * se regresa tal cual.
 */
// Función String: define la lógica de esta operación
fun String?.toAbsoluteUrl(): String? {
    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
    if (this.isNullOrEmpty()) return null
    // Retorna el valor al llamador de la función
    return if (startsWith("http://") || startsWith("https://")) this
    // Accede al cliente Retrofit singleton para realizar peticiones de red
    else RetrofitClient.SERVER_URL + (if (startsWith("/")) this else "/$this")
}

// Función View: define la lógica de esta operación
fun View.visible() { visibility = View.VISIBLE }
// Función View: define la lógica de esta operación
fun View.gone() { visibility = View.GONE }
// Función View: define la lógica de esta operación
fun View.invisible() { visibility = View.INVISIBLE }

// Función Fragment: define la lógica de esta operación
fun Fragment.toast(msg: String) {
    // Muestra un mensaje emergente breve al usuario
    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}

// Función String: define la lógica de esta operación
fun String.formatTimestamp(): String {
    // Retorna el valor al llamador de la función
    return try {
        // Constante inputFmt: valor inmutable que no cambia tras su asignación
        val inputFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFmt.timeZone = TimeZone.getTimeZone("UTC")
        // Constante outputFmt: valor inmutable que no cambia tras su asignación
        val outputFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        // Constante date: valor inmutable que no cambia tras su asignación
        val date = inputFmt.parse(this)
        outputFmt.format(date ?: Date())
    } catch (e: Exception) { this }
}

// Función distanciaLabel: define la lógica de esta operación
fun distanciaLabel(metros: Int): String = when {
    metros < 10 -> "Muy cerca"
    metros < 30 -> "${metros}m"
    metros < 100 -> "${metros}m ⚠️"
    else -> "${metros}m 🚨"
}
```

### Paso 17.2: `SessionManager.kt`

**Gestor de sesión**. Almacena y recupera los datos del usuario autenticado en SharedPreferences: ID, nombre, email, teléfono y avatar.

```kotlin
// mobile/util/SessionManager.kt
// Paquete: com.lomito.seguro.util
package com.lomito.seguro.util

// Importa el contexto de Android
import android.content.Context
// Importa SharedPreferences para persistencia local
import android.content.SharedPreferences

/**
 * [Gestor de la sesión local del usuario]
 *
 * Responsabilidades:
 * - [Almacenar y recuperar datos del usuario en SharedPreferences]
 * - [Manejar las operaciones de login y logout locales]
 */
// Declaración de la clase SessionManager
class SessionManager(context: Context) {
    // Constante prefs: valor inmutable que no cambia tras su asignación
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        // Constante KEY_USER_ID: valor fijo definido en tiempo de compilación
        private const val KEY_USER_ID = "user_id"
        // Constante KEY_USER_NAME: valor fijo definido en tiempo de compilación
        private const val KEY_USER_NAME = "user_name"
        // Constante KEY_USER_EMAIL: valor fijo definido en tiempo de compilación
        private const val KEY_USER_EMAIL = "user_email"
        // Constante KEY_USER_PHONE: valor fijo definido en tiempo de compilación
        private const val KEY_USER_PHONE = "user_phone"
        // Constante KEY_USER_AVATAR: valor fijo definido en tiempo de compilación
        private const val KEY_USER_AVATAR = "user_avatar"
        // Constante KEY_IS_LOGGED_IN: valor fijo definido en tiempo de compilación
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    // Función saveUser: define la lógica de esta operación
    fun saveUser(
        id: String,
        nombre: String,
        correo: String,
        telefono: String = "",
        avatarUrl: String? = null
    ) {
        // Inicia el editor para modificar los SharedPreferences
        prefs.edit().apply {
            putString(KEY_USER_ID, id)
            putString(KEY_USER_NAME, nombre)
            putString(KEY_USER_EMAIL, correo)
            putString(KEY_USER_PHONE, telefono)
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (avatarUrl != null) putString(KEY_USER_AVATAR, avatarUrl)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    // Función getUserId: define la lógica de esta operación
    fun getUserId(): String {
        // Retorna el valor al llamador de la función
        return prefs.getString(KEY_USER_ID, "") ?: ""
    }

    // Función getUserName: define la lógica de esta operación
    fun getUserName(): String {
        // Retorna el valor al llamador de la función
        return prefs.getString(KEY_USER_NAME, "") ?: ""
    }

    // Función getUserEmail: define la lógica de esta operación
    fun getUserEmail(): String {
        // Retorna el valor al llamador de la función
        return prefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    // Función getUserPhone: define la lógica de esta operación
    fun getUserPhone(): String {
        // Retorna el valor al llamador de la función
        return prefs.getString(KEY_USER_PHONE, "") ?: ""
    }

    // Función getUserAvatar: define la lógica de esta operación
    fun getUserAvatar(): String {
        // Retorna el valor al llamador de la función
        return prefs.getString(KEY_USER_AVATAR, "") ?: ""
    }

    // Función isLoggedIn: define la lógica de esta operación
    fun isLoggedIn(): Boolean {
        // Retorna el valor al llamador de la función
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Función getFullUserData: define la lógica de esta operación
    fun getFullUserData(): Map<String, String> {
        // Retorna el valor al llamador de la función
        return mapOf(
            "id" to getUserId(),
            "nombre" to getUserName(),
            "correo" to getUserEmail(),
            "telefono" to getUserPhone(),
            "avatar" to getUserAvatar()
        )
    }

    // Función logout: define la lógica de esta operación
    fun logout() {
        // Inicia el editor para modificar los SharedPreferences
        prefs.edit().clear().apply()
    }

    // Función updateUserId: define la lógica de esta operación
    fun updateUserId(userId: String) {
        // Inicia el editor para modificar los SharedPreferences
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }
}
```

## FASE 18: `com/lomito/seguro/utils`

### Paso 18.1: `DateUtils.kt`

**Utilidades de fecha**. Funciones helper para formatear y convertir fechas en la aplicación.

```kotlin
package com.lomito.seguro.utils

fun formatDate(date: String): String {
    return date.substring(0, 10)
}
```

### Paso 18.2: `ImageUtils.kt`

**Utilidades de imagen**. Funciones helper para comprimir, redimensionar y procesar imágenes antes de subirlas al servidor.

```kotlin
package com.lomito.seguro.utils

fun compressImage(imagePath: String): String {
    // Implementar compresión de imagen
    return imagePath
}
```

### Paso 18.3: `ValidationUtils.kt`

**Utilidades de validación**. Funciones para validar datos de entrada: emails, teléfonos, contraseñas y otros campos de formulario.

```kotlin
package com.lomito.seguro.utils

fun isValidEmail(email: String): Boolean {
    return email.contains('@')
}
```
