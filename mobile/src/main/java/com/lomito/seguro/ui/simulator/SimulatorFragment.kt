package com.lomito.seguro.ui.simulator

import android.os.Bundle
import android.provider.Settings.Secure.putString
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import com.lomito.seguro.data.model.Mascota
import com.lomito.seguro.data.repository.LomitoRepository
import com.lomito.seguro.databinding.FragmentSimulatorBinding
import com.lomito.seguro.util.SessionManager
import com.lomito.seguro.util.gone
import com.lomito.seguro.util.toast
import com.lomito.seguro.util.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * SimulatorFragment
 * Permite al usuario mover un Slider para simular la distancia BLE de una mascota.
 * El valor se envía al Wear OS vía Wearable Data API (MessageClient).
 * Esto simula lo que en producción enviaría el collar BLE real.
 */
class SimulatorViewModel : ViewModel() {
    private val repo = LomitoRepository()

    private val _mascotas = MutableLiveData<List<Mascota>>()
    val mascotas: LiveData<List<Mascota>> = _mascotas

    private val _distanciaSimulada = MutableLiveData(0)
    val distanciaSimulada: LiveData<Int> = _distanciaSimulada

    private val _mensaje = MutableLiveData<String>()
    val mensaje: LiveData<String> = _mensaje

    private var mascotaSeleccionadaId = ""
    private var umbralActual = 50

    fun cargarMascotas(ownerId: String) {
        viewModelScope.launch {
            try {
                val resp = repo.getMascotas(ownerId)
                if (resp.isSuccessful) _mascotas.value = resp.body() ?: emptyList()
            } catch (e: Exception) { _mascotas.value = emptyList() }
        }
    }

    fun seleccionarMascota(mascota: Mascota) {
        mascotaSeleccionadaId = mascota.id
        umbralActual = mascota.distanciaAlerta
    }

    fun setDistancia(distancia: Int) {
        _distanciaSimulada.value = distancia
        val superaUmbral = distancia > umbralActual
        _mensaje.value = if (superaUmbral)
            "🚨 ¡Umbral superado! (${distancia}m > ${umbralActual}m)"
        else
            "✅ Dentro del rango (${distancia}m / umbral ${umbralActual}m)"
    }

    fun getMascotaId() = mascotaSeleccionadaId
    fun getUmbral() = umbralActual
}

class SimulatorFragment : Fragment() {
    private var _binding: FragmentSimulatorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SimulatorViewModel by viewModels()
    private lateinit var session: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSimulatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        // Slider de distancia (0 – 200 metros)
        binding.sliderDistancia.valueFrom = 0f
        binding.sliderDistancia.valueTo = 200f
        binding.sliderDistancia.value = 0f
        binding.sliderDistancia.stepSize = 1f

        binding.sliderDistancia.addOnChangeListener { _, value, _ ->
            val dist = value.toInt()
            binding.tvDistanciaActual.text = "${dist}m"
            viewModel.setDistancia(dist)
            // Enviar al Wear en tiempo real
            enviarAlWear(dist)
        }

        // Spinner de mascotas
        viewModel.mascotas.observe(viewLifecycleOwner) { mascotas ->
            if (mascotas.isEmpty()) {
                binding.tvSinMascotas.visible()
                binding.layoutSimulator.gone()
                return@observe
            }
            binding.tvSinMascotas.gone()
            binding.layoutSimulator.visible()

            val nombres = mascotas.map { "${it.nombre} (umbral: ${it.distanciaAlerta}m)" }
            val adapter = android.widget.ArrayAdapter(
                requireContext(), android.R.layout.simple_spinner_item, nombres
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerMascota.adapter = adapter

            binding.spinnerMascota.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    viewModel.seleccionarMascota(mascotas[pos])
                    binding.tvUmbral.text = "Umbral de alerta: ${mascotas[pos].distanciaAlerta}m"
                    // Resetear slider
                    binding.sliderDistancia.value = 0f
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

            viewModel.seleccionarMascota(mascotas[0])
            binding.tvUmbral.text = "Umbral de alerta: ${mascotas[0].distanciaAlerta}m"
        }

        viewModel.mensaje.observe(viewLifecycleOwner) { msg ->
            binding.tvEstadoSimulacion.text = msg
            val esAlerta = msg.contains("🚨")
            binding.cardEstado.setCardBackgroundColor(
                if (esAlerta) requireContext().getColor(com.lomito.seguro.R.color.alerta_rojo_light)
                else requireContext().getColor(com.lomito.seguro.R.color.verde_light)
            )
        }

        binding.btnEnviarAlerta.setOnClickListener {
            val dist = binding.sliderDistancia.value.toInt()
            enviarAlWear(dist, forceAlert = true)
            enviarAlBackend(dist)
            toast("📡 Señal enviada al Watch")
        }

        viewModel.cargarMascotas(session.getUserId())
    }

