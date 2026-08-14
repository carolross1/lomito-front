# Guía Paso a Paso: Construyendo el Módulo Wear OS (Smartwatch) de Lomito Seguro

Esta guía documenta y desglosa paso a paso la arquitectura, configuración y construcción completa del módulo **Wear OS (Smartwatch)** de **Lomito Seguro**, explicando las decisiones técnicas, patrones de diseño y bloques de código esenciales para un proyecto profesional en **Kotlin** y **Jetpack Compose**.

---


## Objetivo de Esta Guía

Al estudiar y seguir esta guía, comprenderás:

1. Cómo construir una app para **Wear OS** con **Wear Compose** optimizada para pantalla circular pequeña.
2. Cómo implementar comunicación bidireccional con el teléfono usando **Wearable Data Layer API** (MessageClient y DataClient).
3. Cómo implementar **polling** de datos del backend desde el smartwatch.
4. Cómo gestionar el **modo ambiente** de bajo consumo en el reloj.
5. Cómo implementar alertas hápticas (vibración) cuando la mascota supera el umbral de distancia.
6. Cómo persistir configuraciones en el reloj con **SharedPreferences**.

## Arquitectura del Módulo Wear

```
wear/
├── data/
│   ├── MascotaPerdida.kt        → Modelo de datos para mascotas perdidas
│   ├── PollingService.kt        → Servicio de polling al backend
│   ├── WatchPreferences.kt      → Persistencia local en el reloj
│   ├── WatchViewModel.kt        → ViewModel central del reloj
│   └── WearMessageService.kt   → Servicio de mensajes desde el teléfono
└── ui/
    ├── alert/       → Pantalla de alerta de proximidad
    ├── dashboard/   → Dashboard principal del reloj
    ├── home/        → Pantalla de inicio (WearMainActivity)
    ├── mascota/     → CRUD básico de mascotas desde el reloj
    ├── report/      → Reportes de avistamiento y mascotas perdidas
    ├── selection/   → Pantalla de selección genérica
    └── settings/    → Configuración del reloj (umbral, preferencias)
```


---

## FASE 1: `com/lomito/seguro/wear/data`

### Paso 1.1: `MascotaPerdida.kt`

**Modelo de mascota perdida (Wear)**. Data class que representa una mascota perdida en el contexto del smartwatch.

```kotlin
// Paquete: com.lomito.seguro.wear.data.models
// Paquete: com.lomito.seguro.wear.data.models
package com.lomito.seguro.wear.data.models

/**
 * [Modelo de datos para una mascota perdida]
 *
 * Responsabilidades:
 * - [Representar la información de una mascota en estado de pérdida]
 * - [Almacenar datos del dueño y última ubicación]
 *
 * Esta data class se utiliza para estructurar los datos recibidos desde el backend
 * o almacenados localmente que representan a una mascota reportada como perdida.
 */
// Clase de datos MascotaPerdida: modelo inmutable con propiedades de dominio
// Clase de datos MascotaPerdida: modelo inmutable con propiedades de dominio
data class MascotaPerdida(
    // Identificador único de la mascota
    // Constante id: valor inmutable que no cambia tras su asignación
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String,
    
    // Nombre de la mascota
    // Constante nombre: valor inmutable que no cambia tras su asignación
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String,
    
    // Especie de la mascota (ej. Perro, Gato)
    // Constante especie: valor inmutable que no cambia tras su asignación
    // Constante especie: valor inmutable que no cambia tras su asignación
    val especie: String,
    
    // Raza de la mascota, valor por defecto cadena vacía
    // Constante raza: valor inmutable que no cambia tras su asignación
    // Constante raza: valor inmutable que no cambia tras su asignación
    val raza: String = "",
    
    // Color principal de la mascota
    // Constante color: valor inmutable que no cambia tras su asignación
    // Constante color: valor inmutable que no cambia tras su asignación
    val color: String = "",
    
    // URL de la fotografía de la mascota, puede ser nulo si no tiene
    // Constante fotoUrl: valor inmutable que no cambia tras su asignación
    // Constante fotoUrl: valor inmutable que no cambia tras su asignación
    val fotoUrl: String? = null,
    
    // Distancia configurada para activar la alerta en metros, por defecto 50m
    // Constante distanciaAlerta: valor inmutable que no cambia tras su asignación
    // Constante distanciaAlerta: valor inmutable que no cambia tras su asignación
    val distanciaAlerta: Int = 50,
    
    // Estado actual de la mascota, por defecto "PERDIDA"
    // Constante estado: valor inmutable que no cambia tras su asignación
    // Constante estado: valor inmutable que no cambia tras su asignación
    val estado: String = "PERDIDA",
    
    // Identificador único del dueño de la mascota
    // Constante ownerId: valor inmutable que no cambia tras su asignación
    // Constante ownerId: valor inmutable que no cambia tras su asignación
    val ownerId: String = "",
    
    // Nombre completo o de contacto del dueño
    // Constante duenoNombre: valor inmutable que no cambia tras su asignación
    // Constante duenoNombre: valor inmutable que no cambia tras su asignación
    val duenoNombre: String = "",
    
    // Teléfono de contacto del dueño para reportes
    // Constante duenoTelefono: valor inmutable que no cambia tras su asignación
    // Constante duenoTelefono: valor inmutable que no cambia tras su asignación
    val duenoTelefono: String = "",
    
    // Latitud de la última ubicación conocida de la mascota, puede ser nulo
    // Constante ultimaUbicacionLat: valor inmutable que no cambia tras su asignación
    // Constante ultimaUbicacionLat: valor inmutable que no cambia tras su asignación
    val ultimaUbicacionLat: Double? = null,
    
    // Longitud de la última ubicación conocida de la mascota, puede ser nulo
    // Constante ultimaUbicacionLng: valor inmutable que no cambia tras su asignación
    // Constante ultimaUbicacionLng: valor inmutable que no cambia tras su asignación
    val ultimaUbicacionLng: Double? = null
)
```

### Paso 1.2: `PollingService.kt`

**Servicio de polling (Wear)**. Servicio en background que consulta periódicamente el backend para obtener nuevas mascotas perdidas.

```kotlin
// Paquete: com.lomito.seguro.wear.data
// Paquete: com.lomito.seguro.wear.data
package com.lomito.seguro.wear.data
// Importa la dependencia necesaria: BuildConfig
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.wear.BuildConfig

// Importa las clases para manejo de notificaciones
// Importa las clases para manejo de notificaciones
import android.app.NotificationChannel
// Importa las clases para manejo de notificaciones
// Importa las clases para manejo de notificaciones
import android.app.NotificationManager
// Importa la clase Intent para navegación entre componentes
// Importa la clase Intent para navegación entre componentes
import android.app.PendingIntent
// Importa la dependencia necesaria: Service
// Importa la dependencia necesaria: Service
import android.app.Service
// Importa el contexto de Android
// Importa el contexto de Android
import android.content.Context
// Importa la clase Intent para navegación entre componentes
// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa la dependencia necesaria: Build
// Importa la dependencia necesaria: Build
import android.os.Build
// Importa el contenedor de datos Bundle
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: IBinder
// Importa la dependencia necesaria: IBinder
import android.os.IBinder
// Importa la dependencia necesaria: VibrationEffect
// Importa la dependencia necesaria: VibrationEffect
import android.os.VibrationEffect
// Importa la dependencia necesaria: Vibrator
// Importa la dependencia necesaria: Vibrator
import android.os.Vibrator
// Importa las clases para manejo de notificaciones
// Importa las clases para manejo de notificaciones
import androidx.core.app.NotificationCompat
// Importa la dependencia necesaria: AlertActivity
// Importa la dependencia necesaria: AlertActivity
import com.lomito.seguro.wear.ui.alert.AlertActivity
// Importa la dependencia necesaria: DashboardActivity
// Importa la dependencia necesaria: DashboardActivity
import com.lomito.seguro.wear.ui.dashboard.DashboardActivity
// Importa soporte para corrutinas de Kotlin
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.*
// Importa el parser JSON
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: HttpURLConnection
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
// Importa la dependencia necesaria: URL
import java.net.URL

/**
 * [Servicio en primer plano para consultar el estado del simulador]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Ejecutar consultas periódicas al backend para obtener distancia]
 * - [Notificar al usuario si la distancia supera el umbral configurado]
 */
// Servicio PollingService: componente en background para tareas de larga duración
// Servicio PollingService: componente en background para tareas de larga duración
class PollingService : Service() {

    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    private val backendUrl = BuildConfig.BACKEND_URL
    // Variable pollingJob: almacena el estado mutable de este componente
    // Variable pollingJob: almacena el estado mutable de este componente
    private var pollingJob: Job? = null

    companion object {
        // Constante CHANNEL_ID: valor fijo definido en tiempo de compilación
        // Constante CHANNEL_ID: valor fijo definido en tiempo de compilación
        const val CHANNEL_ID = "lomito_polling"
        // Constante NOTIF_ID: valor fijo definido en tiempo de compilación
        // Constante NOTIF_ID: valor fijo definido en tiempo de compilación
        const val NOTIF_ID = 2001
        // Variable distanciaActual: almacena el estado mutable de este componente
        // Variable distanciaActual: almacena el estado mutable de este componente
        var distanciaActual: Int = 0
        // Variable umbralActual: almacena el estado mutable de este componente
        // Variable umbralActual: almacena el estado mutable de este componente
        var umbralActual: Int = 50
        // Variable mascotaIdActual: almacena el estado mutable de este componente
        // Variable mascotaIdActual: almacena el estado mutable de este componente
        var mascotaIdActual: String = ""
        // Variable mascotaNombreActual: almacena el estado mutable de este componente
        // Variable mascotaNombreActual: almacena el estado mutable de este componente
        var mascotaNombreActual: String = ""
        // Variable alertaMostrada: almacena el estado mutable de este componente
        // Variable alertaMostrada: almacena el estado mutable de este componente
        var alertaMostrada: Boolean = false
        // Variable ultimaDistanciaAlerta: almacena el estado mutable de este componente
        // Variable ultimaDistanciaAlerta: almacena el estado mutable de este componente
        var ultimaDistanciaAlerta: Int = 0
        // Constante INCREMENTO_MINIMO: valor fijo definido en tiempo de compilación
        // Constante INCREMENTO_MINIMO: valor fijo definido en tiempo de compilación
        private const val INCREMENTO_MINIMO = 20
    }

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate() {
        // Invoca la implementación del método en la clase padre
        // Invoca la implementación del método en la clase padre
        super.onCreate()
        crearCanalNotificacion()
        startForeground(NOTIF_ID, crearNotificacion("🐾 Lomito Seguro activo"))
        iniciarPolling()
    }

    // Sobreescribe la función onStartCommand de la clase padre
    // Sobreescribe la función onStartCommand de la clase padre
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Retorna el valor al llamador de la función
        // Retorna el valor al llamador de la función
        return START_STICKY
    }

    // Sobreescribe la función onBind de la clase padre
    // Sobreescribe la función onBind de la clase padre
    override fun onBind(intent: Intent?): IBinder? = null

    private fun iniciarPolling() {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        pollingJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (isActive) {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // Constante prefs: valor inmutable que no cambia tras su asignación
                    // Constante prefs: valor inmutable que no cambia tras su asignación
                    val prefs = applicationContext.getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)
                    mascotaNombreActual = prefs.getString("mascota_activa_nombre", "Tu mascota") ?: "Tu mascota"

                    // Constante url: valor inmutable que no cambia tras su asignación
                    // Constante url: valor inmutable que no cambia tras su asignación
                    val url = URL("$backendUrl/api/simulador/estado")
                    // Constante conn: valor inmutable que no cambia tras su asignación
                    // Constante conn: valor inmutable que no cambia tras su asignación
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 2000
                    conn.readTimeout = 2000
                    conn.requestMethod = "GET"

                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        // Constante response: valor inmutable que no cambia tras su asignación
                        // Constante response: valor inmutable que no cambia tras su asignación
                        val response = conn.inputStream.bufferedReader().readText()
                        // Constante json: valor inmutable que no cambia tras su asignación
                        // Constante json: valor inmutable que no cambia tras su asignación
                        val json = JSONObject(response)
                        // Constante distancia: valor inmutable que no cambia tras su asignación
                        // Constante distancia: valor inmutable que no cambia tras su asignación
                        val distancia = json.optInt("distancia", 0)
                        // Constante umbral: valor inmutable que no cambia tras su asignación
                        // Constante umbral: valor inmutable que no cambia tras su asignación
                        val umbral = json.optInt("umbral", 50)
                        // Constante mascotaId: valor inmutable que no cambia tras su asignación
                        // Constante mascotaId: valor inmutable que no cambia tras su asignación
                        val mascotaId = json.optString("mascotaId", "")

                        distanciaActual = distancia
                        umbralActual = umbral
                        mascotaIdActual = mascotaId

                        sendBroadcast(Intent("com.lomito.seguro.wear.BLE_UPDATE").apply {
                            putExtra("distancia", distancia)
                            putExtra("mascotaId", mascotaId)
                            putExtra("umbral", umbral)
                            putExtra("superaUmbral", distancia > umbral)
                            setPackage(packageName)
                        })

                        // Registro de evento en el log de Android para depuración
                        // Registro de evento en el log de Android para depuración
                        android.util.Log.d("POLLING_SVC", "📡 distancia=$distancia umbral=$umbral mascota=$mascotaNombreActual")

                        // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                        // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                        withContext(Dispatchers.Main) {
                            // ✅ Evaluar condiciones
                            // Constante incremento: valor inmutable que no cambia tras su asignación
                            // Constante incremento: valor inmutable que no cambia tras su asignación
                            val incremento = distancia - ultimaDistanciaAlerta
                            // Constante superaUmbral: valor inmutable que no cambia tras su asignación
                            // Constante superaUmbral: valor inmutable que no cambia tras su asignación
                            val superaUmbral = distancia > umbral
                            // Constante distanciaValida: valor inmutable que no cambia tras su asignación
                            // Constante distanciaValida: valor inmutable que no cambia tras su asignación
                            val distanciaValida = distancia > 0
                            // Constante tieneMascota: valor inmutable que no cambia tras su asignación
                            // Constante tieneMascota: valor inmutable que no cambia tras su asignación
                            val tieneMascota = mascotaId.isNotEmpty()
                            // Constante alertaNoActiva: valor inmutable que no cambia tras su asignación
                            // Constante alertaNoActiva: valor inmutable que no cambia tras su asignación
                            val alertaNoActiva = !alertaMostrada
                            // Constante esPrimeraAlerta: valor inmutable que no cambia tras su asignación
                            // Constante esPrimeraAlerta: valor inmutable que no cambia tras su asignación
                            val esPrimeraAlerta = ultimaDistanciaAlerta == 0
                            // Constante aumentoSignificativo: valor inmutable que no cambia tras su asignación
                            // Constante aumentoSignificativo: valor inmutable que no cambia tras su asignación
                            val aumentoSignificativo = incremento >= INCREMENTO_MINIMO

                            // ✅ Determinar si debe mostrar alerta (sin usar if como expresión)
                            // Variable debeMostrar: almacena el estado mutable de este componente
                            // Variable debeMostrar: almacena el estado mutable de este componente
                            var debeMostrar = false
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (superaUmbral && distanciaValida && tieneMascota && alertaNoActiva) {
                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                if (esPrimeraAlerta || aumentoSignificativo) {
                                    debeMostrar = true
                                }
                            }

                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (debeMostrar) {
                                // ✅ Mostrar alerta
                                alertaMostrada = true
                                ultimaDistanciaAlerta = distancia
                                // Registro de evento en el log de Android para depuración
                                // Registro de evento en el log de Android para depuración
                                android.util.Log.d("POLLING_SVC", "🚨 Abriendo AlertActivity (incremento: $incremento m)")
                                vibrar()

                                // Constante intent: valor inmutable que no cambia tras su asignación
                                // Constante intent: valor inmutable que no cambia tras su asignación
                                val intent = Intent(applicationContext, AlertActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                    putExtra("mascota_nombre", mascotaNombreActual)
                                    putExtra("distancia", distancia)
                                    putExtra("incremento", incremento)
                                }
                                startActivity(intent)
                            } else {
                                // ✅ Resetear cuando la distancia es segura
                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                if (distancia <= umbral || distancia == 0) {
                                    alertaMostrada = false
                                }
                            }

                            // ✅ Logs para debugging
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (distancia > umbral && alertaMostrada) {
                                // Registro de evento en el log de Android para depuración
                                // Registro de evento en el log de Android para depuración
                                android.util.Log.d("POLLING_SVC", "⏳ Alerta ya mostrada. Distancia actual: $distancia")
                            }
                        }
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("POLLING_SVC", "Error: ${e.message}")
                }
                delay(2000)
            }
        }
    }

    private fun vibrar() {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Constante vibrator: valor inmutable que no cambia tras su asignación
            // Constante vibrator: valor inmutable que no cambia tras su asignación
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Activa la vibración háptica del smartwatch para retroalimentación táctil
                // Activa la vibración háptica del smartwatch para retroalimentación táctil
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300, 100, 600), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                // Activa la vibración háptica del smartwatch para retroalimentación táctil
                // Activa la vibración háptica del smartwatch para retroalimentación táctil
                vibrator.vibrate(longArrayOf(0, 300, 100, 300, 100, 600), -1)
            }
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("POLLING_SVC", "Error vibrando: ${e.message}")
        }
    }

    private fun crearCanalNotificacion() {
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Constante mgr: valor inmutable que no cambia tras su asignación
            // Constante mgr: valor inmutable que no cambia tras su asignación
            val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            // Crea el canal de notificación requerido en Android 8.0+
            // Crea el canal de notificación requerido en Android 8.0+
            mgr.createNotificationChannel(
                // Crea el canal de notificación requerido en Android 8.0+
                // Crea el canal de notificación requerido en Android 8.0+
                NotificationChannel(
                    CHANNEL_ID,
                    "Lomito Seguro Polling",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private fun crearNotificacion(texto: String) =
        // Construye la notificación con sus propiedades visuales y de comportamiento
        // Construye la notificación con sus propiedades visuales y de comportamiento
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Lomito Seguro")
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    // Método del ciclo de vida: se limpia la actividad antes de destruirse
    // Método del ciclo de vida: se limpia la actividad antes de destruirse
    override fun onDestroy() {
        // Invoca la implementación del método en la clase padre
        // Invoca la implementación del método en la clase padre
        super.onDestroy()
        pollingJob?.cancel()
        alertaMostrada = false
    }
}
```

### Paso 1.3: `WatchPreferences.kt`

**Preferencias del reloj**. Gestiona el almacenamiento de configuraciones del usuario en el smartwatch con SharedPreferences.

```kotlin
// wear/data/WatchPreferences.kt
// Paquete: com.lomito.seguro.wear.data
// Paquete: com.lomito.seguro.wear.data
package com.lomito.seguro.wear.data

// Importa el contexto de Android
// Importa el contexto de Android
import android.content.Context
// Importa SharedPreferences para persistencia local
// Importa SharedPreferences para persistencia local
import android.content.SharedPreferences

/**
 * [Gestor de preferencias del reloj]
 *
 * Responsabilidades:
 * - [Guardar y recuperar datos locales como mascota activa, umbral y usuario]
 * - [Proveer una interfaz simplificada para acceder a SharedPreferences en la app de Wear OS]
 */
// Declaración de la clase WatchPreferences
// Declaración de la clase WatchPreferences
class WatchPreferences(context: Context) {
    // Instancia de SharedPreferences utilizada para guardar datos locales
    // Constante prefs: valor inmutable que no cambia tras su asignación
    // Constante prefs: valor inmutable que no cambia tras su asignación
    private val prefs: SharedPreferences = context.getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)

    // Constantes utilizadas como claves para guardar los valores en SharedPreferences
    companion object {
        // Clave para guardar el ID de la mascota activa
        // Constante KEY_MASCOTA_ACTIVA_ID: valor fijo definido en tiempo de compilación
        // Constante KEY_MASCOTA_ACTIVA_ID: valor fijo definido en tiempo de compilación
        private const val KEY_MASCOTA_ACTIVA_ID = "mascota_activa_id"
        // Clave para guardar el nombre de la mascota activa
        // Constante KEY_MASCOTA_ACTIVA_NOMBRE: valor fijo definido en tiempo de compilación
        // Constante KEY_MASCOTA_ACTIVA_NOMBRE: valor fijo definido en tiempo de compilación
        private const val KEY_MASCOTA_ACTIVA_NOMBRE = "mascota_activa_nombre"
        // Clave para guardar la distancia umbral de la mascota
        // Constante KEY_MASCOTA_UMBRAL: valor fijo definido en tiempo de compilación
        // Constante KEY_MASCOTA_UMBRAL: valor fijo definido en tiempo de compilación
        private const val KEY_MASCOTA_UMBRAL = "mascota_umbral"
        // Clave para guardar el ID del usuario
        // Constante KEY_USER_ID: valor fijo definido en tiempo de compilación
        // Constante KEY_USER_ID: valor fijo definido en tiempo de compilación
        private const val KEY_USER_ID = "user_id"
    }

    /**
     * Propiedad para acceder y modificar el ID de la mascota activa.
     * Si no existe, devuelve una cadena vacía.
     */
    // Variable mascotaActivaId: almacena el estado mutable de este componente
    // Variable mascotaActivaId: almacena el estado mutable de este componente
    var mascotaActivaId: String
        get() = prefs.getString(KEY_MASCOTA_ACTIVA_ID, "") ?: ""
        // Inicia el editor para modificar los SharedPreferences
        // Inicia el editor para modificar los SharedPreferences
        set(value) = prefs.edit().putString(KEY_MASCOTA_ACTIVA_ID, value).apply()

    /**
     * Propiedad para acceder y modificar el nombre de la mascota activa.
     * Si no existe, devuelve "Mascota" por defecto.
     */
    // Variable mascotaActivaNombre: almacena el estado mutable de este componente
    // Variable mascotaActivaNombre: almacena el estado mutable de este componente
    var mascotaActivaNombre: String
        get() = prefs.getString(KEY_MASCOTA_ACTIVA_NOMBRE, "Mascota") ?: "Mascota"
        // Inicia el editor para modificar los SharedPreferences
        // Inicia el editor para modificar los SharedPreferences
        set(value) = prefs.edit().putString(KEY_MASCOTA_ACTIVA_NOMBRE, value).apply()

    /**
     * Propiedad para acceder y modificar el umbral de distancia.
     * Si no existe, devuelve 50 por defecto.
     */
    // Variable mascotaUmbral: almacena el estado mutable de este componente
    // Variable mascotaUmbral: almacena el estado mutable de este componente
    var mascotaUmbral: Int
        get() = prefs.getInt(KEY_MASCOTA_UMBRAL, 50)
        // Inicia el editor para modificar los SharedPreferences
        // Inicia el editor para modificar los SharedPreferences
        set(value) = prefs.edit().putInt(KEY_MASCOTA_UMBRAL, value).apply()

    /**
     * Propiedad para acceder y modificar el ID del usuario.
     * Si no existe, devuelve "usuario_123" por defecto.
     */
    // Variable userId: almacena el estado mutable de este componente
    // Variable userId: almacena el estado mutable de este componente
    var userId: String
        get() = prefs.getString(KEY_USER_ID, "usuario_123") ?: "usuario_123"
        // Inicia el editor para modificar los SharedPreferences
        // Inicia el editor para modificar los SharedPreferences
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()
}
```

### Paso 1.4: `WatchViewModel.kt`

**ViewModel del reloj**. Gestiona el estado central del smartwatch: mascotas, alertas y configuración.

```kotlin
// wear/data/WatchViewModel.kt
// Paquete: com.lomito.seguro.wear.data
// Paquete: com.lomito.seguro.wear.data
package com.lomito.seguro.wear.data

// Importa la dependencia necesaria: Application
// Importa la dependencia necesaria: Application
import android.app.Application
// Importa la clase base ViewModel del ciclo de vida
// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.AndroidViewModel
// Importa el observable de datos reactivos
// Importa el observable de datos reactivos
import androidx.lifecycle.LiveData
// Importa el observable de datos reactivos
// Importa el observable de datos reactivos
import androidx.lifecycle.MutableLiveData

/**
 * [Modelo de datos para el estado BLE de la mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [distancia]: Distancia actual de la mascota
 * - [mascotaId]: Identificador de la mascota
 * - [umbral]: Umbral de alerta
 * - [superaUmbral]: Indica si supera el umbral permitido
 */
// Clase de datos BleState: modelo inmutable con propiedades de dominio
// Clase de datos BleState: modelo inmutable con propiedades de dominio
data class BleState(
    // Constante distancia: valor inmutable que no cambia tras su asignación
    // Constante distancia: valor inmutable que no cambia tras su asignación
    val distancia: Int = 0,
    // Constante mascotaId: valor inmutable que no cambia tras su asignación
    // Constante mascotaId: valor inmutable que no cambia tras su asignación
    val mascotaId: String = "",
    // Constante umbral: valor inmutable que no cambia tras su asignación
    // Constante umbral: valor inmutable que no cambia tras su asignación
    val umbral: Int = 50,
    // Constante superaUmbral: valor inmutable que no cambia tras su asignación
    // Constante superaUmbral: valor inmutable que no cambia tras su asignación
    val superaUmbral: Boolean = false
)

/**
 * [ViewModel principal para la sincronización con el reloj]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mantener el estado de BLE y actualizar la UI de manera reactiva]
 */
// ViewModel WatchViewModel: gestiona el estado y la lógica de negocio de la pantalla
// ViewModel WatchViewModel: gestiona el estado y la lógica de negocio de la pantalla
class WatchViewModel(app: Application) : AndroidViewModel(app) {
    // Constante _bleState: valor inmutable que no cambia tras su asignación
    // Constante _bleState: valor inmutable que no cambia tras su asignación
    private val _bleState = MutableLiveData(BleState())
    // Constante bleState: valor inmutable que no cambia tras su asignación
    // Constante bleState: valor inmutable que no cambia tras su asignación
    val bleState: LiveData<BleState> = _bleState

    /**
     * [Actualiza el estado actual de la mascota en el ViewModel]
     *
     * Responsabilidades (o parámetros en caso de funciones simples):
     * - [distancia]: Distancia de la mascota
     * - [mascotaId]: ID de la mascota
     * - [umbral]: Umbral de seguridad
     * - [superaUmbral]: Estado de alerta
     */
    // Función actualizarEstado: define la lógica de esta operación
    // Función actualizarEstado: define la lógica de esta operación
    fun actualizarEstado(distancia: Int, mascotaId: String, umbral: Int, superaUmbral: Boolean) {
        // Registro de evento en el log de Android para depuración
        // Registro de evento en el log de Android para depuración
        android.util.Log.d("WATCH_VM", "Actualizando: distancia=$distancia, mascotaId=$mascotaId, umbral=$umbral")
        _bleState.postValue(BleState(distancia, mascotaId, umbral, superaUmbral))
    }
}
```

### Paso 1.5: `WearMessageService.kt`

**Servicio de mensajes Wear**. Recibe mensajes del teléfono emparejado a través de la API de Wearable y actualiza el estado del reloj.

