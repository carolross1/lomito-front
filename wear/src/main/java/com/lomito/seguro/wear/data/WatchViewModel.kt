// wear/data/WatchViewModel.kt
package com.lomito.seguro.wear.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
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
data class BleState(
    val distancia: Int = 0,
    val mascotaId: String = "",
    val umbral: Int = 50,
    val superaUmbral: Boolean = false
)

/**
 * [ViewModel principal para la sincronización con el reloj]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Mantener el estado de BLE y actualizar la UI de manera reactiva]
 */
class WatchViewModel(app: Application) : AndroidViewModel(app) {
    private val _bleState = MutableLiveData(BleState())
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
    fun actualizarEstado(distancia: Int, mascotaId: String, umbral: Int, superaUmbral: Boolean) {
        android.util.Log.d("WATCH_VM", "Actualizando: distancia=$distancia, mascotaId=$mascotaId, umbral=$umbral")
        _bleState.postValue(BleState(distancia, mascotaId, umbral, superaUmbral))
    }
}