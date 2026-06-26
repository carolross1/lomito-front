package com.lomito.seguro.wear.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
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
import com.lomito.seguro.wear.data.WatchViewModel
import com.lomito.seguro.wear.data.WearMessageService
import com.lomito.seguro.wear.ui.alert.AlertActivity
import com.lomito.seguro.wear.ui.report.ReportActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL

class WearMainActivity : ComponentActivity() {
    private lateinit var viewModel: WatchViewModel
    private var pollingJob: Job? = null

    // ⚠️ Cambia esta IP a la de tu computadora
    private val backendUrl = "http://192.168.100.12:3000"

    private val bleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val distancia = intent.getIntExtra("distancia", 0)
            val mascotaId = intent.getStringExtra("mascotaId") ?: ""
            val umbral = intent.getIntExtra("umbral", 50)
            val superaUmbral = intent.getBooleanExtra("superaUmbral", false)
            viewModel.actualizarEstado(distancia, mascotaId, umbral, superaUmbral)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[WatchViewModel::class.java]

        val filter = IntentFilter("com.lomito.seguro.wear.BLE_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bleReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(bleReceiver, filter)
        }

        WearMessageService.onUpdate = { dist, mid, umb, supera ->
            viewModel.actualizarEstado(dist, mid, umb, supera)
        }

        // Polling al backend cada 2 segundos para obtener distancia simulada
        iniciarPolling()

        setContent {
            val state by viewModel.bleState.observeAsState(BleState())
            WearMainScreen(
                state = state,
                onAlertClick = { startActivity(Intent(this, AlertActivity::class.java)) },
                onReportClick = { startActivity(Intent(this, ReportActivity::class.java)) }
            )
        }
    }

    private fun iniciarPolling() {
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val url = java.net.URL("http://10.0.2.2:3000/api/simulador/estado")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 3000
                    conn.readTimeout = 3000
                    conn.requestMethod = "GET"
                    val response = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()

                    val json = JSONObject(response)
                    val distancia = json.optInt("distancia", 0)
                    val umbral = json.optInt("umbral", 50)
                    val mascotaId = json.optString("mascotaId", "")
                    val superaUmbral = distancia > umbral

                    android.util.Log.d("WEAR_POLL", "✅ distancia=$distancia umbral=$umbral")

                    withContext(Dispatchers.Main) {
                        viewModel.actualizarEstado(distancia, mascotaId, umbral, superaUmbral)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WEAR_POLL", "Error polling: ${e.message}")
                }
                delay(2000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
        unregisterReceiver(bleReceiver)
        WearMessageService.onUpdate = null  // ✅ CORREGIDO
    }
}

@Composable
fun WearMainScreen(
    state: BleState,
    onAlertClick: () -> Unit,
    onReportClick: () -> Unit
) {
    val pct = if (state.umbral > 0)
        (state.distancia.toFloat() / state.umbral.toFloat()).coerceIn(0f, 1f)
    else 0f

    val ringColor = when {
        pct < 0.5f -> Color(0xFF4CAF50)
        pct < 0.85f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    val bgColor = if (state.superaUmbral) Color(0xFF1A0000) else Color(0xFF1A1A2E)

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(bgColor),
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
                Text(text = if (state.superaUmbral) "🚨" else "🐾", fontSize = 20.sp)
                Text(
                    text = "${state.distancia}m",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = ringColor
                )
                Text(
                    text = if (state.superaUmbral) "¡FUERA DE RANGO!" else "En rango",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "umbral ${state.umbral}m",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactButton(
                        onClick = onAlertClick,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F))
                    ) { Text("⚠️", fontSize = 12.sp) }
                    CompactButton(
                        onClick = onReportClick,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1565C0))
                    ) { Text("📍", fontSize = 12.sp) }
                }
            }
        }
    }
}