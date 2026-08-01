// MapaView.kt
package com.lomito.seguro.tv.ui.detalle

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.lomito.seguro.tv.BuildConfig

@Composable
fun MapaView(
    lat: Double,
    lng: Double,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.domStorageEnabled = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.loadsImagesAutomatically = true
                settings.blockNetworkImage = false

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return false
                    }
                }

                webChromeClient = WebChromeClient()

                val apiKey = BuildConfig.MAPS_API_KEY
                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                        <style>
                            body { margin: 0; padding: 0; }
                            iframe { 
                                width: 100%; 
                                height: 100%; 
                                border: 0; 
                            }
                        </style>
                    </head>
                    <body>
                        <iframe 
                            src="https://www.google.com/maps/embed/v1/view?zoom=15&center=$lat,$lng&key=$apiKey"
                            allowfullscreen>
                        </iframe>
                    </body>
                    </html>
                """.trimIndent()

                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        }
    )
}