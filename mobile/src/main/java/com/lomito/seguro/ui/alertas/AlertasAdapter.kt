package com.lomito.seguro.ui.alertas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lomito.seguro.R
import com.lomito.seguro.databinding.ItemAlertaBinding
import com.lomito.seguro.models.Alerta
import java.text.SimpleDateFormat
import java.util.*

class AlertasAdapter(
    private val onItemClick: (Alerta) -> Unit,
    private val onMarcarLeida: (Int) -> Unit
) : ListAdapter<Alerta, AlertasAdapter.AlertaViewHolder>(AlertaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertaViewHolder {
        val binding = ItemAlertaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlertaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertaViewHolder, position: Int) {
        val alerta = getItem(position)
        holder.bind(alerta)
    }

    inner class AlertaViewHolder(
        private val binding: ItemAlertaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alerta: Alerta) {
            // ✅ Icono según tipo
            val icono = when (alerta.tipo) {
                "AVISTAMIENTO" -> "👁️"
                "PERDIDA" -> "🐾"
                "ENCONTRADA" -> "✅"
                "UBICACION" -> "📍"
                else -> "📢"
            }
            binding.tvIcono.text = icono

            // ✅ Título y mensaje
            binding.tvTitulo.text = when (alerta.tipo) {
                "AVISTAMIENTO" -> "👁️ Avistamiento"
                "PERDIDA" -> "🐾 Mascota perdida"
                "ENCONTRADA" -> "✅ Mascota encontrada"
                "UBICACION" -> "📍 Nueva ubicación"
                else -> alerta.tipo
            }
            binding.tvMensaje.text = alerta.mensaje

            // ✅ Estado de leída
            if (alerta.leida) {
                binding.tvLeida.text = "✓ Leída"
                binding.tvLeida.setTextColor(android.graphics.Color.parseColor("#4CD97B"))
                binding.tvTitulo.setTextColor(android.graphics.Color.parseColor("#8888AA"))
                binding.tvMensaje.setTextColor(android.graphics.Color.parseColor("#8888AA"))
                binding.btnMarcarLeida.visibility = View.GONE
            } else {
                binding.tvLeida.text = "● No leída"
                binding.tvLeida.setTextColor(android.graphics.Color.parseColor("#E85D5D"))
                binding.tvTitulo.setTextColor(android.graphics.Color.parseColor("#1A1A2E"))
                binding.tvMensaje.setTextColor(android.graphics.Color.parseColor("#555577"))
                binding.btnMarcarLeida.visibility = View.VISIBLE
            }

            // ✅ Fecha formateada
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.tvFecha.text = dateFormat.format(alerta.fecha)

            // ✅ Click en el item
            binding.root.setOnClickListener {
                onItemClick(alerta)
            }

            // ✅ Botón marcar como leída
            binding.btnMarcarLeida.setOnClickListener {
                if (!alerta.leida) {
                    onMarcarLeida(alerta.id)
                }
            }
        }
    }
}

class AlertaDiffCallback : DiffUtil.ItemCallback<Alerta>() {
    override fun areItemsTheSame(oldItem: Alerta, newItem: Alerta): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Alerta, newItem: Alerta): Boolean {
        return oldItem == newItem
    }
}