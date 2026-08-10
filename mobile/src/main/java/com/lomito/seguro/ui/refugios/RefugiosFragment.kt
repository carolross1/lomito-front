package com.lomito.seguro.ui.refugios

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lomito.seguro.data.model.Refugio
import com.lomito.seguro.data.repository.LomitoRepository
import com.lomito.seguro.databinding.FragmentRefugiosBinding
import com.lomito.seguro.databinding.ItemRefugioBinding
import com.lomito.seguro.util.gone
import com.lomito.seguro.util.visible
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.launch

class RefugiosViewModel : ViewModel() {
    private val repo = LomitoRepository()
    private val _refugios = MutableLiveData<List<Refugio>>()
    val refugios: LiveData<List<Refugio>> = _refugios

    fun cargar() {
        viewModelScope.launch {
            try {
                val resp = repo.getRefugios()
                if (resp.isSuccessful) _refugios.value = resp.body() ?: emptyList()
            } catch (e: Exception) { _refugios.value = emptyList() }
        }
    }
}

class RefugioAdapter(private val onCall: (String) -> Unit) :
    ListAdapter<Refugio, RefugioAdapter.VH>(DiffCB()) {
    inner class VH(val b: ItemRefugioBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(r: Refugio) {
            b.tvNombre.text = r.nombre
            b.tvDireccion.text = r.direccion
            b.tvTelefono.text = r.telefono
            b.tvHorarios.text = r.horarios
            b.btnLlamar.setOnClickListener { onCall(r.telefono) }
        }
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(
        ItemRefugioBinding.inflate(LayoutInflater.from(p.context), p, false)
    )
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
    class DiffCB : DiffUtil.ItemCallback<Refugio>() {
        override fun areItemsTheSame(a: Refugio, b: Refugio) = a.id == b.id
        override fun areContentsTheSame(a: Refugio, b: Refugio) = a == b
    }
}

class RefugiosFragment : Fragment() {
    private var _binding: FragmentRefugiosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RefugiosViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRefugiosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = RefugioAdapter { tel ->
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel")))
        }
        binding.rvRefugios.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRefugios.adapter = adapter

        viewModel.refugios.observe(viewLifecycleOwner) { refugios ->
            adapter.submitList(refugios)
            if (refugios.isEmpty()) binding.tvEmpty.visible() else binding.tvEmpty.gone()
        }

        viewModel.cargar()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
