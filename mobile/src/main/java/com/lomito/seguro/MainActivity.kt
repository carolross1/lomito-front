// mobile/MainActivity.kt
package com.lomito.seguro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.wearable.Wearable
import com.lomito.seguro.repository.AlertasRepository
import com.lomito.seguro.databinding.ActivityMainBinding
import com.lomito.seguro.util.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * [Actividad principal de la aplicación]
 *
 * Responsabilidades:
 * - [Gestionar la navegación principal de la app]
 * - [Configurar el polling de notificaciones de avistamientos]
 * - [Sincronizar información básica con el smartwatch]
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val alertasRepository = AlertasRepository()
    private val avistamientosNotificados = mutableSetOf<Int>()
    private lateinit var navController: NavController
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        sessionManager = SessionManager(this)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController

        val appBarConfig = AppBarConfiguration(setOf(R.id.loginFragment, R.id.homeFragment))
        setupActionBarWithNavController(navController, appBarConfig)

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.home_menu, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                val destinoActual = navController.currentDestination?.id
                val ocultarEn = setOf(R.id.loginFragment, R.id.registerFragment)
                menu.findItem(R.id.action_logout)?.isVisible = destinoActual !in ocultarEn
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_logout -> {
                        sessionManager.logout()
                        navController.navigate(R.id.action_global_logout)
                        true
                    }
                    else -> false
                }
            }
        })

        navController.addOnDestinationChangedListener { _, _, _ -> invalidateMenu() }

        enviarUserIdAlWatchSiExiste()
        iniciarPollingDeAvistamientos()
        manejarIntentDeNotificacion(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        manejarIntentDeNotificacion(intent)
    }

    private fun manejarIntentDeNotificacion(intent: Intent?) {
        if (intent?.getBooleanExtra("open_alerta", false) == true) {
            navController.navigate(R.id.alertasFragment)
        }
    }

    private fun iniciarPollingDeAvistamientos() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    try {
                        Log.d("POLLING", "========== INICIO POLLING ==========")
                        val ownerId = sessionManager.getUserId().toIntOrNull()
                        Log.d("POLLING", "OwnerId: $ownerId")

                        if (ownerId != null && ownerId != 0) {
                            Log.d("POLLING", "Llamando a getAlertasNoLeidas...")
                            val resultado = alertasRepository.getAlertasNoLeidas(ownerId)
                            Log.d("POLLING", "Resultado success: ${resultado.success}")
                            Log.d("POLLING", "Cantidad de alertas: ${resultado.alertas.size}")

                            if (resultado.success) {
                                // Mostrar todas las alertas que llegaron
                                resultado.alertas.forEach { alerta ->
                                    Log.d("POLLING", "Alerta: id=${alerta.id}, tipo=${alerta.tipo}, leida=${alerta.leida}, mensaje=${alerta.mensaje}")
                                }

                                // ✅ Filtrar SOLO alertas NO leídas y que contengan "AVISTAMIENTO"
                                val avistamientos = resultado.alertas
                                    .filter {
                                        Log.d("POLLING", "Filtrando: tipo=${it.tipo}, leida=${it.leida}, contiene AVISTAMIENTO=${it.tipo.contains("AVISTAMIENTO")}")
                                        it.tipo.contains("AVISTAMIENTO") &&
                                                !it.leida &&  // ✅ SOLO NO LEÍDAS
                                                it.id !in avistamientosNotificados
                                    }

                                Log.d("POLLING", "Avistamientos encontrados: ${avistamientos.size}")
                                Log.d("POLLING", "IDs ya notificados: ${avistamientosNotificados}")

                                avistamientos.forEach { alerta ->
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
                                Log.e("POLLING", "Error en resultado: ${resultado.error}")
                            }
                        } else {
                            Log.d("POLLING", "OwnerId no válido: $ownerId")
                        }
                    } catch (e: Exception) {
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
        val channelId = "lomito_avistamientos"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(channelId, "Avistamientos confirmados", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        // Título dinámico según el tipo
        val titulo = when {
            tipo.contains("CONFIRMADO") -> "✅ ¡Avistamiento confirmado!"
            tipo.contains("REPORTADO") -> "👀 ¡Nuevo avistamiento reportado!"
            else -> "🐾 ¡Avistamiento!"
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_alerta", true)
            putExtra("mascota_nombre", mascotaNombre)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        Log.d("POLLING", "✅ Notificación enviada: $titulo - $mensaje")
    }

    private fun enviarAvistamientoAlWatch(mascotaNombre: String?, mensaje: String, mascotaId: String?) {
        val context = applicationContext
        val payload = JSONObject().apply {
            put("tipo", "AVISTAMIENTO_CONFIRMADO")
            put("mascotaId", mascotaId ?: "")
            put("mascotaNombre", mascotaNombre ?: "")
            put("mensaje", mensaje)
        }.toString().toByteArray()

        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { node ->
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, "/watch/avistamiento_confirmado", payload)
                }
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun enviarUserIdAlWatchSiExiste() {
        val userId = sessionManager.getUserId()
        if (userId.isNotEmpty() && userId != "null") {
            enviarUserIdAlWatch(userId)
        }
    }

    private fun enviarUserIdAlWatch(userId: String) {
        val context = applicationContext
        val payload = JSONObject().apply {
            put("tipo", "USER_ID")
            put("userId", userId)
        }.toString().toByteArray()

        Log.d("USER_ID", "📤 Enviando userId $userId al watch")

        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    Log.d("USER_ID", "⚠️ No hay nodos (watch) conectados")
                    return@addOnSuccessListener
                }

                nodes.forEach { node ->
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, "/watch/user_id", payload)
                        .addOnSuccessListener {
                            Log.d("USER_ID", "✅ userId $userId enviado al watch: ${node.displayName}")
                        }
                        .addOnFailureListener {
                            Log.e("USER_ID", "❌ Error enviando userId: ${it.message}")
                        }
                }
            }
            .addOnFailureListener {
                Log.e("USER_ID", "❌ Error obteniendo nodos: ${it.message}")
            }
    }

    fun actualizarUserIdEnWatch(userId: String) {
        enviarUserIdAlWatch(userId)
    }
}