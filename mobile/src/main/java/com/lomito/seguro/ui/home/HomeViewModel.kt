// Paquete: com.lomito.seguro.ui.home
package com.lomito.seguro.ui.home

// Importa el observable de datos reactivos
import androidx.lifecycle.LiveData
// Importa el observable de datos reactivos
import androidx.lifecycle.MutableLiveData
// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModel
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa la dependencia necesaria: Alerta
import com.lomito.seguro.data.model.Alerta
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.data.model.Mascota
// Importa la dependencia necesaria: LomitoRepository
import com.lomito.seguro.data.repository.LomitoRepository
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch

// ViewModel HomeViewModel: gestiona el estado y la lógica de negocio de la pantalla
class HomeViewModel : ViewModel() {
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo = LomitoRepository()

    // Constante _mascotas: valor inmutable que no cambia tras su asignación
    private val _mascotas = MutableLiveData<List<Mascota>>()
    // Constante mascotas: valor inmutable que no cambia tras su asignación
    val mascotas: LiveData<List<Mascota>> = _mascotas

    // Constante _alertasNoLeidas: valor inmutable que no cambia tras su asignación
    private val _alertasNoLeidas = MutableLiveData<Int>()
    // Constante alertasNoLeidas: valor inmutable que no cambia tras su asignación
    val alertasNoLeidas: LiveData<Int> = _alertasNoLeidas

    // Constante _loading: valor inmutable que no cambia tras su asignación
    private val _loading = MutableLiveData<Boolean>()
    // Constante loading: valor inmutable que no cambia tras su asignación
    val loading: LiveData<Boolean> = _loading

    // Función cargar: define la lógica de esta operación
    fun cargar(ownerId: String) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _loading.value = true
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante mResp: valor inmutable que no cambia tras su asignación
                val mResp = repo.getMascotas(ownerId)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (mResp.isSuccessful) _mascotas.value = mResp.body() ?: emptyList()

                // Constante aResp: valor inmutable que no cambia tras su asignación
                val aResp = repo.getAlertasNoLeidas(ownerId)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (aResp.isSuccessful) _alertasNoLeidas.value = aResp.body()?.size ?: 0
            } catch (e: Exception) {
                _mascotas.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}
