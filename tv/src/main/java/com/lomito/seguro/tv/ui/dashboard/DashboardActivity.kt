package com.lomito.seguro.tv.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.lomito.seguro.tv.data.model.Mascota
import com.lomito.seguro.tv.data.model.Refugio
import com.lomito.seguro.tv.ui.detalle.MascotaDetalleActivity
import com.lomito.seguro.tv.ui.refugio.RefugioDifusionActivity
import com.lomito.seguro.tv.ui.theme.LomitoFoundGreen
import com.lomito.seguro.tv.ui.theme.LomitoAlertRed
import com.lomito.seguro.tv.ui.theme.LomitoOrange
import com.lomito.seguro.tv.ui.theme.LomitoSurfaceAlt
import com.lomito.seguro.tv.ui.theme.LomitoTvTheme
import com.lomito.seguro.tv.util.toAbsoluteUrl

/**
 * Pantalla principal del módulo Smart TV. Pensada para un TV en la sala de un
 * refugio o espacio comunitario: no requiere login, se controla con el
 * control remoto (D-pad) y se auto-refresca sola.
 *
 * Corresponde a la pantalla "Dashboard" del diagrama de flujo (mural
 * comunitario de mascotas perdidas/encontradas + refugios locales).
 */
class DashboardActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LomitoTvTheme {
                DashboardScreen(
                    viewModel = viewModel,
                    onMascotaClick = { mascota ->
                        startActivity(
                            Intent(this, MascotaDetalleActivity::class.java)
                                .putExtra(MascotaDetalleActivity.EXTRA_MASCOTA_ID, mascota.id)
                        )
                    },
                    onRefugioClick = { refugio ->
                        startActivity(
                            Intent(this, RefugioDifusionActivity::class.java)
                                .putExtra(RefugioDifusionActivity.EXTRA_REFUGIO_ID, refugio.id)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onMascotaClick: (Mascota) -> Unit,
    onRefugioClick: (Refugio) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp)
        ) {
            DashboardHeader()

            Column(modifier = Modifier.padding(top = 32.dp)) {
                Text(
                    text = "MURAL DE MASCOTAS PERDIDAS / ENCONTRADAS",
                    color = LomitoOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Box(modifier = Modifier.padding(top = 16.dp)) {
                    when {
                        state.cargando -> Text(
                            text = "Cargando mural comunitario…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.mascotas.isEmpty() -> Text(
                            text = "Sin reportes por el momento. ¡Buenas noticias!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            items(state.mascotas, key = { it.id }) { mascota ->
                                MascotaMuralCard(mascota = mascota, onClick = { onMascotaClick(mascota) })
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(top = 40.dp)) {
                Text(
                    text = "REFUGIOS LOCALES",
                    color = LomitoOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Box(modifier = Modifier.padding(top = 16.dp)) {
                    when {
                        state.cargando -> Text(
                            text = "Cargando refugios…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.refugios.isEmpty() -> Text(
                            text = "No hay refugios registrados todavía.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            items(state.refugios, key = { it.id }) { refugio ->
                                RefugioCard(refugio = refugio, onClick = { onRefugioClick(refugio) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader() {
    Column {
        Text(
            text = "🐾 Lomito Seguro",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp
        )
        Text(
            text = "Comunidad · Dolores Hidalgo, Guanajuato",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun MascotaMuralCard(mascota: Mascota, onClick: () -> Unit) {
    val perdida = mascota.estado.equals("PERDIDA", ignoreCase = true)

    Card(
        onClick = onClick,
        modifier = Modifier.width(220.dp),
        colors = CardDefaults.colors(containerColor = LomitoSurfaceAlt),
        border = CardDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(3.dp, LomitoOrange),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        shape = CardDefaults.shape(RoundedCornerShape(12.dp))
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.3f)) {
                AsyncImage(
                    model = mascota.fotoUrl.toAbsoluteUrl(),
                    contentDescription = mascota.nombre,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                EstadoBadge(
                    texto = if (perdida) "Perdida" else "Encontrada",
                    color = if (perdida) LomitoAlertRed else LomitoFoundGreen,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = mascota.nombre,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${mascota.raza.ifBlank { mascota.especie }} · ${mascota.edad} años",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RefugioCard(refugio: Refugio, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(260.dp).height(140.dp),
        colors = CardDefaults.colors(containerColor = LomitoSurfaceAlt),
        border = CardDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(3.dp, LomitoOrange),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        shape = CardDefaults.shape(RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = refugio.nombre,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = refugio.direccion,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
            if (!refugio.videoUrl.isNullOrBlank()) {
                Text(
                    text = "● EN VIVO",
                    color = LomitoAlertRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun EstadoBadge(texto: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        colors = androidx.tv.material3.SurfaceDefaults.colors(containerColor = color)
    ) {
        Text(
            text = texto,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
