package com.lomito.seguro.tv.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lomito.seguro.tv.data.model.Mascota
import com.lomito.seguro.tv.data.model.ReporteVista
import com.lomito.seguro.tv.data.repository.LomitoTvRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MascotaPerfilUiState(
    val cargando: Boolean = true,
    val mascota: Mascota? = null,
    val reportes: List<ReporteVista> = emptyList()
)

/**
 * [ViewModel del Perfil de Mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Obtener y exponer la información detallada de la mascota]
 * - [Cargar y ordenar el historial de reportes de la mascota]
 */
class MascotaPerfilViewModel(
    private val repo: LomitoTvRepository = LomitoTvRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MascotaPerfilUiState())
    val uiState: StateFlow<MascotaPerfilUiState> = _uiState.asStateFlow()

    fun cargar(mascotaId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true)
            val mascota = repo.getMascotaById(mascotaId)
            val reportes = repo.getReportesDeMascota(mascotaId).sortedByDescending { it.timestamp }
            _uiState.value = MascotaPerfilUiState(cargando = false, mascota = mascota, reportes = reportes)
        }
    }
}
