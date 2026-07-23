package com.lomito.seguro.tv.ui.perfil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.lomito.seguro.tv.data.model.Mascota
import com.lomito.seguro.tv.data.model.ReporteVista
import com.lomito.seguro.tv.ui.theme.LomitoAlertRed
import com.lomito.seguro.tv.ui.theme.LomitoOrange
import com.lomito.seguro.tv.ui.theme.LomitoSurfaceAlt
import com.lomito.seguro.tv.ui.theme.LomitoTvTheme

/**
 * Perfil completo de una mascota: línea de tiempo de todos sus reportes de
 * avistamiento y una visualización simple de la ruta que ha seguido,
 * conectando los puntos reportados en orden cronológico.
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
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        Text(
            text = "${reportes.size} reportes de avistamiento registrados",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxHeight().width(480.dp)) {
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

            Spacer(modifier = Modifier.width(32.dp))

            Column(modifier = Modifier.fillMaxHeight()) {
                Text(
                    text = "RUTA REPORTADA",
                    color = LomitoOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(LomitoSurfaceAlt)
                ) {
                    RutaVisual(reportes = reportes)
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
            .padding(vertical = 10.dp)
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = reporte.timestamp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Visualización simple de la ruta: ubica cada reporte (lat/long) de forma
 * proporcional dentro del panel y traza una línea punteada entre ellos en
 * orden cronológico. No es un mapa real (no hay SDK de mapas en este
 * módulo), pero comunica visualmente el trayecto de avistamientos.
 */
@Composable
private fun RutaVisual(reportes: List<ReporteVista>) {
    if (reportes.size < 2) {
        Text(
            text = if (reportes.isEmpty()) "Sin puntos para trazar la ruta" else "Se necesita más de un reporte para trazar una ruta",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    val cronologico = reportes.sortedBy { it.timestamp }
    val lats = cronologico.map { it.latitud }
    val lngs = cronologico.map { it.longitud }
    val latRange = ((lats.maxOrNull() ?: 0.0) - (lats.minOrNull() ?: 0.0)).let { if (it == 0.0) 1.0 else it }
    val lngRange = ((lngs.maxOrNull() ?: 0.0) - (lngs.minOrNull() ?: 0.0)).let { if (it == 0.0) 1.0 else it }
    val minLat = lats.minOrNull() ?: 0.0
    val minLng = lngs.minOrNull() ?: 0.0

    Canvas(modifier = Modifier.fillMaxSize().padding(28.dp)) {
        val puntos = cronologico.map { reporte ->
            val nx = ((reporte.longitud - minLng) / lngRange).toFloat()
            val ny = 1f - ((reporte.latitud - minLat) / latRange).toFloat()
            Offset(nx * size.width, ny * size.height)
        }

        for (i in 0 until puntos.size - 1) {
            drawLine(
                color = LomitoOrange,
                start = puntos[i],
                end = puntos[i + 1],
                strokeWidth = 4f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), 0f)
            )
        }
        puntos.forEachIndexed { index, offset ->
            drawCircle(
                color = if (index == puntos.size - 1) LomitoAlertRed else LomitoOrange,
                radius = if (index == puntos.size - 1) 10f else 7f,
                center = offset
            )
        }
    }
}
