// Paquete: com.lomito.seguro.repository
package com.lomito.seguro.repository

// Importa la clase de logging de Android
import android.util.Log
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.BuildConfig
// Importa la dependencia necesaria: Alerta
import com.lomito.seguro.models.Alerta
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.Dispatchers
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.withContext
// Importa el parser JSON
import org.json.JSONArray
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
import java.net.URL
// Importa la dependencia necesaria: SimpleDateFormat
import java.text.SimpleDateFormat
// Importa la dependencia necesaria: *
import java.util.*

// Repositorio AlertasRepository: capa de datos que abstrae las fuentes de información
class AlertasRepository {
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    private val backendUrl = BuildConfig.BACKEND_URL
    // Constante TAG: valor inmutable que no cambia tras su asignación
    private val TAG = "AlertasRepository"

    suspend fun getAlertas(ownerId: Int): AlertasResult {
        // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
        return withContext(Dispatchers.IO) {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Registro de evento en el log de Android para depuración
                Log.d(TAG, "Obteniendo alertas para ownerId: $ownerId")

                // Constante url: valor inmutable que no cambia tras su asignación
                val url = URL("$backendUrl/api/alertas?ownerId=$ownerId")
                // Constante conn: valor inmutable que no cambia tras su asignación
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                // Constante responseCode: valor inmutable que no cambia tras su asignación
                val responseCode = conn.responseCode

                // Registro de evento en el log de Android para depuración
                Log.d(TAG, "Response Code: $responseCode")

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Constante response: valor inmutable que no cambia tras su asignación
                    val response = conn.inputStream.bufferedReader().readText()
                    // Registro de evento en el log de Android para depuración
                    Log.d(TAG, "Respuesta: $response")

                    // Constante jsonArray: valor inmutable que no cambia tras su asignación
                    val jsonArray = JSONArray(response)
                    // Constante alertas: valor inmutable que no cambia tras su asignación
                    val alertas = mutableListOf<Alerta>()

                    // Itera sobre la colección para procesar cada elemento
                    for (i in 0 until jsonArray.length()) {
                        // Constante obj: valor inmutable que no cambia tras su asignación
                        val obj = jsonArray.getJSONObject(i)

                        // Parsear fecha de manera segura
                        // Constante fechaStr: valor inmutable que no cambia tras su asignación
                        val fechaStr = obj.optString("timestamp", null) ?: obj.optString("fecha", null)
                        // Constante fecha: valor inmutable que no cambia tras su asignación
                        val fecha = try {
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (fechaStr != null) {
                                // Constante formatos: valor inmutable que no cambia tras su asignación
                                val formatos = listOf(
                                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                    "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                                    "yyyy-MM-dd HH:mm:ss",
                                    "yyyy-MM-dd'T'HH:mm:ss"
                                )
                                // Variable parsedDate: almacena el estado mutable de este componente
                                var parsedDate: Date? = null
                                // Itera sobre la colección para procesar cada elemento
                                for (formato in formatos) {
                                    // Bloque try-catch: maneja posibles excepciones en el código crítico
                                    try {
                                        // Constante sdf: valor inmutable que no cambia tras su asignación
                                        val sdf = SimpleDateFormat(formato, Locale.getDefault())
                                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                                        parsedDate = sdf.parse(fechaStr)
                                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
                            // Registro de evento en el log de Android para depuración
                            Log.e(TAG, "Error parseando fecha: ${e.message}")
                            Date()
                        }

                        // Constante alerta: valor inmutable que no cambia tras su asignación
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
                    // Registro de evento en el log de Android para depuración
                    Log.d(TAG, "Alertas cargadas: ${alertas.size}")
                    AlertasResult(alertas, true, null)
                } else {
                    // Constante errorBody: valor inmutable que no cambia tras su asignación
                    val errorBody = conn.errorStream?.bufferedReader()?.readText()
                    conn.disconnect()
                    // Registro de evento en el log de Android para depuración
                    Log.e(TAG, "Error HTTP $responseCode: $errorBody")
                    AlertasResult(emptyList(), false, "Error al cargar alertas (HTTP $responseCode): $errorBody")
                }
            } catch (e: Exception) {
                // Registro de evento en el log de Android para depuración
                Log.e(TAG, "Error: ${e.message}", e)
                AlertasResult(emptyList(), false, "Error: ${e.message}")
            }
        }
    }

    // ✅ MODIFICADO: Usa el endpoint /avistamientos
    suspend fun getAlertasNoLeidas(ownerId: Int): AlertasResult {
        // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
        return withContext(Dispatchers.IO) {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Registro de evento en el log de Android para depuración
                Log.d(TAG, "Obteniendo avistamientos para ownerId: $ownerId")

                // ✅ Usar /api/alertas/avistamientos
                // Constante url: valor inmutable que no cambia tras su asignación
                val url = URL("$backendUrl/api/alertas/avistamientos?ownerId=$ownerId")
                // Constante conn: valor inmutable que no cambia tras su asignación
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                // Constante responseCode: valor inmutable que no cambia tras su asignación
                val responseCode = conn.responseCode

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Constante response: valor inmutable que no cambia tras su asignación
                    val response = conn.inputStream.bufferedReader().readText()
                    // Registro de evento en el log de Android para depuración
                    Log.d(TAG, "Respuesta avistamientos: $response")

                    // Constante jsonArray: valor inmutable que no cambia tras su asignación
                    val jsonArray = JSONArray(response)
                    // Constante alertas: valor inmutable que no cambia tras su asignación
                    val alertas = mutableListOf<Alerta>()

                    // Itera sobre la colección para procesar cada elemento
                    for (i in 0 until jsonArray.length()) {
                        // Constante obj: valor inmutable que no cambia tras su asignación
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
                    // Registro de evento en el log de Android para depuración
                    Log.d(TAG, "Avistamientos encontrados: ${alertas.size}")
                    AlertasResult(alertas, true, null)
                } else {
                    conn.disconnect()
                    AlertasResult(emptyList(), false, "Error HTTP $responseCode")
                }
            } catch (e: Exception) {
                // Registro de evento en el log de Android para depuración
                Log.e(TAG, "Error en getAlertasNoLeidas: ${e.message}", e)
                AlertasResult(emptyList(), false, "Error: ${e.message}")
            }
        }
    }

    suspend fun marcarComoLeida(alertaId: Int): OperacionResult {
        // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
        return withContext(Dispatchers.IO) {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Registro de evento en el log de Android para depuración
                Log.d(TAG, "Marcando alerta $alertaId como leída")

                // Constante url: valor inmutable que no cambia tras su asignación
                val url = URL("$backendUrl/api/alertas/$alertaId/leida")
                // Constante conn: valor inmutable que no cambia tras su asignación
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                // Constante json: valor inmutable que no cambia tras su asignación
                val json = JSONObject().apply {
                    put("leida", true)
                }

                conn.outputStream.write(json.toString().toByteArray())
                // Constante responseCode: valor inmutable que no cambia tras su asignación
                val responseCode = conn.responseCode
                conn.disconnect()

                // Registro de evento en el log de Android para depuración
                Log.d(TAG, "Response Code: $responseCode")

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (responseCode == 200 || responseCode == 201) {
                    OperacionResult(true, null)
                } else {
                    OperacionResult(false, "Error al marcar como leída (HTTP $responseCode)")
                }
            } catch (e: Exception) {
                // Registro de evento en el log de Android para depuración
                Log.e(TAG, "Error: ${e.message}", e)
                OperacionResult(false, "Error: ${e.message}")
            }
        }
    }

