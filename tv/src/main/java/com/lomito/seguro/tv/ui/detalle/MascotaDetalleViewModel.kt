package com.lomito.seguro.tv.ui.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lomito.seguro.tv.data.model.Mascota
import com.lomito.seguro.tv.data.model.ReporteVista
import com.lomito.seguro.tv.data.repository.LomitoTvRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MascotaDetalleUiState(
    val cargando: Boolean = true,
    val mascota: Mascota? = null,
    val ultimoReporte: ReporteVista? = null,
    val confirmando: Boolean = false,
    val confirmado: Boolean = false
)

class MascotaDetalleViewModel(
    private val repo: LomitoTvRepository = LomitoTvRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MascotaDetalleUiState())
    val uiState: StateFlow<MascotaDetalleUiState> = _uiState.asStateFlow()

    fun cargar(mascotaId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true)
            val mascota = repo.getMascotaById(mascotaId)
            val reportes = repo.getReportesDeMascota(mascotaId)
            _uiState.value = MascotaDetalleUiState(
                cargando = false,
                mascota = mascota,
                ultimoReporte = reportes.maxByOrNull { it.timestamp }
            )
        }
    }

    fun confirmarAvistamiento() {
        val reporteId = _uiState.value.ultimoReporte?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(confirmando = true)
            val ok = repo.confirmarReporte(reporteId)
            _uiState.value = _uiState.value.copy(confirmando = false, confirmado = ok)
        }
    }
}