```kotlin
// Paquete: com.lomito.seguro.wear.data
// Paquete: com.lomito.seguro.wear.data
package com.lomito.seguro.wear.data

// Importa las clases para manejo de notificaciones
// Importa las clases para manejo de notificaciones
import android.app.NotificationChannel
// Importa las clases para manejo de notificaciones
// Importa las clases para manejo de notificaciones
import android.app.NotificationManager
// Importa el contexto de Android
// Importa el contexto de Android
import android.content.Context
// Importa la clase Intent para navegación entre componentes
// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa la dependencia necesaria: Build
// Importa la dependencia necesaria: Build
import android.os.Build
// Importa la dependencia necesaria: VibrationEffect
// Importa la dependencia necesaria: VibrationEffect
import android.os.VibrationEffect
// Importa la dependencia necesaria: Vibrator
// Importa la dependencia necesaria: Vibrator
import android.os.Vibrator
// Importa las clases para manejo de notificaciones
// Importa las clases para manejo de notificaciones
import androidx.core.app.NotificationCompat
// Importa la API de comunicación con Wear OS
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.DataEvent
// Importa la API de comunicación con Wear OS
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.DataEventBuffer
// Importa la API de comunicación con Wear OS
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.DataMapItem
// Importa la API de comunicación con Wear OS
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.MessageEvent
// Importa la API de comunicación con Wear OS
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.WearableListenerService
// Importa la dependencia necesaria: AlertActivity
// Importa la dependencia necesaria: AlertActivity
import com.lomito.seguro.wear.ui.alert.AlertActivity
// Importa el parser JSON
// Importa el parser JSON
import org.json.JSONObject

/**
 * [Servicio de mensajería para comunicarse con la app móvil y backend]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Recibir eventos de conexión BLE y distancias]
 * - [Sincronizar datos de mascotas y estado entre el reloj y el dispositivo móvil]
 */
// Servicio WearMessageService: componente en background para tareas de larga duración
// Servicio WearMessageService: componente en background para tareas de larga duración
class WearMessageService : WearableListenerService() {

    companion object {
        // Constante CHANNEL_ID: valor fijo definido en tiempo de compilación
        // Constante CHANNEL_ID: valor fijo definido en tiempo de compilación
        const val CHANNEL_ID = "lomito_alertas"

        // Variable distancia: almacena el estado mutable de este componente
        // Variable distancia: almacena el estado mutable de este componente
        var distancia: Int = 0
        // Variable mascotaId: almacena el estado mutable de este componente
        // Variable mascotaId: almacena el estado mutable de este componente
        var mascotaId: String = ""
        // Variable umbral: almacena el estado mutable de este componente
        // Variable umbral: almacena el estado mutable de este componente
        var umbral: Int = 50
        // Variable superaUmbral: almacena el estado mutable de este componente
        // Variable superaUmbral: almacena el estado mutable de este componente
        var superaUmbral: Boolean = false

        // Variable onUpdate: almacena el estado mutable de este componente
        // Variable onUpdate: almacena el estado mutable de este componente
        var onUpdate: ((Int, String, Int, Boolean) -> Unit)? = null
    }

    // Callback que se ejecuta cuando llega un mensaje desde el dispositivo Wear
    // Callback que se ejecuta cuando llega un mensaje desde el dispositivo Wear
    override fun onMessageReceived(event: MessageEvent) {
        // Registro de evento en el log de Android para depuración
        // Registro de evento en el log de Android para depuración
        android.util.Log.d("WEAR_MSG", "📩 Mensaje recibido en path: ${event.path}")

        // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
        // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
        when (event.path) {
            "/ble/distancia" -> {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // Constante json: valor inmutable que no cambia tras su asignación
                    // Constante json: valor inmutable que no cambia tras su asignación
                    val json = JSONObject(String(event.data))

                    distancia = json.optInt("distancia", 0)
                    mascotaId = json.optString("mascotaId", "")
                    umbral = json.optInt("umbral", 50)
                    superaUmbral = json.optBoolean("superaUmbral", false)

                    // Registro de evento en el log de Android para depuración
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.d("WEAR_MSG", "📊 Distancia: $distancia, Umbral: $umbral, Supera: $superaUmbral")

                    onUpdate?.invoke(distancia, mascotaId, umbral, superaUmbral)

                    sendBroadcast(Intent("com.lomito.seguro.wear.BLE_UPDATE").apply {
                        putExtra("distancia", distancia)
                        putExtra("mascotaId", mascotaId)
                        putExtra("umbral", umbral)
                        putExtra("superaUmbral", superaUmbral)
                        setPackage(packageName)
                    })

                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (superaUmbral) {
                        vibrar()
                        mostrarNotificacion(distancia, mascotaId)
                        // ✅ Abrir AlertActivity automáticamente
                        startActivity(Intent(applicationContext, AlertActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        })
                    }
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("WEAR_MSG", "❌ Error procesando mensaje BLE: ${e.message}")
                }
            }

            "/watch/user_id" -> {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // Constante json: valor inmutable que no cambia tras su asignación
                    // Constante json: valor inmutable que no cambia tras su asignación
                    val json = JSONObject(String(event.data))
                    // Constante userId: valor inmutable que no cambia tras su asignación
                    // Constante userId: valor inmutable que no cambia tras su asignación
                    val userId = json.getString("userId")
                    // Constante prefs: valor inmutable que no cambia tras su asignación
                    // Constante prefs: valor inmutable que no cambia tras su asignación
                    val prefs = applicationContext.getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)
                    // Inicia el editor para modificar los SharedPreferences
                    // Inicia el editor para modificar los SharedPreferences
                    prefs.edit().putString("user_id", userId).apply()
                    // Registro de evento en el log de Android para depuración
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.d("USER_ID", "✅ userId $userId guardado")
                    sendBroadcast(Intent("com.lomito.seguro.wear.USER_ID_UPDATED").apply {
                        putExtra("user_id", userId)
                        setPackage(packageName)
                    })
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("USER_ID", "❌ Error: ${e.message}")
                }
            }

            "/watch/reporte" -> {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // Constante json: valor inmutable que no cambia tras su asignación
                    // Constante json: valor inmutable que no cambia tras su asignación
                    val json = JSONObject(String(event.data))
                    // Constante mascotaId: valor inmutable que no cambia tras su asignación
                    // Constante mascotaId: valor inmutable que no cambia tras su asignación
                    val mascotaId = json.getString("mascotaId")
                    // Constante latitud: valor inmutable que no cambia tras su asignación
                    // Constante latitud: valor inmutable que no cambia tras su asignación
                    val latitud = json.getDouble("latitud")
                    // Constante longitud: valor inmutable que no cambia tras su asignación
                    // Constante longitud: valor inmutable que no cambia tras su asignación
                    val longitud = json.getDouble("longitud")
                    // Constante direccion: valor inmutable que no cambia tras su asignación
                    // Constante direccion: valor inmutable que no cambia tras su asignación
                    val direccion = json.optString("direccion", "")

                    // Constante payload: valor inmutable que no cambia tras su asignación
                    // Constante payload: valor inmutable que no cambia tras su asignación
                    val payload = JSONObject().apply {
                        put("tipo", "AVISTAMIENTO_REPORTADO")
                        put("mascotaId", mascotaId)
                        put("latitud", latitud)
                        put("longitud", longitud)
                        put("direccion", direccion)
                    }.toString().toByteArray()

                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    com.google.android.gms.wearable.Wearable.getNodeClient(applicationContext).connectedNodes
                        .addOnSuccessListener { nodeList ->
                            // Itera sobre cada elemento de la colección y ejecuta el bloque
                            // Itera sobre cada elemento de la colección y ejecuta el bloque
                            nodeList.forEach { node ->
                                // Usa la API de Wearable para comunicación con dispositivos Wear OS
                                // Usa la API de Wearable para comunicación con dispositivos Wear OS
                                com.google.android.gms.wearable.Wearable.getMessageClient(applicationContext)
                                    // Envía un mensaje al dispositivo Wear OS conectado
                                    // Envía un mensaje al dispositivo Wear OS conectado
                                    .sendMessage(node.id, "/watch/avistamiento", payload)
                            }
                        }
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("WEAR_MSG", "❌ Error procesando reporte: ${e.message}")
                }
            }

            // ✅ Alguien confirmó en la TV que vio a la mascota: vibra y
            // muestra notificación en el watch (llega reenviado por el móvil,
            // que es quien recibe la alerta del backend).
            "/watch/avistamiento_confirmado" -> {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // Constante json: valor inmutable que no cambia tras su asignación
                    // Constante json: valor inmutable que no cambia tras su asignación
                    val json = JSONObject(String(event.data))
                    // Constante nombre: valor inmutable que no cambia tras su asignación
                    // Constante nombre: valor inmutable que no cambia tras su asignación
                    val nombre = json.optString("mascotaNombre", "tu mascota")
                    // Constante mensaje: valor inmutable que no cambia tras su asignación
                    // Constante mensaje: valor inmutable que no cambia tras su asignación
                    val mensaje = json.optString("mensaje", "Alguien confirmó un avistamiento")

                    vibrar()
                    mostrarNotificacionAvistamiento(nombre, mensaje)
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("WEAR_MSG", "❌ Error procesando avistamiento confirmado: ${e.message}")
                }
            }

            "/mascota/perdida/nueva" -> {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // Constante json: valor inmutable que no cambia tras su asignación
                    // Constante json: valor inmutable que no cambia tras su asignación
                    val json = JSONObject(String(event.data))
                    // Constante nombre: valor inmutable que no cambia tras su asignación
                    // Constante nombre: valor inmutable que no cambia tras su asignación
                    val nombre = json.getString("nombre")
                    // Registro de evento en el log de Android para depuración
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.d("WEAR_MSG", "🐾 Nueva mascota perdida: $nombre")

                    // Constante payload: valor inmutable que no cambia tras su asignación
                    // Constante payload: valor inmutable que no cambia tras su asignación
                    val payload = JSONObject().apply {
                        put("tipo", "NUEVA_MASCOTA_PERDIDA")
                        put("nombre", nombre)
                    }.toString().toByteArray()

                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    com.google.android.gms.wearable.Wearable.getNodeClient(applicationContext).connectedNodes
                        .addOnSuccessListener { nodeList ->
                            // Itera sobre cada elemento de la colección y ejecuta el bloque
                            // Itera sobre cada elemento de la colección y ejecuta el bloque
                            nodeList.forEach { node ->
                                // Usa la API de Wearable para comunicación con dispositivos Wear OS
                                // Usa la API de Wearable para comunicación con dispositivos Wear OS
                                com.google.android.gms.wearable.Wearable.getMessageClient(applicationContext)
                                    // Envía un mensaje al dispositivo Wear OS conectado
                                    // Envía un mensaje al dispositivo Wear OS conectado
                                    .sendMessage(node.id, "/mascota/perdida/nueva", payload)
                            }
                        }
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("WEAR_MSG", "❌ Error: ${e.message}")
                }
            }

            "/watch/mascotas" -> {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // Constante json: valor inmutable que no cambia tras su asignación
                    // Constante json: valor inmutable que no cambia tras su asignación
                    val json = JSONObject(String(event.data))
                    // Constante mascotas: valor inmutable que no cambia tras su asignación
                    // Constante mascotas: valor inmutable que no cambia tras su asignación
                    val mascotas = json.getJSONArray("mascotas")
                    // Constante prefs: valor inmutable que no cambia tras su asignación
                    // Constante prefs: valor inmutable que no cambia tras su asignación
                    val prefs = applicationContext.getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)
                    // Inicia el editor para modificar los SharedPreferences
                    // Inicia el editor para modificar los SharedPreferences
                    prefs.edit().putString("mascotas_data", mascotas.toString()).apply()
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("WEAR_MSG", "❌ Error procesando lista de mascotas: ${e.message}")
                }
            }

            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            else -> android.util.Log.d("WEAR_MSG", "⚠️ Path desconocido: ${event.path}")
        }
    }

    // Sobreescribe la función onDataChanged de la clase padre
    // Sobreescribe la función onDataChanged de la clase padre
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // Registro de evento en el log de Android para depuración
        // Registro de evento en el log de Android para depuración
        android.util.Log.d("WEAR_MSG", "📊 DataChanged recibido")

        // Itera sobre cada elemento de la colección y ejecuta el bloque
        // Itera sobre cada elemento de la colección y ejecuta el bloque
        dataEvents.forEach { event ->
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (event.type == DataEvent.TYPE_CHANGED &&
                    event.dataItem.uri.path == "/ble/distancia") {

                    // Constante dataMap: valor inmutable que no cambia tras su asignación
                    // Constante dataMap: valor inmutable que no cambia tras su asignación
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                    distancia = dataMap.getInt("distancia", 0)
                    mascotaId = dataMap.getString("mascotaId") ?: ""
                    umbral = dataMap.getInt("umbral", 50)
                    superaUmbral = dataMap.getBoolean("superaUmbral", false)

                    // Registro de evento en el log de Android para depuración
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.d("WEAR_MSG", "📊 DataClient: Distancia=$distancia, Supera=$superaUmbral")

                    onUpdate?.invoke(distancia, mascotaId, umbral, superaUmbral)

                    sendBroadcast(Intent("com.lomito.seguro.wear.BLE_UPDATE").apply {
                        putExtra("distancia", distancia)
                        putExtra("mascotaId", mascotaId)
                        putExtra("umbral", umbral)
                        putExtra("superaUmbral", superaUmbral)
                        setPackage(packageName)
                    })

                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (superaUmbral) {
                        vibrar()
                        mostrarNotificacion(distancia, mascotaId)
                        // ✅ Abrir AlertActivity automáticamente
                        startActivity(Intent(applicationContext, AlertActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        })
                    }
                }
            } catch (e: Exception) {
                // Registro de evento en el log de Android para depuración
                // Registro de evento en el log de Android para depuración
                android.util.Log.e("WEAR_MSG", "❌ Error procesando DataChanged: ${e.message}")
            }
        }
        dataEvents.close()
    }

    private fun vibrar() {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Constante vibrator: valor inmutable que no cambia tras su asignación
            // Constante vibrator: valor inmutable que no cambia tras su asignación
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Activa la vibración háptica del smartwatch para retroalimentación táctil
                // Activa la vibración háptica del smartwatch para retroalimentación táctil
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300, 100, 600), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                // Activa la vibración háptica del smartwatch para retroalimentación táctil
                // Activa la vibración háptica del smartwatch para retroalimentación táctil
                vibrator.vibrate(longArrayOf(0, 300, 100, 300, 100, 600), -1)
            }
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("WEAR_MSG", "❌ Error vibrando: ${e.message}")
        }
    }

    private fun mostrarNotificacionAvistamiento(nombre: String, mensaje: String) {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Constante mgr: valor inmutable que no cambia tras su asignación
            // Constante mgr: valor inmutable que no cambia tras su asignación
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Crea el canal de notificación requerido en Android 8.0+
                // Crea el canal de notificación requerido en Android 8.0+
                mgr.createNotificationChannel(
                    // Crea el canal de notificación requerido en Android 8.0+
                    // Crea el canal de notificación requerido en Android 8.0+
                    NotificationChannel(CHANNEL_ID, "Alertas Lomito", NotificationManager.IMPORTANCE_HIGH)
                )
            }
            // Constante notification: valor inmutable que no cambia tras su asignación
            // Constante notification: valor inmutable que no cambia tras su asignación
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("🐾 ¡Avistamiento de $nombre!")
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            // Muestra la notificación al usuario en la barra de estado
            // Muestra la notificación al usuario en la barra de estado
            mgr.notify(1002, notification)
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("WEAR_MSG", "❌ Error notificación de avistamiento: ${e.message}")
        }
    }

    private fun mostrarNotificacion(distancia: Int, mascotaId: String) {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Constante mgr: valor inmutable que no cambia tras su asignación
            // Constante mgr: valor inmutable que no cambia tras su asignación
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Crea el canal de notificación requerido en Android 8.0+
                // Crea el canal de notificación requerido en Android 8.0+
                mgr.createNotificationChannel(
                    // Crea el canal de notificación requerido en Android 8.0+
                    // Crea el canal de notificación requerido en Android 8.0+
                    NotificationChannel(CHANNEL_ID, "Alertas Lomito", NotificationManager.IMPORTANCE_HIGH)
                )
            }
            // Constante notification: valor inmutable que no cambia tras su asignación
            // Constante notification: valor inmutable que no cambia tras su asignación
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("🚨 ¡Lomito fuera de rango!")
                .setContentText("Distancia: ${distancia}m")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            // Muestra la notificación al usuario en la barra de estado
            // Muestra la notificación al usuario en la barra de estado
            mgr.notify(1001, notification)
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("WEAR_MSG", "❌ Error notificación: ${e.message}")
        }
    }

    // Método del ciclo de vida: se limpia la actividad antes de destruirse
    // Método del ciclo de vida: se limpia la actividad antes de destruirse
    override fun onDestroy() {
        // Invoca la implementación del método en la clase padre
        // Invoca la implementación del método en la clase padre
        super.onDestroy()
        // Registro de evento en el log de Android para depuración
        // Registro de evento en el log de Android para depuración
        android.util.Log.d("WEAR_MSG", "🛑 WearMessageService destruido")
    }
}
```

## FASE 2: `com/lomito/seguro/wear/ui/alert`

### Paso 2.1: `AlertActivity.kt`

**Actividad de alerta (Wear)**. Muestra una alerta de proximidad en la pantalla del smartwatch cuando la mascota se aleja demasiado.

```kotlin
// Paquete: com.lomito.seguro.wear.ui.alert
// Paquete: com.lomito.seguro.wear.ui.alert
package com.lomito.seguro.wear.ui.alert

// Importa las clases para manejo de notificaciones
// Importa las clases para manejo de notificaciones
import android.app.NotificationChannel
// Importa las clases para manejo de notificaciones
// Importa las clases para manejo de notificaciones
import android.app.NotificationManager
// Importa la clase Intent para navegación entre componentes
// Importa la clase Intent para navegación entre componentes
import android.app.PendingIntent
// Importa el contexto de Android
// Importa el contexto de Android
import android.content.Context
// Importa la clase Intent para navegación entre componentes
// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa la dependencia necesaria: Build
// Importa la dependencia necesaria: Build
import android.os.Build
// Importa el contenedor de datos Bundle
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: WindowManager
// Importa la dependencia necesaria: WindowManager
import android.view.WindowManager
// Importa la dependencia necesaria: ComponentActivity
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.animation.core.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.border
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.CircleShape
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.runtime.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.draw.clip
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.style.TextAlign
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa las clases para manejo de notificaciones
// Importa las clases para manejo de notificaciones
import androidx.core.app.NotificationCompat
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa la dependencia necesaria: Tasks
// Importa la dependencia necesaria: Tasks
import com.google.android.gms.tasks.Tasks
// Importa la API de comunicación con Wear OS
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.Node
// Importa la API de comunicación con Wear OS
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.Wearable
// Importa la dependencia necesaria: PollingService
// Importa la dependencia necesaria: PollingService
import com.lomito.seguro.wear.data.PollingService
// Importa la dependencia necesaria: DashboardActivity
// Importa la dependencia necesaria: DashboardActivity
import com.lomito.seguro.wear.ui.dashboard.DashboardActivity
// Importa soporte para corrutinas de Kotlin
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.*

/**
 * [Actividad principal para mostrar una alerta visual y táctil]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mostrar distancia actual de la mascota y notificar al móvil]
 * - [Responder al aumento crítico de la distancia con vibración y sonidos]
 */
// Activity AlertActivity: pantalla principal que gestiona el ciclo de vida
// Activity AlertActivity: pantalla principal que gestiona el ciclo de vida
class AlertActivity : ComponentActivity() {

    companion object {
        // Constante ALERT_CHANNEL_ID: valor fijo definido en tiempo de compilación
        // Constante ALERT_CHANNEL_ID: valor fijo definido en tiempo de compilación
        const val ALERT_CHANNEL_ID = "lomito_alert_channel"
        // Constante ALERT_NOTIF_ID: valor fijo definido en tiempo de compilación
        // Constante ALERT_NOTIF_ID: valor fijo definido en tiempo de compilación
        const val ALERT_NOTIF_ID = 3001
    }

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)

        // Configurar para que se muestre sobre otras actividades
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        // Crear canal de notificación para la alerta
        crearCanalNotificacionAlerta()

        // Obtener datos del intent
        // Constante mascotaNombre: valor inmutable que no cambia tras su asignación
        // Constante mascotaNombre: valor inmutable que no cambia tras su asignación
        val mascotaNombre = intent.getStringExtra("mascota_nombre")
            ?: PollingService.mascotaNombreActual
            ?: "Tu mascota"

        // Constante distanciaInicial: valor inmutable que no cambia tras su asignación
        // Constante distanciaInicial: valor inmutable que no cambia tras su asignación
        val distanciaInicial = intent.getIntExtra("distancia", PollingService.distanciaActual)
        // Constante incremento: valor inmutable que no cambia tras su asignación
        // Constante incremento: valor inmutable que no cambia tras su asignación
        val incremento = intent.getIntExtra("incremento", 0)

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            // Variable distancia: almacena el estado mutable de este componente
            // Variable distancia: almacena el estado mutable de este componente
            var distancia by remember { mutableStateOf(distanciaInicial) }

            // Actualizar distancia en tiempo real
            LaunchedEffect(Unit) {
                while (true) {
                    distancia = PollingService.distanciaActual
                    kotlinx.coroutines.delay(1000)
                }
            }

            AlertScreen(
                distancia = distancia,
                mascotaNombre = mascotaNombre,
                incremento = incremento,
                onAceptar = {
                    // ✅ Enviar alerta al móvil
                    enviarAlertaAlMovil(PollingService.mascotaIdActual, mascotaNombre, distancia)

                    // Enviar notificación local en el Wear
                    enviarNotificacionLocal(mascotaNombre, distancia, incremento)

                    // Cerrar la alerta y resetear estado
                    PollingService.alertaMostrada = false
                    PollingService.ultimaDistanciaAlerta = distancia

                    // Ir al Dashboard
                    // Constante intent: valor inmutable que no cambia tras su asignación
                    // Constante intent: valor inmutable que no cambia tras su asignación
                    val intent = Intent(this, DashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(intent)
                    finish()
                },
                onIgnorar = {
                    // Solo cerrar la alerta y resetear estado
                    PollingService.alertaMostrada = false
                    PollingService.ultimaDistanciaAlerta = distancia
                    finish()
                }
            )
        }
    }

    /**
     * Enviar alerta al móvil
     */
    private fun enviarAlertaAlMovil(mascotaId: String, mascotaNombre: String, distancia: Int) {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Constante json: valor inmutable que no cambia tras su asignación
            // Constante json: valor inmutable que no cambia tras su asignación
            val json = org.json.JSONObject().apply {
                put("tipo", "ALERTA_MASCOTA")
                put("mascotaId", mascotaId)
                put("mascotaNombre", mascotaNombre)
                put("distancia", distancia)
                put("timestamp", System.currentTimeMillis())
            }

            // Constante payload: valor inmutable que no cambia tras su asignación
            // Constante payload: valor inmutable que no cambia tras su asignación
            val payload = json.toString().toByteArray()

            // Enviar a todos los nodos conectados
            // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
            // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
            CoroutineScope(Dispatchers.IO).launch {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // ✅ Obtener nodos conectados correctamente
                    // Constante nodeClient: valor inmutable que no cambia tras su asignación
                    // Constante nodeClient: valor inmutable que no cambia tras su asignación
                    val nodeClient = Wearable.getNodeClient(applicationContext)
                    // Constante connectedNodes: valor inmutable que no cambia tras su asignación
                    // Constante connectedNodes: valor inmutable que no cambia tras su asignación
                    val connectedNodes = Tasks.await(nodeClient.connectedNodes)

                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (connectedNodes.isNotEmpty()) {
                        // Itera sobre cada elemento de la colección y ejecuta el bloque
                        // Itera sobre cada elemento de la colección y ejecuta el bloque
                        connectedNodes.forEach { node ->
                            // Constante messageClient: valor inmutable que no cambia tras su asignación
                            // Constante messageClient: valor inmutable que no cambia tras su asignación
                            val messageClient = Wearable.getMessageClient(applicationContext)
                            Tasks.await(
                                // Envía un mensaje al dispositivo Wear OS conectado
                                // Envía un mensaje al dispositivo Wear OS conectado
                                messageClient.sendMessage(node.id, "/alerta/mascota", payload)
                            )
                            // Registro de evento en el log de Android para depuración
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.d("ALERT_ACTIVITY", "✅ Alerta enviada al móvil: ${node.displayName}")
                        }
                    } else {
                        // Registro de evento en el log de Android para depuración
                        // Registro de evento en el log de Android para depuración
                        android.util.Log.e("ALERT_ACTIVITY", "⚠️ No hay nodos conectados")
                        // Guardar alerta pendiente
                        guardarAlertaPendiente(mascotaId, mascotaNombre, distancia)
                    }
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("ALERT_ACTIVITY", "❌ Error enviando alerta: ${e.message}")
                    // Si hay error, guardar pendiente
                    guardarAlertaPendiente(mascotaId, mascotaNombre, distancia)
                }
            }

            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("ALERT_ACTIVITY", "📤 Enviando alerta: $mascotaNombre - $distancia m")
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("ALERT_ACTIVITY", "❌ Error preparando alerta: ${e.message}")
        }
    }

    /**
     * Guardar alerta pendiente para enviar cuando se conecte
     */
    private fun guardarAlertaPendiente(mascotaId: String, mascotaNombre: String, distancia: Int) {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Constante prefs: valor inmutable que no cambia tras su asignación
            // Constante prefs: valor inmutable que no cambia tras su asignación
            val prefs = getSharedPreferences("alert_pending", MODE_PRIVATE)
            // Inicia el editor para modificar los SharedPreferences
            // Inicia el editor para modificar los SharedPreferences
            prefs.edit().apply {
                putString("pending_mascota_id", mascotaId)
                putString("pending_mascota_nombre", mascotaNombre)
                putInt("pending_distancia", distancia)
                putLong("pending_timestamp", System.currentTimeMillis())
                apply()
            }
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("ALERT_ACTIVITY", "💾 Alerta guardada como pendiente")
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("ALERT_ACTIVITY", "Error guardando alerta: ${e.message}")
        }
    }

    /**
     * Crear canal de notificación para la alerta
     */
    private fun crearCanalNotificacionAlerta() {
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Constante channel: valor inmutable que no cambia tras su asignación
            // Constante channel: valor inmutable que no cambia tras su asignación
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Alertas de Mascota",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de alerta cuando una mascota se aleja"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            // Constante manager: valor inmutable que no cambia tras su asignación
            // Constante manager: valor inmutable que no cambia tras su asignación
            val manager = getSystemService(NotificationManager::class.java)
            // Crea el canal de notificación requerido en Android 8.0+
            // Crea el canal de notificación requerido en Android 8.0+
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Enviar notificación local en el Wear
     */
    private fun enviarNotificacionLocal(mascotaNombre: String, distancia: Int, incremento: Int) {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Constante intent: valor inmutable que no cambia tras su asignación
            // Constante intent: valor inmutable que no cambia tras su asignación
            val intent = Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            // Constante pendingIntent: valor inmutable que no cambia tras su asignación
            // Constante pendingIntent: valor inmutable que no cambia tras su asignación
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Constante textoNotificacion: valor inmutable que no cambia tras su asignación
            // Constante textoNotificacion: valor inmutable que no cambia tras su asignación
            val textoNotificacion = if (incremento > 0) {
                "$mascotaNombre se ha alejado a $distancia metros (+$incremento m)"
            } else {
                "$mascotaNombre se ha alejado a $distancia metros"
            }

            // Constante notification: valor inmutable que no cambia tras su asignación
            // Constante notification: valor inmutable que no cambia tras su asignación
            val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setContentTitle("🚨 ¡Alerta de Mascota!")
                .setContentText(textoNotificacion)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 300, 200, 300))
                .build()

            // Constante manager: valor inmutable que no cambia tras su asignación
            // Constante manager: valor inmutable que no cambia tras su asignación
            val manager = getSystemService(NotificationManager::class.java)
            // Muestra la notificación al usuario en la barra de estado
            // Muestra la notificación al usuario en la barra de estado
            manager.notify(ALERT_NOTIF_ID, notification)

            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("ALERT_ACTIVITY", "✅ Notificación local enviada")
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("ALERT_ACTIVITY", "Error enviando notificación local: ${e.message}")
        }
    }
}

/**
 * [Pantalla de UI que muestra el estado de alerta]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [distancia]: Distancia de la mascota
 * - [mascotaNombre]: Nombre de la mascota
 * - [incremento]: Incremento en distancia desde la última alerta
 * - [onAceptar]: Callback al confirmar la alerta
 * - [onIgnorar]: Callback al ignorar la alerta
 */
// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función AlertScreen: define la lógica de esta operación
// Función AlertScreen: define la lógica de esta operación
fun AlertScreen(
    distancia: Int,
    mascotaNombre: String,
    incremento: Int,
    onAceptar: () -> Unit,
    onIgnorar: () -> Unit
) {
    // Constante borderAlpha: valor inmutable que no cambia tras su asignación
    // Constante borderAlpha: valor inmutable que no cambia tras su asignación
    val borderAlpha by rememberInfiniteTransition(label = "border").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFF1A0000))
                .border(4.dp, Color(0xFFF44336).copy(alpha = borderAlpha), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Text("⚠️", fontSize = 28.sp)

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "¡ALERTA!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "$mascotaNombre se alejó",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "${distancia}m",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (incremento > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "⬆ +$incremento m",
                        fontSize = 12.sp,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onAceptar,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📱", fontSize = 14.sp)
                            Text("Notificar", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    Button(
                        onClick = onIgnorar,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF666666)),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✕", fontSize = 14.sp)
                            Text("Ignorar", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

            }
        }
    }
}
```

## FASE 3: `com/lomito/seguro/wear/ui/dashboard`

### Paso 3.1: `DashboardActivity.kt`

**Actividad del Dashboard**. Pantalla principal del módulo TV que muestra las mascotas en una cuadrícula optimizada para pantalla grande con control remoto.

```kotlin
// Paquete: com.lomito.seguro.wear.ui.dashboard
// Paquete: com.lomito.seguro.wear.ui.dashboard
package com.lomito.seguro.wear.ui.dashboard

// Importa la dependencia necesaria: Manifest
// Importa la dependencia necesaria: Manifest
import android.Manifest
// Importa la clase Intent para navegación entre componentes
// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa la dependencia necesaria: PackageManager
// Importa la dependencia necesaria: PackageManager
import android.content.pm.PackageManager
// Importa la dependencia necesaria: Build
// Importa la dependencia necesaria: Build
import android.os.Build
// Importa el contenedor de datos Bundle
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa la dependencia necesaria: ActivityCompat
// Importa la dependencia necesaria: ActivityCompat
import androidx.core.app.ActivityCompat
// Importa el contexto de Android
// Importa el contexto de Android
import androidx.core.content.ContextCompat
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.grid.GridCells
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.grid.items
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.CircleShape
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.RoundedCornerShape
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.runtime.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Brush
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.style.TextAlign
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa la dependencia necesaria: PollingService
// Importa la dependencia necesaria: PollingService
import com.lomito.seguro.wear.data.PollingService
// Importa la dependencia necesaria: MarcarPerdidaActivity
// Importa la dependencia necesaria: MarcarPerdidaActivity
import com.lomito.seguro.wear.ui.mascota.MarcarPerdidaActivity
// Importa la dependencia necesaria: MascotaListActivity
// Importa la dependencia necesaria: MascotaListActivity
import com.lomito.seguro.wear.ui.mascota.MascotaListActivity
// Importa la dependencia necesaria: ReportarAvistamientoActivity
// Importa la dependencia necesaria: ReportarAvistamientoActivity
import com.lomito.seguro.wear.ui.report.ReportarAvistamientoActivity
// Importa la dependencia necesaria: SettingsActivity
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
// Clase de datos MenuItem: modelo inmutable con propiedades de dominio
data class MenuItem(
    // Constante id: valor inmutable que no cambia tras su asignación
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String,
    // Constante icon: valor inmutable que no cambia tras su asignación
    // Constante icon: valor inmutable que no cambia tras su asignación
    val icon: String,
    // Constante title: valor inmutable que no cambia tras su asignación
    // Constante title: valor inmutable que no cambia tras su asignación
    val title: String,
    // Constante action: valor inmutable que no cambia tras su asignación
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
// Activity DashboardActivity: pantalla principal que gestiona el ciclo de vida
class DashboardActivity : ComponentActivity() {
    // Método del ciclo de vida: inicializa la actividad y configura la UI
    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)

        // ✅ Solicitar permisos para notificaciones en Android 13+
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
        // Constante prefs: valor inmutable que no cambia tras su asignación
        val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
        // Constante hasMascota: valor inmutable que no cambia tras su asignación
        // Constante hasMascota: valor inmutable que no cambia tras su asignación
        val hasMascota = (prefs.getString("mascota_activa_id", "") ?: "").isNotEmpty()

        // ✅ Iniciar PollingService
        // Constante serviceIntent: valor inmutable que no cambia tras su asignación
        // Constante serviceIntent: valor inmutable que no cambia tras su asignación
        val serviceIntent = Intent(this, PollingService::class.java)
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            DashboardScreen(
                hasMascota = hasMascota,
                onNavigateTo = { target ->
                    // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
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
// Constante BgTop: valor inmutable que no cambia tras su asignación
private val BgTop = Color(0xFF1B1430)
// Constante BgBottom: valor inmutable que no cambia tras su asignación
// Constante BgBottom: valor inmutable que no cambia tras su asignación
private val BgBottom = Color(0xFF0D0B1A)
// Constante CardBg1: valor inmutable que no cambia tras su asignación
// Constante CardBg1: valor inmutable que no cambia tras su asignación
private val CardBg1 = Color(0xFF2A2350) // Mis Mascotas (azul-violeta)
// Constante CardBg2: valor inmutable que no cambia tras su asignación
// Constante CardBg2: valor inmutable que no cambia tras su asignación
private val CardBg2 = Color(0xFF4A1F2E) // Marcar Perdida (rojo oscuro)
// Constante CardBg3: valor inmutable que no cambia tras su asignación
// Constante CardBg3: valor inmutable que no cambia tras su asignación
private val CardBg3 = Color(0xFF1F3A3A) // Reportar (teal oscuro)
// Constante CardBg4: valor inmutable que no cambia tras su asignación
// Constante CardBg4: valor inmutable que no cambia tras su asignación
private val CardBg4 = Color(0xFF2E2A1F) // Config (ámbar oscuro)
// Constante AccentGreen: valor inmutable que no cambia tras su asignación
// Constante AccentGreen: valor inmutable que no cambia tras su asignación
private val AccentGreen = Color(0xFF4CD97B)
// Constante AccentOrange: valor inmutable que no cambia tras su asignación
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
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función DashboardScreen: define la lógica de esta operación
// Función DashboardScreen: define la lógica de esta operación
fun DashboardScreen(
    hasMascota: Boolean,
    onNavigateTo: (String) -> Unit
) {
    // Constante menuItems: valor inmutable que no cambia tras su asignación
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
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función GridMenuItem: define la lógica de esta operación
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
```

