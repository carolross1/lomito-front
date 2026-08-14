// Paquete: com.lomito.seguro.ui.alertas
package com.lomito.seguro.ui.alertas

// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la clase de logging de Android
import android.util.Log
// Importa la dependencia necesaria: *
import android.view.*
// Importa la dependencia necesaria: Toast
import android.widget.Toast
// Importa la clase Fragment de AndroidX
import androidx.fragment.app.Fragment
// Importa la dependencia necesaria: viewModels
import androidx.fragment.app.viewModels
// Importa la dependencia necesaria: lifecycleScope
import androidx.lifecycle.lifecycleScope
// Importa componente de navegación
import androidx.navigation.fragment.findNavController
// Importa la dependencia necesaria: LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
// Importa la dependencia necesaria: MaterialToolbar
import com.google.android.material.appbar.MaterialToolbar
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa la clase Fragment de AndroidX
import com.lomito.seguro.databinding.FragmentAlertasBinding
// Importa la dependencia necesaria: Alerta
import com.lomito.seguro.models.Alerta
// Importa la dependencia necesaria: SessionManager
import com.lomito.seguro.util.SessionManager
// Importa la dependencia necesaria: gone
import com.lomito.seguro.util.gone
// Importa la dependencia necesaria: visible
import com.lomito.seguro.util.visible
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch

/**
 * [Fragmento para listar las notificaciones y alertas del usuario]
 *
 * Responsabilidades:
 * - [Cargar las alertas asociadas al usuario actual]
 * - [Permitir marcar alertas como leídas individual o globalmente]
 */
// Fragment AlertasFragment: componente de UI que representa una sección de la pantalla
class AlertasFragment : Fragment() {
    // Variable _binding: almacena el estado mutable de este componente
    private var _binding: FragmentAlertasBinding? = null
    // Constante binding: valor inmutable que no cambia tras su asignación
    private val binding get() = _binding!!
    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: AlertasViewModel by viewModels()
    private lateinit var session: SessionManager
    private lateinit var adapter: AlertasAdapter
    // Constante TAG: valor inmutable que no cambia tras su asignación
    private val TAG = "AlertasFragment"

