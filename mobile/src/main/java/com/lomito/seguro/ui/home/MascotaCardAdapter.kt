package com.lomito.seguro.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lomito.seguro.R
import com.lomito.seguro.data.model.Mascota
import com.lomito.seguro.databinding.ItemMascotaCardBinding

class MascotaCardAdapter(
    private val onClick: (Mascota) -> Unit
) : ListAdapter<Mascota, MascotaCardAdapter.VH>(DiffCallback()) {

    inner class VH(val binding: ItemMascotaCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mascota: Mascota) {
            binding.tvNombre.text = mascota.nombre
            binding.tvEspecie.text = "${mascota.especie} • ${mascota.raza}"
            binding.tvEstado.text = when (mascota.estado) {
                "EN_CASA" -> "✅ En casa"
                "PERDIDA" -> "🚨 Perdida"
                "ENCONTRADA" -> "✅ Encontrada"
                else -> mascota.estado
            }
            val estadoColor = when (mascota.estado) {
                "PERDIDA" -> R.color.alerta_rojo
                "ENCONTRADA" -> R.color.verde_ok
                else -> R.color.primary
            }
            binding.tvEstado.setTextColor(ContextCompat.getColor(binding.root.context, estadoColor))

            if (!mascota.fotoUrl.isNullOrEmpty()) {
                Glide.with(binding.root).load(mascota.fotoUrl)
                    .placeholder(R.drawable.ic_pet_placeholder).into(binding.ivMascota)
            } else {
                binding.ivMascota.setImageResource(
                    if (mascota.especie == "GATO") R.drawable.ic_cat else R.drawable.ic_dog
                )
            }
            binding.root.setOnClickListener { onClick(mascota) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemMascotaCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Mascota>() {
        override fun areItemsTheSame(a: Mascota, b: Mascota) = a.id == b.id
        override fun areContentsTheSame(a: Mascota, b: Mascota) = a == b
    }
}
