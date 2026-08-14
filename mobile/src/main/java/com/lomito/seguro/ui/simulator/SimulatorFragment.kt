// Paquete: com.lomito.seguro.ui.simulator
package com.lomito.seguro.ui.simulator

// Importa el contenedor de datos Bundle
import android.os.Bundle
// Importa la dependencia necesaria: LayoutInflater
import android.view.LayoutInflater
// Importa componentes de la interfaz gráfica
import android.view.View
// Importa componentes de la interfaz gráfica
import android.view.ViewGroup
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
// Importa la dependencia necesaria: lifecycleScope
import androidx.lifecycle.lifecycleScope
// Importa la API de comunicación con Wear OS
import com.google.android.gms.wearable.Wearable
// Importa la dependencia necesaria: BuildConfig
import com.lomito.seguro.BuildConfig
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.data.model.Mascota
// Importa la dependencia necesaria: LomitoRepository
import com.lomito.seguro.data.repository.LomitoRepository
// Importa la clase Fragment de AndroidX
import com.lomito.seguro.databinding.FragmentSimulatorBinding
// Importa la dependencia necesaria: SessionManager
import com.lomito.seguro.util.SessionManager
// Importa la dependencia necesaria: gone
import com.lomito.seguro.util.gone
// Importa la dependencia necesaria: toast
import com.lomito.seguro.util.toast
// Importa la dependencia necesaria: visible
import com.lomito.seguro.util.visible
// Importa soporte para corrutinas de Kotlin
import kotlinx.coroutines.*
// Importa el parser JSON
import org.json.JSONObject
// Importa la dependencia necesaria: HttpURLConnection
import java.net.HttpURLConnection
// Importa la dependencia necesaria: URL
import java.net.URL

// ViewModel SimulatorViewModel: gestiona el estado y la lógica de negocio de la pantalla
class SimulatorViewModel : ViewModel() {
    // Constante repo: valor inmutable que no cambia tras su asignación
    private val repo = LomitoRepository()

    // Constante _mascotas: valor inmutable que no cambia tras su asignación
    private val _mascotas = MutableLiveData<List<Mascota>>()
    // Constante mascotas: valor inmutable que no cambia tras su asignación
    val mascotas: LiveData<List<Mascota>> = _mascotas

    // Constante _distanciaSimulada: valor inmutable que no cambia tras su asignación
    private val _distanciaSimulada = MutableLiveData(0)
    // Constante distanciaSimulada: valor inmutable que no cambia tras su asignación
    val distanciaSimulada: LiveData<Int> = _distanciaSimulada

    // Constante _mensaje: valor inmutable que no cambia tras su asignación
    private val _mensaje = MutableLiveData<String>()
    // Constante mensaje: valor inmutable que no cambia tras su asignación
    val mensaje: LiveData<String> = _mensaje

    // Variable mascotaSeleccionadaId: almacena el estado mutable de este componente
    private var mascotaSeleccionadaId = ""
    // Variable umbralActual: almacena el estado mutable de este componente
    private var umbralActual = 50

    // Función cargarMascotas: define la lógica de esta operación
    fun cargarMascotas(ownerId: String) {
        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        viewModelScope.launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante resp: valor inmutable que no cambia tras su asignación
                val resp = repo.getMascotas(ownerId)
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (resp.isSuccessful) _mascotas.value = resp.body() ?: emptyList()
            } catch (e: Exception) { _mascotas.value = emptyList() }
        }
    }

    // Función seleccionarMascota: define la lógica de esta operación
    fun seleccionarMascota(mascota: Mascota) {
        mascotaSeleccionadaId = mascota.id
        umbralActual = mascota.distanciaAlerta
    }

    // Función setDistancia: define la lógica de esta operación
    fun setDistancia(distancia: Int) {
        _distanciaSimulada.value = distancia
        // Constante superaUmbral: valor inmutable que no cambia tras su asignación
        val superaUmbral = distancia > umbralActual
        _mensaje.value = if (superaUmbral)
            "🚨 ¡Umbral superado! (${distancia}m > ${umbralActual}m)"
        else
            "✅ Dentro del rango (${distancia}m / umbral ${umbralActual}m)"
    }

    // Función getMascotaId: define la lógica de esta operación
    fun getMascotaId() = mascotaSeleccionadaId
    // Función getUmbral: define la lógica de esta operación
    fun getUmbral() = umbralActual
}