    // Infla el layout del Fragment y retorna la vista raíz
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAlertasBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        // Accede a un componente de UI a través del View Binding type-safe
        return binding.root
    }

    // Se llama cuando la vista del Fragment está lista; se inicializa la UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        // ✅ Configurar toolbar
        // Constante toolbar: valor inmutable que no cambia tras su asignación
        val toolbar = binding.toolbar as MaterialToolbar
        toolbar.title = "🔔 Notificaciones"
        toolbar.setTitleTextColor(resources.getColor(R.color.white, null))
        toolbar.inflateMenu(R.menu.alertas_menu)
        toolbar.setOnMenuItemClickListener { item ->
            // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
            when (item.itemId) {
                R.id.action_marcar_todas -> {
                    marcarTodasComoLeidas()
                    true
                }
                else -> false
            }
        }

        // ✅ Configurar RecyclerView
        // Asigna el adaptador al RecyclerView para mostrar la lista de datos
        adapter = AlertasAdapter(
            onItemClick = { alerta ->
                // Registro de evento en el log de Android para depuración
                Log.d(TAG, "Click en alerta: ${alerta.id}, leída: ${alerta.leida}")
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (!alerta.leida) {
                    marcarComoLeida(alerta.id)
                }
                navegarADetalle(alerta)
            },
            onMarcarLeida = { alertaId ->
                // Registro de evento en el log de Android para depuración
                Log.d(TAG, "Marcar como leída: $alertaId")
                marcarComoLeida(alertaId)
            }
        )
        // Define cómo se organizan visualmente los elementos del RecyclerView
        binding.rvAlertas.layoutManager = LinearLayoutManager(requireContext())
        // Asigna el adaptador al RecyclerView para mostrar la lista de datos
        binding.rvAlertas.adapter = adapter

        // ✅ Observar alertas
        viewModel.alertas.observe(viewLifecycleOwner) { alertas ->
            // Registro de evento en el log de Android para depuración
            Log.d(TAG, "Alertas observadas: ${alertas.size}")
            adapter.submitList(alertas)
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (alertas.isEmpty()) {
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvEmpty.visible()
                // Accede a un componente de UI a través del View Binding type-safe
                binding.rvAlertas.gone()
            } else {
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvEmpty.gone()
                // Accede a un componente de UI a través del View Binding type-safe
                binding.rvAlertas.visible()
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            // Actualiza el componente de UI a través del View Binding
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (error.isNotEmpty()) {
                // Registro de evento en el log de Android para depuración
                Log.e(TAG, "Error: $error")
                // Muestra un mensaje emergente breve al usuario
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
        // Constante ownerId: valor inmutable que no cambia tras su asignación
        val ownerId = session.getUserId().toIntOrNull() ?: 0
        // Registro de evento en el log de Android para depuración
        Log.d(TAG, "Cargando alertas para ownerId: $ownerId")
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (ownerId == 0) {
            // Muestra un mensaje emergente breve al usuario
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            // Retorna el valor al llamador de la función
            return
        }
        viewModel.cargarAlertas(ownerId)
    }

    private fun marcarComoLeida(alertaId: Int) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewLifecycleOwner.lifecycleScope.launch {
            // Constante success: valor inmutable que no cambia tras su asignación
            val success = viewModel.marcarComoLeida(alertaId)
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (success) {
                // Muestra un mensaje emergente breve al usuario
                Toast.makeText(requireContext(), "Marcada como leída", Toast.LENGTH_SHORT).show()
            } else {
                // Muestra un mensaje emergente breve al usuario
                Toast.makeText(requireContext(), "Error al marcar como leída", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun marcarTodasComoLeidas() {
        // Constante ownerId: valor inmutable que no cambia tras su asignación
        val ownerId = session.getUserId().toIntOrNull() ?: 0
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (ownerId == 0) {
            // Muestra un mensaje emergente breve al usuario
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            // Retorna el valor al llamador de la función
            return
        }

        // Constante alertasNoLeidas: valor inmutable que no cambia tras su asignación
        val alertasNoLeidas = viewModel.alertas.value?.filter { !it.leida } ?: emptyList()
        // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
        if (alertasNoLeidas.isEmpty()) {
            // Muestra un mensaje emergente breve al usuario
            Toast.makeText(requireContext(), "No hay notificaciones sin leer", Toast.LENGTH_SHORT).show()
            // Retorna el valor al llamador de la función
            return
        }

        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewLifecycleOwner.lifecycleScope.launch {
            // Constante success: valor inmutable que no cambia tras su asignación
            val success = viewModel.marcarTodasComoLeidas(ownerId)
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (success) {
                // Muestra un mensaje emergente breve al usuario
                Toast.makeText(requireContext(), "Todas las notificaciones marcadas como leídas", Toast.LENGTH_SHORT).show()
            } else {
                // Muestra un mensaje emergente breve al usuario
                Toast.makeText(requireContext(), "Error al marcar todas como leídas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navegarADetalle(alerta: Alerta) {
        // Registro de evento en el log de Android para depuración
        Log.d(TAG, "Navegando a detalle: tipo=${alerta.tipo}, mascotaId=${alerta.mascotaId}")

        // Expresión when: evalúa múltiples condiciones de forma concisa (equivalente a switch)
        when (alerta.tipo) {
            "AVISTAMIENTO", "PERDIDA", "ENCONTRADA" -> {
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (alerta.mascotaId != null) {
                    // Constante bundle: valor inmutable que no cambia tras su asignación
                    val bundle = Bundle().apply {
                        putString("mascotaId", alerta.mascotaId)
                    }
                    // Bloque try-catch: maneja posibles excepciones en el código crítico
                    try {
                        // Navega hacia el destino especificado en el grafo de navegación
                        findNavController().navigate(
                            R.id.action_alertas_to_mascota_detail,
                            bundle
                        )
                    } catch (e: Exception) {
                        // Registro de evento en el log de Android para depuración
                        Log.e(TAG, "Error navegando: ${e.message}")
                        // Muestra un mensaje emergente breve al usuario
                        Toast.makeText(requireContext(), "Detalle: ${alerta.mensaje}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Muestra un mensaje emergente breve al usuario
                    Toast.makeText(requireContext(), "Detalle: ${alerta.mensaje}", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                // Muestra un mensaje emergente breve al usuario
                Toast.makeText(requireContext(), "Detalle: ${alerta.mensaje}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Sobreescribe la función onCreateOptionsMenu de la clase padre
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.alertas_menu, menu)
    }

    // Sobreescribe la función onDestroyView de la clase padre
    override fun onDestroyView() {
        // Invoca la implementación del método en la clase padre
        super.onDestroyView()
        _binding = null
    }
}