## FASE 4: `com/lomito/seguro/wear/ui/home`

### Paso 4.1: `WearMainActivity.kt`

**Actividad principal Wear**. Pantalla principal del smartwatch que muestra el menú de opciones con acceso rápido a las funciones principales.

```kotlin
// Paquete: com.lomito.seguro.wear.ui.home
// Paquete: com.lomito.seguro.wear.ui.home
package com.lomito.seguro.wear.ui.home

// Importa la dependencia necesaria: BroadcastReceiver
// Importa la dependencia necesaria: BroadcastReceiver
import android.content.BroadcastReceiver
// Importa el contexto de Android
// Importa el contexto de Android
import android.content.Context
// Importa la clase Intent para navegación entre componentes
// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa la clase Intent para navegación entre componentes
// Importa la clase Intent para navegación entre componentes
import android.content.IntentFilter
// Importa la dependencia necesaria: Build
// Importa la dependencia necesaria: Build
import android.os.Build
// Importa el contenedor de datos Bundle
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: VibrationEffect
// Importa la dependencia necesaria: VibrationEffect
import android.os.VibrationEffect
// Importa la dependencia necesaria: Vibrator
// Importa la dependencia necesaria: Vibrator
import android.os.Vibrator
// Importa la dependencia necesaria: ComponentActivity
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.runtime.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.runtime.livedata.observeAsState
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.style.TextAlign
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa la clase base ViewModel del ciclo de vida
// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModelProvider
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa la dependencia necesaria: BleState
// Importa la dependencia necesaria: BleState
import com.lomito.seguro.wear.data.BleState
// Importa la dependencia necesaria: PollingService
// Importa la dependencia necesaria: PollingService
import com.lomito.seguro.wear.data.PollingService
// Importa la clase base ViewModel del ciclo de vida
// Importa la clase base ViewModel del ciclo de vida
import com.lomito.seguro.wear.data.WatchViewModel
// Importa la dependencia necesaria: AlertActivity
// Importa la dependencia necesaria: AlertActivity
import com.lomito.seguro.wear.ui.alert.AlertActivity
// Importa la dependencia necesaria: DashboardActivity
// Importa la dependencia necesaria: DashboardActivity
import com.lomito.seguro.wear.ui.dashboard.DashboardActivity
// Importa la dependencia necesaria: ReportActivity
// Importa la dependencia necesaria: ReportActivity
import com.lomito.seguro.wear.ui.report.ReportActivity
// Importa la dependencia necesaria: SelectionActivity
// Importa la dependencia necesaria: SelectionActivity
import com.lomito.seguro.wear.ui.selection.SelectionActivity

/**
 * [Actividad principal del reloj (Wear OS)]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mostrar el estado de la conexión BLE y la distancia actual]
 * - [Gestionar accesos a las funciones principales como alerta, reporte y cambio de mascota]
 */
// Activity WearMainActivity: pantalla principal que gestiona el ciclo de vida
// Activity WearMainActivity: pantalla principal que gestiona el ciclo de vida
class WearMainActivity : ComponentActivity() {
    private lateinit var viewModel: WatchViewModel

    // Constante bleReceiver: valor inmutable que no cambia tras su asignación
    // Constante bleReceiver: valor inmutable que no cambia tras su asignación
    private val bleReceiver = object : BroadcastReceiver() {
        // Sobreescribe la función onReceive de la clase padre
        // Sobreescribe la función onReceive de la clase padre
        override fun onReceive(context: Context, intent: Intent) {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante distancia: valor inmutable que no cambia tras su asignación
                // Constante distancia: valor inmutable que no cambia tras su asignación
                val distancia = intent.getIntExtra("distancia", 0)
                // Constante mascotaId: valor inmutable que no cambia tras su asignación
                // Constante mascotaId: valor inmutable que no cambia tras su asignación
                val mascotaId = intent.getStringExtra("mascotaId") ?: ""
                // Constante umbral: valor inmutable que no cambia tras su asignación
                // Constante umbral: valor inmutable que no cambia tras su asignación
                val umbral = intent.getIntExtra("umbral", 50)
                // Constante superaUmbral: valor inmutable que no cambia tras su asignación
                // Constante superaUmbral: valor inmutable que no cambia tras su asignación
                val superaUmbral = intent.getBooleanExtra("superaUmbral", false)
                viewModel.actualizarEstado(distancia, mascotaId, umbral, superaUmbral)
            } catch (e: Exception) {
                // Registro de evento en el log de Android para depuración
                // Registro de evento en el log de Android para depuración
                android.util.Log.e("WEAR_MAIN", "Error en bleReceiver: ${e.message}")
            }
        }
    }

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            // Invoca la implementación del método en la clase padre
            // Invoca la implementación del método en la clase padre
            super.onCreate(savedInstanceState)
            viewModel = ViewModelProvider(this)[WatchViewModel::class.java]

            // Constante prefs: valor inmutable que no cambia tras su asignación
            // Constante prefs: valor inmutable que no cambia tras su asignación
            val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
            // Constante mascotaId: valor inmutable que no cambia tras su asignación
            // Constante mascotaId: valor inmutable que no cambia tras su asignación
            val mascotaId = prefs.getString("mascota_activa_id", "") ?: ""
            // Constante mascotaNombre: valor inmutable que no cambia tras su asignación
            // Constante mascotaNombre: valor inmutable que no cambia tras su asignación
            val mascotaNombre = prefs.getString("mascota_activa_nombre", "Mascota") ?: "Mascota"
            // Constante umbral: valor inmutable que no cambia tras su asignación
            // Constante umbral: valor inmutable que no cambia tras su asignación
            val umbral = prefs.getInt("mascota_umbral", 50)

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (mascotaId.isEmpty()) {
                startActivity(Intent(this, SelectionActivity::class.java))
                finish()
                // Retorna el valor al llamador de la función
                // Retorna el valor al llamador de la función
                return
            }

            viewModel.actualizarEstado(0, mascotaId, umbral, false)

            // ✅ Iniciar PollingService global
            // Constante serviceIntent: valor inmutable que no cambia tras su asignación
            // Constante serviceIntent: valor inmutable que no cambia tras su asignación
            val serviceIntent = Intent(this, PollingService::class.java)
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            // ✅ Receiver para actualizar UI desde el PollingService
            // Constante filter: valor inmutable que no cambia tras su asignación
            // Constante filter: valor inmutable que no cambia tras su asignación
            val filter = IntentFilter("com.lomito.seguro.wear.BLE_UPDATE")
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(bleReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(bleReceiver, filter)
            }

            // Define el árbol de UI con Jetpack Compose como contenido de la Activity
            // Define el árbol de UI con Jetpack Compose como contenido de la Activity
            setContent {
                // Constante state: valor inmutable que no cambia tras su asignación
                // Constante state: valor inmutable que no cambia tras su asignación
                val state by viewModel.bleState.observeAsState(BleState())
                WearMainScreen(
                    state = state,
                    mascotaNombre = mascotaNombre,
                    onAlertClick = {
                        // Bloque try-catch: maneja posibles excepciones en el código crítico
                        // Bloque try-catch: maneja posibles excepciones en el código crítico
                        try {
                            startActivity(Intent(this, AlertActivity::class.java))
                        } catch (e: Exception) {
                            // Registro de evento en el log de Android para depuración
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.e("WEAR_MAIN", "Error: ${e.message}")
                        }
                    },
                    onReportClick = {
                        // Bloque try-catch: maneja posibles excepciones en el código crítico
                        // Bloque try-catch: maneja posibles excepciones en el código crítico
                        try {
                            startActivity(Intent(this, ReportActivity::class.java).apply {
                                putExtra("mascotaId", mascotaId)
                                putExtra("mascotaNombre", mascotaNombre)
                            })
                        } catch (e: Exception) {
                            // Registro de evento en el log de Android para depuración
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.e("WEAR_MAIN", "Error: ${e.message}")
                        }
                    },
                    onChangeMascota = {
                        // Bloque try-catch: maneja posibles excepciones en el código crítico
                        // Bloque try-catch: maneja posibles excepciones en el código crítico
                        try {
                            startActivity(Intent(this, SelectionActivity::class.java))
                            finish()
                        } catch (e: Exception) {
                            // Registro de evento en el log de Android para depuración
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.e("WEAR_MAIN", "Error: ${e.message}")
                        }
                    },
                    onDashboardClick = {
                        // Bloque try-catch: maneja posibles excepciones en el código crítico
                        // Bloque try-catch: maneja posibles excepciones en el código crítico
                        try {
                            startActivity(Intent(this, DashboardActivity::class.java))
                            finish()
                        } catch (e: Exception) {
                            // Registro de evento en el log de Android para depuración
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.e("WEAR_MAIN", "Error: ${e.message}")
                        }
                    }
                )
            }

        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("WEAR_MAIN", "Error FATAL: ${e.message}", e)
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                startActivity(Intent(this, SelectionActivity::class.java))
            } catch (e2: Exception) {
                // Registro de evento en el log de Android para depuración
                // Registro de evento en el log de Android para depuración
                android.util.Log.e("WEAR_MAIN", "No se pudo abrir Selection: ${e2.message}")
            }
            finish()
        }
    }

    // Método del ciclo de vida: se limpia la actividad antes de destruirse
    // Método del ciclo de vida: se limpia la actividad antes de destruirse
    override fun onDestroy() {
        // Invoca la implementación del método en la clase padre
        // Invoca la implementación del método en la clase padre
        super.onDestroy()
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try { unregisterReceiver(bleReceiver) } catch (e: Exception) {}
    }
}

/**
 * [Pantalla principal de la aplicación en el reloj]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [state]: Estado actual de la mascota y conexión BLE
 * - [mascotaNombre]: Nombre de la mascota actual
 * - [onAlertClick]: Acción al presionar el botón de alerta
 * - [onReportClick]: Acción al presionar el botón de reporte
 * - [onChangeMascota]: Acción para cambiar la mascota activa
 * - [onDashboardClick]: Acción para ir al menú principal
 */
// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función WearMainScreen: define la lógica de esta operación
// Función WearMainScreen: define la lógica de esta operación
fun WearMainScreen(
    state: BleState,
    mascotaNombre: String,
    onAlertClick: () -> Unit,
    onReportClick: () -> Unit,
    onChangeMascota: () -> Unit,
    onDashboardClick: () -> Unit
) {
    // Constante pct: valor inmutable que no cambia tras su asignación
    // Constante pct: valor inmutable que no cambia tras su asignación
    val pct = if (state.umbral > 0)
        (state.distancia.toFloat() / state.umbral.toFloat()).coerceIn(0f, 1f)
    else 0f

    // Constante ringColor: valor inmutable que no cambia tras su asignación
    // Constante ringColor: valor inmutable que no cambia tras su asignación
    val ringColor = when {
        state.distancia > 250 -> Color(0xFF8B0000)
        state.distancia > 150 -> Color(0xFFD32F2F)
        state.distancia > 100 -> Color(0xFFE53935)
        state.distancia > 70  -> Color(0xFFFF5722)
        state.distancia > 50  -> Color(0xFFFF9800)
        state.distancia > 30  -> Color(0xFFFFC107)
        else                  -> Color(0xFF4CAF50)
    }

    // Constante bgColor: valor inmutable que no cambia tras su asignación
    // Constante bgColor: valor inmutable que no cambia tras su asignación
    val bgColor = when {
        state.distancia > 250 -> Color(0xFF1A0000)
        state.distancia > 150 -> Color(0xFF2A0000)
        state.distancia > 100 -> Color(0xFF3A0000)
        state.distancia > 70  -> Color(0xFF3A1A00)
        state.distancia > 50  -> Color(0xFF3A2A00)
        state.distancia > 30  -> Color(0xFF2A2A00)
        else                  -> Color(0xFF1A1A2E)
    }

    // Constante alertLevel: valor inmutable que no cambia tras su asignación
    // Constante alertLevel: valor inmutable que no cambia tras su asignación
    val alertLevel = when {
        state.distancia > 250 -> "🚨 ¡PELIGRO EXTREMO!"
        state.distancia > 150 -> "🚨 ¡ALERTA MÁXIMA!"
        state.distancia > 100 -> "🔴 ¡ALERTA!"
        state.distancia > 70  -> "🟠 ¡Cuidado!"
        state.distancia > 50  -> "🟡 Atención"
        state.distancia > 30  -> "🟢 Distancia media"
        else                  -> "✅ En rango"
    }

    // Constante alertIcon: valor inmutable que no cambia tras su asignación
    // Constante alertIcon: valor inmutable que no cambia tras su asignación
    val alertIcon = when {
        state.distancia > 100 -> "🚨"
        state.distancia > 50  -> "⚠️"
        else                  -> "🐾"
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = pct,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                strokeWidth = 6.dp,
                indicatorColor = ringColor,
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = mascotaNombre,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(text = alertIcon, fontSize = 24.sp)

                Text(
                    text = "${state.distancia}m",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = ringColor
                )

                Text(
                    text = alertLevel,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Umbral: ${state.umbral}m",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CompactButton(
                        onClick = onAlertClick,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F)),
                        modifier = Modifier.size(36.dp)
                    ) { Text("🔔", fontSize = 12.sp) }

                    CompactButton(
                        onClick = onReportClick,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1565C0)),
                        modifier = Modifier.size(36.dp)
                    ) { Text("📍", fontSize = 12.sp) }

                    CompactButton(
                        onClick = onChangeMascota,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6A1B9A)),
                        modifier = Modifier.size(36.dp)
                    ) { Text("🐾", fontSize = 12.sp) }

                    CompactButton(
                        onClick = onDashboardClick,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2E7D32)),
                        modifier = Modifier.size(36.dp)
                    ) { Text("🏠", fontSize = 12.sp) }
                }
            }
        }
    }
}
```

## FASE 5: `com/lomito/seguro/wear/ui/mascota`

### Paso 5.1: `AddMascotaActivity.kt`

**Actividad de agregar mascota (Wear)**. Formulario simplificado para registrar una nueva mascota desde el smartwatch.

```kotlin
// Paquete: com.lomito.seguro.wear.ui.mascota
// Paquete: com.lomito.seguro.wear.ui.mascota
package com.lomito.seguro.wear.ui.mascota
// Importa la dependencia necesaria: BuildConfig
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.wear.BuildConfig

// Importa el contenedor de datos Bundle
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.runtime.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa soporte para corrutinas de Kotlin
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.*
// Importa el parser JSON
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: HttpURLConnection
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
// Importa la dependencia necesaria: URL
import java.net.URL

/**
 * [Actividad para agregar una nueva mascota desde el reloj]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Capturar nombre, especie y umbral]
 * - [Enviar la solicitud al backend para registrar la nueva mascota]
 */
// Activity AddMascotaActivity: pantalla principal que gestiona el ciclo de vida
// Activity AddMascotaActivity: pantalla principal que gestiona el ciclo de vida
class AddMascotaActivity : ComponentActivity() {
    // Variable isSending: almacena el estado mutable de este componente
    // Variable isSending: almacena el estado mutable de este componente
    private var isSending = false
    // Variable isSuccess: almacena el estado mutable de este componente
    // Variable isSuccess: almacena el estado mutable de este componente
    private var isSuccess = false
    // Variable errorMessage: almacena el estado mutable de este componente
    // Variable errorMessage: almacena el estado mutable de este componente
    private var errorMessage = ""
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    private val backendUrl = BuildConfig.BACKEND_URL

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            AddMascotaScreen(
                isSending = isSending,
                isSuccess = isSuccess,
                errorMessage = errorMessage,
                onSave = { nombre, especie, umbral ->
                    crearMascota(nombre, especie, umbral)
                },
                onBack = { finish() }
            )
        }
    }

    private fun crearMascota(nombre: String, especie: String, umbral: Int) {
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (isSending) return
        isSending = true

        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        CoroutineScope(Dispatchers.IO).launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante prefs: valor inmutable que no cambia tras su asignación
                // Constante prefs: valor inmutable que no cambia tras su asignación
                val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
                // Constante userId: valor inmutable que no cambia tras su asignación
                // Constante userId: valor inmutable que no cambia tras su asignación
                val userId = prefs.getString("user_id", "2") ?: "2"

                // Constante url: valor inmutable que no cambia tras su asignación
                // Constante url: valor inmutable que no cambia tras su asignación
                val url = URL("$backendUrl/api/mascotas")
                // Constante conn: valor inmutable que no cambia tras su asignación
                // Constante conn: valor inmutable que no cambia tras su asignación
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                // Constante json: valor inmutable que no cambia tras su asignación
                // Constante json: valor inmutable que no cambia tras su asignación
                val json = JSONObject().apply {
                    put("nombre", nombre)
                    put("especie", especie)
                    put("distancia_alerta", umbral)
                    put("owner_id", userId.toInt())
                }

                conn.outputStream.write(json.toString().toByteArray())
                // Constante responseCode: valor inmutable que no cambia tras su asignación
                // Constante responseCode: valor inmutable que no cambia tras su asignación
                val responseCode = conn.responseCode
                conn.disconnect()

                // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                withContext(Dispatchers.Main) {
                    isSending = false
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (responseCode == 200 || responseCode == 201) {
                        isSuccess = true
                    } else {
                        errorMessage = "Error al crear"
                    }
                }
            } catch (e: Exception) {
                // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                withContext(Dispatchers.Main) {
                    isSending = false
                    errorMessage = e.message ?: "Error desconocido"
                }
            }
        }
    }
}

/**
 * [Pantalla de formulario para agregar mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [isSending]: Estado de envío de la solicitud
 * - [isSuccess]: Estado de éxito tras guardar
 * - [errorMessage]: Mensaje de error a mostrar si falla
 * - [onSave]: Acción al guardar los datos
 * - [onBack]: Acción para volver atrás
 */
// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función AddMascotaScreen: define la lógica de esta operación
// Función AddMascotaScreen: define la lógica de esta operación
fun AddMascotaScreen(
    isSending: Boolean,
    isSuccess: Boolean,
    errorMessage: String,
    onSave: (String, String, Int) -> Unit,
    onBack: () -> Unit
) {
    // Variable nombre: almacena el estado mutable de este componente
    // Variable nombre: almacena el estado mutable de este componente
    var nombre by remember { mutableStateOf("") }
    // Variable especie: almacena el estado mutable de este componente
    // Variable especie: almacena el estado mutable de este componente
    var especie by remember { mutableStateOf("PERRO") }
    // Variable umbral: almacena el estado mutable de este componente
    // Variable umbral: almacena el estado mutable de este componente
    var umbral by remember { mutableStateOf(50) }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2E))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header compacto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "➕ Agregar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                CompactButton(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF666666)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("✕", fontSize = 12.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (isSuccess) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 32.sp)
                        Text(
                            text = "¡Creada!",
                            color = Color(0xFF4CAF50),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CompactButton(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2196F3)),
                            modifier = Modifier.size(width = 80.dp, height = 32.dp)
                        ) {
                            Text("Volver", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            } else {
                // Campo Nombre
                Text(
                    text = "Nombre",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = if (nombre.isEmpty()) "Escribe..." else nombre,
                    color = if (nombre.isEmpty()) Color.White.copy(alpha = 0.3f) else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2C2C3E))
                        .padding(6.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Especie
                Text(
                    text = "Especie",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 9.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CompactButton(
                        onClick = { especie = "PERRO" },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (especie == "PERRO") Color(0xFF4CAF50) else Color(0xFF2C2C3E)
                        ),
                        modifier = Modifier.weight(1f).height(28.dp)
                    ) {
                        Text("🐕", fontSize = 12.sp)
                    }
                    CompactButton(
                        onClick = { especie = "GATO" },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (especie == "GATO") Color(0xFF4CAF50) else Color(0xFF2C2C3E)
                        ),
                        modifier = Modifier.weight(1f).height(28.dp)
                    ) {
                        Text("🐈", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Umbral
                Text(
                    text = "Umbral: $umbral m",
                    color = Color.White,
                    fontSize = 11.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(0.7f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactButton(
                        onClick = { if (umbral > 10) umbral -= 10 },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C2C3E)),
                        modifier = Modifier.weight(1f).height(28.dp)
                    ) {
                        Text("−", fontSize = 14.sp, color = Color.White)
                    }
                    CompactButton(
                        onClick = { if (umbral < 100) umbral += 10 },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C2C3E)),
                        modifier = Modifier.weight(1f).height(28.dp)
                    ) {
                        Text("+", fontSize = 14.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFF44336),
                        fontSize = 8.sp
                    )
                }

                CompactButton(
                    onClick = {
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (nombre.isNotEmpty()) {
                            onSave(nombre, especie, umbral)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (nombre.isNotEmpty()) Color(0xFF4CAF50) else Color(0xFF666666)
                    ),
                    modifier = Modifier.fillMaxWidth(0.8f).height(32.dp)
                ) {
                    Text(
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (isSending) "Creando..." else "Crear",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
```

### Paso 5.2: `MarcarPerdidaActivity.kt`

**Actividad marcar perdida (Wear)**. Permite marcar una mascota como perdida directamente desde el smartwatch con un toque.