// Fragment SimulatorFragment: componente de UI que representa una sección de la pantalla
class SimulatorFragment : Fragment() {
    // Variable _binding: almacena el estado mutable de este componente
    private var _binding: FragmentSimulatorBinding? = null
    // Constante binding: valor inmutable que no cambia tras su asignación
    private val binding get() = _binding!!
    // Constante viewModel: valor inmutable que no cambia tras su asignación
    private val viewModel: SimulatorViewModel by viewModels()
    private lateinit var session: SessionManager
    // Variable debounceJob: almacena el estado mutable de este componente
    private var debounceJob: Job? = null

    // Infla el layout del Fragment y retorna la vista raíz
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSimulatorBinding.inflate(inflater, container, false)
        // Accede a un componente de UI a través del View Binding type-safe
        return binding.root
    }

    // Se llama cuando la vista del Fragment está lista; se inicializa la UI
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Invoca la implementación del método en la clase padre
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        // Actualiza el componente de UI a través del View Binding
        binding.sliderDistancia.valueFrom = 0f
        // Actualiza el componente de UI a través del View Binding
        binding.sliderDistancia.valueTo = 200f
        // Actualiza el componente de UI a través del View Binding
        binding.sliderDistancia.value = 0f
        // Actualiza el componente de UI a través del View Binding
        binding.sliderDistancia.stepSize = 1f

        // Accede a un componente de UI a través del View Binding type-safe
        binding.sliderDistancia.addOnChangeListener { _, value, _ ->
            // Constante dist: valor inmutable que no cambia tras su asignación
            val dist = value.toInt()
            // Actualiza el componente de UI a través del View Binding
            binding.tvDistanciaActual.text = "${dist}m"
            viewModel.setDistancia(dist)

            // ✅ forceAlert automático si supera el umbral
            // Constante superaUmbral: valor inmutable que no cambia tras su asignación
            val superaUmbral = dist > viewModel.getUmbral()
            enviarAlWear(dist, forceAlert = superaUmbral)

            // ✅ Debounce 500ms para no spamear el backend
            debounceJob?.cancel()
            // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
            debounceJob = lifecycleScope.launch {
                delay(500)
                enviarAlBackend(dist)
            }
        }

        viewModel.mascotas.observe(viewLifecycleOwner) { mascotas ->
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (mascotas.isEmpty()) {
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvSinMascotas.visible()
                // Accede a un componente de UI a través del View Binding type-safe
                binding.layoutSimulator.gone()
                return@observe
            }
            // Accede a un componente de UI a través del View Binding type-safe
            binding.tvSinMascotas.gone()
            // Accede a un componente de UI a través del View Binding type-safe
            binding.layoutSimulator.visible()

            // Constante nombres: valor inmutable que no cambia tras su asignación
            val nombres = mascotas.map { "${it.nombre} (umbral: ${it.distanciaAlerta}m)" }
            // Constante adapter: valor inmutable que no cambia tras su asignación
            val adapter = android.widget.ArrayAdapter(
                requireContext(), android.R.layout.simple_spinner_item, nombres
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Asigna el adaptador al RecyclerView para mostrar la lista de datos
            binding.spinnerMascota.adapter = adapter

            // Actualiza el componente de UI a través del View Binding
            binding.spinnerMascota.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                // Sobreescribe la función onItemSelected de la clase padre
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    viewModel.seleccionarMascota(mascotas[pos])
                    // Actualiza el componente de UI a través del View Binding
                    binding.tvUmbral.text = "Umbral de alerta: ${mascotas[pos].distanciaAlerta}m"
                    // Actualiza el componente de UI a través del View Binding
                    binding.sliderDistancia.value = 0f
                }
                // Sobreescribe la función onNothingSelected de la clase padre
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

            viewModel.seleccionarMascota(mascotas[0])
            // Actualiza el componente de UI a través del View Binding
            binding.tvUmbral.text = "Umbral de alerta: ${mascotas[0].distanciaAlerta}m"
        }

        viewModel.mensaje.observe(viewLifecycleOwner) { msg ->
            // Actualiza el componente de UI a través del View Binding
            binding.tvEstadoSimulacion.text = msg
            // Constante esAlerta: valor inmutable que no cambia tras su asignación
            val esAlerta = msg.contains("🚨")
            // Accede a un componente de UI a través del View Binding type-safe
            binding.cardEstado.setCardBackgroundColor(
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (esAlerta) requireContext().getColor(R.color.alerta_rojo_light)
                else requireContext().getColor(R.color.verde_light)
            )
        }

        // Accede a un componente de UI a través del View Binding type-safe
        binding.btnEnviarAlerta.setOnClickListener {
            // Constante dist: valor inmutable que no cambia tras su asignación
            val dist = binding.sliderDistancia.value.toInt()
            enviarAlWear(dist, forceAlert = true)
            enviarAlBackend(dist)
            toast("📡 Señal enviada al Watch")
        }

        viewModel.cargarMascotas(session.getUserId())
    }

    private fun enviarAlWear(distancia: Int, forceAlert: Boolean = false) {
        // Constante context: valor inmutable que no cambia tras su asignación
        val context = requireContext().applicationContext
        // Constante mascotaId: valor inmutable que no cambia tras su asignación
        val mascotaId = viewModel.getMascotaId()
        // Constante umbral: valor inmutable que no cambia tras su asignación
        val umbral = viewModel.getUmbral()
        // Constante superaUmbral: valor inmutable que no cambia tras su asignación
        val superaUmbral = distancia > umbral || forceAlert

        // Constante payload: valor inmutable que no cambia tras su asignación
        val payload = JSONObject().apply {
            put("distancia", distancia)
            put("mascotaId", mascotaId)
            put("umbral", umbral)
            put("superaUmbral", superaUmbral)
        }.toString().toByteArray()

        // Usa la API de Wearable para comunicación con dispositivos Wear OS
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                // Itera sobre cada elemento de la colección y ejecuta el bloque
                nodes.forEach { node ->
                    // Usa la API de Wearable para comunicación con dispositivos Wear OS
                    Wearable.getMessageClient(context)
                        // Envía un mensaje al dispositivo Wear OS conectado
                        .sendMessage(node.id, "/ble/distancia", payload)
                        .addOnSuccessListener {
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.d("SIMULATOR", "✅ Mensaje enviado a ${node.displayName}")
                        }
                        .addOnFailureListener {
                            // Registro de evento en el log de Android para depuración
                            android.util.Log.e("SIMULATOR", "❌ Error: ${it.message}")
                        }
                }
            }

        // Constante putDataRequest: valor inmutable que no cambia tras su asignación
        val putDataRequest = com.google.android.gms.wearable.PutDataMapRequest.create("/ble/distancia").apply {
            dataMap.putInt("distancia", distancia)
            dataMap.putString("mascotaId", mascotaId)
            dataMap.putInt("umbral", umbral)
            dataMap.putBoolean("superaUmbral", superaUmbral)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        // Usa la API de Wearable para comunicación con dispositivos Wear OS
        Wearable.getDataClient(context).putDataItem(putDataRequest)
    }

    private fun enviarAlBackend(distancia: Int) {
        // Constante mascotaId: valor inmutable que no cambia tras su asignación
        val mascotaId = viewModel.getMascotaId()
        // Constante umbral: valor inmutable que no cambia tras su asignación
        val umbral = viewModel.getUmbral()

        // Lanza una nueva corrutina en el scope actual para ejecutar código asíncrono
        CoroutineScope(Dispatchers.IO).launch {
            // Bloque try-catch: maneja posibles excepciones en el código crítico
            try {
                // Constante url: valor inmutable que no cambia tras su asignación
                val url = URL("${BuildConfig.BACKEND_URL}/api/simulador/distancia")
                // Constante conn: valor inmutable que no cambia tras su asignación
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                // Constante json: valor inmutable que no cambia tras su asignación
                val json = JSONObject().apply {
                    put("distancia", distancia)
                    put("umbral", umbral)
                    put("mascotaId", mascotaId)
                }

                conn.outputStream.write(json.toString().toByteArray())
                conn.outputStream.flush()
                conn.outputStream.close()

                // Constante responseCode: valor inmutable que no cambia tras su asignación
                val responseCode = conn.responseCode
                conn.disconnect()
                // Registro de evento en el log de Android para depuración
                android.util.Log.d("SIMULATOR", "📡 Backend actualizado: ${distancia}m (HTTP $responseCode)")
            } catch (e: Exception) {
                // Registro de evento en el log de Android para depuración
                android.util.Log.e("SIMULATOR", "❌ Error backend: ${e.message}")
            }
        }
    }

    // Sobreescribe la función onDestroyView de la clase padre
    override fun onDestroyView() {
        // Invoca la implementación del método en la clase padre
        super.onDestroyView()
        debounceJob?.cancel()
        _binding = null
    }
}