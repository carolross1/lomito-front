package com.lomito.seguro.ui.alertas

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lomito.seguro.data.model.Alerta
import com.lomito.seguro.data.repository.LomitoRepository
import com.lomito.seguro.databinding.FragmentAlertasBinding
import com.lomito.seguro.databinding.ItemAlertaBinding
import com.lomito.seguro.util.SessionManager
import com.lomito.seguro.util.formatTimestamp
import com.lomito.seguro.util.gone
import com.lomito.seguro.util.toast
import com.lomito.seguro.util.visible
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.launch

class AlertasViewModel : ViewModel() {
    private val repo = LomitoRepository()
    private val _alertas = MutableLiveData<List<Alerta>>()
    val alertas: LiveData<List<Alerta>> = _alertas

    fun cargar(ownerId: String) {
        viewModelScope.launch {
            try {
                val resp = repo.getAlertas(ownerId)
                if (resp.isSuccessful) _alertas.value = resp.body() ?: emptyList()
            } catch (e: Exception) { _alertas.value = emptyList() }
        }
    }

    fun marcarTodasLeidas(ownerId: String) {
        viewModelScope.launch {
            try {
                repo.marcarTodasLeidas(ownerId)
                cargar(ownerId)
            } catch (e: Exception) {}
        }
    }
}

class AlertaAdapter : ListAdapter<Alerta, AlertaAdapter.VH>(DiffCB()) {
    inner class VH(val b: ItemAlertaBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(a: Alerta) {
            b.tvTipo.text = when (a.tipo) {
                "DISTANCIA_SUPERADA" -> "📡 Distancia superada"
                "AVISTAMIENTO_REPORTADO" -> "👁️ Avistamiento"
                "MASCOTA_ENCONTRADA" -> "✅ Encontrada"
                else -> a.tipo
            }
            b.tvMensaje.text = a.mensaje
            b.tvFecha.text = a.timestamp.formatTimestamp()
            b.tvDistancia.text = if (a.distancia > 0) "${a.distancia}m" else ""
            b.root.alpha = if (a.leida) 0.5f else 1.0f
        }
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(
        ItemAlertaBinding.inflate(LayoutInflater.from(p.context), p, false)
    )
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
    class DiffCB : DiffUtil.ItemCallback<Alerta>() {
        override fun areItemsTheSame(a: Alerta, b: Alerta) = a.id == b.id
        override fun areContentsTheSame(a: Alerta, b: Alerta) = a == b
    }
}

class AlertasFragment : Fragment() {
    private var _binding: FragmentAlertasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AlertasViewModel by viewModels()
    private lateinit var session: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAlertasBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())
        val adapter = AlertaAdapter()
        binding.rvAlertas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAlertas.adapter = adapter

        binding.btnMarcarTodas.setOnClickListener {
            viewModel.marcarTodasLeidas(session.getUserId())
        }

        viewModel.alertas.observe(viewLifecycleOwner) { alertas ->
            adapter.submitList(alertas)
            if (alertas.isEmpty()) binding.tvEmpty.visible() else binding.tvEmpty.gone()
        }

        viewModel.cargar(session.getUserId())
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
