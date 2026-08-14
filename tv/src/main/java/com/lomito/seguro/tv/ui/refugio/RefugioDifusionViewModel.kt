// Paquete: com.lomito.seguro.tv.ui.refugio
package com.lomito.seguro.tv.ui.refugio

// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModel
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa la dependencia necesaria: Refugio
import com.lomito.seguro.tv.data.model.Refugio
// Importa la dependencia necesaria: LomitoTvRepository
import com.lomito.seguro.tv.data.repository.LomitoTvRepository
// Importa el observable de datos reactivos
import kotlinx.coroutines.flow.MutableStateFlow
// Importa el observable de datos reactivos
import kotlinx.coroutines.flow.StateFlow
// Importa el observable de datos reactivos
import kotlinx.coroutines.flow.asStateFlow
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch

// Clase de datos RefugioDifusionUiState: modelo inmutable con propiedades de dominio
data class RefugioDifusionUiState(
    // Constante cargando: valor inmutable que no cambia tras su asignación
    val cargando: Boolean = true,
    // Constante refugio: valor inmutable que no cambia tras su asignación
    val refugio: Refugio? = null
)

/**
 * [ViewModel de Difusión de Refugio]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Cargar los datos del refugio desde el repositorio]
 * - [Proveer la URL del video del refugio para su reproducción en la TV]
 */
// ViewModel RefugioDifusionViewModel: gestiona el estado y la lógica de negocio de la pantalla
class RefugioDifusionViewModel(
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo: LomitoTvRepository = LomitoTvRepository()
) : ViewModel() {

    // Constante _uiState: valor inmutable que no cambia tras su asignación
    private val _uiState = MutableStateFlow(RefugioDifusionUiState())
    // Constante uiState: valor inmutable que no cambia tras su asignación
    val uiState: StateFlow<RefugioDifusionUiState> = _uiState.asStateFlow()

    // Función cargar: define la lógica de esta operación
    fun cargar(refugioId: String) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true)
            // Constante refugio: valor inmutable que no cambia tras su asignación
            val refugio = repo.getRefugioById(refugioId)
            // ✅ AHORA USA EL videoUrl QUE VIENE DE LA BASE DE DATOS
            _uiState.value = RefugioDifusionUiState(cargando = false, refugio = refugio)
        }
    }
}