```kotlin
// Paquete: com.lomito.seguro.wear.ui.mascota
// Paquete: com.lomito.seguro.wear.ui.mascota
package com.lomito.seguro.wear.ui.mascota
// Importa la dependencia necesaria: BuildConfig
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.wear.BuildConfig

// Importa el contenedor de datos Bundle
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.LazyColumn
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.items
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.CircleShape
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.RoundedCornerShape
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.runtime.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Brush
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa soporte para corrutinas de Kotlin
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.*
// Importa el parser JSON
// Importa el parser JSON
import org.json.JSONArray
// Importa el parser JSON
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: HttpURLConnection
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
// Importa la dependencia necesaria: URL
import java.net.URL

/**
 * [Modelo simplificado para representar a una mascota en la lista de mascotas a perder]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mantener el estado e información básica de la mascota]
 */
// Clase de datos MascotaParaPerder: modelo inmutable con propiedades de dominio
// Clase de datos MascotaParaPerder: modelo inmutable con propiedades de dominio
data class MascotaParaPerder(
    // Constante id: valor inmutable que no cambia tras su asignación
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String,
    // Constante nombre: valor inmutable que no cambia tras su asignación
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String,
    // Constante especie: valor inmutable que no cambia tras su asignación
    // Constante especie: valor inmutable que no cambia tras su asignación
    val especie: String,
    // Constante raza: valor inmutable que no cambia tras su asignación
    // Constante raza: valor inmutable que no cambia tras su asignación
    val raza: String = "",
    // Constante color: valor inmutable que no cambia tras su asignación
    // Constante color: valor inmutable que no cambia tras su asignación
    val color: String = "",
    // Constante fotoUrl: valor inmutable que no cambia tras su asignación
    // Constante fotoUrl: valor inmutable que no cambia tras su asignación
    val fotoUrl: String? = null,
    // Constante distanciaAlerta: valor inmutable que no cambia tras su asignación
    // Constante distanciaAlerta: valor inmutable que no cambia tras su asignación
    val distanciaAlerta: Int = 50,
    // Constante estado: valor inmutable que no cambia tras su asignación
    // Constante estado: valor inmutable que no cambia tras su asignación
    val estado: String = "EN_CASA"
)

/**
 * [Actividad para listar mascotas y marcarlas como perdidas]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Obtener la lista de mascotas del usuario actual]
 * - [Permitir al usuario cambiar el estado de una mascota a "PERDIDA"]
 */
// Activity MarcarPerdidaActivity: pantalla principal que gestiona el ciclo de vida
// Activity MarcarPerdidaActivity: pantalla principal que gestiona el ciclo de vida
class MarcarPerdidaActivity : ComponentActivity() {
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    private val backendUrl = BuildConfig.BACKEND_URL

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)

        // Constante prefs: valor inmutable que no cambia tras su asignación
        // Constante prefs: valor inmutable que no cambia tras su asignación
        val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
        // Variable userId: almacena el estado mutable de este componente
        // Variable userId: almacena el estado mutable de este componente
        var userId = prefs.getString("user_id", "") ?: ""

        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (userId.isEmpty() || !userId.matches(Regex("^\\d+$"))) {
            userId = "2"
            // Inicia el editor para modificar los SharedPreferences
            // Inicia el editor para modificar los SharedPreferences
            prefs.edit().putString("user_id", userId).apply()
        }

        // Registro de evento en el log de Android para depuración
        // Registro de evento en el log de Android para depuración
        android.util.Log.d("MARCAR_PERDIDA", "📱 userId: $userId")

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            // Variable mascotas: almacena el estado mutable de este componente
            // Variable mascotas: almacena el estado mutable de este componente
            var mascotas by remember { mutableStateOf<List<MascotaParaPerder>>(emptyList()) }
            // Variable isLoading: almacena el estado mutable de este componente
            // Variable isLoading: almacena el estado mutable de este componente
            var isLoading by remember { mutableStateOf(true) }
            // Variable errorMessage: almacena el estado mutable de este componente
            // Variable errorMessage: almacena el estado mutable de este componente
            var errorMessage by remember { mutableStateOf("") }
            // Variable successMessage: almacena el estado mutable de este componente
            // Variable successMessage: almacena el estado mutable de este componente
            var successMessage by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                // Constante result: valor inmutable que no cambia tras su asignación
                // Constante result: valor inmutable que no cambia tras su asignación
                val result = withContext(Dispatchers.IO) {
                    cargarMascotas(userId)
                }
                mascotas = result.mascotas
                isLoading = false
                errorMessage = result.errorMessage
            }

            MarcarPerdidaScreen(
                mascotas = mascotas,
                isLoading = isLoading,
                errorMessage = errorMessage,
                successMessage = successMessage,
                onMarcarPerdida = { mascotaId ->
                    // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                    // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                    CoroutineScope(Dispatchers.Main).launch {
                        // Constante result: valor inmutable que no cambia tras su asignación
                        // Constante result: valor inmutable que no cambia tras su asignación
                        val result = withContext(Dispatchers.IO) {
                            marcarComoPerdida(mascotaId, userId)
                        }
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (result.success) {
                            successMessage = "✅ Mascota marcada como PERDIDA"
                            errorMessage = ""
                            // Recargar lista
                            isLoading = true
                            // Constante newResult: valor inmutable que no cambia tras su asignación
                            // Constante newResult: valor inmutable que no cambia tras su asignación
                            val newResult = withContext(Dispatchers.IO) {
                                cargarMascotas(userId)
                            }
                            mascotas = newResult.mascotas
                            isLoading = false
                            errorMessage = newResult.errorMessage
                        } else {
                            errorMessage = result.errorMessage
                            successMessage = ""
                        }
                    }
                },
                onBack = { finish() },
                onRetry = {
                    isLoading = true
                    errorMessage = ""
                    successMessage = ""
                    // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                    // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                    CoroutineScope(Dispatchers.Main).launch {
                        // Constante result: valor inmutable que no cambia tras su asignación
                        // Constante result: valor inmutable que no cambia tras su asignación
                        val result = withContext(Dispatchers.IO) {
                            cargarMascotas(userId)
                        }
                        mascotas = result.mascotas
                        isLoading = false
                        errorMessage = result.errorMessage
                    }
                }
            )
        }
    }

    private suspend fun cargarMascotas(userId: String): CargaResult {
        // Retorna el valor al llamador de la función
        // Retorna el valor al llamador de la función
        return try {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("MARCAR_PERDIDA", "📱 Cargando mascotas para usuario: $userId")

            // Constante url: valor inmutable que no cambia tras su asignación
            // Constante url: valor inmutable que no cambia tras su asignación
            val url = URL("$backendUrl/api/mascotas?ownerId=$userId")
            // Constante conn: valor inmutable que no cambia tras su asignación
            // Constante conn: valor inmutable que no cambia tras su asignación
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            // Constante responseCode: valor inmutable que no cambia tras su asignación
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            val responseCode = conn.responseCode
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("MARCAR_PERDIDA", "📡 Response Code: $responseCode")

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Constante response: valor inmutable que no cambia tras su asignación
                // Constante response: valor inmutable que no cambia tras su asignación
                val response = conn.inputStream.bufferedReader().readText()
                // Registro de evento en el log de Android para depuración
                // Registro de evento en el log de Android para depuración
                android.util.Log.d("MARCAR_PERDIDA", "📥 Respuesta: $response")

                // Constante jsonArray: valor inmutable que no cambia tras su asignación
                // Constante jsonArray: valor inmutable que no cambia tras su asignación
                val jsonArray = JSONArray(response)
                // Registro de evento en el log de Android para depuración
                // Registro de evento en el log de Android para depuración
                android.util.Log.d("MARCAR_PERDIDA", "📊 JSON Array Length: ${jsonArray.length()}")

                // Constante lista: valor inmutable que no cambia tras su asignación
                // Constante lista: valor inmutable que no cambia tras su asignación
                val lista = mutableListOf<MascotaParaPerder>()
                // Itera sobre la colección para procesar cada elemento
                // Itera sobre la colección para procesar cada elemento
                for (i in 0 until jsonArray.length()) {
                    // Constante obj: valor inmutable que no cambia tras su asignación
                    // Constante obj: valor inmutable que no cambia tras su asignación
                    val obj = jsonArray.getJSONObject(i)
                    lista.add(
                        MascotaParaPerder(
                            id = obj.getString("id"),
                            nombre = obj.getString("nombre"),
                            especie = obj.getString("especie"),
                            raza = obj.optString("raza", ""),
                            color = obj.optString("color", ""),
                            fotoUrl = obj.optString("foto_url", null),
                            distanciaAlerta = obj.getInt("distancia_alerta"),
                            estado = obj.getString("estado")
                        )
                    )
                }
                conn.disconnect()

                CargaResult(
                    mascotas = lista,
                    errorMessage = if (lista.isEmpty()) "No tienes mascotas registradas" else ""
                )
            } else {
                conn.disconnect()
                CargaResult(
                    mascotas = emptyList(),
                    errorMessage = "Error al cargar mascotas (HTTP $responseCode)"
                )
            }
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("MARCAR_PERDIDA", "❌ Error: ${e.message}", e)
            CargaResult(
                mascotas = emptyList(),
                errorMessage = "Error: ${e.message}"
            )
        }
    }

    private suspend fun marcarComoPerdida(mascotaId: String, userId: String): OperacionResult {
        // Retorna el valor al llamador de la función
        // Retorna el valor al llamador de la función
        return try {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("MARCAR_PERDIDA", "🔴 Marcando mascota $mascotaId como PERDIDA")

            // Constante url: valor inmutable que no cambia tras su asignación
            // Constante url: valor inmutable que no cambia tras su asignación
            val url = URL("$backendUrl/api/mascotas/$mascotaId")
            // Constante conn: valor inmutable que no cambia tras su asignación
            // Constante conn: valor inmutable que no cambia tras su asignación
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            // Constante json: valor inmutable que no cambia tras su asignación
            // Constante json: valor inmutable que no cambia tras su asignación
            val json = JSONObject().apply {
                put("estado", "PERDIDA")
            }

            conn.outputStream.write(json.toString().toByteArray())
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            val responseCode = conn.responseCode
            conn.disconnect()

            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("MARCAR_PERDIDA", "📡 Response Code: $responseCode")

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (responseCode == 200 || responseCode == 201) {
                OperacionResult(success = true, errorMessage = "")
            } else {
                OperacionResult(success = false, errorMessage = "Error al marcar como perdida (HTTP $responseCode)")
            }
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("MARCAR_PERDIDA", "❌ Error: ${e.message}", e)
            OperacionResult(success = false, errorMessage = "Error: ${e.message}")
        }
    }

    // Clase de datos CargaResult: modelo inmutable con propiedades de dominio
    // Clase de datos CargaResult: modelo inmutable con propiedades de dominio
    data class CargaResult(
        // Constante mascotas: valor inmutable que no cambia tras su asignación
        // Constante mascotas: valor inmutable que no cambia tras su asignación
        val mascotas: List<MascotaParaPerder>,
        // Constante errorMessage: valor inmutable que no cambia tras su asignación
        // Constante errorMessage: valor inmutable que no cambia tras su asignación
        val errorMessage: String
    )

    // Clase de datos OperacionResult: modelo inmutable con propiedades de dominio
    // Clase de datos OperacionResult: modelo inmutable con propiedades de dominio
    data class OperacionResult(
        // Constante success: valor inmutable que no cambia tras su asignación
        // Constante success: valor inmutable que no cambia tras su asignación
        val success: Boolean,
        // Constante errorMessage: valor inmutable que no cambia tras su asignación
        // Constante errorMessage: valor inmutable que no cambia tras su asignación
        val errorMessage: String
    )
}

// 🎨 Paleta temática "mascotas perdidas" (consistente con el resto de la app)
// Constante BgTop: valor inmutable que no cambia tras su asignación
// Constante BgTop: valor inmutable que no cambia tras su asignación
private val BgTop = Color(0xFF1B1430)
// Constante BgBottom: valor inmutable que no cambia tras su asignación
// Constante BgBottom: valor inmutable que no cambia tras su asignación
private val BgBottom = Color(0xFF0D0B1A)
// Constante CardBg: valor inmutable que no cambia tras su asignación
// Constante CardBg: valor inmutable que no cambia tras su asignación
private val CardBg = Color(0xFF252044)
// Constante AccentRed: valor inmutable que no cambia tras su asignación
// Constante AccentRed: valor inmutable que no cambia tras su asignación
private val AccentRed = Color(0xFFE85D5D)
// Constante AccentGreen: valor inmutable que no cambia tras su asignación
// Constante AccentGreen: valor inmutable que no cambia tras su asignación
private val AccentGreen = Color(0xFF4CD97B)
// Constante AccentOrange: valor inmutable que no cambia tras su asignación
// Constante AccentOrange: valor inmutable que no cambia tras su asignación
private val AccentOrange = Color(0xFFFFA94D)
// Constante AccentBlue: valor inmutable que no cambia tras su asignación
// Constante AccentBlue: valor inmutable que no cambia tras su asignación
private val AccentBlue = Color(0xFF4D9FFF)

/**
 * [Pantalla para listar y marcar mascotas como perdidas]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [mascotas]: Lista de mascotas del usuario
 * - [isLoading]: Estado de carga
 * - [errorMessage]: Mensaje de error general
 * - [successMessage]: Mensaje de éxito al realizar la operación
 * - [onMarcarPerdida]: Acción al intentar marcar como perdida
 * - [onBack]: Acción para volver atrás
 * - [onRetry]: Acción para reintentar la carga
 */
// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MarcarPerdidaScreen: define la lógica de esta operación
// Función MarcarPerdidaScreen: define la lógica de esta operación
fun MarcarPerdidaScreen(
    mascotas: List<MascotaParaPerder>,
    isLoading: Boolean,
    errorMessage: String,
    successMessage: String,
    onMarcarPerdida: (String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
        ) {
            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 2.dp,
                                indicatorColor = AccentGreen
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Cargando mascotas...",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                mascotas.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🐾", fontSize = 22.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "No hay mascotas",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (errorMessage.isNotEmpty()) {
                                Text(
                                    text = errorMessage,
                                    color = AccentOrange,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            CompactButton(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
                                modifier = Modifier.size(width = 84.dp, height = 32.dp)
                            ) {
                                Text("Reintentar", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }
                else -> {
                    // ✅ Todo el contenido (header, mensajes, lista,
                    // botón cerrar) va dentro del LazyColumn, así nada
                    // se corta contra el bisel circular y todo es
                    // scrolleable si no alcanza el espacio.
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(top = 22.dp, bottom = 16.dp)
                    ) {
                        item {
                            Text(text = "🆘", fontSize = 20.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Marcar como Perdida",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(6.dp))
                        }

                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (successMessage.isNotEmpty()) {
                            item {
                                Text(
                                    text = successMessage,
                                    color = AccentGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }

                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (errorMessage.isNotEmpty()) {
                            item {
                                Text(
                                    text = errorMessage,
                                    color = AccentOrange,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        item {
                            Text(
                                text = "Selecciona la mascota perdida",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 9.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        items(mascotas) { mascota ->
                            MascotaMarcarPerdidaCard(
                                mascota = mascota,
                                onMarcarPerdida = {
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    if (mascota.estado != "PERDIDA") {
                                        onMarcarPerdida(mascota.id)
                                    }
                                }
                            )
                            Spacer(Modifier.height(6.dp))
                        }

                        item {
                            Spacer(Modifier.height(2.dp))
                            CompactChip(
                                onClick = onBack,
                                label = { Text("Cerrar", fontSize = 10.sp) },
                                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
                                modifier = Modifier.fillMaxWidth(0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MascotaMarcarPerdidaCard: define la lógica de esta operación
// Función MascotaMarcarPerdidaCard: define la lógica de esta operación
fun MascotaMarcarPerdidaCard(
    mascota: MascotaParaPerder,
    onMarcarPerdida: () -> Unit
) {
    // Constante isPerdida: valor inmutable que no cambia tras su asignación
    // Constante isPerdida: valor inmutable que no cambia tras su asignación
    val isPerdida = mascota.estado == "PERDIDA"
    // Constante estadoColor: valor inmutable que no cambia tras su asignación
    // Constante estadoColor: valor inmutable que no cambia tras su asignación
    val estadoColor = if (isPerdida) AccentRed else AccentGreen
    // Constante estadoTexto: valor inmutable que no cambia tras su asignación
    // Constante estadoTexto: valor inmutable que no cambia tras su asignación
    val estadoTexto = if (isPerdida) "PERDIDA" else "EN CASA"

    Card(
        modifier = Modifier.fillMaxWidth(0.92f),
        onClick = { if (!isPerdida) onMarcarPerdida() },
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(CardBg, CardBg.copy(alpha = 0.7f))))
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(estadoColor)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (mascota.especie == "PERRO") "🐕" else "🐈",
                                fontSize = 16.sp
                            )
                            Column {
                                Text(
                                    text = mascota.nombre,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = listOf(mascota.raza, mascota.color)
                                        .filter { it.isNotEmpty() }
                                        .joinToString(" • "),
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(estadoColor, CircleShape)
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (isPerdida) {
                        Text(
                            text = "⚠️ Ya está perdida",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 9.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        CompactButton(
                            onClick = onMarcarPerdida,
                            colors = ButtonDefaults.buttonColors(backgroundColor = AccentRed),
                            modifier = Modifier.fillMaxWidth().height(28.dp)
                        ) {
                            Text("🆘 Marcar como Perdida", fontSize = 9.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
```

### Paso 5.3: `MascotaDetailActivity.kt`

**Actividad de detalle de mascota (Wear)**. Muestra la información básica de una mascota en la pequeña pantalla del reloj.

```kotlin
// Paquete: com.lomito.seguro.wear.ui.mascota
// Paquete: com.lomito.seguro.wear.ui.mascota
package com.lomito.seguro.wear.ui.mascota
// Importa la dependencia necesaria: BuildConfig
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.wear.BuildConfig

// Importa la clase Intent para navegación entre componentes
// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa el contenedor de datos Bundle
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.LazyColumn
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.items
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.CircleShape
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.RoundedCornerShape
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.runtime.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.draw.clip
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Brush
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.layout.ContentScale
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import coil.compose.AsyncImage
// Importa soporte para corrutinas de Kotlin
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.*
// Importa el parser JSON
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: HttpURLConnection
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
// Importa la dependencia necesaria: URL
import java.net.URL

/**
 * [Actividad para mostrar los detalles de una mascota específica]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Visualizar la información detallada de la mascota]
 * - [Permitir al usuario alternar el estado de la mascota (PERDIDA / EN CASA)]
 */
// Activity MascotaDetailActivity: pantalla principal que gestiona el ciclo de vida
// Activity MascotaDetailActivity: pantalla principal que gestiona el ciclo de vida
class MascotaDetailActivity : ComponentActivity() {

    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    private val backendUrl = BuildConfig.BACKEND_URL

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)

        // Constante mascotaId: valor inmutable que no cambia tras su asignación
        // Constante mascotaId: valor inmutable que no cambia tras su asignación
        val mascotaId = intent.getStringExtra("mascota_id") ?: ""
        // Constante nombre: valor inmutable que no cambia tras su asignación
        // Constante nombre: valor inmutable que no cambia tras su asignación
        val nombre = intent.getStringExtra("mascota_nombre") ?: "Mascota"
        // Constante especie: valor inmutable que no cambia tras su asignación
        // Constante especie: valor inmutable que no cambia tras su asignación
        val especie = intent.getStringExtra("mascota_especie") ?: ""
        // Constante raza: valor inmutable que no cambia tras su asignación
        // Constante raza: valor inmutable que no cambia tras su asignación
        val raza = intent.getStringExtra("mascota_raza") ?: ""
        // Constante edad: valor inmutable que no cambia tras su asignación
        // Constante edad: valor inmutable que no cambia tras su asignación
        val edad = intent.getIntExtra("mascota_edad", 0)
        // Constante color: valor inmutable que no cambia tras su asignación
        // Constante color: valor inmutable que no cambia tras su asignación
        val color = intent.getStringExtra("mascota_color") ?: ""
        // Constante peso: valor inmutable que no cambia tras su asignación
        // Constante peso: valor inmutable que no cambia tras su asignación
        val peso = intent.getStringExtra("mascota_peso") ?: ""
        // Constante fotoUrl: valor inmutable que no cambia tras su asignación
        // Constante fotoUrl: valor inmutable que no cambia tras su asignación
        val fotoUrl = intent.getStringExtra("mascota_foto")
        // Constante distanciaAlerta: valor inmutable que no cambia tras su asignación
        // Constante distanciaAlerta: valor inmutable que no cambia tras su asignación
        val distanciaAlerta = intent.getIntExtra("mascota_distancia_alerta", 50)
        // Variable estadoInicial: almacena el estado mutable de este componente
        // Variable estadoInicial: almacena el estado mutable de este componente
        var estadoInicial = intent.getStringExtra("mascota_estado") ?: "EN_CASA"
        // Constante distanciaSimulada: valor inmutable que no cambia tras su asignación
        // Constante distanciaSimulada: valor inmutable que no cambia tras su asignación
        val distanciaSimulada = intent.getIntExtra("mascota_distancia_simulada", 0)

        // Constante fotoUrlAbs: valor inmutable que no cambia tras su asignación
        // Constante fotoUrlAbs: valor inmutable que no cambia tras su asignación
        val fotoUrlAbs = fotoUrl?.takeIf { it.isNotEmpty() }?.let {
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (it.startsWith("http")) it else "$backendUrl$it"
        }

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            // Variable estado: almacena el estado mutable de este componente
            // Variable estado: almacena el estado mutable de este componente
            var estado by remember { mutableStateOf(estadoInicial) }
            // Variable isUpdating: almacena el estado mutable de este componente
            // Variable isUpdating: almacena el estado mutable de este componente
            var isUpdating by remember { mutableStateOf(false) }
            // Variable showToast: almacena el estado mutable de este componente
            // Variable showToast: almacena el estado mutable de este componente
            var showToast by remember { mutableStateOf(false) }
            // Variable toastMessage: almacena el estado mutable de este componente
            // Variable toastMessage: almacena el estado mutable de este componente
            var toastMessage by remember { mutableStateOf("") }

            MascotaDetailScreen(
                nombre = nombre,
                especie = especie,
                raza = raza,
                edad = edad,
                color = color,
                peso = peso,
                fotoUrl = fotoUrlAbs,
                distanciaAlerta = distanciaAlerta,
                estado = estado,
                distanciaSimulada = distanciaSimulada,
                isUpdating = isUpdating,
                onBack = { finish() },
                onCambiarEstado = {
                    isUpdating = true
                    // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                    // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                    CoroutineScope(Dispatchers.IO).launch {
                        // Bloque try-catch: maneja posibles excepciones en el código crítico
                        // Bloque try-catch: maneja posibles excepciones en el código crítico
                        try {
                            // Constante nuevoEstado: valor inmutable que no cambia tras su asignación
                            // Constante nuevoEstado: valor inmutable que no cambia tras su asignación
                            val nuevoEstado = if (estado == "EN_CASA") "PERDIDA" else "EN_CASA"
                            // Constante url: valor inmutable que no cambia tras su asignación
                            // Constante url: valor inmutable que no cambia tras su asignación
                            val url = URL("$backendUrl/api/mascotas/$mascotaId/estado")
                            // Constante conn: valor inmutable que no cambia tras su asignación
                            // Constante conn: valor inmutable que no cambia tras su asignación
                            val conn = url.openConnection() as HttpURLConnection
                            conn.connectTimeout = 5000
                            conn.readTimeout = 5000
                            conn.requestMethod = "PUT"
                            conn.setRequestProperty("Content-Type", "application/json")
                            conn.doOutput = true

                            // Constante json: valor inmutable que no cambia tras su asignación
                            // Constante json: valor inmutable que no cambia tras su asignación
                            val json = JSONObject().apply {
                                put("estado", nuevoEstado)
                            }

                            conn.outputStream.write(json.toString().toByteArray())
                            // Constante responseCode: valor inmutable que no cambia tras su asignación
                            // Constante responseCode: valor inmutable que no cambia tras su asignación
                            val responseCode = conn.responseCode

                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                                // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                                withContext(Dispatchers.Main) {
                                    estado = nuevoEstado
                                    toastMessage = if (nuevoEstado == "PERDIDA") {
                                        "🔴 Marcada como Perdida"
                                    } else {
                                        "🏠 Marcada como En Casa"
                                    }
                                    showToast = true

                                    // ✅ Enviar broadcast para actualizar la lista
                                    enviarBroadcastEstado(mascotaId, nuevoEstado)
                                }
                            } else {
                                // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                                // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                                withContext(Dispatchers.Main) {
                                    toastMessage = "❌ Error actualizando estado"
                                    showToast = true
                                }
                            }
                            conn.disconnect()
                        } catch (e: Exception) {
                            // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                            // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                            withContext(Dispatchers.Main) {
                                toastMessage = "❌ Error: ${e.message}"
                                showToast = true
                            }
                        }
                        // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                        // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                        withContext(Dispatchers.Main) {
                            isUpdating = false
                            delay(2000)
                            showToast = false
                        }
                    }
                }
            )

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (showToast) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 20.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .height(34.dp),
                        onClick = { },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF2E2A52)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = toastMessage,
                                color = Color.White,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * ✅ Enviar broadcast para actualizar la lista
     */
    private fun enviarBroadcastEstado(mascotaId: String, nuevoEstado: String) {
        // Constante intent: valor inmutable que no cambia tras su asignación
        // Constante intent: valor inmutable que no cambia tras su asignación
        val intent = Intent("com.lomito.seguro.wear.ESTADO_ACTUALIZADO").apply {
            putExtra("mascota_id", mascotaId)
            putExtra("nuevo_estado", nuevoEstado)
            setPackage(packageName)
        }
        sendBroadcast(intent)
        // Registro de evento en el log de Android para depuración
        // Registro de evento en el log de Android para depuración
        android.util.Log.d("DETAIL", "📢 Broadcast enviado: $mascotaId -> $nuevoEstado")
    }
}

// 🎨 Paleta temática "mascotas perdidas" (consistente con Dashboard y Lista)
// Constante BgTop: valor inmutable que no cambia tras su asignación
// Constante BgTop: valor inmutable que no cambia tras su asignación
private val BgTop = Color(0xFF1B1430)
// Constante BgBottom: valor inmutable que no cambia tras su asignación
// Constante BgBottom: valor inmutable que no cambia tras su asignación
private val BgBottom = Color(0xFF0D0B1A)
// Constante CardBg: valor inmutable que no cambia tras su asignación
// Constante CardBg: valor inmutable que no cambia tras su asignación
private val CardBg = Color(0xFF252044)
// Constante AccentRed: valor inmutable que no cambia tras su asignación
// Constante AccentRed: valor inmutable que no cambia tras su asignación
private val AccentRed = Color(0xFFE85D5D)
// Constante AccentGreen: valor inmutable que no cambia tras su asignación
// Constante AccentGreen: valor inmutable que no cambia tras su asignación
private val AccentGreen = Color(0xFF4CD97B)
// Constante AccentOrange: valor inmutable que no cambia tras su asignación
// Constante AccentOrange: valor inmutable que no cambia tras su asignación
private val AccentOrange = Color(0xFFFFA94D)

/**
 * [Pantalla de UI con el detalle de la mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [nombre]: Nombre de la mascota
 * - [especie]: Especie de la mascota
 * - [raza]: Raza de la mascota
 * - [edad]: Edad de la mascota
 * - [color]: Color de la mascota
 * - [peso]: Peso de la mascota
 * - [fotoUrl]: URL de la foto de la mascota
 * - [distanciaAlerta]: Distancia máxima permitida
 * - [estado]: Estado actual de la mascota
 * - [distanciaSimulada]: Distancia simulada o actual
 * - [isUpdating]: Estado de actualización
 * - [onBack]: Callback para volver atrás
 * - [onCambiarEstado]: Callback para alternar el estado
 */
// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MascotaDetailScreen: define la lógica de esta operación
// Función MascotaDetailScreen: define la lógica de esta operación
fun MascotaDetailScreen(
    nombre: String,
    especie: String,
    raza: String,
    edad: Int,
    color: String,
    peso: String,
    fotoUrl: String?,
    distanciaAlerta: Int,
    estado: String,
    distanciaSimulada: Int,
    isUpdating: Boolean,
    onBack: () -> Unit,
    onCambiarEstado: () -> Unit
) {
    // Constante estadoColor: valor inmutable que no cambia tras su asignación
    // Constante estadoColor: valor inmutable que no cambia tras su asignación
    val estadoColor = when (estado) {
        "PERDIDA" -> AccentRed
        "ENCONTRADA" -> AccentGreen
        else -> AccentOrange
    }

    // Constante estadoTexto: valor inmutable que no cambia tras su asignación
    // Constante estadoTexto: valor inmutable que no cambia tras su asignación
    val estadoTexto = when (estado) {
        "PERDIDA" -> "Perdida"
        "ENCONTRADA" -> "Encontrada"
        else -> "En Casa"
    }

    // Constante distanciaColor: valor inmutable que no cambia tras su asignación
    // Constante distanciaColor: valor inmutable que no cambia tras su asignación
    val distanciaColor = when {
        distanciaSimulada > distanciaAlerta -> AccentRed
        distanciaSimulada > distanciaAlerta * 0.8 -> AccentOrange
        else -> AccentGreen
    }

    // Constante btnColor: valor inmutable que no cambia tras su asignación
    // Constante btnColor: valor inmutable que no cambia tras su asignación
    val btnColor = if (estado == "EN_CASA") AccentRed else AccentGreen
    // Constante btnText: valor inmutable que no cambia tras su asignación
    // Constante btnText: valor inmutable que no cambia tras su asignación
    val btnText = if (estado == "EN_CASA") "Marcar Perdida" else "Marcar En Casa"
    // Constante btnIcon: valor inmutable que no cambia tras su asignación
    // Constante btnIcon: valor inmutable que no cambia tras su asignación
    val btnIcon = if (estado == "EN_CASA") "🆘" else "🏠"

    Scaffold(
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
        ) {
            // ✅ LazyColumn en vez de Column fijo: si el contenido no
            // cabe completo en el círculo, se puede scrollear en vez
            // de cortarse o aplastarse.
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(top = 22.dp, bottom = 22.dp)
            ) {
                // Header
                item {
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (!fotoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = fotoUrl,
                            contentDescription = nombre,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(CardBg)
                        )
                    } else {
                        Text(
                            text = if (especie == "PERRO") "🐕" else "🐈",
                            fontSize = 30.sp
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = nombre,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(6.dp))
                }

                // ✅ Tarjeta de estado destacada
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        onClick = { },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(estadoColor.copy(alpha = 0.18f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(estadoColor, CircleShape)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = estadoTexto,
                                color = estadoColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ✅ Tarjeta de distancia / alerta
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        onClick = { },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardBg)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📍 Distancia",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = "${distanciaSimulada}m",
                                    color = distanciaColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️ Alerta a",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = "${distanciaAlerta}m",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ✅ Tarjeta de datos (raza, edad, color, peso)
                // Constante datos: valor inmutable que no cambia tras su asignación
                // Constante datos: valor inmutable que no cambia tras su asignación
                val datos = buildList {
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (raza.isNotEmpty()) add("Raza" to raza)
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (edad > 0) add("Edad" to "$edad años")
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (color.isNotEmpty()) add("Color" to color)
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (peso.isNotEmpty()) add("Peso" to peso)
                }
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (datos.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            onClick = { },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardBg)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                datos.forEachIndexed { index, (label, value) ->
                                    DetailRow(label, value)
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    if (index != datos.lastIndex) Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                // ✅ Botón principal de acción
                item {
                    Button(
                        onClick = onCambiarEstado,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = btnColor),
                        enabled = !isUpdating
                    ) {
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                indicatorColor = Color.White
                            )
                        } else {
                            Text(
                                text = "$btnIcon $btnText",
                                fontSize = 11.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                // ✅ Botón de cerrar, discreto, al final (en vez de
                // flotando arriba pegado al borde curvo)
                item {
                    CompactChip(
                        onClick = onBack,
                        label = { Text("Cerrar", fontSize = 10.sp) },
                        colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                }
            }
        }
    }
}

// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función DetailRow: define la lógica de esta operación
// Función DetailRow: define la lógica de esta operación
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 9.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
```

### Paso 5.4: `MascotaListActivity.kt`

**Actividad de lista de mascotas (Wear)**. Muestra la lista de mascotas del usuario en una lista circular optimizada para Wear OS.

