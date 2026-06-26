package com.lomito.seguro.wear.ui.alert

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
import androidx.wear.compose.material.*
import com.lomito.seguro.wear.data.BleState
import com.lomito.seguro.wear.data.WatchViewModel

/**
 * Pantalla de alerta activa en el Watch.
 * Se abre cuando la distancia supera el umbral.
 * Muestra la distancia en rojo pulsante y botón para reportar vista.
 */
class AlertActivity : ComponentActivity() {
    private val viewModel: WatchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.bleState.observeAsState(BleState())
            AlertScreen(
                state = state,
                onDismiss = { finish() }
            )
        }
    }
}

@Composable
fun AlertScreen(state: BleState, onDismiss: () -> Unit) {
    Scaffold(
        timeText = { TimeText() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2D0000)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text("🚨", fontSize = 28.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "¡ALERTA!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
                Text(
                    text = "Lomito fuera de rango",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${state.distancia}m",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
                Text(
                    text = "umbral: ${state.umbral}m",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF424242)),
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text("Entendido", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}
