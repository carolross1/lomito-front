package com.lomito.seguro.tv.ui.refugio

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lomito.seguro.tv.data.model.Refugio
import com.lomito.seguro.tv.ui.theme.LomitoAlertRed
import com.lomito.seguro.tv.ui.theme.LomitoTvTheme

/**
 * Pantalla "Difusión de refugio" del diagrama de flujo: transmite el video
 * en vivo (o grabado) que el refugio publicó (`Refugio.videoUrl`), para que
 * la comunidad vea el estado del refugio directamente en la Smart TV.
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
                RefugioDifusionScreen(viewModel = viewModel, refugioId = refugioId)
            }
        }
    }
}

@Composable
fun RefugioDifusionScreen(viewModel: RefugioDifusionViewModel, refugioId: String) {
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
            else -> RefugioDifusionContenido(refugio = state.refugio!!)
        }
    }
}

@Composable
private fun RefugioDifusionContenido(refugio: Refugio) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context: Context ->
                VideoView(context).apply {
                    setVideoURI(Uri.parse(refugio.videoUrl))
                    val mediaController = MediaController(context)
                    mediaController.setAnchorView(this)
                    setMediaController(mediaController)
                    setOnPreparedListener { player ->
                        player.isLooping = true
                        start()
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
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
                text = "${refugio.direccion} · ${refugio.telefono}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp
            )
        }
    }
}
