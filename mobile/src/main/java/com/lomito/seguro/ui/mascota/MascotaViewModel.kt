package com.lomito.seguro.ui.mascota

import android.content.Context
import android.net.Uri
import androidx.lifecycle.*
import com.lomito.seguro.data.model.CreateMascotaRequest
import com.lomito.seguro.data.model.Mascota
import com.lomito.seguro.data.model.ReporteVista
import com.lomito.seguro.data.repository.LomitoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class MascotaViewModel : ViewModel() {
    private val repo = LomitoRepository()

    private val _mascota = MutableLiveData<Mascota?>()
    val mascota: LiveData<Mascota?> = _mascota

    private val _reportes = MutableLiveData<List<ReporteVista>>()
    val reportes: LiveData<List<ReporteVista>> = _reportes

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    fun cargarMascota(id: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val resp = repo.getMascotaById(id)
                if (resp.isSuccessful) _mascota.value = resp.body()
                val rResp = repo.getUltimoReporte(id)
                if (rResp.isSuccessful) _reportes.value = listOfNotNull(rResp.body())
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun reportarVista(mascotaId: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                val resp = repo.reportarVista(mascotaId, lat, lng, "Ubicación reportada desde Watch")
                if (resp.isSuccessful) _message.value = "✅ Vista reportada exitosamente"
                else _message.value = "Error al reportar vista"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }

    fun crearMascota(request: CreateMascotaRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val resp = repo.createMascota(request)
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
    fun crearMascotaConFoto(context: Context, request: CreateMascotaRequest, fotoUri: Uri?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                var fotoUrl: String? = null

                if (fotoUri != null) {
                    val tempFile = withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openInputStream(fotoUri)
                        val file = File.createTempFile("foto_", ".jpg", context.cacheDir)
                        stream?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                        file
                    }
                    val body = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("foto", tempFile.name, body)
                    val uploadResp = repo.uploadFoto(part)
                    if (uploadResp.isSuccessful) {
                        fotoUrl = uploadResp.body()?.fotoUrl
                    } else {
                        _message.value = "No se pudo subir la foto, se guardará sin ella"
                    }
                }

                val resp = repo.createMascota(request.copy(fotoUrl = fotoUrl))
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
    fun actualizarMascotaConFoto(
        context: Context,
        id: String,
        datos: Map<String, Any>,
        fotoUri: Uri?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val datosFinales = datos.toMutableMap()

                if (fotoUri != null) {
                    val tempFile = withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openInputStream(fotoUri)
                        val file = File.createTempFile("foto_", ".jpg", context.cacheDir)
                        stream?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                        file
                    }
                    val body = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("foto", tempFile.name, body)
                    val uploadResp = repo.uploadFoto(part)
                    if (uploadResp.isSuccessful) {
                        uploadResp.body()?.fotoUrl?.let { datosFinales["foto_url"] = it }
                    } else {
                        _message.value = "No se pudo subir la foto, se guardarán los demás cambios"
                    }
                }

                val resp = repo.updateMascota(id, datosFinales)
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

    fun eliminarMascota(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val resp = repo.deleteMascota(id)
                if (resp.isSuccessful) onSuccess()
                else _message.value = "Error al eliminar"
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            }
        }
    }
}
