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