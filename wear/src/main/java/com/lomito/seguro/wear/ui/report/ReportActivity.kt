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
import com.lomito.seguro.wear.data.WatchViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class ReportViewModel(app: android.app.Application) : AndroidViewModel(app) {
    private val _estado = MutableLiveData<String>("Listo para reportar")
    val estado: LiveData<String> = _estado
    private val _enviado = MutableLiveData(false)
    val enviado: LiveData<Boolean> = _enviado

    fun reportarVista(mascotaId: String) {
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

                val lat = location?.latitude ?: 20.9167
                val lng = location?.longitude ?: -101.1500

                _estado.value = "Enviando al teléfono..."

                val payload = JSONObject().apply {
                    put("mascotaId", mascotaId)
                    put("latitud", lat)
                    put("longitud", lng)
                    put("accion", "reportar_vista")
                }.toString().toByteArray()

                val nodes = Wearable.getNodeClient(getApplication()).connectedNodes.await()
                nodes.forEach { node ->
                    Wearable.getMessageClient(getApplication())
                        .sendMessage(node.id, "/watch/reporte", payload).await()
                }

                _estado.value = "✅ Vista reportada desde el Watch"
                _enviado.value = true
            } catch (e: Exception) {
                _estado.value = "❌ Error: ${e.message}"
            }
        }
    }
}

class ReportActivity : ComponentActivity() {
    private val watchVM: WatchViewModel by viewModels()
    private val reportVM: ReportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val bleState by watchVM.bleState.observeAsState()
            val estado by reportVM.estado.observeAsState("Listo")
            val enviado by reportVM.enviado.observeAsState(false)

            ReportScreen(
                mascotaId = bleState?.mascotaId ?: "",
                estado = estado,
                enviado = enviado,
                onReportar = { reportVM.reportarVista(bleState?.mascotaId ?: "") },
                onDismiss = { finish() }
            )
        }
    }
}

@Composable
fun ReportScreen(
    mascotaId: String,
    estado: String,
    enviado: Boolean,
    onReportar: () -> Unit,
    onDismiss: () -> Unit
) {
    Scaffold(timeText = { TimeText() }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF001F3F)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text("📍", fontSize = 24.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Reportar vista",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = estado,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))

                if (!enviado) {
                    Button(
                        onClick = onReportar,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1565C0)),
                        modifier = Modifier.fillMaxWidth(0.75f)
                    ) {
                        Text("Reportar ahora", fontSize = 12.sp, color = Color.White)
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF424242)),
                    modifier = Modifier.fillMaxWidth(0.75f)
                ) {
                    Text(if (enviado) "Cerrar" else "Cancelar", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}
