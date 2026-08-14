// wear/data/WatchViewModel.kt
// Paquete: com.lomito.seguro.wear.data
package com.lomito.seguro.wear.data

// Importa la dependencia necesaria: Application
import android.app.Application
// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.AndroidViewModel
// Importa el observable de datos reactivos
import androidx.lifecycle.LiveData
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
data class BleState(
    // Constante distancia: valor inmutable que no cambia tras su asignación
    val distancia: Int = 0,
    // Constante mascotaId: valor inmutable que no cambia tras su asignación
    val mascotaId: String = "",
    // Constante umbral: valor inmutable que no cambia tras su asignación
    val umbral: Int = 50,
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
class WatchViewModel(app: Application) : AndroidViewModel(app) {
    // Constante _bleState: valor inmutable que no cambia tras su asignación
    private val _bleState = MutableLiveData(BleState())
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
    fun actualizarEstado(distancia: Int, mascotaId: String, umbral: Int, superaUmbral: Boolean) {
        // Registro de evento en el log de Android para depuración
        android.util.Log.d("WATCH_VM", "Actualizando: distancia=$distancia, mascotaId=$mascotaId, umbral=$umbral")
        _bleState.postValue(BleState(distancia, mascotaId, umbral, superaUmbral))
    }
}