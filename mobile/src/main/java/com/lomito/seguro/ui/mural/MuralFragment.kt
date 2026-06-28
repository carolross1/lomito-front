package com.lomito.seguro.ui.mural

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.lomito.seguro.R
import com.lomito.seguro.data.model.Mascota
import com.lomito.seguro.data.repository.LomitoRepository
import kotlinx.coroutines.launch

class MuralViewModel : ViewModel() {
    private val repo = LomitoRepository()
    private val _mascotasPerdidas = MutableLiveData<List<Mascota>>()
    val mascotasPerdidas: LiveData<List<Mascota>> = _mascotasPerdidas

    fun cargarMascotasPerdidas() {
        viewModelScope.launch {
            try {
                val response = repo.getMascotasByEstado("PERDIDA")
                if (response.isSuccessful) {
                    _mascotasPerdidas.value = response.body() ?: emptyList()
                } else {
                    _mascotasPerdidas.value = emptyList()
                }
            } catch (e: Exception) {
                _mascotasPerdidas.value = emptyList()
            }
        }
    }
}

class MuralFragment : Fragment() {
    private val viewModel: MuralViewModel by viewModels()
    private lateinit var adapter: MascotaPerdidaAdapter

    private val mascotaPerdidaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.cargarMascotasPerdidas()
            Toast.makeText(requireContext(), "🐾 Nueva mascota perdida en el mural", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_mural, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewMural)
        val tvSinMascotas = view.findViewById<android.widget.TextView>(R.id.tvSinMascotas)

        adapter = MascotaPerdidaAdapter { mascota ->
            val bundle = Bundle().apply { putString("mascotaId", mascota.id) }
            findNavController().navigate(R.id.action_mural_to_mascota_detail, bundle)
        }

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

        viewModel.mascotasPerdidas.observe(viewLifecycleOwner) { mascotas ->
            adapter.submitList(mascotas)
            tvSinMascotas.visibility = if (mascotas.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (mascotas.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.cargarMascotasPerdidas()
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            mascotaPerdidaReceiver,
            IntentFilter("com.lomito.seguro.MASCOTA_PERDIDA_NUEVA")
        )
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mascotaPerdidaReceiver)
    }
}