```kotlin
// Paquete: com.lomito.seguro.wear.ui.mascota
// Paquete: com.lomito.seguro.wear.ui.mascota
package com.lomito.seguro.wear.ui.mascota
// Importa la dependencia necesaria: BuildConfig
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.wear.BuildConfig

// Importa la dependencia necesaria: BroadcastReceiver
// Importa la dependencia necesaria: BroadcastReceiver
import android.content.BroadcastReceiver
// Importa el contexto de Android
// Importa el contexto de Android
import android.content.Context
// Importa la clase Intent para navegación entre componentes
// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa la clase Intent para navegación entre componentes
// Importa la clase Intent para navegación entre componentes
import android.content.IntentFilter
// Importa la dependencia necesaria: Build
// Importa la dependencia necesaria: Build
import android.os.Build
// Importa el contenedor de datos Bundle
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.LazyColumn
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.items
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.CircleShape
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.RoundedCornerShape
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.runtime.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.draw.clip
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Brush
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.layout.ContentScale
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import coil.compose.AsyncImage
// Importa soporte para corrutinas de Kotlin
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.*
// Importa el parser JSON
// Importa el parser JSON
import org.json.JSONArray
// Importa el parser JSON
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: HttpURLConnection
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
// Importa la dependencia necesaria: URL
import java.net.URL

/**
 * [Modelo de datos simplificado para la lista de mascotas]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Almacenar los datos básicos necesarios para mostrar en un listado]
 */
// Clase de datos MascotaItem: modelo inmutable con propiedades de dominio
// Clase de datos MascotaItem: modelo inmutable con propiedades de dominio
data class MascotaItem(
    // Constante id: valor inmutable que no cambia tras su asignación
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String,
    // Constante nombre: valor inmutable que no cambia tras su asignación
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String,
    // Constante especie: valor inmutable que no cambia tras su asignación
    // Constante especie: valor inmutable que no cambia tras su asignación
    val especie: String,
    // Constante raza: valor inmutable que no cambia tras su asignación
    // Constante raza: valor inmutable que no cambia tras su asignación
    val raza: String = "",
    // Constante edad: valor inmutable que no cambia tras su asignación
    // Constante edad: valor inmutable que no cambia tras su asignación
    val edad: Int = 0,
    // Constante color: valor inmutable que no cambia tras su asignación
    // Constante color: valor inmutable que no cambia tras su asignación
    val color: String = "",
    // Constante peso: valor inmutable que no cambia tras su asignación
    // Constante peso: valor inmutable que no cambia tras su asignación
    val peso: String = "",
    // Constante fotoUrl: valor inmutable que no cambia tras su asignación
    // Constante fotoUrl: valor inmutable que no cambia tras su asignación
    val fotoUrl: String? = null,
    // Constante distanciaAlerta: valor inmutable que no cambia tras su asignación
    // Constante distanciaAlerta: valor inmutable que no cambia tras su asignación
    val distanciaAlerta: Int = 50,
    // Constante estado: valor inmutable que no cambia tras su asignación
    // Constante estado: valor inmutable que no cambia tras su asignación
    val estado: String = "EN_CASA"
)

/**
 * [Actividad para listar todas las mascotas del usuario]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Obtener la lista de mascotas desde el backend]
 * - [Gestionar la actualización en tiempo real de su estado a través de broadcast y polling]
 */
// Activity MascotaListActivity: pantalla principal que gestiona el ciclo de vida
// Activity MascotaListActivity: pantalla principal que gestiona el ciclo de vida
class MascotaListActivity : ComponentActivity() {
    // Variable pollingJob: almacena el estado mutable de este componente
    // Variable pollingJob: almacena el estado mutable de este componente
    private var pollingJob: Job? = null
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    private val backendUrl = BuildConfig.BACKEND_URL
    // Constante distanciasSimuladas: valor inmutable que no cambia tras su asignación
    // Constante distanciasSimuladas: valor inmutable que no cambia tras su asignación
    private val distanciasSimuladas = mutableStateMapOf<String, Int>()

    // ✅ State para mascotas con actualización inmediata
    // Variable mascotasState: almacena el estado mutable de este componente
    // Variable mascotasState: almacena el estado mutable de este componente
    private var mascotasState by mutableStateOf<List<MascotaItem>>(emptyList())

    // ✅ BroadcastReceiver para actualizar el estado de una mascota
    // Constante estadoUpdateReceiver: valor inmutable que no cambia tras su asignación
    // Constante estadoUpdateReceiver: valor inmutable que no cambia tras su asignación
    private val estadoUpdateReceiver = object : BroadcastReceiver() {
        // Sobreescribe la función onReceive de la clase padre
        // Sobreescribe la función onReceive de la clase padre
        override fun onReceive(context: Context, intent: Intent) {
            // Constante mascotaId: valor inmutable que no cambia tras su asignación
            // Constante mascotaId: valor inmutable que no cambia tras su asignación
            val mascotaId = intent.getStringExtra("mascota_id") ?: return
            // Constante nuevoEstado: valor inmutable que no cambia tras su asignación
            // Constante nuevoEstado: valor inmutable que no cambia tras su asignación
            val nuevoEstado = intent.getStringExtra("nuevo_estado") ?: return

            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("MLIST", "📢 Actualizando estado de $mascotaId a $nuevoEstado")

            // ✅ Actualizar la lista inmediatamente
            mascotasState = mascotasState.map { item ->
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (item.id == mascotaId) {
                    item.copy(estado = nuevoEstado)
                } else {
                    item
                }
            }
        }
    }

    // Constante bleReceiver: valor inmutable que no cambia tras su asignación
    // Constante bleReceiver: valor inmutable que no cambia tras su asignación
    private val bleReceiver = object : BroadcastReceiver() {
        // Sobreescribe la función onReceive de la clase padre
        // Sobreescribe la función onReceive de la clase padre
        override fun onReceive(context: Context, intent: Intent) {
            // Constante mascotaId: valor inmutable que no cambia tras su asignación
            // Constante mascotaId: valor inmutable que no cambia tras su asignación
            val mascotaId = intent.getStringExtra("mascotaId") ?: return
            // Constante distancia: valor inmutable que no cambia tras su asignación
            // Constante distancia: valor inmutable que no cambia tras su asignación
            val distancia = intent.getIntExtra("distancia", 0)
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (mascotaId.isNotEmpty()) {
                distanciasSimuladas[mascotaId] = distancia
            }
        }
    }

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)

        // ✅ Registrar receiver para actualizaciones de estado
        // Constante filter: valor inmutable que no cambia tras su asignación
        // Constante filter: valor inmutable que no cambia tras su asignación
        val filter = IntentFilter("com.lomito.seguro.wear.ESTADO_ACTUALIZADO")
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(estadoUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(estadoUpdateReceiver, filter)
        }

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            // Variable isLoading: almacena el estado mutable de este componente
            // Variable isLoading: almacena el estado mutable de este componente
            var isLoading by remember { mutableStateOf(true) }
            // Variable errorMessage: almacena el estado mutable de este componente
            // Variable errorMessage: almacena el estado mutable de este componente
            var errorMessage by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                // Constante result: valor inmutable que no cambia tras su asignación
                // Constante result: valor inmutable que no cambia tras su asignación
                val result = withContext(Dispatchers.IO) { cargarMascotas() }
                mascotasState = result.mascotas
                isLoading = false
                errorMessage = result.errorMessage
            }

            MascotaListScreen(
                mascotas = mascotasState,
                distanciasSimuladas = distanciasSimuladas,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onSelect = { mascota ->
                    // Constante intent: valor inmutable que no cambia tras su asignación
                    // Constante intent: valor inmutable que no cambia tras su asignación
                    val intent = Intent(this@MascotaListActivity, MascotaDetailActivity::class.java).apply {
                        putExtra("mascota_id", mascota.id)
                        putExtra("mascota_nombre", mascota.nombre)
                        putExtra("mascota_especie", mascota.especie)
                        putExtra("mascota_raza", mascota.raza)
                        putExtra("mascota_edad", mascota.edad)
                        putExtra("mascota_color", mascota.color)
                        putExtra("mascota_peso", mascota.peso)
                        putExtra("mascota_foto", mascota.fotoUrl)
                        putExtra("mascota_distancia_alerta", mascota.distanciaAlerta)
                        putExtra("mascota_estado", mascota.estado)
                        putExtra("mascota_distancia_simulada", distanciasSimuladas[mascota.id] ?: 0)
                    }
                    startActivity(intent)
                },
                onBack = { finish() },
                onRetry = {
                    isLoading = true
                    errorMessage = ""
                    // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                    // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                    CoroutineScope(Dispatchers.Main).launch {
                        // Constante result: valor inmutable que no cambia tras su asignación
                        // Constante result: valor inmutable que no cambia tras su asignación
                        val result = withContext(Dispatchers.IO) { cargarMascotas() }
                        mascotasState = result.mascotas
                        isLoading = false
                        errorMessage = result.errorMessage
                    }
                }
            )
        }
    }

    // Método del ciclo de vida: la actividad se vuelve visible
    // Método del ciclo de vida: la actividad se vuelve visible
    override fun onStart() {
        // Invoca la implementación del método en la clase padre
        // Invoca la implementación del método en la clase padre
        super.onStart()
        // Constante filter: valor inmutable que no cambia tras su asignación
        // Constante filter: valor inmutable que no cambia tras su asignación
        val filter = IntentFilter("com.lomito.seguro.wear.BLE_UPDATE")
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bleReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(bleReceiver, filter)
        }

        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        pollingJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (isActive) {
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                // Bloque try-catch: maneja posibles excepciones en el código crítico
                try {
                    // Constante url: valor inmutable que no cambia tras su asignación
                    // Constante url: valor inmutable que no cambia tras su asignación
                    val url = URL("$backendUrl/api/simulador/estado")
                    // Constante conn: valor inmutable que no cambia tras su asignación
                    // Constante conn: valor inmutable que no cambia tras su asignación
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 2000
                    conn.readTimeout = 2000
                    conn.requestMethod = "GET"
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        // Constante response: valor inmutable que no cambia tras su asignación
                        // Constante response: valor inmutable que no cambia tras su asignación
                        val response = conn.inputStream.bufferedReader().readText()
                        // Constante json: valor inmutable que no cambia tras su asignación
                        // Constante json: valor inmutable que no cambia tras su asignación
                        val json = JSONObject(response)
                        // Constante distancia: valor inmutable que no cambia tras su asignación
                        // Constante distancia: valor inmutable que no cambia tras su asignación
                        val distancia = json.optInt("distancia", 0)
                        // Constante mascotaId: valor inmutable que no cambia tras su asignación
                        // Constante mascotaId: valor inmutable que no cambia tras su asignación
                        val mascotaId = json.optString("mascotaId", "")
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (mascotaId.isNotEmpty()) {
                            // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                            // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                            withContext(Dispatchers.Main) {
                                distanciasSimuladas[mascotaId] = distancia
                            }
                        }
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    // Registro de evento en el log de Android para depuración
                    // Registro de evento en el log de Android para depuración
                    android.util.Log.e("MLIST_POLL", "Error: ${e.message}")
                }
                delay(2000)
            }
        }
    }

    // Método del ciclo de vida: la actividad ya no es visible
    // Método del ciclo de vida: la actividad ya no es visible
    override fun onStop() {
        // Invoca la implementación del método en la clase padre
        // Invoca la implementación del método en la clase padre
        super.onStop()
        unregisterReceiver(bleReceiver)
        pollingJob?.cancel()
    }

    // Método del ciclo de vida: se limpia la actividad antes de destruirse
    // Método del ciclo de vida: se limpia la actividad antes de destruirse
    override fun onDestroy() {
        // Invoca la implementación del método en la clase padre
        // Invoca la implementación del método en la clase padre
        super.onDestroy()
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        // Bloque try-catch: maneja posibles excepciones en el código crítico
        try {
            unregisterReceiver(estadoUpdateReceiver)
        } catch (e: Exception) {
            // Receiver ya fue desregistrado
        }
    }

    private suspend fun cargarMascotas(): CargaResult {
        // Retorna el valor al llamador de la función
        // Retorna el valor al llamador de la función
        return try {
            // Constante prefs: valor inmutable que no cambia tras su asignación
            // Constante prefs: valor inmutable que no cambia tras su asignación
            val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
            // Constante userId: valor inmutable que no cambia tras su asignación
            // Constante userId: valor inmutable que no cambia tras su asignación
            val userId = prefs.getString("user_id", "2") ?: "2"
            // Constante url: valor inmutable que no cambia tras su asignación
            // Constante url: valor inmutable que no cambia tras su asignación
            val url = URL("$backendUrl/api/mascotas?ownerId=$userId")
            // Constante conn: valor inmutable que no cambia tras su asignación
            // Constante conn: valor inmutable que no cambia tras su asignación
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            val responseCode = conn.responseCode
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Constante response: valor inmutable que no cambia tras su asignación
                // Constante response: valor inmutable que no cambia tras su asignación
                val response = conn.inputStream.bufferedReader().readText()
                // Constante jsonArray: valor inmutable que no cambia tras su asignación
                // Constante jsonArray: valor inmutable que no cambia tras su asignación
                val jsonArray = JSONArray(response)
                // Constante lista: valor inmutable que no cambia tras su asignación
                // Constante lista: valor inmutable que no cambia tras su asignación
                val lista = mutableListOf<MascotaItem>()
                // Itera sobre la colección para procesar cada elemento
                // Itera sobre la colección para procesar cada elemento
                for (i in 0 until jsonArray.length()) {
                    // Constante obj: valor inmutable que no cambia tras su asignación
                    // Constante obj: valor inmutable que no cambia tras su asignación
                    val obj = jsonArray.getJSONObject(i)
                    // Constante fotoRelativa: valor inmutable que no cambia tras su asignación
                    // Constante fotoRelativa: valor inmutable que no cambia tras su asignación
                    val fotoRelativa = obj.optString("foto_url", null)
                    // Constante fotoAbsoluta: valor inmutable que no cambia tras su asignación
                    // Constante fotoAbsoluta: valor inmutable que no cambia tras su asignación
                    val fotoAbsoluta = fotoRelativa?.takeIf { it.isNotEmpty() }?.let {
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (it.startsWith("http")) it else "$backendUrl$it"
                    }
                    lista.add(
                        MascotaItem(
                            id = obj.getString("id"),
                            nombre = obj.getString("nombre"),
                            especie = obj.getString("especie"),
                            raza = obj.optString("raza", ""),
                            edad = obj.optInt("edad", 0),
                            color = obj.optString("color", ""),
                            peso = obj.optString("peso", ""),
                            fotoUrl = fotoAbsoluta,
                            distanciaAlerta = obj.getInt("distancia_alerta"),
                            estado = obj.getString("estado")
                        )
                    )
                }
                conn.disconnect()
                CargaResult(lista, if (lista.isEmpty()) "No hay mascotas" else "")
            } else {
                conn.disconnect()
                CargaResult(emptyList(), "Error HTTP $responseCode")
            }
        } catch (e: Exception) {
            CargaResult(emptyList(), e.message ?: "Error desconocido")
        }
    }

    // Clase de datos CargaResult: modelo inmutable con propiedades de dominio
    // Clase de datos CargaResult: modelo inmutable con propiedades de dominio
    data class CargaResult(val mascotas: List<MascotaItem>, val errorMessage: String)
}

// 🎨 Paleta temática "mascotas perdidas" (misma del Dashboard)
// Constante BgTop: valor inmutable que no cambia tras su asignación
// Constante BgTop: valor inmutable que no cambia tras su asignación
private val BgTop = Color(0xFF1B1430)
// Constante BgBottom: valor inmutable que no cambia tras su asignación
// Constante BgBottom: valor inmutable que no cambia tras su asignación
private val BgBottom = Color(0xFF0D0B1A)
// Constante CardBg: valor inmutable que no cambia tras su asignación
// Constante CardBg: valor inmutable que no cambia tras su asignación
private val CardBg = Color(0xFF252044)
// Constante AccentRed: valor inmutable que no cambia tras su asignación
// Constante AccentRed: valor inmutable que no cambia tras su asignación
private val AccentRed = Color(0xFFE85D5D)
// Constante AccentGreen: valor inmutable que no cambia tras su asignación
// Constante AccentGreen: valor inmutable que no cambia tras su asignación
private val AccentGreen = Color(0xFF4CD97B)
// Constante AccentOrange: valor inmutable que no cambia tras su asignación
// Constante AccentOrange: valor inmutable que no cambia tras su asignación
private val AccentOrange = Color(0xFFFFA94D)
// Constante AccentBlue: valor inmutable que no cambia tras su asignación
// Constante AccentBlue: valor inmutable que no cambia tras su asignación
private val AccentBlue = Color(0xFF4D9FFF)

/**
 * [Pantalla con la lista de mascotas del usuario]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [mascotas]: Lista de las mascotas
 * - [distanciasSimuladas]: Mapa de distancias actuales por ID de mascota
 * - [isLoading]: Estado de carga de la lista
 * - [errorMessage]: Mensaje de error general
 * - [onSelect]: Callback al seleccionar una mascota
 * - [onBack]: Callback para regresar
 * - [onRetry]: Callback para reintentar la conexión
 */
// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MascotaListScreen: define la lógica de esta operación
// Función MascotaListScreen: define la lógica de esta operación
fun MascotaListScreen(
    mascotas: List<MascotaItem>,
    distanciasSimuladas: Map<String, Int>,
    isLoading: Boolean,
    errorMessage: String,
    onSelect: (MascotaItem) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                // ✅ Header centrado, con botón de cerrar pequeño y discreto
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🐾", fontSize = 13.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Mis Mascotas",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                Box(modifier = Modifier.align(Alignment.End)) {
                    // espacio reservado, el botón real va abajo flotando
                }

                // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
                // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
                when {
                    isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(26.dp),
                                    strokeWidth = 2.dp,
                                    indicatorColor = AccentGreen
                                )
                                Spacer(Modifier.height(6.dp))
                                Text("Cargando...", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                            }
                        }
                    }
                    mascotas.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🐾", fontSize = 22.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("No hay mascotas", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                if (errorMessage.isNotEmpty()) {
                                    Text(
                                        errorMessage,
                                        color = AccentOrange,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                CompactButton(
                                    onClick = onRetry,
                                    colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
                                    modifier = Modifier.size(width = 84.dp, height = 32.dp)
                                ) {
                                    Text("Reintentar", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(0.94f).align(Alignment.CenterHorizontally)
                        ) {
                            items(mascotas) { mascota ->
                                MascotaItemCard(
                                    mascota = mascota,
                                    distanciaSimulada = distanciasSimuladas[mascota.id],
                                    onClick = { onSelect(mascota) }
                                )
                            }
                            item {
                                Spacer(Modifier.height(4.dp))
                                CompactChip(
                                    onClick = onBack,
                                    label = { Text("Cerrar", fontSize = 10.sp) },
                                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MascotaItemCard: define la lógica de esta operación
// Función MascotaItemCard: define la lógica de esta operación
fun MascotaItemCard(
    mascota: MascotaItem,
    distanciaSimulada: Int?,
    onClick: () -> Unit
) {
    // Constante estadoColor: valor inmutable que no cambia tras su asignación
    // Constante estadoColor: valor inmutable que no cambia tras su asignación
    val estadoColor = when (mascota.estado) {
        "PERDIDA" -> AccentRed
        "ENCONTRADA" -> AccentGreen
        else -> AccentOrange
    }

    // Constante estadoTexto: valor inmutable que no cambia tras su asignación
    // Constante estadoTexto: valor inmutable que no cambia tras su asignación
    val estadoTexto = when (mascota.estado) {
        "PERDIDA" -> "Perdida"
        "ENCONTRADA" -> "Encontrada"
        else -> "En Casa"
    }

    // Constante distanciaColor: valor inmutable que no cambia tras su asignación
    // Constante distanciaColor: valor inmutable que no cambia tras su asignación
    val distanciaColor = when {
        distanciaSimulada == null -> Color.White.copy(alpha = 0.4f)
        distanciaSimulada > mascota.distanciaAlerta -> AccentRed
        distanciaSimulada > mascota.distanciaAlerta * 0.8 -> AccentOrange
        else -> AccentGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(CardBg, CardBg.copy(alpha = 0.7f))
                    )
                )
        ) {
            // ✅ Barra lateral de color según estado (más legible que solo un punto)
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(estadoColor)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (!mascota.fotoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = mascota.fotoUrl,
                                contentDescription = mascota.nombre,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = if (mascota.especie == "PERRO") "🐕" else "🐈",
                                fontSize = 18.sp
                            )
                        }
                        Column {
                            Text(
                                text = mascota.nombre,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (distanciaSimulada != null) "📍${distanciaSimulada}m" else "📍--",
                                    color = distanciaColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "/ ${mascota.distanciaAlerta}m",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 9.sp
                                )
                            }
                            Text(
                                text = estadoTexto,
                                color = estadoColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(estadoColor, CircleShape)
                    )
                }
            }
        }
    }
}
```

## FASE 6: `com/lomito/seguro/wear/ui/report`

### Paso 6.1: `AgregarMascotaPerdidaActivity.kt`

**Actividad de agregar mascota perdida (Wear)**. Permite reportar una nueva mascota perdida desde el smartwatch.

```kotlin
// Paquete: com.lomito.seguro.wear.ui.report
// Paquete: com.lomito.seguro.wear.ui.report
package com.lomito.seguro.wear.ui.report
// Importa la dependencia necesaria: BuildConfig
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.wear.BuildConfig

// Importa la dependencia necesaria: RemoteInput
// Importa la dependencia necesaria: RemoteInput
import android.app.RemoteInput
// Importa el contenedor de datos Bundle
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.activity.compose.rememberLauncherForActivityResult
// Importa la dependencia necesaria: ActivityResultContracts
// Importa la dependencia necesaria: ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.CircleShape
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.RoundedCornerShape
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.runtime.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.draw.clip
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.style.TextAlign
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa la clase Intent para navegación entre componentes
// Importa la clase Intent para navegación entre componentes
import androidx.wear.input.RemoteInputIntentHelper
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa soporte para corrutinas de Kotlin
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.*
// Importa el parser JSON
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: HttpURLConnection
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
// Importa la dependencia necesaria: URL
import java.net.URL

// Constante KEY_TEXTO: valor fijo definido en tiempo de compilación
// Constante KEY_TEXTO: valor fijo definido en tiempo de compilación
private const val KEY_TEXTO = "key_texto_input"

// Declaración de la clase Paso
// Declaración de la clase Paso
enum class Paso { NOMBRE, ESPECIE, RAZA, COLOR, TELEFONO, CONFIRMAR }

/**
 * [Actividad para registrar una mascota que ha sido encontrada o perdida por otra persona]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Recopilar los datos de una mascota ajena mediante pasos (wizard)]
 * - [Publicar el registro de la mascota como perdida en el backend]
 */
// Activity AgregarMascotaPerdidaActivity: pantalla principal que gestiona el ciclo de vida
// Activity AgregarMascotaPerdidaActivity: pantalla principal que gestiona el ciclo de vida
class AgregarMascotaPerdidaActivity : ComponentActivity() {
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    private val backendUrl = BuildConfig.BACKEND_URL

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            // Variable paso: almacena el estado mutable de este componente
            // Variable paso: almacena el estado mutable de este componente
            var paso by remember { mutableStateOf(Paso.NOMBRE) }
            // Variable nombre: almacena el estado mutable de este componente
            // Variable nombre: almacena el estado mutable de este componente
            var nombre by remember { mutableStateOf("") }
            // Variable especie: almacena el estado mutable de este componente
            // Variable especie: almacena el estado mutable de este componente
            var especie by remember { mutableStateOf("PERRO") }
            // Variable raza: almacena el estado mutable de este componente
            // Variable raza: almacena el estado mutable de este componente
            var raza by remember { mutableStateOf("") }
            // Variable color: almacena el estado mutable de este componente
            // Variable color: almacena el estado mutable de este componente
            var color by remember { mutableStateOf("") }
            // Variable telefono: almacena el estado mutable de este componente
            // Variable telefono: almacena el estado mutable de este componente
            var telefono by remember { mutableStateOf("") }
            // Variable isSending: almacena el estado mutable de este componente
            // Variable isSending: almacena el estado mutable de este componente
            var isSending by remember { mutableStateOf(false) }
            // Variable isSuccess: almacena el estado mutable de este componente
            // Variable isSuccess: almacena el estado mutable de este componente
            var isSuccess by remember { mutableStateOf(false) }
            // Variable errorMessage: almacena el estado mutable de este componente
            // Variable errorMessage: almacena el estado mutable de este componente
            var errorMessage by remember { mutableStateOf("") }
            // Variable campoActivo: almacena el estado mutable de este componente
            // Variable campoActivo: almacena el estado mutable de este componente
            var campoActivo by remember { mutableStateOf("") }

            // Constante inputLauncher: valor inmutable que no cambia tras su asignación
            // Constante inputLauncher: valor inmutable que no cambia tras su asignación
            val inputLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                // Constante texto: valor inmutable que no cambia tras su asignación
                // Constante texto: valor inmutable que no cambia tras su asignación
                val texto = RemoteInput.getResultsFromIntent(result.data)
                    ?.getCharSequence(KEY_TEXTO)
                    ?.toString()
                    ?.trim()
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (!texto.isNullOrEmpty()) {
                    // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
                    // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
                    when (campoActivo) {
                        "nombre" -> { nombre = texto; paso = Paso.ESPECIE }
                        "raza" -> { raza = texto; paso = Paso.COLOR }
                        "color" -> { color = texto; paso = Paso.TELEFONO }
                        "telefono" -> { telefono = texto.filter { it.isDigit() }; paso = Paso.CONFIRMAR }
                    }
                }
            }

            // Función pedirInput: define la lógica de esta operación
            // Función pedirInput: define la lógica de esta operación
            fun pedirInput(campo: String, label: String) {
                campoActivo = campo
                // Constante intent: valor inmutable que no cambia tras su asignación
                // Constante intent: valor inmutable que no cambia tras su asignación
                val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                // Constante remoteInputs: valor inmutable que no cambia tras su asignación
                // Constante remoteInputs: valor inmutable que no cambia tras su asignación
                val remoteInputs = listOf(RemoteInput.Builder(KEY_TEXTO).setLabel(label).build())
                RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                inputLauncher.launch(intent)
            }

            // ✅ Pantalla principal
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D0B1A)),
                contentAlignment = Alignment.Center
            ) {
                // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
                // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
                when {
                    isSuccess -> PantallaExito { finish() }
                    else -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // ✅ Indicador de pasos (puntos)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            // Itera sobre cada elemento de la colección y ejecuta el bloque
                            // Itera sobre cada elemento de la colección y ejecuta el bloque
                            Paso.entries.forEach { p ->
                                Box(
                                    modifier = Modifier
                                        .size(if (p == paso) 8.dp else 5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                            if (p.ordinal <= paso.ordinal)
                                                Color(0xFFE85D5D)
                                            else
                                                Color.White.copy(alpha = 0.15f)
                                        )
                                )
                            }
                        }

                        // ✅ Contenido del paso
                        // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
                        // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
                        when (paso) {
                            Paso.NOMBRE -> PasoNombre(
                                nombre = nombre,
                                onPedir = { pedirInput("nombre", "Nombre") },
                                onCancel = { finish() }
                            )
                            Paso.ESPECIE -> PasoEspecie(
                                especie = especie,
                                onSelect = {
                                    especie = it
                                    paso = Paso.RAZA
                                },
                                onAtras = { paso = Paso.NOMBRE }
                            )
                            Paso.RAZA -> PasoInput(
                                titulo = "Raza",
                                emoji = "🐾",
                                valor = raza,
                                esOpcional = true,
                                onPedir = { pedirInput("raza", "Raza") },
                                onSiguiente = { paso = Paso.COLOR },
                                onAtras = { paso = Paso.ESPECIE }
                            )
                            Paso.COLOR -> PasoInput(
                                titulo = "Color",
                                emoji = "🎨",
                                valor = color,
                                esOpcional = true,
                                onPedir = { pedirInput("color", "Color") },
                                onSiguiente = { paso = Paso.TELEFONO },
                                onAtras = { paso = Paso.RAZA }
                            )
                            Paso.TELEFONO -> PasoTelefono(
                                telefono = telefono,
                                onPedir = { pedirInput("telefono", "Teléfono") },
                                onAtras = { paso = Paso.COLOR }
                            )
                            Paso.CONFIRMAR -> PasoConfirmar(
                                nombre = nombre,
                                especie = especie,
                                raza = raza,
                                color = color,
                                telefono = telefono,
                                isSending = isSending,
                                errorMessage = errorMessage,
                                onGuardar = {
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    if (!isSending) {
                                        isSending = true
                                        errorMessage = ""
                                        crearMascotaPerdida(
                                            nombre = nombre,
                                            especie = especie,
                                            raza = raza,
                                            color = color,
                                            telefono = telefono,
                                            onSuccess = {
                                                isSending = false
                                                isSuccess = true
                                                notificarNuevaMascotaPerdida(nombre)
                                            },
                                            onError = {
                                                isSending = false
                                                errorMessage = it
                                            }
                                        )
                                    }
                                },
                                onAtras = { paso = Paso.TELEFONO }
                            )
                        }
                    }
                }
            }
        }
    }

    // ✅ Función para crear mascota perdida
    private fun crearMascotaPerdida(
        nombre: String,
        especie: String,
        raza: String,
        color: String,
        telefono: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        CoroutineScope(Dispatchers.IO).launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante prefs: valor inmutable que no cambia tras su asignación
                // Constante prefs: valor inmutable que no cambia tras su asignación
                val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
                // Constante userId: valor inmutable que no cambia tras su asignación
                // Constante userId: valor inmutable que no cambia tras su asignación
                val userId = prefs.getString("user_id", "2") ?: "2"

                // Constante url: valor inmutable que no cambia tras su asignación
                // Constante url: valor inmutable que no cambia tras su asignación
                val url = URL("$backendUrl/api/mascotas")
                // Constante conn: valor inmutable que no cambia tras su asignación
                // Constante conn: valor inmutable que no cambia tras su asignación
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                // Constante json: valor inmutable que no cambia tras su asignación
                // Constante json: valor inmutable que no cambia tras su asignación
                val json = JSONObject().apply {
                    put("nombre", nombre)
                    put("especie", especie)
                    put("raza", raza)
                    put("color", color)
                    put("telefono", telefono)
                    put("estado", "PERDIDA")
                    put("owner_id", userId.toInt())
                }

                conn.outputStream.write(json.toString().toByteArray())
                // Constante responseCode: valor inmutable que no cambia tras su asignación
                // Constante responseCode: valor inmutable que no cambia tras su asignación
                val responseCode = conn.responseCode
                conn.disconnect()

                // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                withContext(Dispatchers.Main) {
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (responseCode == 200 || responseCode == 201) {
                        onSuccess()
                    } else {
                        onError("Error (HTTP $responseCode)")
                    }
                }
            } catch (e: Exception) {
                // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                // Cambia el contexto de ejecución de la corrutina (ej. a IO para operaciones de red)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Error desconocido")
                }
            }
        }
    }

    // ✅ Función para notificar al móvil
    private fun notificarNuevaMascotaPerdida(nombre: String) {
        // Constante context: valor inmutable que no cambia tras su asignación
        // Constante context: valor inmutable que no cambia tras su asignación
        val context = applicationContext
        // Constante payload: valor inmutable que no cambia tras su asignación
        // Constante payload: valor inmutable que no cambia tras su asignación
        val payload = JSONObject().apply {
            put("tipo", "NUEVA_MASCOTA_PERDIDA")
            put("nombre", nombre)
        }.toString().toByteArray()

        // Usa la API de Wearable para comunicación con dispositivos Wear OS
        // Usa la API de Wearable para comunicación con dispositivos Wear OS
        com.google.android.gms.wearable.Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                nodes.forEach { node ->
                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    com.google.android.gms.wearable.Wearable.getMessageClient(context)
                        // Envía un mensaje al dispositivo Wear OS conectado
                        // Envía un mensaje al dispositivo Wear OS conectado
                        .sendMessage(node.id, "/mascota/perdida/nueva", payload)
                        .addOnSuccessListener {
                            // Registro de evento en el log de Android para depuración
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.d("MASCOTA_PERDIDA", "✅ Notificación enviada al móvil")
                        }
                        .addOnFailureListener { e ->
                            // Registro de evento en el log de Android para depuración
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.e("MASCOTA_PERDIDA", "❌ Error enviando: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                // Registro de evento en el log de Android para depuración
                // Registro de evento en el log de Android para depuración
                android.util.Log.e("MASCOTA_PERDIDA", "❌ Error obteniendo nodos: ${e.message}")
            }
    }
}

// ==================== COMPONENTES UI ====================

// ✅ Paso 1: Nombre
// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
private fun PasoNombre(
    nombre: String,
    onPedir: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("📍", fontSize = 28.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Nombre de la mascota",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
        Spacer(Modifier.height(10.dp))

        Card(
            onClick = onPedir,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C2657))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (nombre.isNotEmpty()) nombre else "Toca para dictar",
                    color = if (nombre.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = if (nombre.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        CompactChip(
            onClick = onCancel,
            label = { Text("Cancelar", fontSize = 9.sp, color = Color.White) },
            colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
            modifier = Modifier.fillMaxWidth(0.5f)
        )
    }
}

// ✅ Paso 2: Especie
// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
private fun PasoEspecie(
    especie: String,
    onSelect: (String) -> Unit,
    onAtras: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "¿Perro o gato?",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotonEspecie(
                emoji = "🐕",
                label = "Perro",
                selected = especie == "PERRO",
                onClick = { onSelect("PERRO") }
            )
            BotonEspecie(
                emoji = "🐈",
                label = "Gato",
                selected = especie == "GATO",
                onClick = { onSelect("GATO") }
            )
        }

        Spacer(Modifier.height(12.dp))

        CompactChip(
            onClick = onAtras,
            label = { Text("Atrás", fontSize = 9.sp, color = Color.White) },
            colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
            modifier = Modifier.fillMaxWidth(0.4f)
        )
    }
}

// ✅ Paso 3/4: Input opcional
// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
private fun PasoInput(
    titulo: String,
    emoji: String,
    valor: String,
    esOpcional: Boolean,
    onPedir: () -> Unit,
    onSiguiente: () -> Unit,
    onAtras: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(emoji, fontSize = 28.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (esOpcional) "$titulo (opcional)" else titulo,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
        Spacer(Modifier.height(10.dp))

        Card(
            onClick = onPedir,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C2657))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (valor.isNotEmpty()) valor else "Toca para dictar",
                    color = if (valor.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = if (valor.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactChip(
                onClick = onAtras,
                label = { Text("Atrás", fontSize = 9.sp, color = Color.White) },
                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
                modifier = Modifier.width(70.dp)
            )
            CompactChip(
                onClick = onSiguiente,
                label = {
                    Text(
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (valor.isEmpty()) "Omitir" else "Siguiente",
                        fontSize = 9.sp,
                        color = Color.White
                    )
                },
                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF4D9FFF)),
                modifier = Modifier.width(85.dp)
            )
        }
    }
}

// ✅ Paso 5: Teléfono
// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
private fun PasoTelefono(
    telefono: String,
    onPedir: () -> Unit,
    onAtras: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("📞", fontSize = 28.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tu número de teléfono",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
        Spacer(Modifier.height(10.dp))

        Card(
            onClick = onPedir,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C2657))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (telefono.isNotEmpty()) telefono else "Ej: 123456789",
                    color = if (telefono.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = if (telefono.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        CompactChip(
            onClick = onAtras,
            label = { Text("Atrás", fontSize = 9.sp, color = Color.White) },
            colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
            modifier = Modifier.fillMaxWidth(0.4f)
        )
    }
}

// ✅ Paso final: Confirmar
// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
private fun PasoConfirmar(
    nombre: String,
    especie: String,
    raza: String,
    color: String,
    telefono: String,
    isSending: Boolean,
    errorMessage: String,
    onGuardar: () -> Unit,
    onAtras: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(if (especie == "PERRO") "🐕" else "🐈", fontSize = 30.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            text = nombre,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))

        // Constante detalles: valor inmutable que no cambia tras su asignación
        // Constante detalles: valor inmutable que no cambia tras su asignación
        val detalles = listOfNotNull(
            raza.ifEmpty { null },
            color.ifEmpty { null },
            telefono.ifEmpty { null }
        )
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (detalles.isNotEmpty()) {
            Text(
                text = detalles.joinToString(" • "),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 8.sp,
                maxLines = 1
            )
        }

        Spacer(Modifier.height(10.dp))

        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (errorMessage.isNotEmpty()) {
            Text(
                text = "⚠️ $errorMessage",
                color = Color(0xFFFFA94D),
                fontSize = 8.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Constante puedeGuardar: valor inmutable que no cambia tras su asignación
        // Constante puedeGuardar: valor inmutable que no cambia tras su asignación
        val puedeGuardar = nombre.isNotBlank() && telefono.isNotBlank() && !isSending

        Button(
            onClick = { if (puedeGuardar) onGuardar() },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(38.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (puedeGuardar) Color(0xFFE85D5D) else Color(0xFF555555)
            ),
            enabled = puedeGuardar
        ) {
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    indicatorColor = Color.White
                )
            } else {
                Text("Publicar", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(6.dp))

        CompactChip(
            onClick = onAtras,
            label = { Text("Atrás", fontSize = 9.sp, color = Color.White) },
            colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
            modifier = Modifier.fillMaxWidth(0.4f)
        )
    }
}

// ✅ Botón de especie
// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
private fun BotonEspecie(
    emoji: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (selected) Color(0xFFE85D5D).copy(alpha = 0.3f)
                    else Color(0xFF2C2657)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(emoji, fontSize = 26.sp)
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = if (selected) Color(0xFFE85D5D) else Color.White.copy(alpha = 0.7f),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ✅ Pantalla de éxito
// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
private fun PantallaExito(onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("✅", fontSize = 40.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "¡Reportada!",
            color = Color(0xFF4CD97B),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Se publicará en el mural",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 9.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        CompactChip(
            onClick = onBack,
            label = { Text("Volver", fontSize = 10.sp, color = Color.White) },
            colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF4D9FFF)),
            modifier = Modifier.fillMaxWidth(0.5f)
        )
    }
}
```

