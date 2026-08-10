package com.lomito.seguro.ui.alertas

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.lomito.seguro.R
import com.lomito.seguro.databinding.FragmentAlertasBinding
import com.lomito.seguro.models.Alerta
import com.lomito.seguro.util.SessionManager
import com.lomito.seguro.util.gone
import com.lomito.seguro.util.visible
import kotlinx.coroutines.launch

class AlertasFragment : Fragment() {
    private var _binding: FragmentAlertasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AlertasViewModel by viewModels()
    private lateinit var session: SessionManager
    private lateinit var adapter: AlertasAdapter
    private val TAG = "AlertasFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAlertasBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        // ✅ Configurar toolbar
        val toolbar = binding.toolbar as MaterialToolbar
        toolbar.title = "🔔 Notificaciones"
        toolbar.setTitleTextColor(resources.getColor(R.color.white, null))
        toolbar.inflateMenu(R.menu.alertas_menu)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_marcar_todas -> {
                    marcarTodasComoLeidas()
                    true
                }
                else -> false
            }
        }

        // ✅ Configurar RecyclerView
        adapter = AlertasAdapter(
            onItemClick = { alerta ->
                Log.d(TAG, "Click en alerta: ${alerta.id}, leída: ${alerta.leida}")
                if (!alerta.leida) {
                    marcarComoLeida(alerta.id)
                }
                navegarADetalle(alerta)
            },
            onMarcarLeida = { alertaId ->
                Log.d(TAG, "Marcar como leída: $alertaId")
                marcarComoLeida(alertaId)
            }
        )
        binding.rvAlertas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAlertas.adapter = adapter

        // ✅ Observar alertas
        viewModel.alertas.observe(viewLifecycleOwner) { alertas ->
            Log.d(TAG, "Alertas observadas: ${alertas.size}")
            adapter.submitList(alertas)
            if (alertas.isEmpty()) {
                binding.tvEmpty.visible()
                binding.rvAlertas.gone()
            } else {
                binding.tvEmpty.gone()
                binding.rvAlertas.visible()
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Log.e(TAG, "Error: $error")
                Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_LONG).show()
            }
        }

        // ✅ Cargar alertas al iniciar
        cargarAlertas()
    }

    // ✅ Remover onResume para evitar llamadas redundantes
    // override fun onResume() {
    //     super.onResume()
    //     cargarAlertas()
    // }

    private fun cargarAlertas() {
        val ownerId = session.getUserId().toIntOrNull() ?: 0
        Log.d(TAG, "Cargando alertas para ownerId: $ownerId")
        if (ownerId == 0) {
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.cargarAlertas(ownerId)
    }

    private fun marcarComoLeida(alertaId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val success = viewModel.marcarComoLeida(alertaId)
            if (success) {
                Toast.makeText(requireContext(), "Marcada como leída", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Error al marcar como leída", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun marcarTodasComoLeidas() {
        val ownerId = session.getUserId().toIntOrNull() ?: 0
        if (ownerId == 0) {
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val alertasNoLeidas = viewModel.alertas.value?.filter { !it.leida } ?: emptyList()
        if (alertasNoLeidas.isEmpty()) {
            Toast.makeText(requireContext(), "No hay notificaciones sin leer", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val success = viewModel.marcarTodasComoLeidas(ownerId)
            if (success) {
                Toast.makeText(requireContext(), "Todas las notificaciones marcadas como leídas", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Error al marcar todas como leídas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navegarADetalle(alerta: Alerta) {
        Log.d(TAG, "Navegando a detalle: tipo=${alerta.tipo}, mascotaId=${alerta.mascotaId}")

        when (alerta.tipo) {
            "AVISTAMIENTO", "PERDIDA", "ENCONTRADA" -> {
                if (alerta.mascotaId != null) {
                    val bundle = Bundle().apply {
                        putString("mascotaId", alerta.mascotaId)
                    }
                    try {
                        findNavController().navigate(
                            R.id.action_alertas_to_mascota_detail,
                            bundle
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error navegando: ${e.message}")
                        Toast.makeText(requireContext(), "Detalle: ${alerta.mensaje}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Detalle: ${alerta.mensaje}", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(requireContext(), "Detalle: ${alerta.mensaje}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.alertas_menu, menu)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}