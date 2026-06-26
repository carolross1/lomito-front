package com.lomito.seguro.ui.mascota

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.lomito.seguro.R
import com.lomito.seguro.databinding.FragmentMascotaDetailBinding
import com.lomito.seguro.util.gone
import com.lomito.seguro.util.toast
import com.lomito.seguro.util.visible

class MascotaDetailFragment : Fragment() {
    private var _binding: FragmentMascotaDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MascotaViewModel by viewModels()
    private val mascotaId: String by lazy {
        arguments?.getString("mascotaId") ?: ""
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMascotaDetailBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.cargarMascota(mascotaId)

        viewModel.mascota.observe(viewLifecycleOwner) { mascota ->
            mascota ?: return@observe
            binding.tvNombre.text = mascota.nombre
            binding.tvEspecie.text = "${mascota.especie} • ${mascota.raza}"
            binding.tvEdad.text = "${mascota.edad} años"
            binding.tvColor.text = "Color: ${mascota.color}"
            binding.tvPeso.text = "Peso: ${mascota.peso} kg"
            binding.tvEstado.text = when (mascota.estado) {
                "EN_CASA" -> "✅ En casa"
                "PERDIDA" -> "🚨 ¡Perdida!"
                "ENCONTRADA" -> "✅ Encontrada"
                else -> mascota.estado
            }
            binding.tvUmbral.text = "Umbral BLE: ${mascota.distanciaAlerta}m"

            if (!mascota.fotoUrl.isNullOrEmpty()) {
                Glide.with(this).load(mascota.fotoUrl)
                    .placeholder(R.drawable.ic_pet_placeholder).into(binding.ivMascota)
            }

            if (mascota.latitud != null && mascota.longitud != null) {
                binding.tvUbicacion.text = "Última ubicación: ${String.format("%.4f", mascota.latitud)}, ${String.format("%.4f", mascota.longitud)}"
            } else {
                binding.tvUbicacion.text = "Sin ubicación registrada"
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            if (loading) binding.progressBar.visible() else binding.progressBar.gone()
        }

        viewModel.message.observe(viewLifecycleOwner) { msg ->
            if (msg.isNotEmpty()) toast(msg)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.mascota_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar mascota")
                    .setMessage("¿Seguro que deseas eliminar esta mascota?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        viewModel.eliminarMascota(mascotaId) {
                            findNavController().navigateUp()
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
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
