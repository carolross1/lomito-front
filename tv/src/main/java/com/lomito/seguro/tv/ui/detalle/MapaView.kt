// MapaView.kt - OSMDroid con SSL ignorado
package com.lomito.seguro.tv.ui.detalle

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.lomito.seguro.tv.data.model.ReporteVista
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
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
@Composable
fun MapaView(
    lat: Double,
    lng: Double,
    reportes: List<ReporteVista> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // ✅ Configuración para ignorar SSL (solo para desarrollo)
    remember {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Configuration.getInstance().load(
            context,
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

            val puntos = mutableListOf<GeoPoint>()

            if (reportes.isNotEmpty()) {
                reportes.forEach { reporte ->
                    val punto = GeoPoint(reporte.latitud, reporte.longitud)
                    puntos.add(punto)

                    val marker = Marker(mapView)
                    marker.position = punto
                    marker.title = "Reporte #${reporte.id}"
                    marker.snippet = reporte.direccion
                    mapView.overlays.add(marker)
                }
            } else {
                val punto = GeoPoint(lat, lng)
                puntos.add(punto)

                val marker = Marker(mapView)
                marker.position = punto
                marker.title = "Última ubicación"
                mapView.overlays.add(marker)
            }

            if (puntos.size > 1) {
                val polyline = Polyline()
                puntos.forEach { punto ->
                    polyline.addPoint(punto)
                }
                polyline.outlinePaint.color = android.graphics.Color.RED
                polyline.outlinePaint.strokeWidth = 5f
                mapView.overlays.add(polyline)
            }

            if (puntos.isNotEmpty()) {
                mapView.controller.setCenter(puntos.first())
                mapView.controller.setZoom(15.0)
            }

            mapView.invalidate()
        }
    )
}