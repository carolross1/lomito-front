// Paquete: com.lomito.seguro.tv.ui.perfil
package com.lomito.seguro.tv.ui.perfil

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
import androidx.compose.foundation.layout.Row
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Spacer
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.aspectRatio
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.fillMaxHeight
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.fillMaxSize
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.fillMaxWidth
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.height
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.padding
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.width
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.LazyColumn
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.items
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.RoundedCornerShape
// Importa componente de Jetpack Compose
import androidx.compose.runtime.Composable
// Importa componente de Jetpack Compose
import androidx.compose.runtime.LaunchedEffect
// Importa componente de Jetpack Compose
import androidx.compose.runtime.collectAsState
// Importa componente de Jetpack Compose
import androidx.compose.runtime.getValue
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
import androidx.compose.ui.draw.clip
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa la dependencia necesaria: MaterialTheme
import androidx.tv.material3.MaterialTheme
// Importa la dependencia necesaria: Surface
import androidx.tv.material3.Surface
// Importa la dependencia necesaria: Text
import androidx.tv.material3.Text
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.tv.data.model.Mascota
// Importa la dependencia necesaria: ReporteVista
import com.lomito.seguro.tv.data.model.ReporteVista
// Importa componentes de la interfaz gráfica
import com.lomito.seguro.tv.ui.detalle.MapaView
// Importa la dependencia necesaria: LomitoAlertRed
import com.lomito.seguro.tv.ui.theme.LomitoAlertRed
// Importa la dependencia necesaria: LomitoOrange
import com.lomito.seguro.tv.ui.theme.LomitoOrange
// Importa la dependencia necesaria: LomitoSurfaceAlt
import com.lomito.seguro.tv.ui.theme.LomitoSurfaceAlt
// Importa la dependencia necesaria: LomitoTvTheme
import com.lomito.seguro.tv.ui.theme.LomitoTvTheme

/**
 * [Actividad de Perfil Completo de Mascota para Android TV]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mostrar el perfil detallado de la mascota]
 * - [Mostrar el historial completo de reportes en un mapa y línea de tiempo]
 */
// Activity MascotaPerfilActivity: pantalla principal que gestiona el ciclo de vida
class MascotaPerfilActivity : ComponentActivity() {

    companion object {
        // Constante EXTRA_MASCOTA_ID: valor fijo definido en tiempo de compilación
        const val EXTRA_MASCOTA_ID = "mascota_id"
    }

    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: MascotaPerfilViewModel by viewModels()

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)
        // Constante mascotaId: valor inmutable que no cambia tras su asignación
        val mascotaId = intent.getStringExtra(EXTRA_MASCOTA_ID).orEmpty()

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
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
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MascotaPerfilScreen: define la lógica de esta operación
fun MascotaPerfilScreen(viewModel: MascotaPerfilViewModel, mascotaId: String) {
    LaunchedEffect(mascotaId) { viewModel.cargar(mascotaId) }
    // Constante state: valor inmutable que no cambia tras su asignación
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp)
    ) {
        // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
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

// Anotación que marca esta función como una función de composición de UI
@Composable
private fun MascotaPerfilContenido(mascota: Mascota, reportes: List<ReporteVista>) {
    // Constante perdida: valor inmutable que no cambia tras su asignación
    val perdida = mascota.estado.equals("PERDIDA", ignoreCase = true)

    // Constante ultimoReporte: valor inmutable que no cambia tras su asignación
    val ultimoReporte = reportes.maxByOrNull { it.timestamp }
    // Constante lat: valor inmutable que no cambia tras su asignación
    val lat = ultimoReporte?.latitud ?: mascota.latitud
    // Constante lng: valor inmutable que no cambia tras su asignación
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
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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

// Anotación que marca esta función como una función de composición de UI
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