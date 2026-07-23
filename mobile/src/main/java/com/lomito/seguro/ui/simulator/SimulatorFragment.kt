package com.lomito.seguro.ui.simulator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.Wearable
import com.lomito.seguro.R
import com.lomito.seguro.data.model.Mascota
import com.lomito.seguro.data.repository.LomitoRepository
import com.lomito.seguro.databinding.FragmentSimulatorBinding
import com.lomito.seguro.util.SessionManager
import com.lomito.seguro.util.gone
import com.lomito.seguro.util.toast
import com.lomito.seguro.util.visible
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
    private var debounceJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSimulatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        binding.sliderDistancia.valueFrom = 0f
        binding.sliderDistancia.valueTo = 200f
        binding.sliderDistancia.value = 0f
        binding.sliderDistancia.stepSize = 1f

        binding.sliderDistancia.addOnChangeListener { _, value, _ ->
            val dist = value.toInt()
            binding.tvDistanciaActual.text = "${dist}m"
            viewModel.setDistancia(dist)

            // ✅ forceAlert automático si supera el umbral
            val superaUmbral = dist > viewModel.getUmbral()
            enviarAlWear(dist, forceAlert = superaUmbral)

            // ✅ Debounce 500ms para no spamear el backend
            debounceJob?.cancel()
            debounceJob = lifecycleScope.launch {
                delay(500)
                enviarAlBackend(dist)
            }
        }

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
                if (esAlerta) requireContext().getColor(R.color.alerta_rojo_light)
                else requireContext().getColor(R.color.verde_light)
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

        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { node ->
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, "/ble/distancia", payload)
                        .addOnSuccessListener {
                            android.util.Log.d("SIMULATOR", "✅ Mensaje enviado a ${node.displayName}")
                        }
                        .addOnFailureListener {
                            android.util.Log.e("SIMULATOR", "❌ Error: ${it.message}")
                        }
                }
            }

        val putDataRequest = com.google.android.gms.wearable.PutDataMapRequest.create("/ble/distancia").apply {
            dataMap.putInt("distancia", distancia)
            dataMap.putString("mascotaId", mascotaId)
            dataMap.putInt("umbral", umbral)
            dataMap.putBoolean("superaUmbral", superaUmbral)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(putDataRequest)
    }

    private fun enviarAlBackend(distancia: Int) {
        val mascotaId = viewModel.getMascotaId()
        val umbral = viewModel.getUmbral()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://192.168.100.12:3000/api/simulador/distancia")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val json = JSONObject().apply {
                    put("distancia", distancia)
                    put("umbral", umbral)
                    put("mascotaId", mascotaId)
                }

                conn.outputStream.write(json.toString().toByteArray())
                conn.outputStream.flush()
                conn.outputStream.close()

                val responseCode = conn.responseCode
                conn.disconnect()
                android.util.Log.d("SIMULATOR", "📡 Backend actualizado: ${distancia}m (HTTP $responseCode)")
            } catch (e: Exception) {
                android.util.Log.e("SIMULATOR", "❌ Error backend: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        debounceJob?.cancel()
        _binding = null
    }
}