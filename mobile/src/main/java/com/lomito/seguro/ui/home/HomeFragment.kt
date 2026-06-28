package com.lomito.seguro.ui.home

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.lomito.seguro.R
import com.lomito.seguro.databinding.FragmentHomeBinding
import com.lomito.seguro.util.SessionManager
import com.lomito.seguro.util.gone
import com.lomito.seguro.util.visible

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var session: SessionManager
    private lateinit var adapter: MascotaCardAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        // ✅ Configurar toolbar con título y estilo
        binding.toolbar.apply {
            title = "🐾 Mis Mascotas"
            setTitleTextColor(resources.getColor(R.color.white, null))
            inflateMenu(R.menu.home_menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_logout -> {
                        session.logout()
                        findNavController().navigate(R.id.action_home_to_login)
                        true
                    }
                    else -> false
                }
            }
        }

        // ✅ Configurar RecyclerView
        adapter = MascotaCardAdapter { mascota ->
            val bundle = android.os.Bundle().apply { putString("mascotaId", mascota.id) }
            findNavController().navigate(R.id.action_home_to_mascota_detail, bundle)
        }
        binding.rvMascotas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMascotas.adapter = adapter

        // ✅ Configurar FAB
        binding.fabAgregarMascota.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_crear_mascota)
        }

        // ✅ Configurar botones de acciones rápidas
        binding.btnAlertas.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_alertas)
        }

        binding.btnRefugios.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_refugios)
        }

        binding.btnSimulador.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_simulator)
        }

        binding.btnMural.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_mural)
        }

        // ✅ Observar datos
        viewModel.mascotas.observe(viewLifecycleOwner) { mascotas ->
            adapter.submitList(mascotas)
            if (mascotas.isEmpty()) {
                binding.tvEmpty.visible()
                binding.ivEmpty.visible()
            } else {
                binding.tvEmpty.gone()
                binding.ivEmpty.gone()
            }
        }

        viewModel.alertasNoLeidas.observe(viewLifecycleOwner) { count ->
            binding.badgeAlertas.text = if (count > 0) count.toString() else ""
            binding.badgeAlertas.visibility = if (count > 0) View.VISIBLE else View.GONE
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.cargar(session.getUserId())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}