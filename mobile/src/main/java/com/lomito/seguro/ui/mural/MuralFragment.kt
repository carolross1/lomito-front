// Paquete: com.lomito.seguro.ui.mural
package com.lomito.seguro.ui.mural

// Importa la dependencia necesaria: BroadcastReceiver
import android.content.BroadcastReceiver
// Importa el contexto de Android
import android.content.Context
// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa la clase Intent para navegación entre componentes
import android.content.IntentFilter
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: LayoutInflater
import android.view.LayoutInflater
// Importa componentes de la interfaz gráfica
import android.view.View
// Importa componentes de la interfaz gráfica
import android.view.ViewGroup
// Importa la dependencia necesaria: Toast
import android.widget.Toast
// Importa la clase Fragment de AndroidX
import androidx.fragment.app.Fragment
// Importa la dependencia necesaria: viewModels
import androidx.fragment.app.viewModels
// Importa el observable de datos reactivos
import androidx.lifecycle.LiveData
// Importa el observable de datos reactivos
import androidx.lifecycle.MutableLiveData
// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModel
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa la dependencia necesaria: LocalBroadcastManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
// Importa componente de navegación
import androidx.navigation.fragment.findNavController
// Importa la dependencia necesaria: GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.data.model.Mascota
// Importa la dependencia necesaria: LomitoRepository
import com.lomito.seguro.data.repository.LomitoRepository
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch

// ViewModel MuralViewModel: gestiona el estado y la lógica de negocio de la pantalla
class MuralViewModel : ViewModel() {
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo = LomitoRepository()
    // Constante _mascotasPerdidas: valor inmutable que no cambia tras su asignación
    private val _mascotasPerdidas = MutableLiveData<List<Mascota>>()
    // Constante mascotasPerdidas: valor inmutable que no cambia tras su asignación
    val mascotasPerdidas: LiveData<List<Mascota>> = _mascotasPerdidas

    // Función cargarMascotasPerdidas: define la lógica de esta operación
    fun cargarMascotasPerdidas() {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante response: valor inmutable que no cambia tras su asignación
                val response = repo.getMascotasByEstado("PERDIDA")
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (response.isSuccessful) {
                    _mascotasPerdidas.value = response.body() ?: emptyList()
                } else {
                    _mascotasPerdidas.value = emptyList()
                }
            } catch (e: Exception) {
                _mascotasPerdidas.value = emptyList()
            }
        }
    }
}

/**
 * [Fragmento del mural de mascotas perdidas]
 *
 * Responsabilidades:
 * - [Mostrar una cuadrícula con todas las mascotas en estado PERDIDA]
 * - [Reaccionar a notificaciones de nuevas mascotas perdidas para actualizar la lista]
 */
// Fragment MuralFragment: componente de UI que representa una sección de la pantalla
class MuralFragment : Fragment() {
    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: MuralViewModel by viewModels()
    private lateinit var adapter: MascotaPerdidaAdapter

    // Constante mascotaPerdidaReceiver: valor inmutable que no cambia tras su asignación
    private val mascotaPerdidaReceiver = object : BroadcastReceiver() {
        // Sobreescribe la función onReceive de la clase padre
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.cargarMascotasPerdidas()
            // Muestra un mensaje emergente breve al usuario
            Toast.makeText(requireContext(), "🐾 Nueva mascota perdida en el mural", Toast.LENGTH_SHORT).show()
        }
    }

    // Infla el layout del Fragment y retorna la vista raíz
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_mural, container, false)

    // Se llama cuando la vista del Fragment está lista; se inicializa la UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onViewCreated(view, savedInstanceState)

        // Constante recyclerView: valor inmutable que no cambia tras su asignación
        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewMural)
        // Constante tvSinMascotas: valor inmutable que no cambia tras su asignación
        val tvSinMascotas = view.findViewById<android.widget.TextView>(R.id.tvSinMascotas)

        // Asigna el adaptador al RecyclerView para mostrar la lista de datos
        adapter = MascotaPerdidaAdapter { mascota ->
            // Constante bundle: valor inmutable que no cambia tras su asignación
            val bundle = Bundle().apply { putString("mascotaId", mascota.id) }
            // Navega hacia el destino especificado en el grafo de navegación
            findNavController().navigate(R.id.action_mural_to_mascota_detail, bundle)
        }

        // Define cómo se organizan visualmente los elementos del RecyclerView
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        // Asigna el adaptador al RecyclerView para mostrar la lista de datos
        recyclerView.adapter = adapter

        viewModel.mascotasPerdidas.observe(viewLifecycleOwner) { mascotas ->
            adapter.submitList(mascotas)
            tvSinMascotas.visibility = if (mascotas.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (mascotas.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.cargarMascotasPerdidas()
    }

    // Método del ciclo de vida: la actividad se vuelve visible
    override fun onStart() {
        // Invoca la implementación del método en la clase padre
        super.onStart()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            mascotaPerdidaReceiver,
            IntentFilter("com.lomito.seguro.MASCOTA_PERDIDA_NUEVA")
        )
    }

    // Método del ciclo de vida: la actividad ya no es visible
    override fun onStop() {
        // Invoca la implementación del método en la clase padre
        super.onStop()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mascotaPerdidaReceiver)
    }
}