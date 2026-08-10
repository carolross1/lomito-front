package com.lomito.seguro.ui.mascota

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.lomito.seguro.data.model.CreateMascotaRequest
import com.lomito.seguro.databinding.FragmentCrearMascotaBinding
import com.lomito.seguro.util.SessionManager
import com.lomito.seguro.util.gone
import com.lomito.seguro.util.toAbsoluteUrl
import com.lomito.seguro.util.toast
import com.lomito.seguro.util.visible

class CrearMascotaFragment : Fragment() {
    private var _binding: FragmentCrearMascotaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MascotaViewModel by viewModels()
    private lateinit var session: SessionManager
    private var fotoUri: Uri? = null
    private var datosPrecargados = false

    // Si viene un mascotaId, estamos editando; si no, estamos creando
    private val mascotaId: String? by lazy { arguments?.getString("mascotaId") }
    private val esEdicion get() = mascotaId != null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            fotoUri = uri
            Glide.with(this).load(uri).into(binding.ivFoto)
        }
    }

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

        val elegirFoto = {
            pickMedia.launch(androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            ))
        }
        binding.ivFoto.setOnClickListener { elegirFoto() }
        binding.btnElegirFoto.setOnClickListener { elegirFoto() }

        if (esEdicion) {
            binding.tvTitulo.text = "Editar mascota"
            binding.btnGuardar.text = "Guardar cambios"
            (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = "Editar mascota"
            viewModel.cargarMascota(mascotaId!!)

            viewModel.mascota.observe(viewLifecycleOwner) { mascota ->
                if (mascota == null || datosPrecargados) return@observe
                datosPrecargados = true

                binding.etNombre.setText(mascota.nombre)
                binding.etRaza.setText(mascota.raza)
                binding.etColor.setText(mascota.color)
                binding.etEdad.setText(mascota.edad.toString())
                binding.etPeso.setText(mascota.peso.toString())
                binding.etUmbral.setText(mascota.distanciaAlerta.toString())
                val especieIndex = especies.indexOf(mascota.especie)
                if (especieIndex >= 0) binding.spinnerEspecie.setSelection(especieIndex)

                if (!mascota.fotoUrl.isNullOrEmpty()) {
                    Glide.with(this).load(mascota.fotoUrl.toAbsoluteUrl()).into(binding.ivFoto)
                }
            }
        }

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

            if (esEdicion) {
                val datos = mapOf(
                    "nombre" to nombre,
                    "especie" to binding.spinnerEspecie.selectedItem.toString(),
                    "raza" to raza,
                    "color" to color,
                    "edad" to (edadStr.toIntOrNull() ?: 0),
                    "peso" to (pesoStr.toDoubleOrNull() ?: 0.0),
                    "distancia_alerta" to (umbralStr.toIntOrNull() ?: 50)
                )
                viewModel.actualizarMascotaConFoto(requireContext(), mascotaId!!, datos, fotoUri) {
                    findNavController().navigateUp()
                }
            } else {
                viewModel.crearMascotaConFoto(
                    requireContext(),
                    CreateMascotaRequest(
                        nombre = nombre,
                        especie = binding.spinnerEspecie.selectedItem.toString(),
                        ownerId = session.getUserId(),
                        raza = raza,
                        color = color,
                        edad = edadStr.toIntOrNull() ?: 0,
                        peso = pesoStr.toDoubleOrNull() ?: 0.0,
                        distanciaAlerta = umbralStr.toIntOrNull() ?: 50
                    ),
                    fotoUri
                ) {
                    findNavController().navigateUp()
                }
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