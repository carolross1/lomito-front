// Paquete: com.lomito.seguro.ui.mascota
package com.lomito.seguro.ui.mascota

// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: *
import android.view.*
// Importa la dependencia necesaria: AlertDialog
import androidx.appcompat.app.AlertDialog
// Importa la dependencia necesaria: MenuProvider
import androidx.core.view.MenuProvider
// Importa la clase Fragment de AndroidX
import androidx.fragment.app.Fragment
// Importa la dependencia necesaria: viewModels
import androidx.fragment.app.viewModels
// Importa la dependencia necesaria: Lifecycle
import androidx.lifecycle.Lifecycle
// Importa componente de navegación
import androidx.navigation.fragment.findNavController
// Importa la librería de carga de imágenes
import com.bumptech.glide.Glide
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa la clase Fragment de AndroidX
import com.lomito.seguro.databinding.FragmentMascotaDetailBinding
// Importa la dependencia necesaria: gone
import com.lomito.seguro.util.gone
// Importa la dependencia necesaria: toAbsoluteUrl
import com.lomito.seguro.util.toAbsoluteUrl
// Importa la dependencia necesaria: toast
import com.lomito.seguro.util.toast
// Importa la dependencia necesaria: visible
import com.lomito.seguro.util.visible

/**
 * [Fragmento que muestra el detalle de una mascota]
 *
 * Responsabilidades:
 * - [Cargar y mostrar los datos específicos de la mascota]
 * - [Permitir editar o eliminar la mascota desde el menú superior]
 */
// Fragment MascotaDetailFragment: componente de UI que representa una sección de la pantalla
class MascotaDetailFragment : Fragment() {
    // Variable _binding: almacena el estado mutable de este componente
    private var _binding: FragmentMascotaDetailBinding? = null
    // Constante binding: valor inmutable que no cambia tras su asignación
    private val binding get() = _binding!!
    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: MascotaViewModel by viewModels()
    // Constante mascotaId: valor inmutable que no cambia tras su asignación
    private val mascotaId: String by lazy {
        arguments?.getString("mascotaId") ?: ""
    }
    // Infla el layout del Fragment y retorna la vista raíz
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMascotaDetailBinding.inflate(inflater, container, false)
        // Accede a un componente de UI a través del View Binding type-safe
        return binding.root
    }

    // Se llama cuando la vista del Fragment está lista; se inicializa la UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onViewCreated(view, savedInstanceState)

        // ✅ Editar / Eliminar en la barra superior única de la app (MenuProvider,
        // se agrega y quita automáticamente según el ciclo de vida de este fragmento)
        requireActivity().addMenuProvider(object : MenuProvider {
            // Sobreescribe la función onCreateMenu de la clase padre
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.mascota_menu, menu)
            }

            // Sobreescribe la función onMenuItemSelected de la clase padre
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Retorna el valor al llamador de la función
                return when (menuItem.itemId) {
                    R.id.action_edit -> {
                        // Constante bundle: valor inmutable que no cambia tras su asignación
                        val bundle = Bundle().apply { putString("mascotaId", mascotaId) }
                        // Navega hacia el destino especificado en el grafo de navegación
                        findNavController().navigate(R.id.action_mascota_detail_to_editar, bundle)
                        true
                    }
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
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        viewModel.cargarMascota(mascotaId)

        viewModel.mascota.observe(viewLifecycleOwner) { mascota ->
            mascota ?: return@observe
            // Actualiza el componente de UI a través del View Binding
            binding.tvNombre.text = mascota.nombre
            // Actualiza el componente de UI a través del View Binding
            binding.tvEspecie.text = "${mascota.especie} • ${mascota.raza}"
            // Actualiza el componente de UI a través del View Binding
            binding.tvEdad.text = "${mascota.edad} años"
            // Actualiza el componente de UI a través del View Binding
            binding.tvColor.text = "Color: ${mascota.color}"
            // Actualiza el componente de UI a través del View Binding
            binding.tvPeso.text = "Peso: ${mascota.peso} kg"
            // Actualiza el componente de UI a través del View Binding
            binding.tvEstado.text = when (mascota.estado) {
                "EN_CASA" -> "✅ En casa"
                "PERDIDA" -> "🚨 ¡Perdida!"
                "ENCONTRADA" -> "✅ Encontrada"
                else -> mascota.estado
            }
            // Actualiza el componente de UI a través del View Binding
            binding.tvUmbral.text = "Umbral BLE: ${mascota.distanciaAlerta}m"

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (!mascota.fotoUrl.isNullOrEmpty()) {
                Glide.with(this).load(mascota.fotoUrl.toAbsoluteUrl())
                    // Accede a un componente de UI a través del View Binding type-safe
                    .placeholder(R.drawable.ic_pet_placeholder).into(binding.ivMascota)
            }

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (mascota.latitud != null && mascota.longitud != null) {
                // Actualiza el componente de UI a través del View Binding
                binding.tvUbicacion.text = "Última ubicación: ${String.format("%.4f", mascota.latitud)}, ${String.format("%.4f", mascota.longitud)}"
            } else {
                // Actualiza el componente de UI a través del View Binding
                binding.tvUbicacion.text = "Sin ubicación registrada"
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
