// Paquete: com.lomito.seguro.wear.ui.dashboard
package com.lomito.seguro.wear.ui.dashboard

// Importa la dependencia necesaria: Manifest
import android.Manifest
// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa la dependencia necesaria: PackageManager
import android.content.pm.PackageManager
// Importa la dependencia necesaria: Build
import android.os.Build
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa la dependencia necesaria: ActivityCompat
import androidx.core.app.ActivityCompat
// Importa el contexto de Android
import androidx.core.content.ContextCompat
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.*
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.grid.GridCells
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.grid.items
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.CircleShape
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.RoundedCornerShape
// Importa componente de Jetpack Compose
import androidx.compose.runtime.*
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Brush
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.style.TextAlign
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa la dependencia necesaria: PollingService
import com.lomito.seguro.wear.data.PollingService
// Importa la dependencia necesaria: MarcarPerdidaActivity
import com.lomito.seguro.wear.ui.mascota.MarcarPerdidaActivity
// Importa la dependencia necesaria: MascotaListActivity
import com.lomito.seguro.wear.ui.mascota.MascotaListActivity
// Importa la dependencia necesaria: ReportarAvistamientoActivity
import com.lomito.seguro.wear.ui.report.ReportarAvistamientoActivity
// Importa la dependencia necesaria: SettingsActivity
import com.lomito.seguro.wear.ui.settings.SettingsActivity

/**
 * [Modelo de datos de las opciones del menú en el Dashboard]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [id]: Identificador único de la opción
 * - [icon]: Icono o emoji que lo representa
 * - [title]: Título a mostrar
 * - [action]: Acción a ejecutar al hacer clic
 */
// Clase de datos MenuItem: modelo inmutable con propiedades de dominio
data class MenuItem(
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String,
    // Constante icon: valor inmutable que no cambia tras su asignación
    val icon: String,
    // Constante title: valor inmutable que no cambia tras su asignación
    val title: String,
    // Constante action: valor inmutable que no cambia tras su asignación
    val action: () -> Unit
)

/**
 * [Actividad principal del menú de inicio]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mostrar las diferentes opciones de navegación del reloj]
 * - [Iniciar los servicios necesarios en segundo plano]
 */
// Activity DashboardActivity: pantalla principal que gestiona el ciclo de vida
class DashboardActivity : ComponentActivity() {
    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)

        // ✅ Solicitar permisos para notificaciones en Android 13+
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // Constante prefs: valor inmutable que no cambia tras su asignación
        val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
        // Constante hasMascota: valor inmutable que no cambia tras su asignación
        val hasMascota = (prefs.getString("mascota_activa_id", "") ?: "").isNotEmpty()

        // ✅ Iniciar PollingService
        // Constante serviceIntent: valor inmutable que no cambia tras su asignación
        val serviceIntent = Intent(this, PollingService::class.java)
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            DashboardScreen(
                hasMascota = hasMascota,
                onNavigateTo = { target ->
                    // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
                    when (target) {
                        "mascotas" -> startActivity(Intent(this, MascotaListActivity::class.java))
                        "reportar_avistamiento" -> startActivity(Intent(this, ReportarAvistamientoActivity::class.java))
                        "settings" -> startActivity(Intent(this, SettingsActivity::class.java))
                    }
                }
            )
        }
    }
}

// 🎨 Paleta temática "mascotas perdidas"
// Constante BgTop: valor inmutable que no cambia tras su asignación
private val BgTop = Color(0xFF1B1430)
// Constante BgBottom: valor inmutable que no cambia tras su asignación
private val BgBottom = Color(0xFF0D0B1A)
// Constante CardBg1: valor inmutable que no cambia tras su asignación
private val CardBg1 = Color(0xFF2A2350) // Mis Mascotas (azul-violeta)
// Constante CardBg2: valor inmutable que no cambia tras su asignación
private val CardBg2 = Color(0xFF4A1F2E) // Marcar Perdida (rojo oscuro)
// Constante CardBg3: valor inmutable que no cambia tras su asignación
private val CardBg3 = Color(0xFF1F3A3A) // Reportar (teal oscuro)
// Constante CardBg4: valor inmutable que no cambia tras su asignación
private val CardBg4 = Color(0xFF2E2A1F) // Config (ámbar oscuro)
// Constante AccentGreen: valor inmutable que no cambia tras su asignación
private val AccentGreen = Color(0xFF4CD97B)
// Constante AccentOrange: valor inmutable que no cambia tras su asignación
private val AccentOrange = Color(0xFFFFA94D)

/**
 * [Pantalla principal del menú de opciones (Dashboard)]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [hasMascota]: Indica si existe una mascota configurada
 * - [onNavigateTo]: Callback para manejar la navegación entre pantallas
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función DashboardScreen: define la lógica de esta operación
fun DashboardScreen(
    hasMascota: Boolean,
    onNavigateTo: (String) -> Unit
) {
    // Constante menuItems: valor inmutable que no cambia tras su asignación
    val menuItems = listOf(
        MenuItem("mascotas", "🐾", "Mascotas") { onNavigateTo("mascotas") } to CardBg1,
        MenuItem("reportar_avistamiento", "📍", "Avistar") { onNavigateTo("reportar_avistamiento") } to CardBg3,
        MenuItem("settings", "⚙️", "Ajustes") { onNavigateTo("settings") } to CardBg4
    )

    Scaffold(
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(BgTop, BgBottom))
                ),
            contentAlignment = Alignment.Center
        ) {
            // ✅ Todo el contenido va dentro de un padding generoso
            // y centrado, calculado para no salir del área visible
            // circular del reloj (evita que las esquinas se corten).
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ✅ Header compacto y centrado: no se corta porque
                // está cerca del centro vertical, lejos del borde curvo.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(text = "🐾", fontSize = 13.sp)
                    Text(
                        text = "Lomito Seguro",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                color = if (hasMascota) AccentGreen else AccentOrange,
                                shape = CircleShape
                            )
                    )
                }

                // ✅ Grid 2x2 dimensionado para caber completo dentro
                // del círculo, con textos cortos de una sola línea.
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .weight(1f, fill = false)
                ) {
                    items(menuItems) { (item, color) ->
                        GridMenuItem(
                            item = item,
                            bgColor = color,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}

// Anotación que marca esta función como una función de composición de UI
@Composable
// Función GridMenuItem: define la lógica de esta operación
fun GridMenuItem(
    item: MenuItem,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = item.action,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(bgColor, bgColor.copy(alpha = 0.6f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = item.icon,
                    fontSize = 22.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    lineHeight = 10.sp
                )
            }
        }
    }
}