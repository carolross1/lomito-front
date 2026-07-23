// wear/ui/report/ReportActivity.kt
package com.lomito.seguro.wear.ui.report

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.wear.compose.material.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.net.URL

// --- COLORES COHERENTES CON EL MURAL ---
private val ThemeBg = Color(0xFF1A1A2E)
private val CardBg = Color(0xFF2C2C3E)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentBlue = Color(0xFF2196F3)

class ReportViewModel(app: android.app.Application) : AndroidViewModel(app) {
    private val _estado = MutableLiveData<String>("¿Viste a esta mascota?")
    val estado: LiveData<String> = _estado
    private val _enviado = MutableLiveData(false)
    val enviado: LiveData<Boolean> = _enviado

    fun reportarVista(mascotaId: String, mascotaNombre: String) {
        _estado.value = "Obteniendo ubicación..."
        viewModelScope.launch {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(getApplication())
                val location: Location? = if (
                    ActivityCompat.checkSelfPermission(
                        getApplication(), Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedClient.lastLocation.await()
                } else null

                val lat = location?.latitude ?: 0.0
                val lng = location?.longitude ?: 0.0

                _estado.value = "Enviando reporte..."

                val payload = JSONObject().apply {
                    put("mascotaId", mascotaId)
                    put("latitud", lat)
                    put("longitud", lng)
                    put("accion", "reportar_vista")
                    put("mascotaNombre", mascotaNombre)
                }.toString().toByteArray()

                val nodes = Wearable.getNodeClient(getApplication()).connectedNodes.await()
                nodes.forEach { node ->
                    Wearable.getMessageClient(getApplication())
                        .sendMessage(node.id, "/watch/reporte", payload).await()
                }

                val url = URL("http://192.168.100.12:3000/api/reportes")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val json = JSONObject().apply {
                    put("mascotaId", mascotaId)
                    put("latitud", lat)
                    put("longitud", lng)
                    put("reportadoPorId", "usuario_watch")
                }

                conn.outputStream.write(json.toString().toByteArray())
                val responseCode = conn.responseCode
                conn.disconnect()

                if (responseCode == 200 || responseCode == 201) {
                    _estado.value = "✅ ¡Enviado!"
                    _enviado.value = true
                } else {
                    _estado.value = "⚠️ Error HTTP $responseCode"
                }
            } catch (e: Exception) {
                _estado.value = "❌ Error de conexión"
            }
        }
    }
}

class ReportActivity : ComponentActivity() {
    private val reportVM: ReportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mascotaId = intent.getStringExtra("mascotaId") ?: ""
        val mascotaNombre = intent.getStringExtra("mascotaNombre") ?: "Mascota"

        setContent {
            val estado by reportVM.estado.observeAsState("Cargando...")
            val enviado by reportVM.enviado.observeAsState(false)

            ReportScreen(
                mascotaNombre = mascotaNombre,
                estado = estado,
                enviado = enviado,
                onReportar = { reportVM.reportarVista(mascotaId, mascotaNombre) },
                onDismiss = { finish() }
            )
        }
    }
}

@Composable
fun ReportScreen(
    mascotaNombre: String,
    estado: String,
    enviado: Boolean,
    onReportar: () -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeBg),
            contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            autoCentering = AutoCenteringParams(itemIndex = 1) // Centra el nombre de la mascota
        ) {
            // Icono y Título
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (enviado) "🎉" else "📍",
                        fontSize = 28.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (enviado) "¡Éxito!" else "Nuevo Reporte",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Cuerpo del mensaje
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = mascotaNombre,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (enviado) AccentGreen else AccentBlue,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = estado,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Acciones o Progreso
            item {
                if (!enviado) {
                    if (estado.contains("Enviando") || estado.contains("Obteniendo")) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp).padding(4.dp),
                            strokeWidth = 3.dp,
                            indicatorColor = AccentBlue
                        )
                    } else {
                        Button(
                            onClick = onReportar,
                            colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Text("Reportar Avistamiento", fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // Botón Salir/Cancelar
            item {
                Spacer(Modifier.height(8.dp))
                CompactButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (enviado) AccentGreen else Color(0xFF424242)
                    ),
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text(
                        text = if (enviado) "Terminar" else "Cancelar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}