package com.lomito.seguro.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val LomitoOrange = Color(0xFFFF8A00)
val LomitoOrangeVariant = Color(0xFFFFB454)
val LomitoBackground = Color(0xFF121212)
val LomitoSurface = Color(0xFF1E1E1E)
val LomitoSurfaceAlt = Color(0xFF262626)
val LomitoOnSurface = Color(0xFFF5F5F5)
val LomitoOnSurfaceMuted = Color(0xFFA0A0A0)
val LomitoAlertRed = Color(0xFFE53935)
val LomitoFoundGreen = Color(0xFF4CAF50)

private val LomitoTvColorScheme = darkColorScheme(
    primary = LomitoOrange,
    onPrimary = Color.Black,
    secondary = LomitoOrangeVariant,
    background = LomitoBackground,
    onBackground = LomitoOnSurface,
    surface = LomitoSurface,
    onSurface = LomitoOnSurface,
    surfaceVariant = LomitoSurfaceAlt,
    onSurfaceVariant = LomitoOnSurfaceMuted,
    error = LomitoAlertRed
)

/**
 * [Tema principal de la aplicación Lomito Seguro TV]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - content: [Contenido componible que será estilizado por el tema]
 */
@Composable
fun LomitoTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LomitoTvColorScheme,
        content = content
    )
}
