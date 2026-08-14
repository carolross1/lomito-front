// Paquete: com.lomito.seguro.ui.alertas
package com.lomito.seguro.ui.alertas

// Importa el observable de datos reactivos
import androidx.lifecycle.LiveData
// Importa el observable de datos reactivos
import androidx.lifecycle.MutableLiveData
// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModel
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa la dependencia necesaria: Alerta
import com.lomito.seguro.models.Alerta
// Importa la dependencia necesaria: AlertasRepository
import com.lomito.seguro.repository.AlertasRepository
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.Dispatchers
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.withContext

// ViewModel AlertasViewModel: gestiona el estado y la lógica de negocio de la pantalla
class AlertasViewModel : ViewModel() {
    // Constante repository: valor inmutable que no cambia tras su asignación
    private val repository = AlertasRepository()

    // Constante _alertas: valor inmutable que no cambia tras su asignación
    private val _alertas = MutableLiveData<List<Alerta>>(emptyList())
    // Constante alertas: valor inmutable que no cambia tras su asignación
    val alertas: LiveData<List<Alerta>> = _alertas

    // Constante _loading: valor inmutable que no cambia tras su asignación
    private val _loading = MutableLiveData(false)
    // Constante loading: valor inmutable que no cambia tras su asignación
    val loading: LiveData<Boolean> = _loading

    // Constante _errorMessage: valor inmutable que no cambia tras su asignación
    private val _errorMessage = MutableLiveData("")
    // Constante errorMessage: valor inmutable que no cambia tras su asignación
    val errorMessage: LiveData<String> = _errorMessage

    // Función cargarAlertas: define la lógica de esta operación
    fun cargarAlertas(ownerId: Int) {
        _loading.value = true
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // ✅ Forzar la ejecución en un hilo de IO
                // Constante result: valor inmutable que no cambia tras su asignación
                val result = withContext(Dispatchers.IO) {
                    repository.getAlertas(ownerId)
                }
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
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
        // Retorna el valor al llamador de la función
        return try {
            // Constante result: valor inmutable que no cambia tras su asignación
            val result = withContext(Dispatchers.IO) {
                repository.marcarComoLeida(alertaId)
            }
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (result.success) {
                // Actualizar la lista localmente
                // Constante alertasActualizadas: valor inmutable que no cambia tras su asignación
                val alertasActualizadas = _alertas.value?.map { alerta ->
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (alerta.id == alertaId) {
                        alerta.copy(leida = true)
                    } else {
                        alerta
                    }
                }
                _alertas.value = alertasActualizadas ?: emptyList()
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
        // Retorna el valor al llamador de la función
        return try {
            // Constante result: valor inmutable que no cambia tras su asignación
            val result = withContext(Dispatchers.IO) {
                repository.marcarTodasComoLeidas(ownerId)
            }
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (result.success) {
                // Actualizar la lista localmente
                // Constante alertasActualizadas: valor inmutable que no cambia tras su asignación
                val alertasActualizadas = _alertas.value?.map { alerta ->
                    alerta.copy(leida = true)
                }
                _alertas.value = alertasActualizadas ?: emptyList()
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