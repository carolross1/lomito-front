package com.lomito.seguro.ui.mascota

import androidx.lifecycle.*
import com.lomito.seguro.data.model.CreateMascotaRequest
import com.lomito.seguro.data.model.Mascota
import com.lomito.seguro.data.model.ReporteVista
import com.lomito.seguro.data.repository.LomitoRepository
import kotlinx.coroutines.launch

class MascotaViewModel : ViewModel() {
    private val repo = LomitoRepository()

    private val _mascota = MutableLiveData<Mascota?>()
    val mascota: LiveData<Mascota?> = _mascota

    private val _reportes = MutableLiveData<List<ReporteVista>>()
    val reportes: LiveData<List<ReporteVista>> = _reportes

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun cargarMascota(id: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val resp = repo.getMascotaById(id)
                if (resp.isSuccessful) _mascota.value = resp.body()
                val rResp = repo.getUltimoReporte(id)
                if (rResp.isSuccessful) _reportes.value = listOfNotNull(rResp.body())
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun reportarVista(mascotaId: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                val resp = repo.reportarVista(mascotaId, lat, lng, "Ubicación reportada desde Watch")
                if (resp.isSuccessful) _message.value = "✅ Vista reportada exitosamente"
                else _message.value = "Error al reportar vista"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }

    fun crearMascota(request: CreateMascotaRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val resp = repo.createMascota(request)
                if (resp.isSuccessful) {
                    _message.value = "✅ Mascota registrada"
                    onSuccess()
                } else {
                    _message.value = "Error al crear mascota"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun eliminarMascota(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val resp = repo.deleteMascota(id)
                if (resp.isSuccessful) onSuccess()
                else _message.value = "Error al eliminar"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }
}
