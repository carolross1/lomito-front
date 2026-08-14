// Paquete: com.lomito.seguro.tv.ui.perfil
package com.lomito.seguro.tv.ui.perfil

// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModel
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.tv.data.model.Mascota
// Importa la dependencia necesaria: ReporteVista
import com.lomito.seguro.tv.data.model.ReporteVista
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

// Clase de datos MascotaPerfilUiState: modelo inmutable con propiedades de dominio
data class MascotaPerfilUiState(
    // Constante cargando: valor inmutable que no cambia tras su asignación
    val cargando: Boolean = true,
    // Constante mascota: valor inmutable que no cambia tras su asignación
    val mascota: Mascota? = null,
    // Constante reportes: valor inmutable que no cambia tras su asignación
    val reportes: List<ReporteVista> = emptyList()
)

/**
 * [ViewModel del Perfil de Mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Obtener y exponer la información detallada de la mascota]
 * - [Cargar y ordenar el historial de reportes de la mascota]
 */
// ViewModel MascotaPerfilViewModel: gestiona el estado y la lógica de negocio de la pantalla
class MascotaPerfilViewModel(
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo: LomitoTvRepository = LomitoTvRepository()
) : ViewModel() {

    // Constante _uiState: valor inmutable que no cambia tras su asignación
    private val _uiState = MutableStateFlow(MascotaPerfilUiState())
    // Constante uiState: valor inmutable que no cambia tras su asignación
    val uiState: StateFlow<MascotaPerfilUiState> = _uiState.asStateFlow()

    // Función cargar: define la lógica de esta operación
    fun cargar(mascotaId: String) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true)
            // Constante mascota: valor inmutable que no cambia tras su asignación
            val mascota = repo.getMascotaById(mascotaId)
            // Constante reportes: valor inmutable que no cambia tras su asignación
            val reportes = repo.getReportesDeMascota(mascotaId).sortedByDescending { it.timestamp }
            _uiState.value = MascotaPerfilUiState(cargando = false, mascota = mascota, reportes = reportes)
        }
    }
}
