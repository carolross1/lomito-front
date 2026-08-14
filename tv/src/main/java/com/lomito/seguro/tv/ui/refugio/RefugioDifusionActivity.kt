// Paquete: com.lomito.seguro.tv.ui.refugio
package com.lomito.seguro.tv.ui.refugio

// Importa el contexto de Android
import android.content.Context
// Importa la dependencia necesaria: Uri
import android.net.Uri
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa la dependencia necesaria: viewModels
import androidx.activity.viewModels
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Box
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Column
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.fillMaxSize
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.padding
// Importa componente de Jetpack Compose
import androidx.compose.runtime.Composable
// Importa componente de Jetpack Compose
import androidx.compose.runtime.LaunchedEffect
// Importa componente de Jetpack Compose
import androidx.compose.runtime.collectAsState
// Importa componente de Jetpack Compose
import androidx.compose.runtime.getValue
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa componente de Jetpack Compose
import androidx.compose.ui.viewinterop.AndroidView
// Importa el reproductor multimedia ExoPlayer
import androidx.media3.common.MediaItem
// Importa el reproductor multimedia ExoPlayer
import androidx.media3.exoplayer.ExoPlayer
// Importa componentes de la interfaz gráfica
import androidx.media3.ui.PlayerView
// Importa la dependencia necesaria: Button
import androidx.tv.material3.Button
// Importa la dependencia necesaria: MaterialTheme
import androidx.tv.material3.MaterialTheme
// Importa la dependencia necesaria: Text
import androidx.tv.material3.Text
// Importa la dependencia necesaria: Refugio
import com.lomito.seguro.tv.data.model.Refugio
// Importa la dependencia necesaria: LomitoAlertRed
import com.lomito.seguro.tv.ui.theme.LomitoAlertRed
// Importa la dependencia necesaria: LomitoTvTheme
import com.lomito.seguro.tv.ui.theme.LomitoTvTheme

/**
 * [Actividad de Difusión de Refugio para Android TV]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Transmitir el video en vivo o grabado del refugio en la Smart TV]
 * - [Mostrar los detalles de contacto y horarios del refugio]
 */
// Activity RefugioDifusionActivity: pantalla principal que gestiona el ciclo de vida
class RefugioDifusionActivity : ComponentActivity() {

    companion object {
        // Constante EXTRA_REFUGIO_ID: valor fijo definido en tiempo de compilación
        const val EXTRA_REFUGIO_ID = "refugio_id"
    }

    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: RefugioDifusionViewModel by viewModels()

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)
        // Constante refugioId: valor inmutable que no cambia tras su asignación
        val refugioId = intent.getStringExtra(EXTRA_REFUGIO_ID).orEmpty()

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
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
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función RefugioDifusionScreen: define la lógica de esta operación
fun RefugioDifusionScreen(viewModel: RefugioDifusionViewModel, refugioId: String, onBackClick: () -> Unit) {
    LaunchedEffect(refugioId) { viewModel.cargar(refugioId) }
    // Constante state: valor inmutable que no cambia tras su asignación
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
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

// Anotación que marca esta función como una función de composición de UI
@Composable
private fun RefugioDifusionContenido(refugio: Refugio, onBackClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context: Context ->
                PlayerView(context).apply {
                    useController = true
                    // Configura el reproductor multimedia ExoPlayer para streaming de video
                    player = ExoPlayer.Builder(context).build().apply {
                        // Configura el reproductor multimedia ExoPlayer para streaming de video
                        setMediaItem(MediaItem.fromUri(Uri.parse(refugio.videoUrl)))
                        prepare()
                        playWhenReady = true
                        // Configura el reproductor multimedia ExoPlayer para streaming de video
                        repeatMode = ExoPlayer.REPEAT_MODE_ALL
                    }
                }
            },
            onRelease = { playerView ->
                // Libera los recursos del reproductor multimedia
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
