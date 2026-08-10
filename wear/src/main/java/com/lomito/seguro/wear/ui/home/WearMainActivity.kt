package com.lomito.seguro.wear.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.wear.compose.material.*
import com.lomito.seguro.wear.data.BleState
import com.lomito.seguro.wear.data.PollingService
import com.lomito.seguro.wear.data.WatchViewModel
import com.lomito.seguro.wear.ui.alert.AlertActivity
import com.lomito.seguro.wear.ui.dashboard.DashboardActivity
import com.lomito.seguro.wear.ui.report.ReportActivity
import com.lomito.seguro.wear.ui.selection.SelectionActivity

class WearMainActivity : ComponentActivity() {
    private lateinit var viewModel: WatchViewModel

    private val bleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val distancia = intent.getIntExtra("distancia", 0)
                val mascotaId = intent.getStringExtra("mascotaId") ?: ""
                val umbral = intent.getIntExtra("umbral", 50)
                val superaUmbral = intent.getBooleanExtra("superaUmbral", false)
                viewModel.actualizarEstado(distancia, mascotaId, umbral, superaUmbral)
            } catch (e: Exception) {
                android.util.Log.e("WEAR_MAIN", "Error en bleReceiver: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            viewModel = ViewModelProvider(this)[WatchViewModel::class.java]

            val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
            val mascotaId = prefs.getString("mascota_activa_id", "") ?: ""
            val mascotaNombre = prefs.getString("mascota_activa_nombre", "Mascota") ?: "Mascota"
            val umbral = prefs.getInt("mascota_umbral", 50)

            if (mascotaId.isEmpty()) {
                startActivity(Intent(this, SelectionActivity::class.java))
                finish()
                return
            }

            viewModel.actualizarEstado(0, mascotaId, umbral, false)

            // ✅ Iniciar PollingService global
            val serviceIntent = Intent(this, PollingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            // ✅ Receiver para actualizar UI desde el PollingService
            val filter = IntentFilter("com.lomito.seguro.wear.BLE_UPDATE")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(bleReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(bleReceiver, filter)
            }

            setContent {
                val state by viewModel.bleState.observeAsState(BleState())
                WearMainScreen(
                    state = state,
                    mascotaNombre = mascotaNombre,
                    onAlertClick = {
                        try {
                            startActivity(Intent(this, AlertActivity::class.java))
                        } catch (e: Exception) {
                            android.util.Log.e("WEAR_MAIN", "Error: ${e.message}")
                        }
                    },
                    onReportClick = {
                        try {
                            startActivity(Intent(this, ReportActivity::class.java).apply {
                                putExtra("mascotaId", mascotaId)
                                putExtra("mascotaNombre", mascotaNombre)
                            })
                        } catch (e: Exception) {
                            android.util.Log.e("WEAR_MAIN", "Error: ${e.message}")
                        }
                    },
                    onChangeMascota = {
                        try {
                            startActivity(Intent(this, SelectionActivity::class.java))
                            finish()
                        } catch (e: Exception) {
                            android.util.Log.e("WEAR_MAIN", "Error: ${e.message}")
                        }
                    },
                    onDashboardClick = {
                        try {
                            startActivity(Intent(this, DashboardActivity::class.java))
                            finish()
                        } catch (e: Exception) {
                            android.util.Log.e("WEAR_MAIN", "Error: ${e.message}")
                        }
                    }
                )
            }

        } catch (e: Exception) {
            android.util.Log.e("WEAR_MAIN", "Error FATAL: ${e.message}", e)
            try {
                startActivity(Intent(this, SelectionActivity::class.java))
            } catch (e2: Exception) {
                android.util.Log.e("WEAR_MAIN", "No se pudo abrir Selection: ${e2.message}")
            }
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(bleReceiver) } catch (e: Exception) {}
    }
}

@Composable
fun WearMainScreen(
    state: BleState,
    mascotaNombre: String,
    onAlertClick: () -> Unit,
    onReportClick: () -> Unit,
    onChangeMascota: () -> Unit,
    onDashboardClick: () -> Unit
) {
    val pct = if (state.umbral > 0)
        (state.distancia.toFloat() / state.umbral.toFloat()).coerceIn(0f, 1f)
    else 0f

    val ringColor = when {
        state.distancia > 250 -> Color(0xFF8B0000)
        state.distancia > 150 -> Color(0xFFD32F2F)
        state.distancia > 100 -> Color(0xFFE53935)
        state.distancia > 70  -> Color(0xFFFF5722)
        state.distancia > 50  -> Color(0xFFFF9800)
        state.distancia > 30  -> Color(0xFFFFC107)
        else                  -> Color(0xFF4CAF50)
    }

    val bgColor = when {
        state.distancia > 250 -> Color(0xFF1A0000)
        state.distancia > 150 -> Color(0xFF2A0000)
        state.distancia > 100 -> Color(0xFF3A0000)
        state.distancia > 70  -> Color(0xFF3A1A00)
        state.distancia > 50  -> Color(0xFF3A2A00)
        state.distancia > 30  -> Color(0xFF2A2A00)
        else                  -> Color(0xFF1A1A2E)
    }

    val alertLevel = when {
        state.distancia > 250 -> "🚨 ¡PELIGRO EXTREMO!"
        state.distancia > 150 -> "🚨 ¡ALERTA MÁXIMA!"
        state.distancia > 100 -> "🔴 ¡ALERTA!"
        state.distancia > 70  -> "🟠 ¡Cuidado!"
        state.distancia > 50  -> "🟡 Atención"
        state.distancia > 30  -> "🟢 Distancia media"
        else                  -> "✅ En rango"
    }

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