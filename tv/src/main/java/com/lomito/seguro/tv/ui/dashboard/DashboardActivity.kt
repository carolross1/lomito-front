// Paquete: com.lomito.seguro.tv.ui.dashboard
package com.lomito.seguro.tv.ui.dashboard

// Importa la clase Intent para navegación entre componentes
import android.content.Intent
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
import androidx.compose.foundation.layout.Arrangement
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Box
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.Column
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.RoundedCornerShape
// Importa componente de Jetpack Compose
import androidx.compose.runtime.Composable
// Importa componente de Jetpack Compose
import androidx.compose.runtime.collectAsState
// Importa componente de Jetpack Compose
import androidx.compose.runtime.getValue
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
import androidx.compose.ui.layout.ContentScale
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.style.TextOverflow
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.LazyRow
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.items
// Importa la dependencia necesaria: Card
import androidx.tv.material3.Card
// Importa la dependencia necesaria: CardDefaults
import androidx.tv.material3.CardDefaults
// Importa la dependencia necesaria: MaterialTheme
import androidx.tv.material3.MaterialTheme
// Importa la dependencia necesaria: Surface
import androidx.tv.material3.Surface
// Importa la dependencia necesaria: Text
import androidx.tv.material3.Text
// Importa componente de Jetpack Compose
import coil.compose.AsyncImage
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.tv.data.model.Mascota
// Importa la dependencia necesaria: Refugio
import com.lomito.seguro.tv.data.model.Refugio
// Importa la dependencia necesaria: MascotaDetalleActivity
import com.lomito.seguro.tv.ui.detalle.MascotaDetalleActivity
// Importa la dependencia necesaria: RefugioDifusionActivity
import com.lomito.seguro.tv.ui.refugio.RefugioDifusionActivity
// Importa la dependencia necesaria: LomitoFoundGreen
import com.lomito.seguro.tv.ui.theme.LomitoFoundGreen
// Importa la dependencia necesaria: LomitoAlertRed
import com.lomito.seguro.tv.ui.theme.LomitoAlertRed
// Importa la dependencia necesaria: LomitoOrange
import com.lomito.seguro.tv.ui.theme.LomitoOrange
// Importa la dependencia necesaria: LomitoSurfaceAlt
import com.lomito.seguro.tv.ui.theme.LomitoSurfaceAlt
// Importa la dependencia necesaria: LomitoTvTheme
import com.lomito.seguro.tv.ui.theme.LomitoTvTheme
// Importa la dependencia necesaria: toAbsoluteUrl
import com.lomito.seguro.tv.util.toAbsoluteUrl

/**
 * [Actividad principal del Dashboard para Android TV]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mostrar el mural comunitario de mascotas perdidas y encontradas]
 * - [Mostrar el directorio de refugios locales]
 */
// Activity DashboardActivity: pantalla principal que gestiona el ciclo de vida
class DashboardActivity : ComponentActivity() {

    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: DashboardViewModel by viewModels()

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)
        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
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

/**
 * [Pantalla componible del Dashboard]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - viewModel: [ViewModel que provee el estado de la pantalla]
 * - onMascotaClick: [Callback al seleccionar una mascota]
 * - onRefugioClick: [Callback al seleccionar un refugio]
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función DashboardScreen: define la lógica de esta operación
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onMascotaClick: (Mascota) -> Unit,
    onRefugioClick: (Refugio) -> Unit
) {
    // Constante state: valor inmutable que no cambia tras su asignación
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
                    // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
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
                    // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
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

// Anotación que marca esta función como una función de composición de UI
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

// Anotación que marca esta función como una función de composición de UI
@Composable
private fun MascotaMuralCard(mascota: Mascota, onClick: () -> Unit) {
    // Constante perdida: valor inmutable que no cambia tras su asignación
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

// Anotación que marca esta función como una función de composición de UI
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
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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

// Anotación que marca esta función como una función de composición de UI
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
