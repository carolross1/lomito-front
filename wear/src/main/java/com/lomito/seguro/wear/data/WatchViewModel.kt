// wear/data/WatchViewModel.kt
package com.lomito.seguro.wear.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

data class BleState(
    val distancia: Int = 0,
    val mascotaId: String = "",
    val umbral: Int = 50,
    val superaUmbral: Boolean = false
)

class WatchViewModel(app: Application) : AndroidViewModel(app) {
    private val _bleState = MutableLiveData(BleState())
    val bleState: LiveData<BleState> = _bleState

    fun actualizarEstado(distancia: Int, mascotaId: String, umbral: Int, superaUmbral: Boolean) {
        android.util.Log.d("WATCH_VM", "Actualizando: distancia=$distancia, mascotaId=$mascotaId, umbral=$umbral")
        _bleState.postValue(BleState(distancia, mascotaId, umbral, superaUmbral))
    }
}