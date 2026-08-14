// Paquete: com.lomito.seguro.tv.ui.dashboard
package com.lomito.seguro.tv.ui.dashboard

// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModel
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.tv.data.model.Mascota
// Importa la dependencia necesaria: Refugio
import com.lomito.seguro.tv.data.model.Refugio
// Importa la dependencia necesaria: LomitoTvRepository
import com.lomito.seguro.tv.data.repository.LomitoTvRepository
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.delay
// Importa el observable de datos reactivos
import kotlinx.coroutines.flow.MutableStateFlow
// Importa el observable de datos reactivos
import kotlinx.coroutines.flow.StateFlow
// Importa el observable de datos reactivos
import kotlinx.coroutines.flow.asStateFlow
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch

// Clase de datos DashboardUiState: modelo inmutable con propiedades de dominio
data class DashboardUiState(
    // Constante cargando: valor inmutable que no cambia tras su asignación
    val cargando: Boolean = true,
    // Constante mascotas: valor inmutable que no cambia tras su asignación
    val mascotas: List<Mascota> = emptyList(),
    // Constante refugios: valor inmutable que no cambia tras su asignación
    val refugios: List<Refugio> = emptyList()
)

/** Cada cuánto se refresca el mural sin interacción del usuario (pantalla comunitaria). */
// Constante AUTO_REFRESH_MS: valor fijo definido en tiempo de compilación
private const val AUTO_REFRESH_MS = 30_000L

/**
 * [ViewModel del Dashboard]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Cargar y exponer el estado de las mascotas y refugios]
 * - [Manejar la lógica de auto-refresco periódico]
 */
// ViewModel DashboardViewModel: gestiona el estado y la lógica de negocio de la pantalla
class DashboardViewModel(
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo: LomitoTvRepository = LomitoTvRepository()
) : ViewModel() {

    // Constante _uiState: valor inmutable que no cambia tras su asignación
    private val _uiState = MutableStateFlow(DashboardUiState())
    // Constante uiState: valor inmutable que no cambia tras su asignación
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        cargar()
        autoRefresh()
    }

    // Función cargar: define la lógica de esta operación
    fun cargar() {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true)
            // Constante mascotas: valor inmutable que no cambia tras su asignación
            val mascotas = repo.getMuralMascotas()
            // Constante refugios: valor inmutable que no cambia tras su asignación
            val refugios = repo.getRefugios()
            _uiState.value = DashboardUiState(cargando = false, mascotas = mascotas, refugios = refugios)
        }
    }

    private fun autoRefresh() {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_MS)
                // Constante mascotas: valor inmutable que no cambia tras su asignación
                val mascotas = repo.getMuralMascotas()
                // Constante refugios: valor inmutable que no cambia tras su asignación
                val refugios = repo.getRefugios()
                _uiState.value = _uiState.value.copy(mascotas = mascotas, refugios = refugios)
            }
        }
    }
}
