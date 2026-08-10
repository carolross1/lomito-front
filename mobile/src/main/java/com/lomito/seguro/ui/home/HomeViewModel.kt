package com.lomito.seguro.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lomito.seguro.data.model.Alerta
import com.lomito.seguro.data.model.Mascota
import com.lomito.seguro.data.repository.LomitoRepository
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repo = LomitoRepository()

    private val _mascotas = MutableLiveData<List<Mascota>>()
    val mascotas: LiveData<List<Mascota>> = _mascotas

    private val _alertasNoLeidas = MutableLiveData<Int>()
    val alertasNoLeidas: LiveData<Int> = _alertasNoLeidas

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun cargar(ownerId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val mResp = repo.getMascotas(ownerId)
                if (mResp.isSuccessful) _mascotas.value = mResp.body() ?: emptyList()

                val aResp = repo.getAlertasNoLeidas(ownerId)
                if (aResp.isSuccessful) _alertasNoLeidas.value = aResp.body()?.size ?: 0
            } catch (e: Exception) {
                _mascotas.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}
