package com.lomito.seguro.ui.alertas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lomito.seguro.models.Alerta
import com.lomito.seguro.repository.AlertasRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlertasViewModel : ViewModel() {
    private val repository = AlertasRepository()

    private val _alertas = MutableLiveData<List<Alerta>>(emptyList())
    val alertas: LiveData<List<Alerta>> = _alertas

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _errorMessage = MutableLiveData("")
    val errorMessage: LiveData<String> = _errorMessage

    fun cargarAlertas(ownerId: Int) {
        _loading.value = true
        viewModelScope.launch {
            try {
                // ✅ Forzar la ejecución en un hilo de IO
                val result = withContext(Dispatchers.IO) {
                    repository.getAlertas(ownerId)
                }
                if (result.success) {
                    _alertas.value = result.alertas
                    _errorMessage.value = ""
                } else {
                    _errorMessage.value = result.error ?: "Error al cargar alertas"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    suspend fun marcarComoLeida(alertaId: Int): Boolean {
        return try {
            val result = withContext(Dispatchers.IO) {
                repository.marcarComoLeida(alertaId)
            }
            if (result.success) {
                // Actualizar la lista localmente
                val alertasActualizadas = _alertas.value?.map { alerta ->
                    if (alerta.id == alertaId) {
                        alerta.copy(leida = true)
                    } else {
                        alerta
                    }
                }
                _alertas.value = alertasActualizadas
                true
            } else {
                _errorMessage.value = result.error ?: "Error al marcar como leída"
                false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error: ${e.message}"
            false
        }
    }

    suspend fun marcarTodasComoLeidas(ownerId: Int): Boolean {
        return try {
            val result = withContext(Dispatchers.IO) {
                repository.marcarTodasComoLeidas(ownerId)
            }
            if (result.success) {
                // Actualizar la lista localmente
                val alertasActualizadas = _alertas.value?.map { alerta ->
                    alerta.copy(leida = true)
                }
                _alertas.value = alertasActualizadas
                true
            } else {
                _errorMessage.value = result.error ?: "Error al marcar todas como leídas"
                false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error: ${e.message}"
            false
        }
    }
}