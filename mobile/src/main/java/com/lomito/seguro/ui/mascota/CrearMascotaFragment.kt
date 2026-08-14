// Paquete: com.lomito.seguro.ui.mascota
package com.lomito.seguro.ui.mascota

// Importa la dependencia necesaria: Uri
import android.net.Uri
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: *
import android.view.*
// Importa la dependencia necesaria: ArrayAdapter
import android.widget.ArrayAdapter
// Importa la dependencia necesaria: ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts
// Importa la clase Fragment de AndroidX
import androidx.fragment.app.Fragment
// Importa la dependencia necesaria: viewModels
import androidx.fragment.app.viewModels
// Importa componente de navegación
import androidx.navigation.fragment.findNavController
// Importa la librería de carga de imágenes
import com.bumptech.glide.Glide
// Importa la dependencia necesaria: CreateMascotaRequest
import com.lomito.seguro.data.model.CreateMascotaRequest
// Importa la clase Fragment de AndroidX
import com.lomito.seguro.databinding.FragmentCrearMascotaBinding
// Importa la dependencia necesaria: SessionManager
import com.lomito.seguro.util.SessionManager
// Importa la dependencia necesaria: gone
import com.lomito.seguro.util.gone
// Importa la dependencia necesaria: toAbsoluteUrl
import com.lomito.seguro.util.toAbsoluteUrl
// Importa la dependencia necesaria: toast
import com.lomito.seguro.util.toast
// Importa la dependencia necesaria: visible
import com.lomito.seguro.util.visible

// Fragment CrearMascotaFragment: componente de UI que representa una sección de la pantalla
class CrearMascotaFragment : Fragment() {
    // Variable _binding: almacena el estado mutable de este componente
    private var _binding: FragmentCrearMascotaBinding? = null
    // Constante binding: valor inmutable que no cambia tras su asignación
    private val binding get() = _binding!!
    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: MascotaViewModel by viewModels()
    private lateinit var session: SessionManager
    // Variable fotoUri: almacena el estado mutable de este componente
    private var fotoUri: Uri? = null
    // Variable datosPrecargados: almacena el estado mutable de este componente
    private var datosPrecargados = false

    // Si viene un mascotaId, estamos editando; si no, estamos creando
    // Constante mascotaId: valor inmutable que no cambia tras su asignación
    private val mascotaId: String? by lazy { arguments?.getString("mascotaId") }
    // Constante esEdicion: valor inmutable que no cambia tras su asignación
    private val esEdicion get() = mascotaId != null

    // Constante pickMedia: valor inmutable que no cambia tras su asignación
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (uri != null) {
            fotoUri = uri
            // Accede a un componente de UI a través del View Binding type-safe
            Glide.with(this).load(uri).into(binding.ivFoto)
        }
    }

    // Infla el layout del Fragment y retorna la vista raíz
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCrearMascotaBinding.inflate(inflater, container, false)
        // Accede a un componente de UI a través del View Binding type-safe
        return binding.root
    }

    // Se llama cuando la vista del Fragment está lista; se inicializa la UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        // Constante especies: valor inmutable que no cambia tras su asignación
        val especies = arrayOf("PERRO", "GATO")
        // Constante especieAdapter: valor inmutable que no cambia tras su asignación
        val especieAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, especies)
        especieAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Asigna el adaptador al RecyclerView para mostrar la lista de datos
        binding.spinnerEspecie.adapter = especieAdapter

        // Constante elegirFoto: valor inmutable que no cambia tras su asignación
        val elegirFoto = {
            pickMedia.launch(androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            ))
        }
        // Accede a un componente de UI a través del View Binding type-safe
        binding.ivFoto.setOnClickListener { elegirFoto() }
        // Accede a un componente de UI a través del View Binding type-safe
        binding.btnElegirFoto.setOnClickListener { elegirFoto() }

        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (esEdicion) {
            // Actualiza el componente de UI a través del View Binding
            binding.tvTitulo.text = "Editar mascota"
            // Actualiza el componente de UI a través del View Binding
            binding.btnGuardar.text = "Guardar cambios"
            (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.title = "Editar mascota"
            viewModel.cargarMascota(mascotaId!!)

            viewModel.mascota.observe(viewLifecycleOwner) { mascota ->
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (mascota == null || datosPrecargados) return@observe
                datosPrecargados = true

                // Accede a un componente de UI a través del View Binding type-safe
                binding.etNombre.setText(mascota.nombre)
                // Accede a un componente de UI a través del View Binding type-safe
                binding.etRaza.setText(mascota.raza)
                // Accede a un componente de UI a través del View Binding type-safe
                binding.etColor.setText(mascota.color)
                // Accede a un componente de UI a través del View Binding type-safe
                binding.etEdad.setText(mascota.edad.toString())
                // Accede a un componente de UI a través del View Binding type-safe
                binding.etPeso.setText(mascota.peso.toString())
                // Accede a un componente de UI a través del View Binding type-safe
                binding.etUmbral.setText(mascota.distanciaAlerta.toString())
                // Constante especieIndex: valor inmutable que no cambia tras su asignación
                val especieIndex = especies.indexOf(mascota.especie)
                // Actualiza el componente de UI a través del View Binding
                if (especieIndex >= 0) binding.spinnerEspecie.setSelection(especieIndex)

                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (!mascota.fotoUrl.isNullOrEmpty()) {
                    // Accede a un componente de UI a través del View Binding type-safe
                    Glide.with(this).load(mascota.fotoUrl.toAbsoluteUrl()).into(binding.ivFoto)
                }
            }
        }

        // Accede a un componente de UI a través del View Binding type-safe
        binding.btnGuardar.setOnClickListener {
            // Constante nombre: valor inmutable que no cambia tras su asignación
            val nombre = binding.etNombre.text.toString().trim()
            // Constante raza: valor inmutable que no cambia tras su asignación
            val raza = binding.etRaza.text.toString().trim()
            // Constante color: valor inmutable que no cambia tras su asignación
            val color = binding.etColor.text.toString().trim()
            // Constante edadStr: valor inmutable que no cambia tras su asignación
            val edadStr = binding.etEdad.text.toString().trim()
            // Constante pesoStr: valor inmutable que no cambia tras su asignación
            val pesoStr = binding.etPeso.text.toString().trim()
            // Constante umbralStr: valor inmutable que no cambia tras su asignación
            val umbralStr = binding.etUmbral.text.toString().trim()

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (nombre.isEmpty()) {
                toast("El nombre es requerido")
                return@setOnClickListener
            }

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (esEdicion) {
                // Constante datos: valor inmutable que no cambia tras su asignación
                val datos = mapOf(
                    "nombre" to nombre,
                    // Accede a un componente de UI a través del View Binding type-safe
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
                        // Actualiza el componente de UI a través del View Binding
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
            // Accede a un componente de UI a través del View Binding type-safe
            if (loading) binding.progressBar.visible() else binding.progressBar.gone()
        }

        viewModel.message.observe(viewLifecycleOwner) { msg ->
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (msg.isNotEmpty()) toast(msg)
        }
    }

    // Sobreescribe la función onDestroyView de la clase padre
    override fun onDestroyView() {
        // Invoca la implementación del método en la clase padre
        super.onDestroyView()
        _binding = null
    }
}