    private fun enviarAlWear(distancia: Int, forceAlert: Boolean = false) {
        val context = requireContext().applicationContext
        val mascotaId = viewModel.getMascotaId()
        val umbral = viewModel.getUmbral()
        val superaUmbral = distancia > umbral || forceAlert

        val payload = JSONObject().apply {
            put("distancia", distancia)
            put("mascotaId", mascotaId)
            put("umbral", umbral)
            put("superaUmbral", superaUmbral)
        }.toString().toByteArray()

        // Método 1 — MessageClient (funciona con dispositivo físico)
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                android.util.Log.d("LOMITO_BLE", "Nodos: ${nodes.size}")
                nodes.forEach { node ->
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, "/ble/distancia", payload)
                        .addOnSuccessListener {
                            android.util.Log.d("LOMITO_BLE", "✅ Mensaje enviado a ${node.displayName}")
                        }
                        .addOnFailureListener {
                            android.util.Log.e("LOMITO_BLE", "❌ Error: ${it.message}")
                        }
                }
            }

        // Método 2 — DataClient (más confiable entre emuladores)
        val dataClient = Wearable.getDataClient(context)
        val putDataRequest = com.google.android.gms.wearable.PutDataMapRequest.create("/ble/distancia").apply {
            dataMap.putInt("distancia", distancia)
            dataMap.putString("mascotaId", mascotaId)
            dataMap.putInt("umbral", umbral)
            dataMap.putBoolean("superaUmbral", superaUmbral)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(putDataRequest)
            .addOnSuccessListener { android.util.Log.d("LOMITO_BLE", "✅ DataClient enviado") }
            .addOnFailureListener { android.util.Log.e("LOMITO_BLE", "❌ DataClient error: ${it.message}") }
    }
    private fun enviarAlBackend(distancia: Int) {
        val mascotaId = viewModel.getMascotaId()
        val umbral = viewModel.getUmbral()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ✅ CAMBIADO: Usa el endpoint POST /api/simulador/distancia
                val url = java.net.URL("http://192.168.100.12:3000/api/simulador/distancia")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                // Crear JSON con los datos
                val json = JSONObject().apply {
                    put("distancia", distancia)
                    put("umbral", umbral)
                    put("mascotaId", mascotaId)
                }

                // Enviar datos
                conn.outputStream.write(json.toString().toByteArray())
                conn.outputStream.flush()
                conn.outputStream.close()

                val responseCode = conn.responseCode
                conn.disconnect()

                if (responseCode == 200) {
                    android.util.Log.d("LOMITO_SIM", "✅ Distancia $distancia m enviada al backend")
                    // Mostrar mensaje de éxito en la UI
                    requireActivity().runOnUiThread {
                        toast("✅ Distancia $distancia m enviada al backend")
                    }
                } else {
                    android.util.Log.e("LOMITO_SIM", "❌ Error HTTP: $responseCode")
                    requireActivity().runOnUiThread {
                        toast("❌ Error al enviar: $responseCode")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LOMITO_SIM", "❌ Error: ${e.message}", e)
                requireActivity().runOnUiThread {
                    toast("❌ Error: ${e.message}")
                }
            }
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
