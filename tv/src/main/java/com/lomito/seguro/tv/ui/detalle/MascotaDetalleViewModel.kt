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
    val mascotaId: String = "",
    val enviando: Boolean = false,
    val mostrandoDialogoContacto: Boolean = false,
    val mostrandoConfirmacionEnvio: Boolean = false,
    val errorEnvio: Boolean = false,
    val numeroContacto: String = ""
)

/**
 * [ViewModel de Detalle de Mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Cargar los detalles y último reporte de la mascota]
 * - [Gestionar la lógica de envío de nuevos reportes de avistamiento]
 */
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
                mascotaId = mascotaId,
                ultimoReporte = reportes.maxByOrNull { it.timestamp }
            )
        }
    }

    // ✅ El botón "Ayudar" ya no confirma directo: primero abre el teclado
    // numérico para pedir un contacto con el que el dueño pueda comunicarse.
    fun abrirDialogoContacto() {
        _uiState.value = _uiState.value.copy(mostrandoDialogoContacto = true, numeroContacto = "")
    }

    fun cerrarDialogoContacto() {
        _uiState.value = _uiState.value.copy(mostrandoDialogoContacto = false, numeroContacto = "")
    }

    fun agregarDigito(digito: String) {
        val actual = _uiState.value.numeroContacto
        if (actual.length < 10) {
            _uiState.value = _uiState.value.copy(numeroContacto = actual + digito)
        }
    }

    fun borrarDigito() {
        val actual = _uiState.value.numeroContacto
        if (actual.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(numeroContacto = actual.dropLast(1))
        }
    }

    // ✅ Ya no depende de un reporte previo (el mural podía mostrar uno
    // "simulado" con id falso cuando aún no había ninguno real, y confirmar
    // contra ese id fallaba en silencio y nunca avisaba al dueño). Ahora
    // crea el reporte y lo confirma en un solo paso.
    fun confirmarAvistamiento() {
        val mascotaId = _uiState.value.mascotaId
        val contacto = _uiState.value.numeroContacto
        if (mascotaId.isBlank() || contacto.length < 10) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(enviando = true, mostrandoDialogoContacto = false)
            val ok = repo.reportarAvistamiento(mascotaId, contacto)
            _uiState.value = _uiState.value.copy(
                enviando = false,
                mostrandoConfirmacionEnvio = ok,
                errorEnvio = !ok
            )
        }
    }

    fun cerrarConfirmacionEnvio() {
        _uiState.value = _uiState.value.copy(mostrandoConfirmacionEnvio = false, errorEnvio = false)
    }
}