### Paso 6.2: `ReportActivity.kt`

**Actividad de reportes (Wear)**. Menú de reportes disponibles en el smartwatch: avistamiento, mascota perdida, etc.

```kotlin
// wear/ui/report/ReportActivity.kt
// Paquete: com.lomito.seguro.wear.ui.report
// Paquete: com.lomito.seguro.wear.ui.report
package com.lomito.seguro.wear.ui.report

// Importa la dependencia necesaria: BuildConfig
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.wear.BuildConfig
// Importa la dependencia necesaria: Manifest
// Importa la dependencia necesaria: Manifest
import android.Manifest
// Importa la dependencia necesaria: PackageManager
// Importa la dependencia necesaria: PackageManager
import android.content.pm.PackageManager
// Importa la dependencia necesaria: Location
// Importa la dependencia necesaria: Location
import android.location.Location
// Importa el contenedor de datos Bundle
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa la dependencia necesaria: viewModels
// Importa la dependencia necesaria: viewModels
import androidx.activity.viewModels
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.runtime.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.runtime.livedata.observeAsState
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.style.TextAlign
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa la dependencia necesaria: ActivityCompat
// Importa la dependencia necesaria: ActivityCompat
import androidx.core.app.ActivityCompat
// Importa la clase base ViewModel del ciclo de vida
// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.AndroidViewModel
// Importa el observable de datos reactivos
// Importa el observable de datos reactivos
import androidx.lifecycle.LiveData
// Importa el observable de datos reactivos
// Importa el observable de datos reactivos
import androidx.lifecycle.MutableLiveData
// Importa la dependencia necesaria: viewModelScope
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa la dependencia necesaria: LocationServices
// Importa la dependencia necesaria: LocationServices
import com.google.android.gms.location.LocationServices
// Importa la API de comunicación con Wear OS
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.Wearable
// Importa soporte para corrutinas de Kotlin
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch
// Importa soporte para corrutinas de Kotlin
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.tasks.await
// Importa el parser JSON
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: URL
// Importa la dependencia necesaria: URL
import java.net.URL

// --- COLORES COHERENTES CON EL MURAL ---
// Constante ThemeBg: valor inmutable que no cambia tras su asignación
// Constante ThemeBg: valor inmutable que no cambia tras su asignación
private val ThemeBg = Color(0xFF1A1A2E)
// Constante CardBg: valor inmutable que no cambia tras su asignación
// Constante CardBg: valor inmutable que no cambia tras su asignación
private val CardBg = Color(0xFF2C2C3E)
// Constante AccentGreen: valor inmutable que no cambia tras su asignación
// Constante AccentGreen: valor inmutable que no cambia tras su asignación
private val AccentGreen = Color(0xFF4CAF50)
// Constante AccentBlue: valor inmutable que no cambia tras su asignación
// Constante AccentBlue: valor inmutable que no cambia tras su asignación
private val AccentBlue = Color(0xFF2196F3)

/**
 * [ViewModel para manejar la lógica de reporte]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Obtener la ubicación del dispositivo]
 * - [Enviar el reporte de avistamiento al backend y al móvil conectado]
 */
// ViewModel ReportViewModel: gestiona el estado y la lógica de negocio de la pantalla
// ViewModel ReportViewModel: gestiona el estado y la lógica de negocio de la pantalla
class ReportViewModel(app: android.app.Application) : AndroidViewModel(app) {
    // Constante _estado: valor inmutable que no cambia tras su asignación
    // Constante _estado: valor inmutable que no cambia tras su asignación
    private val _estado = MutableLiveData<String>("¿Viste a esta mascota?")
    // Constante estado: valor inmutable que no cambia tras su asignación
    // Constante estado: valor inmutable que no cambia tras su asignación
    val estado: LiveData<String> = _estado
    // Constante _enviado: valor inmutable que no cambia tras su asignación
    // Constante _enviado: valor inmutable que no cambia tras su asignación
    private val _enviado = MutableLiveData(false)
    // Constante enviado: valor inmutable que no cambia tras su asignación
    // Constante enviado: valor inmutable que no cambia tras su asignación
    val enviado: LiveData<Boolean> = _enviado

    // Función reportarVista: define la lógica de esta operación
    // Función reportarVista: define la lógica de esta operación
    fun reportarVista(mascotaId: String, mascotaNombre: String) {
        _estado.value = "Obteniendo ubicación..."
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante fusedClient: valor inmutable que no cambia tras su asignación
                // Constante fusedClient: valor inmutable que no cambia tras su asignación
                val fusedClient = LocationServices.getFusedLocationProviderClient(getApplication())
                // Constante location: valor inmutable que no cambia tras su asignación
                // Constante location: valor inmutable que no cambia tras su asignación
                val location: Location? = if (
                    ActivityCompat.checkSelfPermission(
                        getApplication(), Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedClient.lastLocation.await()
                } else null

                // Constante lat: valor inmutable que no cambia tras su asignación
                // Constante lat: valor inmutable que no cambia tras su asignación
                val lat = location?.latitude ?: 0.0
                // Constante lng: valor inmutable que no cambia tras su asignación
                // Constante lng: valor inmutable que no cambia tras su asignación
                val lng = location?.longitude ?: 0.0

                _estado.value = "Enviando reporte..."

                // Constante payload: valor inmutable que no cambia tras su asignación
                // Constante payload: valor inmutable que no cambia tras su asignación
                val payload = JSONObject().apply {
                    put("mascotaId", mascotaId)
                    put("latitud", lat)
                    put("longitud", lng)
                    put("accion", "reportar_vista")
                    put("mascotaNombre", mascotaNombre)
                }.toString().toByteArray()

                // Constante nodes: valor inmutable que no cambia tras su asignación
                // Constante nodes: valor inmutable que no cambia tras su asignación
                val nodes = Wearable.getNodeClient(getApplication()).connectedNodes.await()
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                nodes.forEach { node ->
                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    Wearable.getMessageClient(getApplication())
                        // Envía un mensaje al dispositivo Wear OS conectado
                        // Envía un mensaje al dispositivo Wear OS conectado
                        .sendMessage(node.id, "/watch/reporte", payload).await()
                }

                // Constante url: valor inmutable que no cambia tras su asignación
                // Constante url: valor inmutable que no cambia tras su asignación
                val url = URL("${BuildConfig.BACKEND_URL}/api/reportes")
                // Constante conn: valor inmutable que no cambia tras su asignación
                // Constante conn: valor inmutable que no cambia tras su asignación
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                // Constante json: valor inmutable que no cambia tras su asignación
                // Constante json: valor inmutable que no cambia tras su asignación
                val json = JSONObject().apply {
                    put("mascotaId", mascotaId)
                    put("latitud", lat)
                    put("longitud", lng)
                    put("reportadoPorId", "usuario_watch")
                }

                conn.outputStream.write(json.toString().toByteArray())
                // Constante responseCode: valor inmutable que no cambia tras su asignación
                // Constante responseCode: valor inmutable que no cambia tras su asignación
                val responseCode = conn.responseCode
                conn.disconnect()

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (responseCode == 200 || responseCode == 201) {
                    _estado.value = "✅ ¡Enviado!"
                    _enviado.value = true
                } else {
                    _estado.value = "⚠️ Error HTTP $responseCode"
                }
            } catch (e: Exception) {
                _estado.value = "❌ Error de conexión"
            }
        }
    }
}

/**
 * [Actividad para reportar que se ha visto una mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Iniciar la pantalla de reporte con el ID y nombre de la mascota]
 */
// Activity ReportActivity: pantalla principal que gestiona el ciclo de vida
// Activity ReportActivity: pantalla principal que gestiona el ciclo de vida
class ReportActivity : ComponentActivity() {
    // Constante reportVM: valor inmutable que no cambia tras su asignación
    // Constante reportVM: valor inmutable que no cambia tras su asignación
    private val reportVM: ReportViewModel by viewModels()

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)
        // Constante mascotaId: valor inmutable que no cambia tras su asignación
        // Constante mascotaId: valor inmutable que no cambia tras su asignación
        val mascotaId = intent.getStringExtra("mascotaId") ?: ""
        // Constante mascotaNombre: valor inmutable que no cambia tras su asignación
        // Constante mascotaNombre: valor inmutable que no cambia tras su asignación
        val mascotaNombre = intent.getStringExtra("mascotaNombre") ?: "Mascota"

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            // Constante estado: valor inmutable que no cambia tras su asignación
            // Constante estado: valor inmutable que no cambia tras su asignación
            val estado by reportVM.estado.observeAsState("Cargando...")
            // Constante enviado: valor inmutable que no cambia tras su asignación
            // Constante enviado: valor inmutable que no cambia tras su asignación
            val enviado by reportVM.enviado.observeAsState(false)

            ReportScreen(
                mascotaNombre = mascotaNombre,
                estado = estado,
                enviado = enviado,
                onReportar = { reportVM.reportarVista(mascotaId, mascotaNombre) },
                onDismiss = { finish() }
            )
        }
    }
}

/**
 * [Pantalla de confirmación para reportar avistamiento]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [mascotaNombre]: Nombre de la mascota vista
 * - [estado]: Estado actual del proceso de reporte
 * - [enviado]: Indica si el reporte fue enviado con éxito
 * - [onReportar]: Acción para ejecutar el envío del reporte
 * - [onDismiss]: Acción para cerrar o cancelar
 */
// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función ReportScreen: define la lógica de esta operación
// Función ReportScreen: define la lógica de esta operación
fun ReportScreen(
    mascotaNombre: String,
    estado: String,
    enviado: Boolean,
    onReportar: () -> Unit,
    onDismiss: () -> Unit
) {
    // Constante listState: valor inmutable que no cambia tras su asignación
    // Constante listState: valor inmutable que no cambia tras su asignación
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeBg),
            contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            autoCentering = AutoCenteringParams(itemIndex = 1) // Centra el nombre de la mascota
        ) {
            // Icono y Título
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (enviado) "🎉" else "📍",
                        fontSize = 28.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (enviado) "¡Éxito!" else "Nuevo Reporte",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Cuerpo del mensaje
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = mascotaNombre,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (enviado) AccentGreen else AccentBlue,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = estado,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Acciones o Progreso
            item {
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (!enviado) {
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (estado.contains("Enviando") || estado.contains("Obteniendo")) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp).padding(4.dp),
                            strokeWidth = 3.dp,
                            indicatorColor = AccentBlue
                        )
                    } else {
                        Button(
                            onClick = onReportar,
                            colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Text("Reportar Avistamiento", fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // Botón Salir/Cancelar
            item {
                Spacer(Modifier.height(8.dp))
                CompactButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (enviado) AccentGreen else Color(0xFF424242)
                    ),
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text(
                        text = if (enviado) "Terminar" else "Cancelar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
```

### Paso 6.3: `ReportarAvistamientoActivity.kt`

**Actividad de reportar avistamiento (Wear)**. Permite reportar que se ha visto una mascota perdida, enviando la ubicación actual.

