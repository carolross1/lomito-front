// mobile/MainActivity.kt
package com.lomito.seguro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.wearable.Wearable
import com.lomito.seguro.databinding.ActivityMainBinding
import com.lomito.seguro.util.SessionManager
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
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

        // ✅ Enviar userId al watch si el usuario ya está logueado
        enviarUserIdAlWatchSiExiste()
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

    // ✅ Función para enviar userId al watch
    private fun enviarUserIdAlWatch(userId: String) {
        val context = applicationContext
        val payload = JSONObject().apply {
            put("tipo", "USER_ID")
            put("userId", userId)
        }.toString().toByteArray()

        android.util.Log.d("USER_ID", "📤 Enviando userId $userId al watch")

        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    android.util.Log.d("USER_ID", "⚠️ No hay nodos (watch) conectados")
                    return@addOnSuccessListener
                }

                nodes.forEach { node ->
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, "/watch/user_id", payload)
                        .addOnSuccessListener {
                            android.util.Log.d("USER_ID", "✅ userId $userId enviado al watch: ${node.displayName}")
                        }
                        .addOnFailureListener {
                            android.util.Log.e("USER_ID", "❌ Error enviando userId: ${it.message}")
                        }
                }
            }
            .addOnFailureListener {
                android.util.Log.e("USER_ID", "❌ Error obteniendo nodos: ${it.message}")
            }
    }

    // ✅ Función pública para llamar desde cualquier fragment después del login
    fun actualizarUserIdEnWatch(userId: String) {
        enviarUserIdAlWatch(userId)
    }
}