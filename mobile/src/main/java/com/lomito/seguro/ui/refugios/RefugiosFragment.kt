// Paquete: com.lomito.seguro.ui.refugios
package com.lomito.seguro.ui.refugios

// Importa la clase Intent para navegación entre componentes
import android.content.Intent
// Importa la dependencia necesaria: Uri
import android.net.Uri
// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: *
import android.view.*
// Importa la clase Fragment de AndroidX
import androidx.fragment.app.Fragment
// Importa la clase base ViewModel del ciclo de vida
import androidx.lifecycle.ViewModel
// Importa la dependencia necesaria: viewModelScope
import androidx.lifecycle.viewModelScope
// Importa la dependencia necesaria: DiffUtil
import androidx.recyclerview.widget.DiffUtil
// Importa la dependencia necesaria: LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
// Importa la dependencia necesaria: ListAdapter
import androidx.recyclerview.widget.ListAdapter
// Importa componentes de la interfaz gráfica
import androidx.recyclerview.widget.RecyclerView
// Importa la dependencia necesaria: Refugio
import com.lomito.seguro.data.model.Refugio
// Importa la dependencia necesaria: LomitoRepository
import com.lomito.seguro.data.repository.LomitoRepository
// Importa la clase Fragment de AndroidX
import com.lomito.seguro.databinding.FragmentRefugiosBinding
// Importa el sistema de View Binding
import com.lomito.seguro.databinding.ItemRefugioBinding
// Importa la dependencia necesaria: gone
import com.lomito.seguro.util.gone
// Importa la dependencia necesaria: visible
import com.lomito.seguro.util.visible
// Importa la dependencia necesaria: viewModels
import androidx.fragment.app.viewModels
// Importa el observable de datos reactivos
import androidx.lifecycle.LiveData
// Importa el observable de datos reactivos
import androidx.lifecycle.MutableLiveData
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.launch

// ViewModel RefugiosViewModel: gestiona el estado y la lógica de negocio de la pantalla
class RefugiosViewModel : ViewModel() {
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo = LomitoRepository()
    // Constante _refugios: valor inmutable que no cambia tras su asignación
    private val _refugios = MutableLiveData<List<Refugio>>()
    // Constante refugios: valor inmutable que no cambia tras su asignación
    val refugios: LiveData<List<Refugio>> = _refugios

    // Función cargar: define la lógica de esta operación
    fun cargar() {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante resp: valor inmutable que no cambia tras su asignación
                val resp = repo.getRefugios()
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (resp.isSuccessful) _refugios.value = resp.body() ?: emptyList()
            } catch (e: Exception) { _refugios.value = emptyList() }
        }
    }
}

// Adaptador RefugioAdapter: conecta los datos con la vista del RecyclerView
class RefugioAdapter(private val onCall: (String) -> Unit) :
    ListAdapter<Refugio, RefugioAdapter.VH>(DiffCB()) {
    inner class VH(val b: ItemRefugioBinding) : RecyclerView.ViewHolder(b.root) {
        // Función bind: define la lógica de esta operación
        fun bind(r: Refugio) {
            b.tvNombre.text = r.nombre
            b.tvDireccion.text = r.direccion
            b.tvTelefono.text = r.telefono
            b.tvHorarios.text = r.horarios
            b.btnLlamar.setOnClickListener { onCall(r.telefono) }
        }
    }
    // Infla el layout y crea el ViewHolder para el RecyclerView
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(
        ItemRefugioBinding.inflate(LayoutInflater.from(p.context), p, false)
    )
    // Vincula los datos del modelo con las vistas del ViewHolder
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
    // Declaración de la clase DiffCB
    class DiffCB : DiffUtil.ItemCallback<Refugio>() {
        // Sobreescribe la función areItemsTheSame de la clase padre
        override fun areItemsTheSame(a: Refugio, b: Refugio) = a.id == b.id
        // Sobreescribe la función areContentsTheSame de la clase padre
        override fun areContentsTheSame(a: Refugio, b: Refugio) = a == b
    }
}

// Fragment RefugiosFragment: componente de UI que representa una sección de la pantalla
class RefugiosFragment : Fragment() {
    // Variable _binding: almacena el estado mutable de este componente
    private var _binding: FragmentRefugiosBinding? = null
    // Constante binding: valor inmutable que no cambia tras su asignación
    private val binding get() = _binding!!
    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: RefugiosViewModel by viewModels()

    // Infla el layout del Fragment y retorna la vista raíz
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRefugiosBinding.inflate(inflater, container, false)
        // Accede a un componente de UI a través del View Binding type-safe
        return binding.root
    }

    // Se llama cuando la vista del Fragment está lista; se inicializa la UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onViewCreated(view, savedInstanceState)
        // Constante adapter: valor inmutable que no cambia tras su asignación
        val adapter = RefugioAdapter { tel ->
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel")))
        }
        // Define cómo se organizan visualmente los elementos del RecyclerView
        binding.rvRefugios.layoutManager = LinearLayoutManager(requireContext())
        // Asigna el adaptador al RecyclerView para mostrar la lista de datos
        binding.rvRefugios.adapter = adapter

        viewModel.refugios.observe(viewLifecycleOwner) { refugios ->
            adapter.submitList(refugios)
            // Accede a un componente de UI a través del View Binding type-safe
            if (refugios.isEmpty()) binding.tvEmpty.visible() else binding.tvEmpty.gone()
        }

        viewModel.cargar()
    }

    // Sobreescribe la función onDestroyView de la clase padre
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
