package com.lomito.seguro.tv.ui.refugio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lomito.seguro.tv.data.model.Refugio
import com.lomito.seguro.tv.data.repository.LomitoTvRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RefugioDifusionUiState(
    val cargando: Boolean = true,
    val refugio: Refugio? = null
)

/**
 * [ViewModel de Difusión de Refugio]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Cargar los datos del refugio desde el repositorio]
 * - [Proveer la URL del video del refugio para su reproducción en la TV]
 */
class RefugioDifusionViewModel(
    private val repo: LomitoTvRepository = LomitoTvRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RefugioDifusionUiState())
    val uiState: StateFlow<RefugioDifusionUiState> = _uiState.asStateFlow()

    fun cargar(refugioId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true)
            val refugio = repo.getRefugioById(refugioId)
            // ✅ AHORA USA EL videoUrl QUE VIENE DE LA BASE DE DATOS
            _uiState.value = RefugioDifusionUiState(cargando = false, refugio = refugio)
        }
    }
}