    suspend fun marcarTodasComoLeidas(ownerId: Int): OperacionResult {
        // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
        return withContext(Dispatchers.IO) {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Registro de evento en el log de Android para depuración
                Log.d(TAG, "Marcando todas las alertas como leídas para ownerId: $ownerId")

                // Constante url: valor inmutable que no cambia tras su asignación
                val url = URL("$backendUrl/api/alertas/leidas/$ownerId")
                // Constante conn: valor inmutable que no cambia tras su asignación
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                // Constante json: valor inmutable que no cambia tras su asignación
                val json = JSONObject().apply {
                    put("ownerId", ownerId)
                }

                conn.outputStream.write(json.toString().toByteArray())
                // Constante responseCode: valor inmutable que no cambia tras su asignación
                val responseCode = conn.responseCode
                conn.disconnect()

                // Registro de evento en el log de Android para depuración
                Log.d(TAG, "Response Code: $responseCode")

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (responseCode == 200 || responseCode == 201) {
                    OperacionResult(true, null)
                } else {
                    OperacionResult(false, "Error al marcar todas como leídas (HTTP $responseCode)")
                }
            } catch (e: Exception) {
                // Registro de evento en el log de Android para depuración
                Log.e(TAG, "Error: ${e.message}", e)
                OperacionResult(false, "Error: ${e.message}")
            }
        }
    }

    // Clase de datos AlertasResult: modelo inmutable con propiedades de dominio
    data class AlertasResult(
        // Constante alertas: valor inmutable que no cambia tras su asignación
        val alertas: List<Alerta>,
        // Constante success: valor inmutable que no cambia tras su asignación
        val success: Boolean,
        // Constante error: valor inmutable que no cambia tras su asignación
        val error: String?
    )

    // Clase de datos OperacionResult: modelo inmutable con propiedades de dominio
    data class OperacionResult(
        // Constante success: valor inmutable que no cambia tras su asignación
        val success: Boolean,
        // Constante error: valor inmutable que no cambia tras su asignación
        val error: String?
    )
}