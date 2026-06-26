package com.lomito.seguro.ui.mascota

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.lomito.seguro.data.model.CreateMascotaRequest
import com.lomito.seguro.databinding.FragmentCrearMascotaBinding
import com.lomito.seguro.util.SessionManager
import com.lomito.seguro.util.gone
import com.lomito.seguro.util.toast
import com.lomito.seguro.util.visible

class CrearMascotaFragment : Fragment() {
    private var _binding: FragmentCrearMascotaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MascotaViewModel by viewModels()
    private lateinit var session: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCrearMascotaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        val especies = arrayOf("PERRO", "GATO")
        val especieAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, especies)
        especieAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEspecie.adapter = especieAdapter

        binding.btnGuardar.setOnClickListener {
            val nombre = binding.etNombre.text.toString().trim()
            val raza = binding.etRaza.text.toString().trim()
            val color = binding.etColor.text.toString().trim()
            val edadStr = binding.etEdad.text.toString().trim()
            val pesoStr = binding.etPeso.text.toString().trim()
            val umbralStr = binding.etUmbral.text.toString().trim()

            if (nombre.isEmpty()) {
                toast("El nombre es requerido")
                return@setOnClickListener
            }

            val request = CreateMascotaRequest(
                nombre = nombre,
                especie = binding.spinnerEspecie.selectedItem.toString(),
                ownerId = session.getUserId(),
                raza = raza,
                color = color,
                edad = edadStr.toIntOrNull() ?: 0,
                peso = pesoStr.toDoubleOrNull() ?: 0.0,
                distanciaAlerta = umbralStr.toIntOrNull() ?: 50
            )

            viewModel.crearMascota(request) {
                findNavController().navigateUp()
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            if (loading) binding.progressBar.visible() else binding.progressBar.gone()
        }

        viewModel.message.observe(viewLifecycleOwner) { msg ->
            if (msg.isNotEmpty()) toast(msg)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}