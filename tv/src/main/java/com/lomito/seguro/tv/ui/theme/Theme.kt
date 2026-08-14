// Paquete: com.lomito.seguro.tv.ui.theme
package com.lomito.seguro.tv.ui.theme

// Importa componente de Jetpack Compose
import androidx.compose.runtime.Composable
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa la dependencia necesaria: MaterialTheme
import androidx.tv.material3.MaterialTheme
// Importa la dependencia necesaria: darkColorScheme
import androidx.tv.material3.darkColorScheme

// Constante LomitoOrange: valor inmutable que no cambia tras su asignación
val LomitoOrange = Color(0xFFFF8A00)
// Constante LomitoOrangeVariant: valor inmutable que no cambia tras su asignación
val LomitoOrangeVariant = Color(0xFFFFB454)
// Constante LomitoBackground: valor inmutable que no cambia tras su asignación
val LomitoBackground = Color(0xFF121212)
// Constante LomitoSurface: valor inmutable que no cambia tras su asignación
val LomitoSurface = Color(0xFF1E1E1E)
// Constante LomitoSurfaceAlt: valor inmutable que no cambia tras su asignación
val LomitoSurfaceAlt = Color(0xFF262626)
// Constante LomitoOnSurface: valor inmutable que no cambia tras su asignación
val LomitoOnSurface = Color(0xFFF5F5F5)
// Constante LomitoOnSurfaceMuted: valor inmutable que no cambia tras su asignación
val LomitoOnSurfaceMuted = Color(0xFFA0A0A0)
// Constante LomitoAlertRed: valor inmutable que no cambia tras su asignación
val LomitoAlertRed = Color(0xFFE53935)
// Constante LomitoFoundGreen: valor inmutable que no cambia tras su asignación
val LomitoFoundGreen = Color(0xFF4CAF50)

// Constante LomitoTvColorScheme: valor inmutable que no cambia tras su asignación
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
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función LomitoTvTheme: define la lógica de esta operación
fun LomitoTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LomitoTvColorScheme,
        content = content
    )
}