```kotlin
// Paquete: com.lomito.seguro.wear.ui.report
// Paquete: com.lomito.seguro.wear.ui.report
package com.lomito.seguro.wear.ui.report
// Importa la dependencia necesaria: BuildConfig
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.wear.BuildConfig

// Importa la dependencia necesaria: Manifest
// Importa la dependencia necesaria: Manifest
import android.Manifest
// Importa la dependencia necesaria: PackageManager
// Importa la dependencia necesaria: PackageManager
import android.content.pm.PackageManager
// Importa el contenedor de datos Bundle
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa la dependencia necesaria: ActivityResultContracts
// Importa la dependencia necesaria: ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.clickable
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.CircleShape
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.RoundedCornerShape
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.runtime.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.draw.clip
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Brush
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.style.TextAlign
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa el contexto de Android
// Importa el contexto de Android
import androidx.core.content.ContextCompat
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa la dependencia necesaria: LocationServices
// Importa la dependencia necesaria: LocationServices
import com.google.android.gms.location.LocationServices
// Importa la API de comunicación con Wear OS
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.Wearable
// Importa soporte para corrutinas de Kotlin
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.*
// Importa el parser JSON
// Importa el parser JSON
import org.json.JSONArray
// Importa el parser JSON
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: HttpURLConnection
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
// Importa la dependencia necesaria: URL
import java.net.URL

/**
 * [Modelo para representar una mascota en el mural de perdidas]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Almacenar los datos de la mascota a buscar, incluyendo dueño e ubicación]
 */
// Clase de datos MascotaPerdida: modelo inmutable con propiedades de dominio
// Clase de datos MascotaPerdida: modelo inmutable con propiedades de dominio
data class MascotaPerdida(
    // Constante id: valor inmutable que no cambia tras su asignación
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String,
    // Constante nombre: valor inmutable que no cambia tras su asignación
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String,
    // Constante especie: valor inmutable que no cambia tras su asignación
    // Constante especie: valor inmutable que no cambia tras su asignación
    val especie: String,
    // Constante raza: valor inmutable que no cambia tras su asignación
    // Constante raza: valor inmutable que no cambia tras su asignación
    val raza: String = "",
    // Constante color: valor inmutable que no cambia tras su asignación
    // Constante color: valor inmutable que no cambia tras su asignación
    val color: String = "",
    // Constante fotoUrl: valor inmutable que no cambia tras su asignación
    // Constante fotoUrl: valor inmutable que no cambia tras su asignación
    val fotoUrl: String? = null,
    // Constante distanciaAlerta: valor inmutable que no cambia tras su asignación
    // Constante distanciaAlerta: valor inmutable que no cambia tras su asignación
    val distanciaAlerta: Int = 50,
    // Constante estado: valor inmutable que no cambia tras su asignación
    // Constante estado: valor inmutable que no cambia tras su asignación
    val estado: String = "PERDIDA",
    // Constante ownerId: valor inmutable que no cambia tras su asignación
    // Constante ownerId: valor inmutable que no cambia tras su asignación
    val ownerId: String = "",
    // Constante duenoNombre: valor inmutable que no cambia tras su asignación
    // Constante duenoNombre: valor inmutable que no cambia tras su asignación
    val duenoNombre: String = "",
    // Constante duenoTelefono: valor inmutable que no cambia tras su asignación
    // Constante duenoTelefono: valor inmutable que no cambia tras su asignación
    val duenoTelefono: String = "",
    // Constante ultimaUbicacionLat: valor inmutable que no cambia tras su asignación
    // Constante ultimaUbicacionLat: valor inmutable que no cambia tras su asignación
    val ultimaUbicacionLat: Double? = null,
    // Constante ultimaUbicacionLng: valor inmutable que no cambia tras su asignación
    // Constante ultimaUbicacionLng: valor inmutable que no cambia tras su asignación
    val ultimaUbicacionLng: Double? = null
)

/**
 * [Actividad principal para el mural de mascotas perdidas y reportar avistamientos]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mostrar la lista de mascotas reportadas como perdidas por la comunidad]
 * - [Permitir agregar nuevas mascotas al mural de perdidas]
 * - [Permitir reportar el avistamiento de una mascota con la ubicación GPS]
 */
// Activity ReportarAvistamientoActivity: pantalla principal que gestiona el ciclo de vida
// Activity ReportarAvistamientoActivity: pantalla principal que gestiona el ciclo de vida
class ReportarAvistamientoActivity : ComponentActivity() {
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    private val backendUrl = BuildConfig.BACKEND_URL
    // Variable ubicacionLat: almacena el estado mutable de este componente
    // Variable ubicacionLat: almacena el estado mutable de este componente
    private var ubicacionLat = 0.0
    // Variable ubicacionLng: almacena el estado mutable de este componente
    // Variable ubicacionLng: almacena el estado mutable de este componente
    private var ubicacionLng = 0.0
    // Variable ubicacionTexto: almacena el estado mutable de este componente
    // Variable ubicacionTexto: almacena el estado mutable de este componente
    private var ubicacionTexto = "Obteniendo ubicacion..."
    // Variable ubicacionValida: almacena el estado mutable de este componente
    // Variable ubicacionValida: almacena el estado mutable de este componente
    private var ubicacionValida = false

    // Constante locationPermissionRequest: valor inmutable que no cambia tras su asignación
    // Constante locationPermissionRequest: valor inmutable que no cambia tras su asignación
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (isGranted) {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("AVISTAMIENTO", "Permiso de ubicacion concedido")
            obtenerUbicacion()
        } else {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("AVISTAMIENTO", "Permiso de ubicacion denegado")
            ubicacionValida = false
            ubicacionTexto = "Sin permiso de ubicacion"
        }
    }

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)
        // Registro de evento en el log de Android para depuración
        // Registro de evento en el log de Android para depuración
        android.util.Log.d("AVISTAMIENTO", "INICIANDO")

        // Constante prefs: valor inmutable que no cambia tras su asignación
        // Constante prefs: valor inmutable que no cambia tras su asignación
        val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
        // Variable userId: almacena el estado mutable de este componente
        // Variable userId: almacena el estado mutable de este componente
        var userId = prefs.getString("user_id", "") ?: ""
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (userId.isEmpty() || !userId.matches(Regex("^\\d+$"))) {
            userId = "2"
            // Inicia el editor para modificar los SharedPreferences
            // Inicia el editor para modificar los SharedPreferences
            prefs.edit().putString("user_id", userId).apply()
        }

        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            obtenerUbicacion()
        }

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            // Variable mascotasPerdidas: almacena el estado mutable de este componente
            // Variable mascotasPerdidas: almacena el estado mutable de este componente
            var mascotasPerdidas by remember { mutableStateOf<List<MascotaPerdida>>(emptyList()) }
            // Variable isLoading: almacena el estado mutable de este componente
            // Variable isLoading: almacena el estado mutable de este componente
            var isLoading by remember { mutableStateOf(true) }
            // Variable errorMessage: almacena el estado mutable de este componente
            // Variable errorMessage: almacena el estado mutable de este componente
            var errorMessage by remember { mutableStateOf("") }
            // Variable successMessage: almacena el estado mutable de este componente
            // Variable successMessage: almacena el estado mutable de este componente
            var successMessage by remember { mutableStateOf("") }
            // Variable mostrarFormulario: almacena el estado mutable de este componente
            // Variable mostrarFormulario: almacena el estado mutable de este componente
            var mostrarFormulario by remember { mutableStateOf(false) }
            // Variable mostrarDetalles: almacena el estado mutable de este componente
            // Variable mostrarDetalles: almacena el estado mutable de este componente
            var mostrarDetalles by remember { mutableStateOf(false) }
            // Variable mascotaSeleccionada: almacena el estado mutable de este componente
            // Variable mascotaSeleccionada: almacena el estado mutable de este componente
            var mascotaSeleccionada by remember { mutableStateOf<MascotaPerdida?>(null) }
            // Variable isSendingReport: almacena el estado mutable de este componente
            // Variable isSendingReport: almacena el estado mutable de este componente
            var isSendingReport by remember { mutableStateOf(false) }
            // Variable isCreating: almacena el estado mutable de este componente
            // Variable isCreating: almacena el estado mutable de este componente
            var isCreating by remember { mutableStateOf(false) }

            // Variable paso: almacena el estado mutable de este componente
            // Variable paso: almacena el estado mutable de este componente
            var paso by remember { mutableStateOf(0) }
            // Variable nombre: almacena el estado mutable de este componente
            // Variable nombre: almacena el estado mutable de este componente
            var nombre by remember { mutableStateOf("") }
            // Variable especie: almacena el estado mutable de este componente
            // Variable especie: almacena el estado mutable de este componente
            var especie by remember { mutableStateOf("PERRO") }
            // Variable raza: almacena el estado mutable de este componente
            // Variable raza: almacena el estado mutable de este componente
            var raza by remember { mutableStateOf("") }
            // Variable color: almacena el estado mutable de este componente
            // Variable color: almacena el estado mutable de este componente
            var color by remember { mutableStateOf("") }
            // Variable telefono: almacena el estado mutable de este componente
            // Variable telefono: almacena el estado mutable de este componente
            var telefono by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                // Constante result: valor inmutable que no cambia tras su asignación
                // Constante result: valor inmutable que no cambia tras su asignación
                val result = withContext(Dispatchers.IO) {
                    cargarMascotasPerdidas()
                }
                mascotasPerdidas = result.mascotas
                isLoading = false
                errorMessage = result.errorMessage
                // Registro de evento en el log de Android para depuración
                // Registro de evento en el log de Android para depuración
                android.util.Log.d("AVISTAMIENTO", "Mascotas cargadas: ${result.mascotas.size}")
            }

            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
            when {
                mostrarDetalles && mascotaSeleccionada != null -> {
                    MascotaPerdidaDetailScreen(
                        mascota = mascotaSeleccionada!!,
                        onBack = {
                            mostrarDetalles = false
                            mascotaSeleccionada = null
                        },
                        onReportar = {
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (ubicacionValida) {
                                isSendingReport = true
                                // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                                // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                                CoroutineScope(Dispatchers.Main).launch {
                                    // Constante result: valor inmutable que no cambia tras su asignación
                                    // Constante result: valor inmutable que no cambia tras su asignación
                                    val result = withContext(Dispatchers.IO) {
                                        reportarAvistamiento(
                                            mascotaId = mascotaSeleccionada!!.id,
                                            lat = ubicacionLat,
                                            lng = ubicacionLng,
                                            direccion = ubicacionTexto
                                        )
                                    }
                                    isSendingReport = false
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    if (result.success) {
                                        successMessage = "Reporte enviado"
                                        errorMessage = ""
                                        mostrarDetalles = false
                                        mascotaSeleccionada = null
                                        isLoading = true
                                        // Constante newResult: valor inmutable que no cambia tras su asignación
                                        // Constante newResult: valor inmutable que no cambia tras su asignación
                                        val newResult = withContext(Dispatchers.IO) {
                                            cargarMascotasPerdidas()
                                        }
                                        mascotasPerdidas = newResult.mascotas
                                        isLoading = false
                                        errorMessage = newResult.errorMessage
                                    } else {
                                        errorMessage = result.errorMessage
                                    }
                                }
                            } else {
                                errorMessage = "Sin ubicacion. Activa el GPS."
                            }
                        }
                    )
                }
                mostrarFormulario -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        FormularioScreenSimplificado(
                            paso = paso,
                            nombre = nombre,
                            especie = especie,
                            raza = raza,
                            color = color,
                            telefono = telefono,
                            isCreating = isCreating,
                            onNombreChange = { nombre = it },
                            onEspecieChange = { especie = it },
                            onRazaChange = { raza = it },
                            onColorChange = { color = it },
                            onTelefonoChange = { telefono = it },
                            onPasoChange = { paso = it },
                            onSave = {
                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                if (nombre.isNotEmpty() && telefono.isNotEmpty()) {
                                    isCreating = true
                                    // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                                    // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                                    CoroutineScope(Dispatchers.Main).launch {
                                        // Constante result: valor inmutable que no cambia tras su asignación
                                        // Constante result: valor inmutable que no cambia tras su asignación
                                        val result = withContext(Dispatchers.IO) {
                                            crearMascotaPerdida(nombre, especie, raza, color, telefono)
                                        }
                                        isCreating = false
                                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                        if (result.success) {
                                            successMessage = "Mascota publicada en el mural"
                                            errorMessage = ""
                                            mostrarFormulario = false
                                            paso = 0
                                            nombre = ""
                                            raza = ""
                                            color = ""
                                            telefono = ""
                                            isLoading = true
                                            // Constante newResult: valor inmutable que no cambia tras su asignación
                                            // Constante newResult: valor inmutable que no cambia tras su asignación
                                            val newResult = withContext(Dispatchers.IO) {
                                                cargarMascotasPerdidas()
                                            }
                                            mascotasPerdidas = newResult.mascotas
                                            isLoading = false
                                            errorMessage = newResult.errorMessage
                                            notificarNuevaMascotaPerdida(nombre)
                                        } else {
                                            errorMessage = result.errorMessage
                                        }
                                    }
                                } else {
                                    errorMessage = "Nombre y telefono son obligatorios"
                                }
                            },
                            onBack = {
                                mostrarFormulario = false
                                paso = 0
                                errorMessage = ""
                            }
                        )
                    }
                }
                else -> {
                    MainScreenGrid(
                        mascotas = mascotasPerdidas,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        successMessage = successMessage,
                        isSendingReport = isSendingReport,
                        ubicacionTexto = ubicacionTexto,
                        ubicacionValida = ubicacionValida,
                        onMascotaClick = { mascota ->
                            mascotaSeleccionada = mascota
                            mostrarDetalles = true
                        },
                        onAgregarClick = {
                            // Registro de evento en el log de Android para depuración
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.d("AVISTAMIENTO", "Boton + presionado")
                            mostrarFormulario = true
                            paso = 0
                            errorMessage = ""
                            nombre = ""
                            raza = ""
                            color = ""
                            telefono = ""
                        },
                        onRetry = {
                            isLoading = true
                            errorMessage = ""
                            successMessage = ""
                            // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                            // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
                            CoroutineScope(Dispatchers.Main).launch {
                                // Constante result: valor inmutable que no cambia tras su asignación
                                // Constante result: valor inmutable que no cambia tras su asignación
                                val result = withContext(Dispatchers.IO) {
                                    cargarMascotasPerdidas()
                                }
                                mascotasPerdidas = result.mascotas
                                isLoading = false
                                errorMessage = result.errorMessage
                            }
                        }
                    )
                }
            }
        }
    }

    private fun obtenerUbicacion() {
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ubicacionValida = false
            ubicacionTexto = "Sin permiso de ubicacion"
            // Retorna el valor al llamador de la función
            // Retorna el valor al llamador de la función
            return
        }

        // Constante fusedClient: valor inmutable que no cambia tras su asignación
        // Constante fusedClient: valor inmutable que no cambia tras su asignación
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        fusedClient.lastLocation.addOnSuccessListener { location ->
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (location != null) {
                ubicacionLat = location.latitude
                ubicacionLng = location.longitude
                ubicacionTexto = "Lat: ${String.format("%.4f", location.latitude)}, Lng: ${String.format("%.4f", location.longitude)}"
                ubicacionValida = true
                // Registro de evento en el log de Android para depuración
                // Registro de evento en el log de Android para depuración
                android.util.Log.d("AVISTAMIENTO", "Ubicacion obtenida: $ubicacionTexto")
            } else {
                ubicacionTexto = "Ubicacion no disponible"
                ubicacionValida = false
            }
        }.addOnFailureListener {
            ubicacionTexto = "Error obteniendo ubicacion"
            ubicacionValida = false
        }
    }

    private suspend fun cargarMascotasPerdidas(): CargaResult {
        // Retorna el valor al llamador de la función
        // Retorna el valor al llamador de la función
        return try {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("AVISTAMIENTO", "Cargando mascotas del mural...")

            // Constante url: valor inmutable que no cambia tras su asignación
            // Constante url: valor inmutable que no cambia tras su asignación
            val url = URL("$backendUrl/api/mascotas/mural")
            // Constante conn: valor inmutable que no cambia tras su asignación
            // Constante conn: valor inmutable que no cambia tras su asignación
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            val responseCode = conn.responseCode

            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("AVISTAMIENTO", "Response Code: $responseCode")

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Constante response: valor inmutable que no cambia tras su asignación
                // Constante response: valor inmutable que no cambia tras su asignación
                val response = conn.inputStream.bufferedReader().readText()
                // Registro de evento en el log de Android para depuración
                // Registro de evento en el log de Android para depuración
                android.util.Log.d("AVISTAMIENTO", "Respuesta: $response")

                // Constante jsonArray: valor inmutable que no cambia tras su asignación
                // Constante jsonArray: valor inmutable que no cambia tras su asignación
                val jsonArray = JSONArray(response)
                // Constante lista: valor inmutable que no cambia tras su asignación
                // Constante lista: valor inmutable que no cambia tras su asignación
                val lista = mutableListOf<MascotaPerdida>()

                // Itera sobre la colección para procesar cada elemento
                // Itera sobre la colección para procesar cada elemento
                for (i in 0 until jsonArray.length()) {
                    // Constante obj: valor inmutable que no cambia tras su asignación
                    // Constante obj: valor inmutable que no cambia tras su asignación
                    val obj = jsonArray.getJSONObject(i)
                    // Constante ownerId: valor inmutable que no cambia tras su asignación
                    // Constante ownerId: valor inmutable que no cambia tras su asignación
                    val ownerId = obj.optString("owner_id", "")
                    lista.add(
                        MascotaPerdida(
                            id = obj.getString("id"),
                            nombre = obj.getString("nombre"),
                            especie = obj.getString("especie"),
                            raza = obj.optString("raza", ""),
                            color = obj.optString("color", ""),
                            fotoUrl = obj.optString("foto_url", null),
                            distanciaAlerta = obj.getInt("distancia_alerta"),
                            estado = obj.getString("estado"),
                            ownerId = ownerId,
                            duenoNombre = if (ownerId == "0") "Reportado por comunidad" else obj.optString("dueno_nombre", "Dueño"),
                            duenoTelefono = obj.optString("telefono", "")
                        )
                    )
                }
                conn.disconnect()
                // Registro de evento en el log de Android para depuración
                // Registro de evento en el log de Android para depuración
                android.util.Log.d("AVISTAMIENTO", "${lista.size} mascotas cargadas")
                CargaResult(lista, if (lista.isEmpty()) "No hay mascotas perdidas en el mural" else "")
            } else {
                // Constante errorBody: valor inmutable que no cambia tras su asignación
                // Constante errorBody: valor inmutable que no cambia tras su asignación
                val errorBody = conn.errorStream?.bufferedReader()?.readText()
                conn.disconnect()
                // Registro de evento en el log de Android para depuración
                // Registro de evento en el log de Android para depuración
                android.util.Log.e("AVISTAMIENTO", "Error HTTP $responseCode: $errorBody")
                CargaResult(emptyList(), "Error al cargar (HTTP $responseCode)")
            }
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("AVISTAMIENTO", "Error: ${e.message}", e)
            CargaResult(emptyList(), "Error: ${e.message}")
        }
    }

    private suspend fun reportarAvistamiento(
        mascotaId: String,
        lat: Double,
        lng: Double,
        direccion: String
    ): OperacionResult {
        // Retorna el valor al llamador de la función
        // Retorna el valor al llamador de la función
        return try {
            // Constante prefs: valor inmutable que no cambia tras su asignación
            // Constante prefs: valor inmutable que no cambia tras su asignación
            val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
            // Constante userIdStr: valor inmutable que no cambia tras su asignación
            // Constante userIdStr: valor inmutable que no cambia tras su asignación
            val userIdStr = prefs.getString("user_id", "2") ?: "2"
            // Constante reportadoPorId: valor inmutable que no cambia tras su asignación
            // Constante reportadoPorId: valor inmutable que no cambia tras su asignación
            val reportadoPorId = userIdStr.toIntOrNull() ?: 2

            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("AVISTAMIENTO", "Reportando: $mascotaId en $lat, $lng")

            // Constante url: valor inmutable que no cambia tras su asignación
            // Constante url: valor inmutable que no cambia tras su asignación
            val url = URL("$backendUrl/api/reportes")
            // Constante conn: valor inmutable que no cambia tras su asignación
            // Constante conn: valor inmutable que no cambia tras su asignación
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            // Constante json: valor inmutable que no cambia tras su asignación
            // Constante json: valor inmutable que no cambia tras su asignación
            val json = JSONObject().apply {
                put("mascota_id", mascotaId)
                put("latitud", lat)
                put("longitud", lng)
                put("reportado_por_id", reportadoPorId)
                put("direccion", direccion)
            }

            conn.outputStream.write(json.toString().toByteArray())
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            val responseCode = conn.responseCode
            conn.disconnect()

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (responseCode == 200 || responseCode == 201) {
                enviarNotificacionAlMovil(mascotaId, lat, lng, direccion)
                OperacionResult(true, "")
            } else {
                OperacionResult(false, "Error al reportar (HTTP $responseCode)")
            }
        } catch (e: Exception) {
            OperacionResult(false, "Error: ${e.message}")
        }
    }

    private suspend fun crearMascotaPerdida(
        nombre: String,
        especie: String,
        raza: String,
        color: String,
        telefono: String
    ): OperacionResult {
        // Retorna el valor al llamador de la función
        // Retorna el valor al llamador de la función
        return try {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("AVISTAMIENTO", "Creando mascota perdida para el mural: $nombre")

            // Constante url: valor inmutable que no cambia tras su asignación
            // Constante url: valor inmutable que no cambia tras su asignación
            val url = URL("$backendUrl/api/mascotas/mural")
            // Constante conn: valor inmutable que no cambia tras su asignación
            // Constante conn: valor inmutable que no cambia tras su asignación
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            // Constante json: valor inmutable que no cambia tras su asignación
            // Constante json: valor inmutable que no cambia tras su asignación
            val json = JSONObject().apply {
                put("nombre", nombre)
                put("especie", especie)
                put("raza", raza)
                put("color", color)
                put("telefono", telefono)
            }

            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("AVISTAMIENTO", "JSON enviado: $json")

            conn.outputStream.write(json.toString().toByteArray())
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            val responseCode = conn.responseCode
            // Constante responseBody: valor inmutable que no cambia tras su asignación
            // Constante responseBody: valor inmutable que no cambia tras su asignación
            val responseBody = if (responseCode == 200 || responseCode == 201) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText()
            }
            conn.disconnect()

            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("AVISTAMIENTO", "Response Code: $responseCode")
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("AVISTAMIENTO", "Response: $responseBody")

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (responseCode == 200 || responseCode == 201) {
                OperacionResult(true, "")
            } else {
                OperacionResult(false, "Error al crear (HTTP $responseCode): $responseBody")
            }
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("AVISTAMIENTO", "Error: ${e.message}", e)
            OperacionResult(false, "Error: ${e.message}")
        }
    }

    private fun enviarNotificacionAlMovil(mascotaId: String, lat: Double, lng: Double, direccion: String) {
        // Constante context: valor inmutable que no cambia tras su asignación
        // Constante context: valor inmutable que no cambia tras su asignación
        val context = applicationContext
        // Constante payload: valor inmutable que no cambia tras su asignación
        // Constante payload: valor inmutable que no cambia tras su asignación
        val payload = JSONObject().apply {
            put("tipo", "AVISTAMIENTO_REPORTADO")
            put("mascota_id", mascotaId)
            put("latitud", lat)
            put("longitud", lng)
            put("direccion", direccion)
        }.toString().toByteArray()

        // Usa la API de Wearable para comunicación con dispositivos Wear OS
        // Usa la API de Wearable para comunicación con dispositivos Wear OS
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                nodes.forEach { node ->
                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    Wearable.getMessageClient(context)
                        // Envía un mensaje al dispositivo Wear OS conectado
                        // Envía un mensaje al dispositivo Wear OS conectado
                        .sendMessage(node.id, "/watch/avistamiento", payload)
                        .addOnSuccessListener {
                            // Registro de evento en el log de Android para depuración
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.d("AVISTAMIENTO", "Notificacion enviada al movil")
                        }
                }
            }
    }

    private fun notificarNuevaMascotaPerdida(nombre: String) {
        // Constante context: valor inmutable que no cambia tras su asignación
        // Constante context: valor inmutable que no cambia tras su asignación
        val context = applicationContext
        // Constante payload: valor inmutable que no cambia tras su asignación
        // Constante payload: valor inmutable que no cambia tras su asignación
        val payload = JSONObject().apply {
            put("tipo", "NUEVA_MASCOTA_PERDIDA")
            put("nombre", nombre)
        }.toString().toByteArray()

        // Usa la API de Wearable para comunicación con dispositivos Wear OS
        // Usa la API de Wearable para comunicación con dispositivos Wear OS
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                nodes.forEach { node ->
                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    Wearable.getMessageClient(context)
                        // Envía un mensaje al dispositivo Wear OS conectado
                        // Envía un mensaje al dispositivo Wear OS conectado
                        .sendMessage(node.id, "/mascota/perdida/nueva", payload)
                        .addOnSuccessListener {
                            // Registro de evento en el log de Android para depuración
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.d("AVISTAMIENTO", "Notificacion de nueva mascota enviada")
                        }
                }
            }
    }

    // Clase de datos CargaResult: modelo inmutable con propiedades de dominio
    // Clase de datos CargaResult: modelo inmutable con propiedades de dominio
    data class CargaResult(
        // Constante mascotas: valor inmutable que no cambia tras su asignación
        // Constante mascotas: valor inmutable que no cambia tras su asignación
        val mascotas: List<MascotaPerdida>,
        // Constante errorMessage: valor inmutable que no cambia tras su asignación
        // Constante errorMessage: valor inmutable que no cambia tras su asignación
        val errorMessage: String
    )

    // Clase de datos OperacionResult: modelo inmutable con propiedades de dominio
    // Clase de datos OperacionResult: modelo inmutable con propiedades de dominio
    data class OperacionResult(
        // Constante success: valor inmutable que no cambia tras su asignación
        // Constante success: valor inmutable que no cambia tras su asignación
        val success: Boolean,
        // Constante errorMessage: valor inmutable que no cambia tras su asignación
        // Constante errorMessage: valor inmutable que no cambia tras su asignación
        val errorMessage: String
    )
}

// 🎨 Paleta temática "mascotas perdidas"
// Constante BgTop: valor inmutable que no cambia tras su asignación
// Constante BgTop: valor inmutable que no cambia tras su asignación
private val BgTop = Color(0xFF1B1430)
// Constante BgBottom: valor inmutable que no cambia tras su asignación
// Constante BgBottom: valor inmutable que no cambia tras su asignación
private val BgBottom = Color(0xFF0D0B1A)
// Constante CardBg: valor inmutable que no cambia tras su asignación
// Constante CardBg: valor inmutable que no cambia tras su asignación
private val CardBg = Color(0xFF2C2C3E)
// Constante FieldBg: valor inmutable que no cambia tras su asignación
// Constante FieldBg: valor inmutable que no cambia tras su asignación
private val FieldBg = Color(0xFF2C2657)
// Constante AccentRed: valor inmutable que no cambia tras su asignación
// Constante AccentRed: valor inmutable que no cambia tras su asignación
private val AccentRed = Color(0xFFE85D5D)
// Constante AccentGreen: valor inmutable que no cambia tras su asignación
// Constante AccentGreen: valor inmutable que no cambia tras su asignación
private val AccentGreen = Color(0xFF4CD97B)
// Constante AccentBlue: valor inmutable que no cambia tras su asignación
// Constante AccentBlue: valor inmutable que no cambia tras su asignación
private val AccentBlue = Color(0xFF4D9FFF)

// ============================================================
// ✅ PANTALLA PRINCIPAL DEL MURAL - CON SCROLL VERTICAL
// ============================================================

/**
 * [Pantalla con la cuadrícula del mural de mascotas perdidas]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [mascotas]: Lista de mascotas del mural
 * - [isLoading]: Estado de carga del mural
 * - [errorMessage]: Mensaje de error a mostrar
 * - [successMessage]: Mensaje de éxito de la última operación
 * - [isSendingReport]: Indica si hay un reporte en proceso de envío
 * - [ubicacionTexto]: Texto descriptivo de la ubicación actual
 * - [ubicacionValida]: Indica si se tiene una ubicación GPS válida
 * - [onMascotaClick]: Acción al presionar una mascota del mural
 * - [onAgregarClick]: Acción al presionar el botón de agregar
 * - [onRetry]: Acción para recargar el mural
 */
// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MainScreenGrid: define la lógica de esta operación
// Función MainScreenGrid: define la lógica de esta operación
fun MainScreenGrid(
    mascotas: List<MascotaPerdida>,
    isLoading: Boolean,
    errorMessage: String,
    successMessage: String,
    isSendingReport: Boolean,
    ubicacionTexto: String,
    ubicacionValida: Boolean,
    onMascotaClick: (MascotaPerdida) -> Unit,
    onAgregarClick: () -> Unit,
    onRetry: () -> Unit
) {
    // Constante listState: valor inmutable que no cambia tras su asignación
    // Constante listState: valor inmutable que no cambia tras su asignación
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp, start = 6.dp, end = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ✅ HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🐾 Mural",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (ubicacionValida) "●" else "○",
                            fontSize = 8.sp,
                            color = if (ubicacionValida) AccentGreen else Color(0xFFFF9800)
                        )
                        Button(
                            onClick = onAgregarClick,
                            colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("+", fontSize = 14.sp, color = Color.White)
                        }
                    }
                }
            }

            // ✅ Mensaje de éxito
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (successMessage.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentGreen.copy(alpha = 0.15f))
                            .clip(RoundedCornerShape(6.dp))
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "✅ $successMessage",
                            color = AccentGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ✅ Mensaje de error
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (errorMessage.isNotEmpty() && !isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentRed.copy(alpha = 0.15f))
                            .clip(RoundedCornerShape(6.dp))
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "⚠️ ${errorMessage.take(25)}",
                            color = AccentRed,
                            fontSize = 8.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ✅ Estado de envío
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (isSendingReport) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 1.5.dp,
                            indicatorColor = AccentBlue
                        )
                        Text(
                            text = "Enviando...",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 8.sp
                        )
                    }
                }
            }

            // ✅ CONTENIDO PRINCIPAL
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                indicatorColor = AccentGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Cargando...",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            } else if (mascotas.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "📭 Sin mascotas",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(backgroundColor = AccentBlue),
                                modifier = Modifier.size(width = 70.dp, height = 28.dp)
                            ) {
                                Text("↻", fontSize = 14.sp, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                // ✅ Grid de 2 columnas con items
                items(mascotas.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Itera sobre cada elemento de la colección y ejecuta el bloque
                        // Itera sobre cada elemento de la colección y ejecuta el bloque
                        pair.forEach { mascota ->
                            MascotaGridItemCompacto(
                                mascota = mascota,
                                onClick = { onMascotaClick(mascota) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Si es impar, agregar un espacio vacío
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Espacio al final
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ✅ ITEM DE MASCOTA MÁS COMPACTO
// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MascotaGridItemCompacto: define la lógica de esta operación
// Función MascotaGridItemCompacto: define la lógica de esta operación
fun MascotaGridItemCompacto(
    mascota: MascotaPerdida,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .padding(vertical = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CardBg)
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF3D3D5C)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (mascota.especie == "PERRO") "🐕" else "🐈",
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = mascota.nombre.take(8),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1
            )

            Text(
                text = "⚠️",
                fontSize = 6.sp
            )
        }
    }
}

// ============================================================
// ✅ PANTALLA DE DETALLES - COMPLETA Y SIN CORTES
// ============================================================

// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función MascotaPerdidaDetailScreen: define la lógica de esta operación
// Función MascotaPerdidaDetailScreen: define la lógica de esta operación
fun MascotaPerdidaDetailScreen(
    mascota: MascotaPerdida,
    onBack: () -> Unit,
    onReportar: () -> Unit
) {
    // Constante listState: valor inmutable que no cambia tras su asignación
    // Constante listState: valor inmutable que no cambia tras su asignación
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp, start = 6.dp, end = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ✅ Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔍 Detalles",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    CompactButton(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF666666)),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Text("✕", fontSize = 10.sp, color = Color.White)
                    }
                }
            }

            // ✅ Avatar
            item {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(CardBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (mascota.especie == "PERRO") "🐕" else "🐈",
                        fontSize = 22.sp
                    )
                }
            }

            // ✅ Nombre y estado
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = mascota.nombre.take(10),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "⚠️",
                        fontSize = 10.sp,
                        color = AccentRed
                    )
                }
            }

            // ✅ Información en tarjeta
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    onClick = { }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardBg)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InfoRowCompacta("Especie", mascota.especie)
                            InfoRowCompacta("Raza", if (mascota.raza.isNotEmpty()) mascota.raza.take(6) else "-")
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InfoRowCompacta("Color", if (mascota.color.isNotEmpty()) mascota.color.take(6) else "-")
                            InfoRowCompacta("Dueño", mascota.duenoNombre.take(8))
                        }
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (mascota.duenoTelefono.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                InfoRowCompacta("Teléfono", mascota.duenoTelefono, AccentGreen)
                                Text("", fontSize = 8.sp)
                            }
                        }
                    }
                }
            }

            // ✅ Botón de reporte - SIMPLIFICADO Y LIMPIO
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onReportar,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AccentRed,
                        contentColor = Color.White
                    ),
                    // fillMaxWidth() hace que el botón tome el ancho disponible
                    // Usamos un height mayor para que el texto de varias líneas no se corte
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 4.dp), // Un pequeño margen para que no toque los bordes
                    shape = RoundedCornerShape(12.dp) // Los bordes redondeados hacen que el texto se ajuste mejor
                ) {
                    Text(
                        text = "Reportar ", // Tu texto largo
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, // Vital para textos largos
                        maxLines = 2 // Permite que el texto salte de línea en lugar de cortarse
                    )
                }
            }

            // Espacio al final
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// ✅ InfoRow más compacta
// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función InfoRowCompacta: define la lógica de esta operación
// Función InfoRowCompacta: define la lógica de esta operación
fun InfoRowCompacta(label: String, value: String, color: Color = Color.White) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 7.sp
        )
        Text(
            text = value,
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

// ============================================================
// ✅ FORMULARIO - MANTENIDO INTACTO
// ============================================================

// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función FormularioScreenSimplificado: define la lógica de esta operación
// Función FormularioScreenSimplificado: define la lógica de esta operación
fun FormularioScreenSimplificado(
    paso: Int,
    nombre: String,
    especie: String,
    raza: String,
    color: String,
    telefono: String,
    isCreating: Boolean,
    onNombreChange: (String) -> Unit,
    onEspecieChange: (String) -> Unit,
    onRazaChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onPasoChange: (Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    // Variable mostrarTeclado: almacena el estado mutable de este componente
    // Variable mostrarTeclado: almacena el estado mutable de este componente
    var mostrarTeclado by remember { mutableStateOf(false) }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0B1A))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                (0..5).forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == paso) 6.dp else 4.dp)
                            .clip(CircleShape)
                            .background(
                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                if (i <= paso) AccentRed
                                else Color.White.copy(alpha = 0.15f)
                            )
                    )
                }
            }

            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
            when (paso) {
                0 -> PasoNombreForm(
                    nombre = nombre,
                    onNombreChange = onNombreChange,
                    onSiguiente = { if (nombre.isNotEmpty()) onPasoChange(1) },
                    onAtras = onBack,
                    onMostrarTeclado = { mostrarTeclado = true }
                )
                1 -> PasoEspecieForm(
                    especie = especie,
                    onSelect = {
                        onEspecieChange(it)
                        onPasoChange(2)
                    },
                    onAtras = { onPasoChange(0) }
                )
                2 -> PasoOpcionalForm(
                    titulo = "Raza",
                    valor = raza,
                    placeholder = "Toca para escribir",
                    onValorChange = onRazaChange,
                    onSiguiente = { onPasoChange(3) },
                    onAtras = { onPasoChange(1) },
                    onMostrarTeclado = { mostrarTeclado = true }
                )
                3 -> PasoOpcionalForm(
                    titulo = "Color",
                    valor = color,
                    placeholder = "Toca para escribir",
                    onValorChange = onColorChange,
                    onSiguiente = { onPasoChange(4) },
                    onAtras = { onPasoChange(2) },
                    onMostrarTeclado = { mostrarTeclado = true }
                )
                4 -> PasoTelefonoForm(
                    telefono = telefono,
                    onTelefonoChange = onTelefonoChange,
                    onSiguiente = { if (telefono.isNotEmpty()) onPasoChange(5) },
                    onAtras = { onPasoChange(3) },
                    onMostrarTeclado = { mostrarTeclado = true }
                )
                5 -> PasoConfirmarForm(
                    nombre = nombre,
                    especie = especie,
                    raza = raza,
                    color = color,
                    telefono = telefono,
                    isCreating = isCreating,
                    onGuardar = onSave,
                    onAtras = { onPasoChange(4) }
                )
            }
        }
    }

    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
    if (mostrarTeclado) {
        // Constante valorActual: valor inmutable que no cambia tras su asignación
        // Constante valorActual: valor inmutable que no cambia tras su asignación
        val valorActual = when (paso) {
            0 -> nombre
            2 -> raza
            3 -> color
            4 -> telefono
            else -> ""
        }

        // Constante esNumerico: valor inmutable que no cambia tras su asignación
        // Constante esNumerico: valor inmutable que no cambia tras su asignación
        val esNumerico = paso == 4

        // Constante onLetraClick: valor inmutable que no cambia tras su asignación
        // Constante onLetraClick: valor inmutable que no cambia tras su asignación
        val onLetraClick: (String) -> Unit = { letra ->
            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
            when (paso) {
                0 -> onNombreChange(nombre + letra)
                2 -> onRazaChange(raza + letra)
                3 -> onColorChange(color + letra)
                4 -> onTelefonoChange(telefono + letra)
            }
        }

        // Constante onBorrarClick: valor inmutable que no cambia tras su asignación
        // Constante onBorrarClick: valor inmutable que no cambia tras su asignación
        val onBorrarClick: () -> Unit = {
            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
            when (paso) {
                0 -> { if (nombre.isNotEmpty()) onNombreChange(nombre.dropLast(1)) }
                2 -> { if (raza.isNotEmpty()) onRazaChange(raza.dropLast(1)) }
                3 -> { if (color.isNotEmpty()) onColorChange(color.dropLast(1)) }
                4 -> { if (telefono.isNotEmpty()) onTelefonoChange(telefono.dropLast(1)) }
            }
        }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { mostrarTeclado = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                TecladoSimple(
                    valor = valorActual,
                    esNumerico = esNumerico,
                    onLetraClick = onLetraClick,
                    onBorrarClick = onBorrarClick,
                    onCerrar = { mostrarTeclado = false }
                )
            }
        }
    }
}

// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función PasoNombreForm: define la lógica de esta operación
// Función PasoNombreForm: define la lógica de esta operación
fun PasoNombreForm(
    nombre: String,
    onNombreChange: (String) -> Unit,
    onSiguiente: () -> Unit,
    onAtras: () -> Unit,
    onMostrarTeclado: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("🐾", fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Nombre",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))

        Card(
            onClick = onMostrarTeclado,
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldBg)
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (nombre.isNotEmpty()) nombre else "Toca",
                    color = if (nombre.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = if (nombre.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactChip(
                onClick = onAtras,
                label = { Text("Cancelar", fontSize = 8.sp, color = Color.White) },
                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
                modifier = Modifier.width(60.dp)
            )
            CompactChip(
                onClick = onSiguiente,
                label = { Text("Sig", fontSize = 9.sp, color = Color.White) },
                colors = ChipDefaults.chipColors(
                    backgroundColor = if (nombre.isNotEmpty()) AccentBlue else Color(0xFF555555)
                ),
                modifier = Modifier.width(60.dp),
                enabled = nombre.isNotEmpty()
            )
        }
    }
}

// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función PasoOpcionalForm: define la lógica de esta operación
// Función PasoOpcionalForm: define la lógica de esta operación
fun PasoOpcionalForm(
    titulo: String,
    valor: String,
    placeholder: String,
    onValorChange: (String) -> Unit,
    onSiguiente: () -> Unit,
    onAtras: () -> Unit,
    onMostrarTeclado: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("✏️", fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = titulo,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))

        Card(
            onClick = onMostrarTeclado,
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldBg)
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (valor.isNotEmpty()) valor else placeholder,
                    color = if (valor.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = if (valor.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactChip(
                onClick = onAtras,
                label = { Text("Atrás", fontSize = 9.sp, color = Color.White) },
                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
                modifier = Modifier.width(60.dp)
            )
            CompactChip(
                onClick = onSiguiente,
                label = { Text("Omitir", fontSize = 9.sp, color = Color.White) },
                colors = ChipDefaults.chipColors(backgroundColor = AccentBlue),
                modifier = Modifier.width(60.dp)
            )
        }
    }
}

// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función PasoTelefonoForm: define la lógica de esta operación
// Función PasoTelefonoForm: define la lógica de esta operación
fun PasoTelefonoForm(
    telefono: String,
    onTelefonoChange: (String) -> Unit,
    onSiguiente: () -> Unit,
    onAtras: () -> Unit,
    onMostrarTeclado: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("📞", fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Teléfono",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))

        Card(
            onClick = onMostrarTeclado,
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldBg)
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (telefono.isNotEmpty()) telefono else "Ej: 123456",
                    color = if (telefono.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = if (telefono.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactChip(
                onClick = onAtras,
                label = { Text("Atrás", fontSize = 9.sp, color = Color.White) },
                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
                modifier = Modifier.width(60.dp)
            )
            CompactChip(
                onClick = onSiguiente,
                label = { Text("Sig", fontSize = 9.sp, color = Color.White) },
                colors = ChipDefaults.chipColors(
                    backgroundColor = if (telefono.isNotEmpty()) AccentBlue else Color(0xFF555555)
                ),
                modifier = Modifier.width(60.dp),
                enabled = telefono.isNotEmpty()
            )
        }
    }
}

// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función PasoEspecieForm: define la lógica de esta operación
// Función PasoEspecieForm: define la lógica de esta operación
fun PasoEspecieForm(
    especie: String,
    onSelect: (String) -> Unit,
    onAtras: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Perro o gato?",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                onClick = { onSelect("PERRO") },
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (especie == "PERRO") AccentRed.copy(alpha = 0.3f)
                            else FieldBg
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🐕", fontSize = 22.sp)
                        Text(
                            text = "Perro",
                            fontSize = 9.sp,
                            color = if (especie == "PERRO") AccentRed else Color.White.copy(alpha = 0.7f),
                            fontWeight = if (especie == "PERRO") FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Card(
                onClick = { onSelect("GATO") },
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (especie == "GATO") AccentRed.copy(alpha = 0.3f)
                            else FieldBg
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🐈", fontSize = 22.sp)
                        Text(
                            text = "Gato",
                            fontSize = 9.sp,
                            color = if (especie == "GATO") AccentRed else Color.White.copy(alpha = 0.7f),
                            fontWeight = if (especie == "GATO") FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        CompactChip(
            onClick = onAtras,
            label = { Text("Atrás", fontSize = 9.sp, color = Color.White) },
            colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
            modifier = Modifier.fillMaxWidth(0.35f)
        )
    }
}

// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función PasoConfirmarForm: define la lógica de esta operación
// Función PasoConfirmarForm: define la lógica de esta operación
fun PasoConfirmarForm(
    nombre: String,
    especie: String,
    raza: String,
    color: String,
    telefono: String,
    isCreating: Boolean,
    onGuardar: () -> Unit,
    onAtras: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(if (especie == "PERRO") "🐕" else "🐈", fontSize = 26.sp)
        Spacer(Modifier.height(2.dp))

        Text(
            text = nombre,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))

        // Constante detalles: valor inmutable que no cambia tras su asignación
        // Constante detalles: valor inmutable que no cambia tras su asignación
        val detalles = listOfNotNull(
            raza.ifEmpty { null },
            color.ifEmpty { null },
            telefono.ifEmpty { null }
        )
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (detalles.isNotEmpty()) {
            Text(
                text = detalles.joinToString(" - "),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 8.sp,
                maxLines = 1
            )
        }

        Spacer(Modifier.height(10.dp))

        // Constante puedeGuardar: valor inmutable que no cambia tras su asignación
        // Constante puedeGuardar: valor inmutable que no cambia tras su asignación
        val puedeGuardar = nombre.isNotBlank() && telefono.isNotBlank() && !isCreating

        Button(
            onClick = { if (puedeGuardar) onGuardar() },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(36.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (puedeGuardar) AccentRed else Color(0xFF555555)
            ),
            enabled = puedeGuardar
        ) {
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (isCreating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    indicatorColor = Color.White
                )
            } else {
                Text(
                    text = "Publicar",
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        CompactChip(
            onClick = onAtras,
            label = { Text("Atrás", fontSize = 9.sp, color = Color.White) },
            colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF3A3360)),
            modifier = Modifier.fillMaxWidth(0.35f)
        )
    }
}

// ============================================================
// ✅ TECLADO REDISEÑADO - COMPLETO Y SIN CORTES
// ============================================================

// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
// Función TecladoSimple: define la lógica de esta operación
// Función TecladoSimple: define la lógica de esta operación
fun TecladoSimple(
    valor: String,
    esNumerico: Boolean,
    onLetraClick: (String) -> Unit,
    onBorrarClick: () -> Unit,
    onCerrar: () -> Unit
) {
    // Constante keyBg: valor inmutable que no cambia tras su asignación
    // Constante keyBg: valor inmutable que no cambia tras su asignación
    val keyBg = Color(0xFF3A3360)
    // Constante displayBg: valor inmutable que no cambia tras su asignación
    // Constante displayBg: valor inmutable que no cambia tras su asignación
    val displayBg = Color(0xFF252044)
    // Constante accentRed: valor inmutable que no cambia tras su asignación
    // Constante accentRed: valor inmutable que no cambia tras su asignación
    val accentRed = Color(0xFFE85D5D)
    // Constante accentGreen: valor inmutable que no cambia tras su asignación
    // Constante accentGreen: valor inmutable que no cambia tras su asignación
    val accentGreen = Color(0xFF4CD97B)
    // Constante accentBlue: valor inmutable que no cambia tras su asignación
    // Constante accentBlue: valor inmutable que no cambia tras su asignación
    val accentBlue = Color(0xFF4D9FFF)

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(14.dp),
        onClick = { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
                .padding(horizontal = 3.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Display
            Card(
                modifier = Modifier.fillMaxWidth(0.92f),
                shape = RoundedCornerShape(8.dp),
                onClick = { }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(displayBg)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = (if (valor.isEmpty()) "" else valor.takeLast(10)) + "▏",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (esNumerico) accentGreen.copy(alpha = 0.2f) else accentBlue.copy(alpha = 0.2f))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (esNumerico) "123" else "ABC",
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (esNumerico) accentGreen else accentBlue
                        )
                    }
                }
            }

            Spacer(Modifier.height(3.dp))

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (esNumerico) {
                // Constante filas: valor inmutable que no cambia tras su asignación
                // Constante filas: valor inmutable que no cambia tras su asignación
                val filas = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                filas.forEach { fila ->
                    Row(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // Itera sobre cada elemento de la colección y ejecuta el bloque
                        // Itera sobre cada elemento de la colección y ejecuta el bloque
                        fila.forEach { num ->
                            TeclaRedondaCompacta(num, keyBg, modifier = Modifier.weight(1f)) { onLetraClick(num) }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    TeclaAccionCompacta("⌫", accentRed, modifier = Modifier.weight(1f), onClick = onBorrarClick)
                    TeclaRedondaCompacta("0", keyBg, modifier = Modifier.weight(1f)) { onLetraClick("0") }
                    TeclaAccionCompacta("✓", accentGreen, modifier = Modifier.weight(1f), onClick = onCerrar)
                }
            } else {
                // Constante filaQ: valor inmutable que no cambia tras su asignación
                // Constante filaQ: valor inmutable que no cambia tras su asignación
                val filaQ = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
                // Constante filaA: valor inmutable que no cambia tras su asignación
                // Constante filaA: valor inmutable que no cambia tras su asignación
                val filaA = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
                // Constante filaZ: valor inmutable que no cambia tras su asignación
                // Constante filaZ: valor inmutable que no cambia tras su asignación
                val filaZ = listOf("Z", "X", "C", "V", "B", "N", "M")

                TecladoFilaCompacta(filaQ, keyBg, onLetraClick)
                Spacer(Modifier.height(2.dp))
                TecladoFilaCompacta(filaA, keyBg, onLetraClick, indentFraction = 0.5f)
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(modifier = Modifier.weight(1f))
                    // Itera sobre cada elemento de la colección y ejecuta el bloque
                    // Itera sobre cada elemento de la colección y ejecuta el bloque
                    filaZ.forEach { letra ->
                        TeclaPequeñaCompacta(letra, keyBg, modifier = Modifier.weight(1f)) { onLetraClick(letra) }
                    }
                    Box(modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    TeclaAccionCompacta("⌫", accentRed, modifier = Modifier.weight(1f), onClick = onBorrarClick)
                    TeclaAccionCompacta("␣", keyBg, modifier = Modifier.weight(1f)) { onLetraClick(" ") }
                    TeclaAccionCompacta("Listo", accentGreen, modifier = Modifier.weight(2f), onClick = onCerrar)
                }
            }
        }
    }
}

// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
private fun TecladoFilaCompacta(
    letras: List<String>,
    keyBg: Color,
    onLetraClick: (String) -> Unit,
    indentFraction: Float = 0f
) {
    Row(
        modifier = Modifier.fillMaxWidth(0.92f),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (indentFraction > 0f) Box(modifier = Modifier.weight(indentFraction))
        // Itera sobre cada elemento de la colección y ejecuta el bloque
        // Itera sobre cada elemento de la colección y ejecuta el bloque
        letras.forEach { letra ->
            TeclaPequeñaCompacta(letra, keyBg, modifier = Modifier.weight(1f)) { onLetraClick(letra) }
        }
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (indentFraction > 0f) Box(modifier = Modifier.weight(indentFraction))
    }
}

// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
private fun TeclaPequeñaCompacta(
    letra: String,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(letra, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
private fun TeclaRedondaCompacta(
    numero: String,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(numero, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

// Anotación que marca esta función como una función de composición de UI
// Anotación que marca esta función como una función de composición de UI
@Composable
private fun TeclaAccionCompacta(
    label: String,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
```

