// Paquete: com.lomito.seguro.ui.mascota
package com.lomito.seguro.ui.mascota

// Importa el contexto de Android
import android.content.Context
// Importa la dependencia necesaria: Uri
import android.net.Uri
// Importa la dependencia necesaria: *
import androidx.lifecycle.*
// Importa la dependencia necesaria: CreateMascotaRequest
import com.lomito.seguro.data.model.CreateMascotaRequest
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.data.model.Mascota
// Importa la dependencia necesaria: ReporteVista
import com.lomito.seguro.data.model.ReporteVista
// Importa la dependencia necesaria: LomitoRepository
import com.lomito.seguro.data.repository.LomitoRepository
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.Dispatchers
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.withContext
// Importa la dependencia necesaria: toMediaTypeOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
// Importa la dependencia necesaria: MultipartBody
import okhttp3.MultipartBody
// Importa la dependencia necesaria: asRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
// Importa la dependencia necesaria: File
import java.io.File

// ViewModel MascotaViewModel: gestiona el estado y la lógica de negocio de la pantalla
class MascotaViewModel : ViewModel() {
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo = LomitoRepository()

    // Constante _mascota: valor inmutable que no cambia tras su asignación
    private val _mascota = MutableLiveData<Mascota?>()
    // Constante mascota: valor inmutable que no cambia tras su asignación
    val mascota: LiveData<Mascota?> = _mascota

    // Constante _reportes: valor inmutable que no cambia tras su asignación
    private val _reportes = MutableLiveData<List<ReporteVista>>()
    // Constante reportes: valor inmutable que no cambia tras su asignación
    val reportes: LiveData<List<ReporteVista>> = _reportes

    // Constante _loading: valor inmutable que no cambia tras su asignación
    private val _loading = MutableLiveData<Boolean>()
    // Constante loading: valor inmutable que no cambia tras su asignación
    val loading: LiveData<Boolean> = _loading

    // Constante _message: valor inmutable que no cambia tras su asignación
    private val _message = MutableLiveData<String>()
    // Constante message: valor inmutable que no cambia tras su asignación
    val message: LiveData<String> = _message

    // Función cargarMascota: define la lógica de esta operación
    fun cargarMascota(id: String) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _loading.value = true
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante resp: valor inmutable que no cambia tras su asignación
                val resp = repo.getMascotaById(id)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (resp.isSuccessful) _mascota.value = resp.body()
                // Constante rResp: valor inmutable que no cambia tras su asignación
                val rResp = repo.getUltimoReporte(id)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (rResp.isSuccessful) _reportes.value = listOfNotNull(rResp.body())
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // Función reportarVista: define la lógica de esta operación
    fun reportarVista(mascotaId: String, lat: Double, lng: Double) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante resp: valor inmutable que no cambia tras su asignación
                val resp = repo.reportarVista(mascotaId, lat, lng, "Ubicación reportada desde Watch")
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (resp.isSuccessful) _message.value = "✅ Vista reportada exitosamente"
                else _message.value = "Error al reportar vista"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }

    // Función crearMascota: define la lógica de esta operación
    fun crearMascota(request: CreateMascotaRequest, onSuccess: () -> Unit) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _loading.value = true
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante resp: valor inmutable que no cambia tras su asignación
                val resp = repo.createMascota(request)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (resp.isSuccessful) {
                    _message.value = "✅ Mascota registrada"
                    onSuccess()
                } else {
                    _message.value = "Error al crear mascota"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // Sube la foto elegida (si hay) y luego crea la mascota con el foto_url resultante
    // Función crearMascotaConFoto: define la lógica de esta operación
    fun crearMascotaConFoto(context: Context, request: CreateMascotaRequest, fotoUri: Uri?, onSuccess: () -> Unit) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _loading.value = true
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Variable fotoUrl: almacena el estado mutable de este componente
                var fotoUrl: String? = null

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (fotoUri != null) {
                    // Constante tempFile: valor inmutable que no cambia tras su asignación
                    val tempFile = withContext(Dispatchers.IO) {
                        // Constante stream: valor inmutable que no cambia tras su asignación
                        val stream = context.contentResolver.openInputStream(fotoUri)
                        // Constante file: valor inmutable que no cambia tras su asignación
                        val file = File.createTempFile("foto_", ".jpg", context.cacheDir)
                        stream?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                        file
                    }
                    // Constante body: valor inmutable que no cambia tras su asignación
                    val body = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                    // Constante part: valor inmutable que no cambia tras su asignación
                    val part = MultipartBody.Part.createFormData("foto", tempFile.name, body)
                    // Constante uploadResp: valor inmutable que no cambia tras su asignación
                    val uploadResp = repo.uploadFoto(part)
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (uploadResp.isSuccessful) {
                        fotoUrl = uploadResp.body()?.fotoUrl
                    } else {
                        _message.value = "No se pudo subir la foto, se guardará sin ella"
                    }
                }

                // Constante resp: valor inmutable que no cambia tras su asignación
                val resp = repo.createMascota(request.copy(fotoUrl = fotoUrl))
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (resp.isSuccessful) {
                    _message.value = "✅ Mascota registrada"
                    onSuccess()
                } else {
                    _message.value = "Error al crear mascota"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // Sube foto nueva (si se eligió una) y actualiza los datos de la mascota existente
    // Función actualizarMascotaConFoto: define la lógica de esta operación
    fun actualizarMascotaConFoto(
        context: Context,
        id: String,
        datos: Map<String, Any>,
        fotoUri: Uri?,
        onSuccess: () -> Unit
    ) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            _loading.value = true
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante datosFinales: valor inmutable que no cambia tras su asignación
                val datosFinales = datos.toMutableMap()

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (fotoUri != null) {
                    // Constante tempFile: valor inmutable que no cambia tras su asignación
                    val tempFile = withContext(Dispatchers.IO) {
                        // Constante stream: valor inmutable que no cambia tras su asignación
                        val stream = context.contentResolver.openInputStream(fotoUri)
                        // Constante file: valor inmutable que no cambia tras su asignación
                        val file = File.createTempFile("foto_", ".jpg", context.cacheDir)
                        stream?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                        file
                    }
                    // Constante body: valor inmutable que no cambia tras su asignación
                    val body = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                    // Constante part: valor inmutable que no cambia tras su asignación
                    val part = MultipartBody.Part.createFormData("foto", tempFile.name, body)
                    // Constante uploadResp: valor inmutable que no cambia tras su asignación
                    val uploadResp = repo.uploadFoto(part)
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (uploadResp.isSuccessful) {
                        uploadResp.body()?.fotoUrl?.let { datosFinales["foto_url"] = it }
                    } else {
                        _message.value = "No se pudo subir la foto, se guardarán los demás cambios"
                    }
                }

                // Constante resp: valor inmutable que no cambia tras su asignación
                val resp = repo.updateMascota(id, datosFinales)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (resp.isSuccessful) {
                    _message.value = "✅ Cambios guardados"
                    onSuccess()
                } else {
                    _message.value = "Error al actualizar mascota"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // Función eliminarMascota: define la lógica de esta operación
    fun eliminarMascota(id: String, onSuccess: () -> Unit) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante resp: valor inmutable que no cambia tras su asignación
                val resp = repo.deleteMascota(id)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (resp.isSuccessful) onSuccess()
                else _message.value = "Error al eliminar"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }
}
