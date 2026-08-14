package com.lomito.seguro.tv.ui.perfil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.lomito.seguro.tv.data.model.Mascota
import com.lomito.seguro.tv.data.model.ReporteVista
import com.lomito.seguro.tv.ui.detalle.MapaView
import com.lomito.seguro.tv.ui.theme.LomitoAlertRed
import com.lomito.seguro.tv.ui.theme.LomitoOrange
import com.lomito.seguro.tv.ui.theme.LomitoSurfaceAlt
import com.lomito.seguro.tv.ui.theme.LomitoTvTheme

/**
 * [Actividad de Perfil Completo de Mascota para Android TV]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mostrar el perfil detallado de la mascota]
 * - [Mostrar el historial completo de reportes en un mapa y línea de tiempo]
 */
class MascotaPerfilActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MASCOTA_ID = "mascota_id"
    }

    private val viewModel: MascotaPerfilViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mascotaId = intent.getStringExtra(EXTRA_MASCOTA_ID).orEmpty()

        setContent {
            LomitoTvTheme {
                MascotaPerfilScreen(viewModel = viewModel, mascotaId = mascotaId)
            }
        }
    }
}

/**
 * [Pantalla componible del Perfil de Mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - viewModel: [ViewModel que provee los datos del perfil]
 * - mascotaId: [Identificador de la mascota a consultar]
 */
@Composable
fun MascotaPerfilScreen(viewModel: MascotaPerfilViewModel, mascotaId: String) {
    LaunchedEffect(mascotaId) { viewModel.cargar(mascotaId) }
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp)
    ) {
        when {
            state.cargando -> Text(
                text = "Cargando perfil…",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.mascota == null -> Text(
                text = "No se encontró información de esta mascota.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> MascotaPerfilContenido(mascota = state.mascota!!, reportes = state.reportes)
        }
    }
}

@Composable
private fun MascotaPerfilContenido(mascota: Mascota, reportes: List<ReporteVista>) {
    val perdida = mascota.estado.equals("PERDIDA", ignoreCase = true)

    val ultimoReporte = reportes.maxByOrNull { it.timestamp }
    val lat = ultimoReporte?.latitud ?: mascota.latitud
    val lng = ultimoReporte?.longitud ?: mascota.longitud

    Column(modifier = Modifier.fillMaxSize()) {
        Row {
            Text(
                text = mascota.nombre,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                colors = androidx.tv.material3.SurfaceDefaults.colors(
                    containerColor = if (perdida) LomitoAlertRed else LomitoOrange
                )
            ) {
                Text(
                    text = if (perdida) "Perdida" else mascota.estado,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        Text(
            text = "${reportes.size} reportes de avistamiento registrados",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxHeight().width(380.dp)) {
                Text(
                    text = "LÍNEA DE TIEMPO",
                    color = LomitoOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                if (reportes.isEmpty()) {
                    Text(
                        text = "Aún no hay avistamientos reportados.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn {
                        items(reportes, key = { it.id }) { reporte ->
                            ReporteRow(reporte)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
                Text(
                    text = "📍 MAPA DE AVISTAMIENTOS",
                    color = LomitoOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (lat != null && lng != null) {
                    MapaView(
                        lat = lat,
                        lng = lng,
                        reportes = reportes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LomitoSurfaceAlt)
                    )

                    Text(
                        text = "Última ubicación: ${ultimoReporte?.direccion?.ifBlank { "Sin dirección" } ?: "Sin dirección"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LomitoSurfaceAlt)
                    ) {
                        Text(
                            text = "📍 Ubicación no disponible",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReporteRow(reporte: ReporteVista) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(LomitoOrange)
        )
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(
                text = reporte.direccion.ifBlank { "Ubicación sin dirección" },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = reporte.timestamp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}