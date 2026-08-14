// Paquete: com.lomito.seguro.wear.ui.home
package com.lomito.seguro.wear.ui.home

// Importa la dependencia necesaria: BroadcastReceiver
import android.content.BroadcastReceiver
// Importa el contexto de Android
import android.content.Context
// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa la clase Intent para navegación entre componentes
import android.content.IntentFilter
// Importa la dependencia necesaria: Build
import android.os.Build
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: VibrationEffect
import android.os.VibrationEffect
// Importa la dependencia necesaria: Vibrator
import android.os.Vibrator
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.*
// Importa componente de Jetpack Compose
import androidx.compose.runtime.*
// Importa componente de Jetpack Compose
import androidx.compose.runtime.livedata.observeAsState
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
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
// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModelProvider
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa la dependencia necesaria: BleState
import com.lomito.seguro.wear.data.BleState
// Importa la dependencia necesaria: PollingService
import com.lomito.seguro.wear.data.PollingService
// Importa la clase base ViewModel del ciclo de vida
import com.lomito.seguro.wear.data.WatchViewModel
// Importa la dependencia necesaria: AlertActivity
import com.lomito.seguro.wear.ui.alert.AlertActivity
// Importa la dependencia necesaria: DashboardActivity
import com.lomito.seguro.wear.ui.dashboard.DashboardActivity
// Importa la dependencia necesaria: ReportActivity
import com.lomito.seguro.wear.ui.report.ReportActivity
// Importa la dependencia necesaria: SelectionActivity
import com.lomito.seguro.wear.ui.selection.SelectionActivity

/**
 * [Actividad principal del reloj (Wear OS)]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mostrar el estado de la conexión BLE y la distancia actual]
 * - [Gestionar accesos a las funciones principales como alerta, reporte y cambio de mascota]
 */
// Activity WearMainActivity: pantalla principal que gestiona el ciclo de vida
class WearMainActivity : ComponentActivity() {
    private lateinit var viewModel: WatchViewModel

    // Constante bleReceiver: valor inmutable que no cambia tras su asignación
    private val bleReceiver = object : BroadcastReceiver() {
        // Sobreescribe la función onReceive de la clase padre
        override fun onReceive(context: Context, intent: Intent) {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante distancia: valor inmutable que no cambia tras su asignación
                val distancia = intent.getIntExtra("distancia", 0)
                // Constante mascotaId: valor inmutable que no cambia tras su asignación
                val mascotaId = intent.getStringExtra("mascotaId") ?: ""
                // Constante umbral: valor inmutable que no cambia tras su asignación
                val umbral = intent.getIntExtra("umbral", 50)
                // Constante superaUmbral: valor inmutable que no cambia tras su asignación
                val superaUmbral = intent.getBooleanExtra("superaUmbral", false)
                viewModel.actualizarEstado(distancia, mascotaId, umbral, superaUmbral)
            } catch (e: Exception) {
                // Registro de evento en el log de Android para depuración
                android.util.Log.e("WEAR_MAIN", "Error en bleReceiver: ${e.message}")
            }
        }
    }

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Invoca la implementación del método en la clase padre
            super.onCreate(savedInstanceState)
            viewModel = ViewModelProvider(this)[WatchViewModel::class.java]

            // Constante prefs: valor inmutable que no cambia tras su asignación
            val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
            // Constante mascotaId: valor inmutable que no cambia tras su asignación
            val mascotaId = prefs.getString("mascota_activa_id", "") ?: ""
            // Constante mascotaNombre: valor inmutable que no cambia tras su asignación
            val mascotaNombre = prefs.getString("mascota_activa_nombre", "Mascota") ?: "Mascota"
            // Constante umbral: valor inmutable que no cambia tras su asignación
            val umbral = prefs.getInt("mascota_umbral", 50)

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (mascotaId.isEmpty()) {
                startActivity(Intent(this, SelectionActivity::class.java))
                finish()
                // Retorna el valor al llamador de la función
                return
            }

            viewModel.actualizarEstado(0, mascotaId, umbral, false)

            // ✅ Iniciar PollingService global
            // Constante serviceIntent: valor inmutable que no cambia tras su asignación
            val serviceIntent = Intent(this, PollingService::class.java)
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            // ✅ Receiver para actualizar UI desde el PollingService
            // Constante filter: valor inmutable que no cambia tras su asignación
            val filter = IntentFilter("com.lomito.seguro.wear.BLE_UPDATE")
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(bleReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(bleReceiver, filter)
            }

            // Define el árbol de UI con Jetpack Compose como contenido de la Activity
            setContent {
                // Constante state: valor inmutable que no cambia tras su asignación
                val state by viewModel.bleState.observeAsState(BleState())
                WearMainScreen(
                    state = state,
                    mascotaNombre = mascotaNombre,
                    onAlertClick = {
                        // Bloque try-catch: maneja posibles excepciones en el código crítico
                        try {
                            startActivity(Intent(this, AlertActivity::class.java))
                        } catch (e: Exception) {
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.e("WEAR_MAIN", "Error: ${e.message}")
                        }
                    },
                    onReportClick = {
                        // Bloque try-catch: maneja posibles excepciones en el código crítico
                        try {
                            startActivity(Intent(this, ReportActivity::class.java).apply {
                                putExtra("mascotaId", mascotaId)
                                putExtra("mascotaNombre", mascotaNombre)
                            })
                        } catch (e: Exception) {
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.e("WEAR_MAIN", "Error: ${e.message}")
                        }
                    },
                    onChangeMascota = {
                        // Bloque try-catch: maneja posibles excepciones en el código crítico
                        try {
                            startActivity(Intent(this, SelectionActivity::class.java))
                            finish()
                        } catch (e: Exception) {
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.e("WEAR_MAIN", "Error: ${e.message}")
                        }
                    },
                    onDashboardClick = {
                        // Bloque try-catch: maneja posibles excepciones en el código crítico
                        try {
                            startActivity(Intent(this, DashboardActivity::class.java))
                            finish()
                        } catch (e: Exception) {
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.e("WEAR_MAIN", "Error: ${e.message}")
                        }
                    }
                )
            }

        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("WEAR_MAIN", "Error FATAL: ${e.message}", e)
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                startActivity(Intent(this, SelectionActivity::class.java))
            } catch (e2: Exception) {
                // Registro de evento en el log de Android para depuración
                android.util.Log.e("WEAR_MAIN", "No se pudo abrir Selection: ${e2.message}")
            }
            finish()
        }
    }

    // Método del ciclo de vida: se limpia la actividad antes de destruirse
    override fun onDestroy() {
        // Invoca la implementación del método en la clase padre
        super.onDestroy()
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try { unregisterReceiver(bleReceiver) } catch (e: Exception) {}
    }
}

