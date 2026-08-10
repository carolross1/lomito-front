package com.lomito.seguro.tv.ui.detalle

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.lomito.seguro.tv.data.model.Mascota
import com.lomito.seguro.tv.data.model.ReporteVista
import com.lomito.seguro.tv.ui.perfil.MascotaPerfilActivity
import com.lomito.seguro.tv.ui.theme.LomitoAlertRed
import com.lomito.seguro.tv.ui.theme.LomitoFoundGreen
import com.lomito.seguro.tv.ui.theme.LomitoOrange
import com.lomito.seguro.tv.ui.theme.LomitoSurfaceAlt
import com.lomito.seguro.tv.ui.theme.LomitoTvTheme

/**
 * Pantalla "Detalle de mascota" del diagrama de flujo: la comunidad ve la
 * ficha completa de una mascota reportada en el mural (foto, estado, última
 * ubicación vista) y puede confirmar que también la vio, o pasar al perfil
 * completo con su historial de reportes.
 */
class MascotaDetalleActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MASCOTA_ID = "mascota_id"
    }

    private val viewModel: MascotaDetalleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mascotaId = intent.getStringExtra(EXTRA_MASCOTA_ID).orEmpty()

        setContent {
            LomitoTvTheme {
                MascotaDetalleScreen(
                    viewModel = viewModel,
                    mascotaId = mascotaId,
                    onVerPerfilCompleto = {
                        startActivity(
                            Intent(this, MascotaPerfilActivity::class.java)
                                .putExtra(MascotaPerfilActivity.EXTRA_MASCOTA_ID, mascotaId)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun MascotaDetalleScreen(
    viewModel: MascotaDetalleViewModel,
    mascotaId: String,
    onVerPerfilCompleto: () -> Unit
) {
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
                text = "Cargando ficha…",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.mascota == null -> Text(
                text = "No se encontró información de esta mascota.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> MascotaDetalleContenido(
                mascota = state.mascota!!,
                ultimoReporte = state.ultimoReporte,
                confirmando = state.confirmando,
                confirmado = state.confirmado,
                onAyudar = { viewModel.confirmarAvistamiento() },
                onVerPerfilCompleto = onVerPerfilCompleto
            )
        }
    }
}

@Composable
private fun MascotaDetalleContenido(
    mascota: Mascota,
    ultimoReporte: ReporteVista?,
    confirmando: Boolean,
    confirmado: Boolean,
    onAyudar: () -> Unit,
    onVerPerfilCompleto: () -> Unit
) {
    val perdida = mascota.estado.equals("PERDIDA", ignoreCase = true)

    Row(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = mascota.fotoUrl,
            contentDescription = mascota.nombre,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(420.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(LomitoSurfaceAlt)
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 40.dp)
        ) {
            Row {
                Text(
                    text = mascota.nombre,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    colors = androidx.tv.material3.SurfaceDefaults.colors(
                        containerColor = if (perdida) LomitoAlertRed else LomitoFoundGreen
                    )
                ) {
                    Text(
                        text = if (perdida) "Perdida" else "Encontrada",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            InfoRow(label = "Raza / especie", valor = "${mascota.raza.ifBlank { "—" }} · ${mascota.especie}")
            InfoRow(label = "Edad", valor = "${mascota.edad} años")
            InfoRow(label = "Color", valor = mascota.color.ifBlank { "No especificado" })
            InfoRow(
                label = "Última ubicación vista",
                valor = ultimoReporte?.direccion?.ifBlank { "Sin dirección registrada" }
                    ?: "Aún sin reportes de avistamiento"
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row {
                Button(onClick = onAyudar) {
                    Text(
                        text = when {
                            confirmado -> "¡Gracias por ayudar!"
                            confirmando -> "Enviando…"
                            else -> "Ayudar (confirmar avistado)"
                        }
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onVerPerfilCompleto) {
                    Text(text = "Ver perfil completo")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, valor: String) {
    Column(modifier = Modifier.padding(top = 18.dp)) {
        Text(
            text = label.uppercase(),
            color = LomitoOrange,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = valor,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
