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

        adapter = MascotaCardAdapter { mascota ->
            val bundle = android.os.Bundle().apply { putString("mascotaId", mascota.id) }
            findNavController().navigate(R.id.action_home_to_mascota_detail, bundle)
        }
        binding.rvMascotas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMascotas.adapter = adapter

        binding.fabAgregarMascota.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_crear_mascota)
        }

        binding.btnAlertas.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_alertas)
        }

        binding.btnRefugios.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_refugios)
        }

        binding.btnSimulador.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_simulator)
        }

        viewModel.mascotas.observe(viewLifecycleOwner) { mascotas ->
            adapter.submitList(mascotas)
            if (mascotas.isEmpty()) binding.tvEmpty.visible() else binding.tvEmpty.gone()
        }

        viewModel.alertasNoLeidas.observe(viewLifecycleOwner) { count ->
            binding.badgeAlertas.text = if (count > 0) count.toString() else ""
            if (count > 0) binding.badgeAlertas.visible() else binding.badgeAlertas.gone()
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            if (loading) binding.progressBar.visible() else binding.progressBar.gone()
        }

        viewModel.cargar(session.getUserId())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                session.logout()
                findNavController().navigate(R.id.action_home_to_login)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
