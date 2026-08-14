// Paquete: com.lomito.seguro.ui.alertas
package com.lomito.seguro.ui.alertas

// Importa la dependencia necesaria: LayoutInflater
import android.view.LayoutInflater
// Importa componentes de la interfaz gráfica
import android.view.View
// Importa componentes de la interfaz gráfica
import android.view.ViewGroup
// Importa la dependencia necesaria: DiffUtil
import androidx.recyclerview.widget.DiffUtil
// Importa la dependencia necesaria: ListAdapter
import androidx.recyclerview.widget.ListAdapter
// Importa componentes de la interfaz gráfica
import androidx.recyclerview.widget.RecyclerView
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa el sistema de View Binding
import com.lomito.seguro.databinding.ItemAlertaBinding
// Importa la dependencia necesaria: Alerta
import com.lomito.seguro.models.Alerta
// Importa la dependencia necesaria: SimpleDateFormat
import java.text.SimpleDateFormat
// Importa la dependencia necesaria: *
import java.util.*

// Adaptador AlertasAdapter: conecta los datos con la vista del RecyclerView
class AlertasAdapter(
    // Constante onItemClick: valor inmutable que no cambia tras su asignación
    private val onItemClick: (Alerta) -> Unit,
    // Constante onMarcarLeida: valor inmutable que no cambia tras su asignación
    private val onMarcarLeida: (Int) -> Unit
) : ListAdapter<Alerta, AlertasAdapter.AlertaViewHolder>(AlertaDiffCallback()) {

    // Infla el layout y crea el ViewHolder para el RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertaViewHolder {
        // Constante binding: valor inmutable que no cambia tras su asignación
        val binding = ItemAlertaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Retorna el valor al llamador de la función
        return AlertaViewHolder(binding)
    }

    // Vincula los datos del modelo con las vistas del ViewHolder
    override fun onBindViewHolder(holder: AlertaViewHolder, position: Int) {
        // Constante alerta: valor inmutable que no cambia tras su asignación
        val alerta = getItem(position)
        holder.bind(alerta)
    }

    inner class AlertaViewHolder(
        // Constante binding: valor inmutable que no cambia tras su asignación
        private val binding: ItemAlertaBinding
    // Accede a un componente de UI a través del View Binding type-safe
    ) : RecyclerView.ViewHolder(binding.root) {

        // Función bind: define la lógica de esta operación
        fun bind(alerta: Alerta) {
            // ✅ Icono según tipo
            // Constante icono: valor inmutable que no cambia tras su asignación
            val icono = when (alerta.tipo) {
                "AVISTAMIENTO" -> "👁️"
                "PERDIDA" -> "🐾"
                "ENCONTRADA" -> "✅"
                "UBICACION" -> "📍"
                else -> "📢"
            }
            // Actualiza el componente de UI a través del View Binding
            binding.tvIcono.text = icono

            // ✅ Título y mensaje
            // Actualiza el componente de UI a través del View Binding
            binding.tvTitulo.text = when (alerta.tipo) {
                "AVISTAMIENTO" -> "👁️ Avistamiento"
                "PERDIDA" -> "🐾 Mascota perdida"
                "ENCONTRADA" -> "✅ Mascota encontrada"
                "UBICACION" -> "📍 Nueva ubicación"
                else -> alerta.tipo
            }
            // Actualiza el componente de UI a través del View Binding
            binding.tvMensaje.text = alerta.mensaje

            // ✅ Estado de leída
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (alerta.leida) {
                // Actualiza el componente de UI a través del View Binding
                binding.tvLeida.text = "✓ Leída"
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvLeida.setTextColor(android.graphics.Color.parseColor("#4CD97B"))
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvTitulo.setTextColor(android.graphics.Color.parseColor("#8888AA"))
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvMensaje.setTextColor(android.graphics.Color.parseColor("#8888AA"))
                // Actualiza el componente de UI a través del View Binding
                binding.btnMarcarLeida.visibility = View.GONE
            } else {
                // Actualiza el componente de UI a través del View Binding
                binding.tvLeida.text = "● No leída"
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvLeida.setTextColor(android.graphics.Color.parseColor("#E85D5D"))
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvTitulo.setTextColor(android.graphics.Color.parseColor("#1A1A2E"))
                // Accede a un componente de UI a través del View Binding type-safe
                binding.tvMensaje.setTextColor(android.graphics.Color.parseColor("#555577"))
                // Actualiza el componente de UI a través del View Binding
                binding.btnMarcarLeida.visibility = View.VISIBLE
            }

            // ✅ Fecha formateada
            // Constante dateFormat: valor inmutable que no cambia tras su asignación
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            // Actualiza el componente de UI a través del View Binding
            binding.tvFecha.text = dateFormat.format(alerta.fecha)

            // ✅ Click en el item
            // Accede a un componente de UI a través del View Binding type-safe
            binding.root.setOnClickListener {
                onItemClick(alerta)
            }

            // ✅ Botón marcar como leída
            // Accede a un componente de UI a través del View Binding type-safe
            binding.btnMarcarLeida.setOnClickListener {
                // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                if (!alerta.leida) {
                    onMarcarLeida(alerta.id)
                }
            }
        }
    }
}

// Declaración de la clase AlertaDiffCallback
class AlertaDiffCallback : DiffUtil.ItemCallback<Alerta>() {
    // Sobreescribe la función areItemsTheSame de la clase padre
    override fun areItemsTheSame(oldItem: Alerta, newItem: Alerta): Boolean {
        // Retorna el valor al llamador de la función
        return oldItem.id == newItem.id
    }

    // Sobreescribe la función areContentsTheSame de la clase padre
    override fun areContentsTheSame(oldItem: Alerta, newItem: Alerta): Boolean {
        // Retorna el valor al llamador de la función
        return oldItem == newItem
    }
}