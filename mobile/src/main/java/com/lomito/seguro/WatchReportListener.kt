package com.lomito.seguro

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.lomito.seguro.data.api.RetrofitClient
import com.lomito.seguro.data.model.ReporteRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Recibe /watch/reporte desde el Watch y lo reenvía al backend Node.js.
 */
class WatchReportListener : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != "/watch/reporte") return

        val json = runCatching { JSONObject(String(event.data)) }.getOrNull() ?: return
        val mascotaId = json.optString("mascotaId", "")
        val lat = json.optDouble("latitud", 20.9167)
        val lng = json.optDouble("longitud", -101.1500)

        if (mascotaId.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                RetrofitClient.api.reportarVista(ReporteRequest(mascotaId, lat, lng, "Reportado desde Watch"))
            }
        }
    }
}
