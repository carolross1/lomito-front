// Paquete: com.lomito.seguro.ui.home
package com.lomito.seguro.ui.home

// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: *
import android.view.*
// Importa la clase Fragment de AndroidX
import androidx.fragment.app.Fragment
// Importa la dependencia necesaria: viewModels
import androidx.fragment.app.viewModels
// Importa componente de navegación
import androidx.navigation.fragment.findNavController
// Importa la dependencia necesaria: LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa la clase Fragment de AndroidX
import com.lomito.seguro.databinding.FragmentHomeBinding
// Importa la dependencia necesaria: SessionManager
import com.lomito.seguro.util.SessionManager
// Importa la dependencia necesaria: gone
import com.lomito.seguro.util.gone
// Importa la dependencia necesaria: visible
import com.lomito.seguro.util.visible

/**
 * [Fragmento principal (Home) de la aplicación]
 *
 * Responsabilidades:
 * - [Mostrar la lista de mascotas del usuario logueado]
 * - [Ofrecer navegación a las diferentes secciones de la app]
 */
// Fragment HomeFragment: componente de UI que representa una sección de la pantalla
class HomeFragment : Fragment() {
    // Variable _binding: almacena el estado mutable de este componente
    private var _binding: FragmentHomeBinding? = null
    // Constante binding: valor inmutable que no cambia tras su asignación
    private val binding get() = _binding!!
    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var session: SessionManager
    private lateinit var adapter: MascotaCardAdapter

    // Infla el layout del Fragment y retorna la vista raíz
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        // Accede a un componente de UI a través del View Binding type-safe
        return binding.root
    }

    // Se llama cuando la vista del Fragment está lista; se inicializa la UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        // ✅ Configurar RecyclerView
        // Asigna el adaptador al RecyclerView para mostrar la lista de datos
        adapter = MascotaCardAdapter { mascota ->
            // Constante bundle: valor inmutable que no cambia tras su asignación
            val bundle = android.os.Bundle().apply { putString("mascotaId", mascota.id) }
            // Navega hacia el destino especificado en el grafo de navegación
            findNavController().navigate(R.id.action_home_to_mascota_detail, bundle)
        }
        // Define cómo se organizan visualmente los elementos del RecyclerView
        binding.rvMascotas.layoutManager = LinearLayoutManager(requireContext())
        // Asigna el adaptador al RecyclerView para mostrar la lista de datos
        binding.rvMascotas.adapter = adapter

        // ✅ Configurar FAB
        // Accede a un componente de UI a través del View Binding type-safe
        binding.fabAgregarMascota.setOnClickListener {
            // Navega hacia el destino especificado en el grafo de navegación
            findNavController().navigate(R.id.action_home_to_crear_mascota)
        }

        // ✅ Configurar botones de acciones rápidas
        // Accede a un componente de UI a través del View Binding type-safe
        binding.btnAlertas.setOnClickListener {
            // Navega hacia el destino especificado en el grafo de navegación
            findNavController().navigate(R.id.action_home_to_alertas)
        }

        // Accede a un componente de UI a través del View Binding type-safe
        binding.btnRefugios.setOnClickListener {
            // Navega hacia el destino especificado en el grafo de navegación
            findNavController().navigate(R.id.action_home_to_refugios)
        }

        // Accede a un componente de UI a través del View Binding type-safe
        binding.btnSimulador.setOnClickListener {
            // Navega hacia el destino especificado en el grafo de navegación
            findNavController().navigate(R.id.action_home_to_simulator)
        }

        // Accede a un componente de UI a través del View Binding type-safe
        binding.btnMural.setOnClickListener {
            // Navega hacia el destino especificado en el grafo de navegación
            findNavController().navigate(R.id.action_home_to_mural)
        }

        // ✅ Observar datos
        viewModel.mascotas.observe(viewLifecycleOwner) { mascotas ->
            adapter.submitList(mascotas)
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (mascotas.isEmpty()) {
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvEmpty.visible()
                // Accede a un componente de UI a través del View Binding type-safe
                binding.ivEmpty.visible()
            } else {
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvEmpty.gone()
                // Accede a un componente de UI a través del View Binding type-safe
                binding.ivEmpty.gone()
            }
        }

        viewModel.alertasNoLeidas.observe(viewLifecycleOwner) { count ->
            // Actualiza el componente de UI a través del View Binding
            binding.badgeAlertas.text = if (count > 0) count.toString() else ""
            // Actualiza el componente de UI a través del View Binding
            binding.badgeAlertas.visibility = if (count > 0) View.VISIBLE else View.GONE
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            // Actualiza el componente de UI a través del View Binding
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.cargar(session.getUserId())
    }

    // Sobreescribe la función onDestroyView de la clase padre
    override fun onDestroyView() {
        // Invoca la implementación del método en la clase padre
        super.onDestroyView()
        _binding = null
    }
}