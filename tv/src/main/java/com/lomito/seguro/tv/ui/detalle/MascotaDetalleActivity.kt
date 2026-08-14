package com.lomito.seguro.tv.ui.detalle

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
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
import com.lomito.seguro.tv.util.toAbsoluteUrl

/**
 * [Actividad de Detalle de Mascota para Android TV]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mostrar la información detallada de una mascota específica]
 * - [Permitir confirmar avistamientos solicitando número de contacto]
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

/**
 * [Pantalla componible de Detalle de Mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - viewModel: [ViewModel que gestiona el estado y lógica del detalle]
 * - mascotaId: [Identificador único de la mascota a mostrar]
 * - onVerPerfilCompleto: [Callback para navegar al perfil completo]
 */
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
                enviando = state.enviando,
                onAyudar = { viewModel.abrirDialogoContacto() },
                onVerPerfilCompleto = onVerPerfilCompleto
            )
        }

        if (state.mostrandoDialogoContacto) {
            DialogoContacto(
                numero = state.numeroContacto,
                onDigito = { viewModel.agregarDigito(it) },
                onBorrar = { viewModel.borrarDigito() },
                onCancelar = { viewModel.cerrarDialogoContacto() },
                onConfirmar = { viewModel.confirmarAvistamiento() }
            )
        }

        if (state.mostrandoConfirmacionEnvio || state.errorEnvio) {
            DialogoResultadoEnvio(
                exito = state.mostrandoConfirmacionEnvio,
                mascotaNombre = state.mascota?.nombre ?: "la mascota",
                onCerrar = { viewModel.cerrarConfirmacionEnvio() }
            )
        }
    }
}

@Composable
private fun MascotaDetalleContenido(
    mascota: Mascota,
    ultimoReporte: ReporteVista?,
    enviando: Boolean,
    onAyudar: () -> Unit,
    onVerPerfilCompleto: () -> Unit
) {
    val perdida = mascota.estado.equals("PERDIDA", ignoreCase = true)

    Row(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = mascota.fotoUrl.toAbsoluteUrl(),
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
                    colors = SurfaceDefaults.colors(
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
                Button(onClick = onAyudar, enabled = !enviando) {
                    Text(
                        text = if (enviando) "Enviando…" else "Ayudar (confirmar avistado)"
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

/**
 * Teclado numérico para pedir el contacto de quien confirma el avistamiento.
 * Se navega con el D-pad del control remoto, por eso es un grid de botones
 * en vez de un campo de texto con teclado del sistema (mismo criterio que se
 * usó para el teclado del módulo wear).
 */
@Composable
private fun DialogoContacto(
    numero: String,
    onDigito: (String) -> Unit,
    onBorrar: () -> Unit,
    onCancelar: () -> Unit,
    onConfirmar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            colors = SurfaceDefaults.colors(containerColor = LomitoSurfaceAlt)
        ) {
            // ✅ Layout horizontal (teclado a la izquierda, info/acciones a la
            // derecha) en vez de todo apilado en una sola columna: una TV es
            // mucho más ancha que alta, y apilado se salía de la pantalla y
            // dejaba el botón "Confirmar" fuera de la vista/alcance del control.
            Row(
                modifier = Modifier.padding(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filas = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("Borrar", "0", "")
                )
                Column {
                    filas.forEach { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            fila.forEach { tecla ->
                                if (tecla.isEmpty()) {
                                    Spacer(modifier = Modifier.size(56.dp))
                                } else {
                                    Button(
                                        onClick = { if (tecla == "Borrar") onBorrar() else onDigito(tecla) },
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Text(text = if (tecla == "Borrar") "⌫" else tecla, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                Column(
                    modifier = Modifier.width(320.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "¿A qué número te pueden contactar?",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Así el dueño puede pedirte más información",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
                    ) {
                        Text(
                            text = numero.ifEmpty { "__________" },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ✅ Botón para guardar/enviar el número: queda deshabilitado
                    // (y se ve apagado) hasta tener 10 dígitos.
                    Button(
                        onClick = onConfirmar,
                        enabled = numero.length >= 10
                    ) {
                        Text(text = "Confirmar avistamiento")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onCancelar) {
                        Text(text = "Cancelar")
                    }
                }
            }
        }
    }
}

/**
 * Mensaje de confirmación tras enviar el reporte: le dice claramente a quien
 * está frente a la TV si el aviso se mandó (y que el dueño fue notificado)
 * o si algo falló y debe intentar de nuevo.
 */
@Composable
private fun DialogoResultadoEnvio(
    exito: Boolean,
    mascotaNombre: String,
    onCerrar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            colors = SurfaceDefaults.colors(containerColor = LomitoSurfaceAlt)
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (exito) "✅ ¡Reporte enviado!" else "❌ No se pudo enviar",
                    color = if (exito) LomitoFoundGreen else LomitoAlertRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                )
                Text(
                    text = if (exito)
                        "Le avisamos al dueño de $mascotaNombre, se pondrá en contacto contigo."
                    else
                        "Ocurrió un problema al enviar tu reporte. Intenta de nuevo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
                )
                Button(onClick = onCerrar) {
                    Text(text = "Cerrar")
                }
            }
        }
    }
}