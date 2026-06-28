// mobile/ui/mural/MascotaPerdidaAdapter.kt
package com.lomito.seguro.ui.mural

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lomito.seguro.R
import com.lomito.seguro.data.model.Mascota

class MascotaPerdidaAdapter(
    private val onItemClick: (Mascota) -> Unit
) : RecyclerView.Adapter<MascotaPerdidaAdapter.ViewHolder>() {

    private var items: List<Mascota> = emptyList()

    fun submitList(list: List<Mascota>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mascota_mural, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mascota = items[position]
        holder.bind(mascota)
        holder.itemView.setOnClickListener { onItemClick(mascota) }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivFoto: ImageView = itemView.findViewById(R.id.ivFotoMascota)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreMascota)
        private val tvRaza: TextView = itemView.findViewById(R.id.tvRazaMascota)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstadoMascota)

        fun bind(mascota: Mascota) {
            tvNombre.text = mascota.nombre
            tvRaza.text = mascota.raza
            tvEstado.text = "🔴 PERDIDA"

            // Si tiene foto, cargarla (usar Glide o Coil)
            // Por ahora usamos placeholder
            ivFoto.setImageResource(
                if (mascota.especie == "PERRO") R.drawable.ic_dog else R.drawable.ic_cat
            )
        }
    }
}