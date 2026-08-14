// MapaView.kt - OSMDroid con SSL ignorado
// Paquete: com.lomito.seguro.tv.ui.detalle
package com.lomito.seguro.tv.ui.detalle

// Importa el contexto de Android
import android.content.Context
// Importa componente de Jetpack Compose
import androidx.compose.runtime.Composable
// Importa componente de Jetpack Compose
import androidx.compose.runtime.remember
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
import androidx.compose.ui.platform.LocalContext
// Importa componente de Jetpack Compose
import androidx.compose.ui.viewinterop.AndroidView
// Importa la dependencia necesaria: ReporteVista
import com.lomito.seguro.tv.data.model.ReporteVista
// Importa la dependencia necesaria: Configuration
import org.osmdroid.config.Configuration
// Importa la dependencia necesaria: TileSourceFactory
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
// Importa la dependencia necesaria: GeoPoint
import org.osmdroid.util.GeoPoint
// Importa componentes de la interfaz gráfica
import org.osmdroid.views.MapView
// Importa la dependencia necesaria: Marker
import org.osmdroid.views.overlay.Marker
// Importa la dependencia necesaria: Polyline
import org.osmdroid.views.overlay.Polyline
// Importa la dependencia necesaria: File
import java.io.File
// Importa la dependencia necesaria: HttpsURLConnection
import javax.net.ssl.HttpsURLConnection
// Importa el contexto de Android
import javax.net.ssl.SSLContext
// Importa la dependencia necesaria: TrustManager
import javax.net.ssl.TrustManager
// Importa la dependencia necesaria: X509TrustManager
import javax.net.ssl.X509TrustManager
// Importa la dependencia necesaria: X509Certificate
import java.security.cert.X509Certificate

/**
 * [Componente de Mapa interactivo para Android TV]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - lat: [Latitud central del mapa]
 * - lng: [Longitud central del mapa]
 * - reportes: [Lista de reportes a mostrar como marcadores]
 * - modifier: [Modificador para el componente visual]
 */
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MapaView: define la lógica de esta operación
fun MapaView(
    lat: Double,
    lng: Double,
    reportes: List<ReporteVista> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Constante context: valor inmutable que no cambia tras su asignación
    val context = LocalContext.current

    // ✅ Configuración para ignorar SSL (solo para desarrollo)
    remember {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Constante trustAllCerts: valor inmutable que no cambia tras su asignación
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                // Sobreescribe la función checkClientTrusted de la clase padre
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                // Sobreescribe la función checkServerTrusted de la clase padre
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                // Sobreescribe la función getAcceptedIssuers de la clase padre
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            // Constante sslContext: valor inmutable que no cambia tras su asignación
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Configuration.getInstance().load(
            context,
            // Accede al almacenamiento clave-valor persistente de la aplicación
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().osmdroidBasePath = File(context.cacheDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        // ✅ Usar tile source con HTTP en lugar de HTTPS
        System.setProperty("http.agent", "")
        Unit
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setBuiltInZoomControls(true)
                setMultiTouchControls(true)
                setMinZoomLevel(5.0)
                setMaxZoomLevel(19.0)
            }
        },
        update = { mapView ->
            mapView.overlays.clear()

            // Constante puntos: valor inmutable que no cambia tras su asignación
            val puntos = mutableListOf<GeoPoint>()

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (reportes.isNotEmpty()) {
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                reportes.forEach { reporte ->
                    // Constante punto: valor inmutable que no cambia tras su asignación
                    val punto = GeoPoint(reporte.latitud, reporte.longitud)
                    puntos.add(punto)

                    // Constante marker: valor inmutable que no cambia tras su asignación
                    val marker = Marker(mapView)
                    marker.position = punto
                    marker.title = "Reporte #${reporte.id}"
                    marker.snippet = reporte.direccion
                    mapView.overlays.add(marker)
                }
            } else {
                // Constante punto: valor inmutable que no cambia tras su asignación
                val punto = GeoPoint(lat, lng)
                puntos.add(punto)

                // Constante marker: valor inmutable que no cambia tras su asignación
                val marker = Marker(mapView)
                marker.position = punto
                marker.title = "Última ubicación"
                mapView.overlays.add(marker)
            }

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (puntos.size > 1) {
                // Constante polyline: valor inmutable que no cambia tras su asignación
                val polyline = Polyline()
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                puntos.forEach { punto ->
                    polyline.addPoint(punto)
                }
                polyline.outlinePaint.color = android.graphics.Color.RED
                polyline.outlinePaint.strokeWidth = 5f
                mapView.overlays.add(polyline)
            }

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (puntos.isNotEmpty()) {
                mapView.controller.setCenter(puntos.first())
                mapView.controller.setZoom(15.0)
            }

            mapView.invalidate()
        }
    )
}