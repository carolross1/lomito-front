package com.lomito.seguro.repository

import android.util.Log
import com.lomito.seguro.BuildConfig
import com.lomito.seguro.models.Alerta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class AlertasRepository {
    private val backendUrl = BuildConfig.BACKEND_URL
    private val TAG = "AlertasRepository"

    suspend fun getAlertas(ownerId: Int): AlertasResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Obteniendo alertas para ownerId: $ownerId")

                val url = URL("$backendUrl/api/alertas?ownerId=$ownerId")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                val responseCode = conn.responseCode

                Log.d(TAG, "Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().readText()
                    Log.d(TAG, "Respuesta: $response")

                    val jsonArray = JSONArray(response)
                    val alertas = mutableListOf<Alerta>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)

                        // Parsear fecha de manera segura
                        val fechaStr = obj.optString("timestamp", null) ?: obj.optString("fecha", null)
                        val fecha = try {
                            if (fechaStr != null) {
                                val formatos = listOf(
                                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                    "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                                    "yyyy-MM-dd HH:mm:ss",
                                    "yyyy-MM-dd'T'HH:mm:ss"
                                )
                                var parsedDate: Date? = null
                                for (formato in formatos) {
                                    try {
                                        val sdf = SimpleDateFormat(formato, Locale.getDefault())
                                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                                        parsedDate = sdf.parse(fechaStr)
                                        if (parsedDate != null) break
                                    } catch (e: Exception) {
                                        // Intentar con el siguiente formato
                                    }
                                }
                                parsedDate ?: Date()
                            } else {
                                Date()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parseando fecha: ${e.message}")
                            Date()
                        }

                        val alerta = Alerta(
                            id = obj.getInt("id"),
                            titulo = obj.optString("tipo", "Notificación"),
                            mensaje = obj.optString("mensaje", ""),
                            tipo = obj.optString("tipo", "GENERAL"),
                            fecha = fecha,
                            leida = obj.optBoolean("leida", false),
                            mascotaId = obj.optString("mascota_id", null),
                            mascotaNombre = obj.optString("mascota_nombre", null)
                        )
                        alertas.add(alerta)
                    }
                    conn.disconnect()
                    Log.d(TAG, "Alertas cargadas: ${alertas.size}")
                    AlertasResult(alertas, true, null)
                } else {
                    val errorBody = conn.errorStream?.bufferedReader()?.readText()
                    conn.disconnect()
                    Log.e(TAG, "Error HTTP $responseCode: $errorBody")
                    AlertasResult(emptyList(), false, "Error al cargar alertas (HTTP $responseCode): $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}", e)
                AlertasResult(emptyList(), false, "Error: ${e.message}")
            }
        }
    }

    // ✅ MODIFICADO: Usa el endpoint /avistamientos
    suspend fun getAlertasNoLeidas(ownerId: Int): AlertasResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Obteniendo avistamientos para ownerId: $ownerId")

                // ✅ Usar /api/alertas/avistamientos
                val url = URL("$backendUrl/api/alertas/avistamientos?ownerId=$ownerId")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().readText()
                    Log.d(TAG, "Respuesta avistamientos: $response")

                    val jsonArray = JSONArray(response)
                    val alertas = mutableListOf<Alerta>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        alertas.add(
                            Alerta(
                                id = obj.getInt("id"),
                                titulo = obj.optString("tipo", "Notificación"),
                                mensaje = obj.optString("mensaje", ""),
                                tipo = obj.optString("tipo", "GENERAL"),
                                fecha = Date(),
                                leida = obj.optBoolean("leida", false),
                                mascotaId = obj.optString("mascota_id", null),
                                mascotaNombre = obj.optString("mascota_nombre", null)
                            )
                        )
                    }
                    conn.disconnect()
                    Log.d(TAG, "Avistamientos encontrados: ${alertas.size}")
                    AlertasResult(alertas, true, null)
                } else {
                    conn.disconnect()
                    AlertasResult(emptyList(), false, "Error HTTP $responseCode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en getAlertasNoLeidas: ${e.message}", e)
                AlertasResult(emptyList(), false, "Error: ${e.message}")
            }
        }
    }

    suspend fun marcarComoLeida(alertaId: Int): OperacionResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Marcando alerta $alertaId como leída")

                val url = URL("$backendUrl/api/alertas/$alertaId/leida")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val json = JSONObject().apply {
                    put("leida", true)
                }

                conn.outputStream.write(json.toString().toByteArray())
                val responseCode = conn.responseCode
                conn.disconnect()

                Log.d(TAG, "Response Code: $responseCode")

                if (responseCode == 200 || responseCode == 201) {
                    OperacionResult(true, null)
                } else {
                    OperacionResult(false, "Error al marcar como leída (HTTP $responseCode)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}", e)
                OperacionResult(false, "Error: ${e.message}")
            }
        }
    }

    suspend fun marcarTodasComoLeidas(ownerId: Int): OperacionResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Marcando todas las alertas como leídas para ownerId: $ownerId")

                val url = URL("$backendUrl/api/alertas/leidas/$ownerId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val json = JSONObject().apply {
                    put("ownerId", ownerId)
                }

                conn.outputStream.write(json.toString().toByteArray())
                val responseCode = conn.responseCode
                conn.disconnect()

                Log.d(TAG, "Response Code: $responseCode")

                if (responseCode == 200 || responseCode == 201) {
                    OperacionResult(true, null)
                } else {
                    OperacionResult(false, "Error al marcar todas como leídas (HTTP $responseCode)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}", e)
                OperacionResult(false, "Error: ${e.message}")
            }
        }
    }

    data class AlertasResult(
        val alertas: List<Alerta>,
        val success: Boolean,
        val error: String?
    )

    data class OperacionResult(
        val success: Boolean,
        val error: String?
    )
}