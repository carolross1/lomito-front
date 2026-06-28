package com.lomito.seguro.wear.ui.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.lomito.seguro.wear.data.PollingService
import com.lomito.seguro.wear.ui.mascota.MarcarPerdidaActivity
import com.lomito.seguro.wear.ui.mascota.MascotaListActivity
import com.lomito.seguro.wear.ui.report.ReportarAvistamientoActivity
import com.lomito.seguro.wear.ui.settings.SettingsActivity

data class MenuItem(
    val id: String,
    val icon: String,
    val title: String,
    val action: () -> Unit
)

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Solicitar permisos para notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

        val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
        val hasMascota = (prefs.getString("mascota_activa_id", "") ?: "").isNotEmpty()

        // ✅ Iniciar PollingService
        val serviceIntent = Intent(this, PollingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setContent {
            DashboardScreen(
                hasMascota = hasMascota,
                onNavigateTo = { target ->
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
private val BgTop = Color(0xFF1B1430)
private val BgBottom = Color(0xFF0D0B1A)
private val CardBg1 = Color(0xFF2A2350) // Mis Mascotas (azul-violeta)
private val CardBg2 = Color(0xFF4A1F2E) // Marcar Perdida (rojo oscuro)
private val CardBg3 = Color(0xFF1F3A3A) // Reportar (teal oscuro)
private val CardBg4 = Color(0xFF2E2A1F) // Config (ámbar oscuro)
private val AccentGreen = Color(0xFF4CD97B)
private val AccentOrange = Color(0xFFFFA94D)

@Composable
fun DashboardScreen(
    hasMascota: Boolean,
    onNavigateTo: (String) -> Unit
) {
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

@Composable
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