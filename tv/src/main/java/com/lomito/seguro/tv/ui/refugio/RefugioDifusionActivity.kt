package com.lomito.seguro.tv.ui.refugio

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lomito.seguro.tv.data.model.Refugio
import com.lomito.seguro.tv.ui.theme.LomitoAlertRed
import com.lomito.seguro.tv.ui.theme.LomitoTvTheme

/**
 * [Actividad de Difusión de Refugio para Android TV]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Transmitir el video en vivo o grabado del refugio en la Smart TV]
 * - [Mostrar los detalles de contacto y horarios del refugio]
 */
class RefugioDifusionActivity : ComponentActivity() {

    companion object {
        const val EXTRA_REFUGIO_ID = "refugio_id"
    }

    private val viewModel: RefugioDifusionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val refugioId = intent.getStringExtra(EXTRA_REFUGIO_ID).orEmpty()

        setContent {
            LomitoTvTheme {
                RefugioDifusionScreen(
                    viewModel = viewModel, 
                    refugioId = refugioId,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

/**
 * [Pantalla componible de Difusión de Refugio]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - viewModel: [ViewModel que provee la información del refugio y URL del video]
 * - refugioId: [Identificador del refugio a difundir]
 * - onBackClick: [Callback para regresar a la pantalla anterior]
 */
@Composable
fun RefugioDifusionScreen(viewModel: RefugioDifusionViewModel, refugioId: String, onBackClick: () -> Unit) {
    LaunchedEffect(refugioId) { viewModel.cargar(refugioId) }
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            state.cargando -> Text(
                text = "Conectando con el refugio…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
            state.refugio == null -> Text(
                text = "No se encontró información de este refugio.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
            state.refugio?.videoUrl.isNullOrBlank() -> Text(
                text = "Este refugio todavía no tiene una transmisión disponible.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
            else -> RefugioDifusionContenido(refugio = state.refugio!!, onBackClick = onBackClick)
        }
    }
}

@Composable
private fun RefugioDifusionContenido(refugio: Refugio, onBackClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context: Context ->
                PlayerView(context).apply {
                    useController = true
                    player = ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(Uri.parse(refugio.videoUrl)))
                        prepare()
                        playWhenReady = true
                        repeatMode = ExoPlayer.REPEAT_MODE_ALL
                    }
                }
            },
            onRelease = { playerView ->
                playerView.player?.release()
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .padding(32.dp)
        ) {
            Text(
                text = "● EN VIVO",
                color = LomitoAlertRed,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = refugio.nombre,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            )
            Text(
                text = "${refugio.direccion}\nTel: ${refugio.telefono}\nHorarios: ${refugio.horarios}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            
            Button(onClick = onBackClick) {
                Text(text = "VOLVER AL DASHBOARD")
            }
        }
    }
}