## FASE 7: `com/lomito/seguro/wear/ui/selection`

### Paso 7.1: `SelectionActivity.kt`

**Actividad de selección (Wear)**. Pantalla de selección genérica usada para elegir entre opciones en el smartwatch.

```kotlin
// Paquete: com.lomito.seguro.wear.ui.selection
// Paquete: com.lomito.seguro.wear.ui.selection
package com.lomito.seguro.wear.ui.selection

// Importa la dependencia necesaria: BuildConfig
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.wear.BuildConfig
// Importa el contenedor de datos Bundle
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.LazyColumn
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.lazy.items
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.runtime.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa soporte para corrutinas de Kotlin
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.*
// Importa el parser JSON
// Importa el parser JSON
import org.json.JSONArray
// Importa la dependencia necesaria: BufferedReader
// Importa la dependencia necesaria: BufferedReader
import java.io.BufferedReader
// Importa la dependencia necesaria: InputStreamReader
// Importa la dependencia necesaria: InputStreamReader
import java.io.InputStreamReader
// Importa la dependencia necesaria: HttpURLConnection
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
// Importa la dependencia necesaria: URL
import java.net.URL

/**
 * [Modelo de datos mínimo para la pantalla de selección]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Proveer la información necesaria para mostrar la lista inicial de selección]
 */
// Clase de datos MascotaSeleccion: modelo inmutable con propiedades de dominio
// Clase de datos MascotaSeleccion: modelo inmutable con propiedades de dominio
data class MascotaSeleccion(
    // Constante id: valor inmutable que no cambia tras su asignación
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String,
    // Constante nombre: valor inmutable que no cambia tras su asignación
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String,
    // Constante especie: valor inmutable que no cambia tras su asignación
    // Constante especie: valor inmutable que no cambia tras su asignación
    val especie: String,
    // Constante fotoUrl: valor inmutable que no cambia tras su asignación
    // Constante fotoUrl: valor inmutable que no cambia tras su asignación
    val fotoUrl: String = "",
    // Constante distanciaAlerta: valor inmutable que no cambia tras su asignación
    // Constante distanciaAlerta: valor inmutable que no cambia tras su asignación
    val distanciaAlerta: Int = 50
)

/**
 * [Actividad inicial para seleccionar la mascota a monitorear]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Cargar la lista de mascotas desde el backend]
 * - [Guardar en SharedPreferences la mascota elegida y navegar a la pantalla principal]
 */
// Activity SelectionActivity: pantalla principal que gestiona el ciclo de vida
// Activity SelectionActivity: pantalla principal que gestiona el ciclo de vida
class SelectionActivity : ComponentActivity() {
    // Estado de la UI
    // Variable mascotasList: almacena el estado mutable de este componente
    // Variable mascotasList: almacena el estado mutable de este componente
    private var mascotasList = mutableStateListOf<MascotaSeleccion>()
    // Variable isLoading: almacena el estado mutable de este componente
    // Variable isLoading: almacena el estado mutable de este componente
    private var isLoading = mutableStateOf(true)
    // Variable errorMsg: almacena el estado mutable de este componente
    // Variable errorMsg: almacena el estado mutable de este componente
    private var errorMsg = mutableStateOf("")
    // Variable debugMsg: almacena el estado mutable de este componente
    // Variable debugMsg: almacena el estado mutable de este componente
    private var debugMsg = mutableStateOf("")

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)

        // Limpiar preferencias para prueba
        // Accede al almacenamiento clave-valor persistente de la aplicación
        // Accede al almacenamiento clave-valor persistente de la aplicación
        getSharedPreferences("watch_prefs", MODE_PRIVATE).edit().clear().apply()

        // Cargar datos
        cargarMascotas()

        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            Scaffold(
                timeText = { TimeText() },
                vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A2E))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🐾 Selecciona tu mascota",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Debug info
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (debugMsg.value.isNotEmpty()) {
                        Text(
                            text = debugMsg.value,
                            color = Color(0xFF2196F3),
                            fontSize = 9.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Error
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (errorMsg.value.isNotEmpty() && !isLoading.value) {
                        Text(
                            text = errorMsg.value,
                            color = Color(0xFFFF9800),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Contenido
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (isLoading.value) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp,
                                    indicatorColor = Color(0xFF4CAF50)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Cargando mascotas...",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else if (mascotasList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No hay mascotas",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                CompactButton(
                                    onClick = {
                                        isLoading.value = true
                                        errorMsg.value = ""
                                        debugMsg.value = ""
                                        cargarMascotas()
                                    },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2196F3))
                                ) {
                                    Text("🔄 Reintentar", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        LazyColumn {
                            items(mascotasList) { mascota ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        // Guardar selección
                                        // Accede al almacenamiento clave-valor persistente de la aplicación
                                        // Accede al almacenamiento clave-valor persistente de la aplicación
                                        getSharedPreferences("watch_prefs", MODE_PRIVATE)
                                            .edit()
                                            .putString("mascota_activa_id", mascota.id)
                                            .putString("mascota_activa_nombre", mascota.nombre)
                                            .putInt("mascota_umbral", mascota.distanciaAlerta)
                                            // Aplica los cambios de forma asíncrona en el hilo principal
                                            // Aplica los cambios de forma asíncrona en el hilo principal
                                            .apply()

                                        // Constante intent: valor inmutable que no cambia tras su asignación
                                        // Constante intent: valor inmutable que no cambia tras su asignación
                                        val intent = android.content.Intent(
                                            this@SelectionActivity,
                                            com.lomito.seguro.wear.ui.home.WearMainActivity::class.java
                                        )
                                        intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                        startActivity(intent)
                                        finish()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${mascota.especie} ${mascota.nombre}",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Umbral: ${mascota.distanciaAlerta}m",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 11.sp
                                            )
                                        }
                                        Text(
                                            text = "→",
                                            color = Color(0xFF4CAF50),
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun cargarMascotas() {
        // Ejecutar en un hilo separado
        Thread {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                runOnUiThread {
                    isLoading.value = true
                    debugMsg.value = "Conectando a $backendUrl..."
                }

                // Constante userId: valor inmutable que no cambia tras su asignación
                // Constante userId: valor inmutable que no cambia tras su asignación
                val userId = "2"  // ✅ Usuario con mascotas
                // Constante url: valor inmutable que no cambia tras su asignación
                // Constante url: valor inmutable que no cambia tras su asignación
                val url = URL("$backendUrl/api/mascotas?ownerId=$userId")

                runOnUiThread { debugMsg.value = "Conectando a $url..." }

                // Constante conn: valor inmutable que no cambia tras su asignación
                // Constante conn: valor inmutable que no cambia tras su asignación
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/json")

                // Constante responseCode: valor inmutable que no cambia tras su asignación
                // Constante responseCode: valor inmutable que no cambia tras su asignación
                val responseCode = conn.responseCode

                runOnUiThread { debugMsg.value = "Response Code: $responseCode" }

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Constante reader: valor inmutable que no cambia tras su asignación
                    // Constante reader: valor inmutable que no cambia tras su asignación
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    // Constante response: valor inmutable que no cambia tras su asignación
                    // Constante response: valor inmutable que no cambia tras su asignación
                    val response = StringBuilder()
                    // Variable line: almacena el estado mutable de este componente
                    // Variable line: almacena el estado mutable de este componente
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // Constante responseText: valor inmutable que no cambia tras su asignación
                    // Constante responseText: valor inmutable que no cambia tras su asignación
                    val responseText = response.toString()
                    runOnUiThread { debugMsg.value = "Respuesta: ${responseText.take(100)}..." }

                    // Parsear JSON
                    // Constante jsonArray: valor inmutable que no cambia tras su asignación
                    // Constante jsonArray: valor inmutable que no cambia tras su asignación
                    val jsonArray = JSONArray(responseText)
                    // Constante lista: valor inmutable que no cambia tras su asignación
                    // Constante lista: valor inmutable que no cambia tras su asignación
                    val lista = mutableListOf<MascotaSeleccion>()

                    // Itera sobre la colección para procesar cada elemento
                    // Itera sobre la colección para procesar cada elemento
                    for (i in 0 until jsonArray.length()) {
                        // Constante obj: valor inmutable que no cambia tras su asignación
                        // Constante obj: valor inmutable que no cambia tras su asignación
                        val obj = jsonArray.getJSONObject(i)
                        lista.add(
                            MascotaSeleccion(
                                id = obj.getString("id"),
                                nombre = obj.getString("nombre"),
                                especie = obj.getString("especie"),
                                distanciaAlerta = obj.getInt("distancia_alerta")
                            )
                        )
                    }

                    conn.disconnect()

                    runOnUiThread {
                        mascotasList.clear()
                        mascotasList.addAll(lista)
                        isLoading.value = false
                        debugMsg.value = "✅ ${lista.size} mascotas cargadas"
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                        if (lista.isEmpty()) {
                            errorMsg.value = "No hay mascotas para este usuario"
                        } else {
                            errorMsg.value = ""
                        }
                    }
                } else {
                    // Constante errorReader: valor inmutable que no cambia tras su asignación
                    // Constante errorReader: valor inmutable que no cambia tras su asignación
                    val errorReader = BufferedReader(InputStreamReader(conn.errorStream))
                    // Constante errorResponse: valor inmutable que no cambia tras su asignación
                    // Constante errorResponse: valor inmutable que no cambia tras su asignación
                    val errorResponse = StringBuilder()
                    // Variable line: almacena el estado mutable de este componente
                    // Variable line: almacena el estado mutable de este componente
                    var line: String?
                    while (errorReader.readLine().also { line = it } != null) {
                        errorResponse.append(line)
                    }
                    errorReader.close()
                    conn.disconnect()

                    runOnUiThread {
                        isLoading.value = false
                        errorMsg.value = "Error HTTP $responseCode"
                        debugMsg.value = errorResponse.toString().take(100)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    isLoading.value = false
                    errorMsg.value = "Error: ${e.message}"
                    debugMsg.value = e.stackTraceToString().take(200)
                }
                e.printStackTrace()
            }
        }.start()
    }

    companion object {
        // Constante backendUrl: valor inmutable que no cambia tras su asignación
        // Constante backendUrl: valor inmutable que no cambia tras su asignación
        private val backendUrl = BuildConfig.BACKEND_URL
    }
}
```

## FASE 8: `com/lomito/seguro/wear/ui/settings`

### Paso 8.1: `SettingsActivity.kt`

**Actividad de configuración (Wear)**. Permite configurar el umbral de distancia de alerta y otras preferencias del smartwatch.

```kotlin
// Paquete: com.lomito.seguro.wear.ui.settings
// Paquete: com.lomito.seguro.wear.ui.settings
package com.lomito.seguro.wear.ui.settings
// Importa la dependencia necesaria: BuildConfig
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.wear.BuildConfig

// Importa el contenedor de datos Bundle
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: ComponentActivity
// Importa la dependencia necesaria: ComponentActivity
import androidx.activity.ComponentActivity
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.activity.compose.setContent
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.background
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.clickable
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.layout.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.CircleShape
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.foundation.shape.RoundedCornerShape
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.runtime.*
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Alignment
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.Modifier
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.draw.clip
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.graphics.Color
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.font.FontWeight
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.text.style.TextAlign
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.dp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.compose.ui.unit.sp
// Importa componente de Jetpack Compose
// Importa componente de Jetpack Compose
import androidx.wear.compose.material.*
// Importa soporte para corrutinas de Kotlin
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.*
// Importa el parser JSON
// Importa el parser JSON
import org.json.JSONArray
// Importa el parser JSON
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: HttpURLConnection
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
// Importa la dependencia necesaria: URL
import java.net.URL

/**
 * [Modelo para representar los ajustes de una mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mantener el estado de la configuración (ej. distancia de alerta)]
 */
// Clase de datos MascotaSetting: modelo inmutable con propiedades de dominio
// Clase de datos MascotaSetting: modelo inmutable con propiedades de dominio
data class MascotaSetting(
    // Constante id: valor inmutable que no cambia tras su asignación
    // Constante id: valor inmutable que no cambia tras su asignación
    val id: String,
    // Constante nombre: valor inmutable que no cambia tras su asignación
    // Constante nombre: valor inmutable que no cambia tras su asignación
    val nombre: String,
    // Constante especie: valor inmutable que no cambia tras su asignación
    // Constante especie: valor inmutable que no cambia tras su asignación
    val especie: String,
    // Variable distanciaAlerta: almacena el estado mutable de este componente
    // Variable distanciaAlerta: almacena el estado mutable de este componente
    var distanciaAlerta: Int = 50,
    // Constante ownerId: valor inmutable que no cambia tras su asignación
    // Constante ownerId: valor inmutable que no cambia tras su asignación
    val ownerId: Int = 0
)

/**
 * [Actividad para la configuración de preferencias en el reloj]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Cargar la lista de mascotas para cambiar su configuración individualmente]
 * - [Actualizar el umbral de distancia y la preferencia de vibración]
 */
// Activity SettingsActivity: pantalla principal que gestiona el ciclo de vida
// Activity SettingsActivity: pantalla principal que gestiona el ciclo de vida
class SettingsActivity : ComponentActivity() {
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    // Constante backendUrl: valor inmutable que no cambia tras su asignación
    private val backendUrl = BuildConfig.BACKEND_URL
    // Constante userId: valor inmutable que no cambia tras su asignación
    // Constante userId: valor inmutable que no cambia tras su asignación
    private val userId = 2 // Usuario fijo por ahora

    // Método del ciclo de vida: inicializa la actividad y configura la UI
    // Método del ciclo de vida: inicializa la actividad y configura la UI
    override fun onCreate(savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        // Invoca la implementación del método en la clase padre
        super.onCreate(savedInstanceState)
        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        // Define el árbol de UI con Jetpack Compose como contenido de la Activity
        setContent {
            SettingsScreen()
        }
    }

    // Anotación que marca esta función como una función de composición de UI
    // Anotación que marca esta función como una función de composición de UI
    @Composable
    // Función SettingsScreen: define la lógica de esta operación
    // Función SettingsScreen: define la lógica de esta operación
    fun SettingsScreen() {
        // Constante prefs: valor inmutable que no cambia tras su asignación
        // Constante prefs: valor inmutable que no cambia tras su asignación
        val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
        // Constante listState: valor inmutable que no cambia tras su asignación
        // Constante listState: valor inmutable que no cambia tras su asignación
        val listState = rememberScalingLazyListState()

        // Estados
        // Variable mascotas: almacena el estado mutable de este componente
        // Variable mascotas: almacena el estado mutable de este componente
        var mascotas by remember { mutableStateOf<List<MascotaSetting>>(emptyList()) }
        // Variable isLoading: almacena el estado mutable de este componente
        // Variable isLoading: almacena el estado mutable de este componente
        var isLoading by remember { mutableStateOf(true) }
        // Variable isSaving: almacena el estado mutable de este componente
        // Variable isSaving: almacena el estado mutable de este componente
        var isSaving by remember { mutableStateOf(false) }
        // Variable errorMessage: almacena el estado mutable de este componente
        // Variable errorMessage: almacena el estado mutable de este componente
        var errorMessage by remember { mutableStateOf("") }
        // Variable successMessage: almacena el estado mutable de este componente
        // Variable successMessage: almacena el estado mutable de este componente
        var successMessage by remember { mutableStateOf("") }
        // Variable mascotaSeleccionada: almacena el estado mutable de este componente
        // Variable mascotaSeleccionada: almacena el estado mutable de este componente
        var mascotaSeleccionada by remember {
            mutableStateOf<MascotaSetting?>(null)
        }
        // Variable umbralActual: almacena el estado mutable de este componente
        // Variable umbralActual: almacena el estado mutable de este componente
        var umbralActual by remember { mutableStateOf(50) }
        // Variable vibracionActual: almacena el estado mutable de este componente
        // Variable vibracionActual: almacena el estado mutable de este componente
        var vibracionActual by remember { mutableStateOf(prefs.getBoolean("vibracion", true)) }

        // Cargar mascotas del usuario al inicio
        LaunchedEffect(Unit) {
            isLoading = true
            // Constante result: valor inmutable que no cambia tras su asignación
            // Constante result: valor inmutable que no cambia tras su asignación
            val result = withContext(Dispatchers.IO) {
                cargarMascotasDelUsuario(userId)
            }
            mascotas = result.mascotas
            isLoading = false
            errorMessage = result.errorMessage

            // Si hay mascotas, seleccionar la primera o la guardada
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (mascotas.isNotEmpty()) {
                // Constante savedId: valor inmutable que no cambia tras su asignación
                // Constante savedId: valor inmutable que no cambia tras su asignación
                val savedId = prefs.getString("mascota_seleccionada_id", "") ?: ""
                mascotaSeleccionada = if (savedId.isNotEmpty()) {
                    mascotas.find { it.id == savedId }
                } else {
                    mascotas.firstOrNull()
                }
                // Actualizar umbral actual
                umbralActual = mascotaSeleccionada?.distanciaAlerta ?: 50
            }
        }

        // Actualizar umbral cuando cambia la mascota seleccionada
        LaunchedEffect(mascotaSeleccionada) {
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (mascotaSeleccionada != null) {
                umbralActual = mascotaSeleccionada!!.distanciaAlerta
            }
        }

        Scaffold(
            timeText = { TimeText() },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
        ) {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A2E)),
                contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp, start = 6.dp, end = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ✅ Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚙️ Configuración",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ✅ Botón Guardar
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                            if (!isLoading && mascotas.isNotEmpty()) {
                                CompactButton(
                                    onClick = {
                                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                        if (mascotaSeleccionada != null) {
                                            guardarCambios(
                                                mascotaId = mascotaSeleccionada!!.id,
                                                nuevaDistancia = umbralActual,
                                                vibracion = vibracionActual,
                                                onSuccess = {
                                                    successMessage = "✅ Configuración guardada"
                                                    errorMessage = ""
                                                    // Actualizar la distancia en la lista local
                                                    mascotas = mascotas.map {
                                                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                                        if (it.id == mascotaSeleccionada!!.id) {
                                                            it.copy(distanciaAlerta = umbralActual)
                                                        } else {
                                                            it
                                                        }
                                                    }
                                                    mascotaSeleccionada = mascotaSeleccionada?.copy(distanciaAlerta = umbralActual)
                                                },
                                                onError = { msg ->
                                                    errorMessage = msg
                                                    successMessage = ""
                                                }
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = if (isSaving) Color(0xFF666666) else Color(0xFF4CAF50)
                                    ),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape),
                                    enabled = !isSaving
                                ) {
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    if (isSaving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            indicatorColor = Color.White
                                        )
                                    } else {
                                        Text("💾", fontSize = 14.sp)
                                    }
                                }
                            }
                            // ✅ Botón cerrar
                            CompactButton(
                                onClick = { finish() },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF666666)),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("✕", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }

                // ✅ Mensajes de estado
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (successMessage.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                                .clip(RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = successMessage,
                                color = Color(0xFF4CAF50),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (errorMessage.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE85D5D).copy(alpha = 0.15f))
                                .clip(RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = "⚠️ $errorMessage",
                                color = Color(0xFFE85D5D),
                                fontSize = 8.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ✅ Estado de carga
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    indicatorColor = Color(0xFF4CAF50)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Cargando...",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
                // ✅ Error de carga
                else if (errorMessage.isNotEmpty() && mascotas.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚠️ $errorMessage",
                                color = Color(0xFFFF9800),
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                // ✅ Sin mascotas
                else if (mascotas.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "📭 Sin mascotas",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Agrega una mascota primero",
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }
                // ✅ Contenido
                else {
                    // ✅ Selector de mascota
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2C2C3E))
                                .clip(RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Seleccionar mascota",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 9.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            // Grid de mascotas en 2 columnas
                            // Itera sobre cada elemento de la colección y ejecuta el bloque
                            // Itera sobre cada elemento de la colección y ejecuta el bloque
                            mascotas.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Itera sobre cada elemento de la colección y ejecuta el bloque
                                    // Itera sobre cada elemento de la colección y ejecuta el bloque
                                    pair.forEach { mascota ->
                                        // Constante isSelected: valor inmutable que no cambia tras su asignación
                                        // Constante isSelected: valor inmutable que no cambia tras su asignación
                                        val isSelected = mascotaSeleccionada?.id == mascota.id
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            onClick = {
                                                mascotaSeleccionada = mascota
                                                // Inicia el editor para modificar los SharedPreferences
                                                // Inicia el editor para modificar los SharedPreferences
                                                prefs.edit().putString("mascota_seleccionada_id", mascota.id).apply()
                                                // Limpiar mensajes al cambiar de mascota
                                                successMessage = ""
                                                errorMessage = ""
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                                        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                                        if (isSelected) Color(0xFF4CAF50).copy(alpha = 0.3f)
                                                        else Color(0xFF3D3D5C)
                                                    )
                                                    .padding(horizontal = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = if (mascota.especie == "PERRO") "🐕" else "🐈",
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = mascota.nombre.take(6),
                                                    color = if (isSelected) Color(0xFF4CAF50) else Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1
                                                )
                                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                                if (isSelected) {
                                                    Text(
                                                        text = "✓",
                                                        color = Color(0xFF4CAF50),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }

                    // ✅ Umbral de alerta
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (mascotaSeleccionada != null) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2C2C3E))
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📏 Distancia alerta",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 9.sp
                                    )
                                    Text(
                                        text = "$umbralActual m",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CompactButton(
                                        onClick = {
                                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                            if (umbralActual > 10) {
                                                umbralActual -= 10
                                                successMessage = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF666666)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("−10", fontSize = 11.sp, color = Color.White)
                                    }
                                    CompactButton(
                                        onClick = {
                                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                            if (umbralActual < 100) {
                                                umbralActual += 10
                                                successMessage = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF666666)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("+10", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // ✅ Vibración
                    item {
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2C2C3E))
                                .clip(RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "📳 Vibración",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (vibracionActual) "Activada" else "Desactivada",
                                    color = if (vibracionActual) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    fontSize = 8.sp
                                )
                            }
                            CompactButton(
                                onClick = {
                                    vibracionActual = !vibracionActual
                                    successMessage = ""
                                },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (vibracionActual) Color(0xFF4CAF50) else Color(0xFF666666)
                                ),
                                modifier = Modifier.size(width = 50.dp, height = 28.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                                    if (vibracionActual) "ON" else "OFF",
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Espacio al final
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    private fun guardarCambios(
        mascotaId: String,
        nuevaDistancia: Int,
        vibracion: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        CoroutineScope(Dispatchers.Main).launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // 1. Guardar vibración en SharedPreferences
                // Constante prefs: valor inmutable que no cambia tras su asignación
                // Constante prefs: valor inmutable que no cambia tras su asignación
                val prefs = getSharedPreferences("watch_prefs", MODE_PRIVATE)
                // Inicia el editor para modificar los SharedPreferences
                // Inicia el editor para modificar los SharedPreferences
                prefs.edit().putBoolean("vibracion", vibracion).apply()
                // Inicia el editor para modificar los SharedPreferences
                // Inicia el editor para modificar los SharedPreferences
                prefs.edit().putInt("umbral_$mascotaId", nuevaDistancia).apply()

                // 2. Actualizar la distancia en el backend
                // Constante result: valor inmutable que no cambia tras su asignación
                // Constante result: valor inmutable que no cambia tras su asignación
                val result = withContext(Dispatchers.IO) {
                    actualizarDistanciaMascota(mascotaId, nuevaDistancia)
                }

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (result.success) {
                    onSuccess()
                } else {
                    onError(result.errorMessage ?: "Error al guardar")
                }
            } catch (e: Exception) {
                onError("Error: ${e.message}")
            }
        }
    }

    private suspend fun actualizarDistanciaMascota(
        mascotaId: String,
        nuevaDistancia: Int
    ): OperacionResult {
        // Retorna el valor al llamador de la función
        // Retorna el valor al llamador de la función
        return try {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("SETTINGS", "Actualizando distancia de mascota $mascotaId a $nuevaDistancia")

            // Constante url: valor inmutable que no cambia tras su asignación
            // Constante url: valor inmutable que no cambia tras su asignación
            val url = URL("$backendUrl/api/mascotas/$mascotaId")
            // Constante conn: valor inmutable que no cambia tras su asignación
            // Constante conn: valor inmutable que no cambia tras su asignación
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            // Constante json: valor inmutable que no cambia tras su asignación
            // Constante json: valor inmutable que no cambia tras su asignación
            val json = JSONObject().apply {
                put("distancia_alerta", nuevaDistancia)
            }

            conn.outputStream.write(json.toString().toByteArray())
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            val responseCode = conn.responseCode
            // Constante responseBody: valor inmutable que no cambia tras su asignación
            // Constante responseBody: valor inmutable que no cambia tras su asignación
            val responseBody = if (responseCode == 200 || responseCode == 201) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText()
            }
            conn.disconnect()

            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("SETTINGS", "Response Code: $responseCode")
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("SETTINGS", "Response: $responseBody")

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (responseCode == 200 || responseCode == 201) {
                OperacionResult(true, "")
            } else {
                OperacionResult(false, "Error al actualizar (HTTP $responseCode)")
            }
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("SETTINGS", "Error: ${e.message}", e)
            OperacionResult(false, "Error: ${e.message}")
        }
    }

    private suspend fun cargarMascotasDelUsuario(ownerId: Int): CargaMascotasResult {
        // Retorna el valor al llamador de la función
        // Retorna el valor al llamador de la función
        return try {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("SETTINGS", "Cargando mascotas del usuario $ownerId")

            // Constante url: valor inmutable que no cambia tras su asignación
            // Constante url: valor inmutable que no cambia tras su asignación
            val url = URL("$backendUrl/api/mascotas?ownerId=$ownerId")
            // Constante conn: valor inmutable que no cambia tras su asignación
            // Constante conn: valor inmutable que no cambia tras su asignación
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            // Constante responseCode: valor inmutable que no cambia tras su asignación
            val responseCode = conn.responseCode

            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.d("SETTINGS", "Response Code: $responseCode")

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Constante response: valor inmutable que no cambia tras su asignación
                // Constante response: valor inmutable que no cambia tras su asignación
                val response = conn.inputStream.bufferedReader().readText()
                // Registro de evento en el log de Android para depuración
                // Registro de evento en el log de Android para depuración
                android.util.Log.d("SETTINGS", "Respuesta: $response")

                // Constante jsonArray: valor inmutable que no cambia tras su asignación
                // Constante jsonArray: valor inmutable que no cambia tras su asignación
                val jsonArray = JSONArray(response)
                // Constante lista: valor inmutable que no cambia tras su asignación
                // Constante lista: valor inmutable que no cambia tras su asignación
                val lista = mutableListOf<MascotaSetting>()

                // Itera sobre la colección para procesar cada elemento
                // Itera sobre la colección para procesar cada elemento
                for (i in 0 until jsonArray.length()) {
                    // Constante obj: valor inmutable que no cambia tras su asignación
                    // Constante obj: valor inmutable que no cambia tras su asignación
                    val obj = jsonArray.getJSONObject(i)
                    lista.add(
                        MascotaSetting(
                            id = obj.getString("id"),
                            nombre = obj.getString("nombre"),
                            especie = obj.getString("especie"),
                            distanciaAlerta = obj.optInt("distancia_alerta", 50),
                            ownerId = obj.optInt("owner_id", 0)
                        )
                    )
                }
                conn.disconnect()
                // Registro de evento en el log de Android para depuración
                // Registro de evento en el log de Android para depuración
                android.util.Log.d("SETTINGS", "${lista.size} mascotas cargadas")
                CargaMascotasResult(lista, "")
            } else {
                // Constante errorBody: valor inmutable que no cambia tras su asignación
                // Constante errorBody: valor inmutable que no cambia tras su asignación
                val errorBody = conn.errorStream?.bufferedReader()?.readText()
                conn.disconnect()
                // Registro de evento en el log de Android para depuración
                // Registro de evento en el log de Android para depuración
                android.util.Log.e("SETTINGS", "Error HTTP $responseCode: $errorBody")
                CargaMascotasResult(emptyList(), "Error al cargar (HTTP $responseCode)")
            }
        } catch (e: Exception) {
            // Registro de evento en el log de Android para depuración
            // Registro de evento en el log de Android para depuración
            android.util.Log.e("SETTINGS", "Error: ${e.message}", e)
            CargaMascotasResult(emptyList(), "Error: ${e.message}")
        }
    }

    // Clase de datos CargaMascotasResult: modelo inmutable con propiedades de dominio
    // Clase de datos CargaMascotasResult: modelo inmutable con propiedades de dominio
    data class CargaMascotasResult(
        // Constante mascotas: valor inmutable que no cambia tras su asignación
        // Constante mascotas: valor inmutable que no cambia tras su asignación
        val mascotas: List<MascotaSetting>,
        // Constante errorMessage: valor inmutable que no cambia tras su asignación
        // Constante errorMessage: valor inmutable que no cambia tras su asignación
        val errorMessage: String
    )

    // Clase de datos OperacionResult: modelo inmutable con propiedades de dominio
    // Clase de datos OperacionResult: modelo inmutable con propiedades de dominio
    data class OperacionResult(
        // Constante success: valor inmutable que no cambia tras su asignación
        // Constante success: valor inmutable que no cambia tras su asignación
        val success: Boolean,
        // Constante errorMessage: valor inmutable que no cambia tras su asignación
        // Constante errorMessage: valor inmutable que no cambia tras su asignación
        val errorMessage: String?
    )
}
```