/**
 * [Pantalla principal de la aplicación en el reloj]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [state]: Estado actual de la mascota y conexión BLE
 * - [mascotaNombre]: Nombre de la mascota actual
 * - [onAlertClick]: Acción al presionar el botón de alerta
 * - [onReportClick]: Acción al presionar el botón de reporte
 * - [onChangeMascota]: Acción para cambiar la mascota activa
 * - [onDashboardClick]: Acción para ir al menú principal
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función WearMainScreen: define la lógica de esta operación
fun WearMainScreen(
    state: BleState,
    mascotaNombre: String,
    onAlertClick: () -> Unit,
    onReportClick: () -> Unit,
    onChangeMascota: () -> Unit,
    onDashboardClick: () -> Unit
) {
    // Constante pct: valor inmutable que no cambia tras su asignación
    val pct = if (state.umbral > 0)
        (state.distancia.toFloat() / state.umbral.toFloat()).coerceIn(0f, 1f)
    else 0f

    // Constante ringColor: valor inmutable que no cambia tras su asignación
    val ringColor = when {
        state.distancia > 250 -> Color(0xFF8B0000)
        state.distancia > 150 -> Color(0xFFD32F2F)
        state.distancia > 100 -> Color(0xFFE53935)
        state.distancia > 70  -> Color(0xFFFF5722)
        state.distancia > 50  -> Color(0xFFFF9800)
        state.distancia > 30  -> Color(0xFFFFC107)
        else                  -> Color(0xFF4CAF50)
    }

    // Constante bgColor: valor inmutable que no cambia tras su asignación
    val bgColor = when {
        state.distancia > 250 -> Color(0xFF1A0000)
        state.distancia > 150 -> Color(0xFF2A0000)
        state.distancia > 100 -> Color(0xFF3A0000)
        state.distancia > 70  -> Color(0xFF3A1A00)
        state.distancia > 50  -> Color(0xFF3A2A00)
        state.distancia > 30  -> Color(0xFF2A2A00)
        else                  -> Color(0xFF1A1A2E)
    }

    // Constante alertLevel: valor inmutable que no cambia tras su asignación
    val alertLevel = when {
        state.distancia > 250 -> "🚨 ¡PELIGRO EXTREMO!"
        state.distancia > 150 -> "🚨 ¡ALERTA MÁXIMA!"
        state.distancia > 100 -> "🔴 ¡ALERTA!"
        state.distancia > 70  -> "🟠 ¡Cuidado!"
        state.distancia > 50  -> "🟡 Atención"
        state.distancia > 30  -> "🟢 Distancia media"
        else                  -> "✅ En rango"
    }

    // Constante alertIcon: valor inmutable que no cambia tras su asignación
    val alertIcon = when {
        state.distancia > 100 -> "🚨"
        state.distancia > 50  -> "⚠️"
        else                  -> "🐾"
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = pct,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                strokeWidth = 6.dp,
                indicatorColor = ringColor,
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = mascotaNombre,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(text = alertIcon, fontSize = 24.sp)

                Text(
                    text = "${state.distancia}m",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = ringColor
                )

                Text(
                    text = alertLevel,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Umbral: ${state.umbral}m",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CompactButton(
                        onClick = onAlertClick,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F)),
                        modifier = Modifier.size(36.dp)
                    ) { Text("🔔", fontSize = 12.sp) }

                    CompactButton(
                        onClick = onReportClick,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1565C0)),
                        modifier = Modifier.size(36.dp)
                    ) { Text("📍", fontSize = 12.sp) }

                    CompactButton(
                        onClick = onChangeMascota,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6A1B9A)),
                        modifier = Modifier.size(36.dp)
                    ) { Text("🐾", fontSize = 12.sp) }

                    CompactButton(
                        onClick = onDashboardClick,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32)),
                        modifier = Modifier.size(36.dp)
                    ) { Text("🏠", fontSize = 12.sp) }
                }
            }
        }
    }
}