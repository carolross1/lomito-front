// Paquete: com.lomito.seguro.tv.ui.detalle
package com.lomito.seguro.tv.ui.detalle

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

// Clase de datos MascotaDetalleUiState: modelo inmutable con propiedades de dominio
data class MascotaDetalleUiState(
    // Constante cargando: valor inmutable que no cambia tras su asignación
    val cargando: Boolean = true,
    // Constante mascota: valor inmutable que no cambia tras su asignación
    val mascota: Mascota? = null,
    // Constante ultimoReporte: valor inmutable que no cambia tras su asignación
    val ultimoReporte: ReporteVista? = null,
    // Constante mascotaId: valor inmutable que no cambia tras su asignación
    val mascotaId: String = "",
    // Constante enviando: valor inmutable que no cambia tras su asignación
    val enviando: Boolean = false,
    // Constante mostrandoDialogoContacto: valor inmutable que no cambia tras su asignación
    val mostrandoDialogoContacto: Boolean = false,
    // Constante mostrandoConfirmacionEnvio: valor inmutable que no cambia tras su asignación
    val mostrandoConfirmacionEnvio: Boolean = false,
    // Constante errorEnvio: valor inmutable que no cambia tras su asignación
    val errorEnvio: Boolean = false,
    // Constante numeroContacto: valor inmutable que no cambia tras su asignación
    val numeroContacto: String = ""
)

/**
 * [ViewModel de Detalle de Mascota]
 *
 * Responsabilidades (o parámetros en caso de funciones simples):
 * - [Cargar los detalles y último reporte de la mascota]
 * - [Gestionar la lógica de envío de nuevos reportes de avistamiento]
 */
// ViewModel MascotaDetalleViewModel: gestiona el estado y la lógica de negocio de la pantalla
class MascotaDetalleViewModel(
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo: LomitoTvRepository = LomitoTvRepository()
) : ViewModel() {

    // Constante _uiState: valor inmutable que no cambia tras su asignación
    private val _uiState = MutableStateFlow(MascotaDetalleUiState())
    // Constante uiState: valor inmutable que no cambia tras su asignación
    val uiState: StateFlow<MascotaDetalleUiState> = _uiState.asStateFlow()

    // Función cargar: define la lógica de esta operación
    fun cargar(mascotaId: String) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true)
            // Constante mascota: valor inmutable que no cambia tras su asignación
            val mascota = repo.getMascotaById(mascotaId)
            // Constante reportes: valor inmutable que no cambia tras su asignación
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
    // Función abrirDialogoContacto: define la lógica de esta operación
    fun abrirDialogoContacto() {
        _uiState.value = _uiState.value.copy(mostrandoDialogoContacto = true, numeroContacto = "")
    }

    // Función cerrarDialogoContacto: define la lógica de esta operación
    fun cerrarDialogoContacto() {
        _uiState.value = _uiState.value.copy(mostrandoDialogoContacto = false, numeroContacto = "")
    }

    // Función agregarDigito: define la lógica de esta operación
    fun agregarDigito(digito: String) {
        // Constante actual: valor inmutable que no cambia tras su asignación
        val actual = _uiState.value.numeroContacto
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (actual.length < 10) {
            _uiState.value = _uiState.value.copy(numeroContacto = actual + digito)
        }
    }

    // Función borrarDigito: define la lógica de esta operación
    fun borrarDigito() {
        // Constante actual: valor inmutable que no cambia tras su asignación
        val actual = _uiState.value.numeroContacto
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (actual.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(numeroContacto = actual.dropLast(1))
        }
    }

    // ✅ Ya no depende de un reporte previo (el mural podía mostrar uno
    // "simulado" con id falso cuando aún no había ninguno real, y confirmar
    // contra ese id fallaba en silencio y nunca avisaba al dueño). Ahora
    // crea el reporte y lo confirma en un solo paso.
    // Función confirmarAvistamiento: define la lógica de esta operación
    fun confirmarAvistamiento() {
        // Constante mascotaId: valor inmutable que no cambia tras su asignación
        val mascotaId = _uiState.value.mascotaId
        // Constante contacto: valor inmutable que no cambia tras su asignación
        val contacto = _uiState.value.numeroContacto
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (mascotaId.isBlank() || contacto.length < 10) return
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(enviando = true, mostrandoDialogoContacto = false)
            // Constante ok: valor inmutable que no cambia tras su asignación
            val ok = repo.reportarAvistamiento(mascotaId, contacto)
            _uiState.value = _uiState.value.copy(
                enviando = false,
                mostrandoConfirmacionEnvio = ok,
                errorEnvio = !ok
            )
        }
    }

    // Función cerrarConfirmacionEnvio: define la lógica de esta operación
    fun cerrarConfirmacionEnvio() {
        _uiState.value = _uiState.value.copy(mostrandoConfirmacionEnvio = false, errorEnvio = false)
    }
}
