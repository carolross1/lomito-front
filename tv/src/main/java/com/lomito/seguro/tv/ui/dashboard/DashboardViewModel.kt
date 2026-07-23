package com.lomito.seguro.tv.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lomito.seguro.tv.data.model.Mascota
import com.lomito.seguro.tv.data.model.Refugio
import com.lomito.seguro.tv.data.repository.LomitoTvRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val cargando: Boolean = true,
    val mascotas: List<Mascota> = emptyList(),
    val refugios: List<Refugio> = emptyList()
)

/** Cada cuánto se refresca el mural sin interacción del usuario (pantalla comunitaria). */
private const val AUTO_REFRESH_MS = 30_000L

class DashboardViewModel(
    private val repo: LomitoTvRepository = LomitoTvRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        cargar()
        autoRefresh()
    }

    fun cargar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true)
            val mascotas = repo.getMuralMascotas()
            val refugios = repo.getRefugios()
            _uiState.value = DashboardUiState(cargando = false, mascotas = mascotas, refugios = refugios)
        }
    }

    private fun autoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_MS)
                val mascotas = repo.getMuralMascotas()
                val refugios = repo.getRefugios()
                _uiState.value = _uiState.value.copy(mascotas = mascotas, refugios = refugios)
            }
        }
    }
}
