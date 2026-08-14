// mobile/ui/mural/MascotaPerdidaAdapter.kt
// Paquete: com.lomito.seguro.ui.mural
package com.lomito.seguro.ui.mural

// Importa la dependencia necesaria: LayoutInflater
import android.view.LayoutInflater
// Importa componentes de la interfaz gráfica
import android.view.View
// Importa componentes de la interfaz gráfica
import android.view.ViewGroup
// Importa componentes de la interfaz gráfica
import android.widget.ImageView
// Importa componentes de la interfaz gráfica
import android.widget.TextView
// Importa componentes de la interfaz gráfica
import androidx.recyclerview.widget.RecyclerView
// Importa la librería de carga de imágenes
import com.bumptech.glide.Glide
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.data.model.Mascota
// Importa la dependencia necesaria: toAbsoluteUrl
import com.lomito.seguro.util.toAbsoluteUrl

// Adaptador MascotaPerdidaAdapter: conecta los datos con la vista del RecyclerView
class MascotaPerdidaAdapter(
    // Constante onItemClick: valor inmutable que no cambia tras su asignación
    private val onItemClick: (Mascota) -> Unit
) : RecyclerView.Adapter<MascotaPerdidaAdapter.ViewHolder>() {

    // Variable items: almacena el estado mutable de este componente
    private var items: List<Mascota> = emptyList()

    // Función submitList: define la lógica de esta operación
    fun submitList(list: List<Mascota>) {
        items = list
        notifyDataSetChanged()
    }

    // Infla el layout y crea el ViewHolder para el RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Constante view: valor inmutable que no cambia tras su asignación
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mascota_mural, parent, false)
        // Retorna el valor al llamador de la función
        return ViewHolder(view)
    }

    // Vincula los datos del modelo con las vistas del ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Constante mascota: valor inmutable que no cambia tras su asignación
        val mascota = items[position]
        holder.bind(mascota)
        holder.itemView.setOnClickListener { onItemClick(mascota) }
    }

    // Retorna el número total de elementos en la lista
    override fun getItemCount(): Int = items.size

    // Declaración de la clase ViewHolder
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Constante ivFoto: valor inmutable que no cambia tras su asignación
        private val ivFoto: ImageView = itemView.findViewById(R.id.ivFotoMascota)
        // Constante tvNombre: valor inmutable que no cambia tras su asignación
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreMascota)
        // Constante tvRaza: valor inmutable que no cambia tras su asignación
        private val tvRaza: TextView = itemView.findViewById(R.id.tvRazaMascota)
        // Constante tvEstado: valor inmutable que no cambia tras su asignación
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstadoMascota)

        // Función bind: define la lógica de esta operación
        fun bind(mascota: Mascota) {
            tvNombre.text = mascota.nombre
            tvRaza.text = mascota.raza
            tvEstado.text = "🔴 PERDIDA"

            // Constante placeholder: valor inmutable que no cambia tras su asignación
            val placeholder = if (mascota.especie == "PERRO") R.drawable.ic_dog else R.drawable.ic_cat
            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (!mascota.fotoUrl.isNullOrEmpty()) {
                Glide.with(ivFoto).load(mascota.fotoUrl.toAbsoluteUrl())
                    .placeholder(placeholder).into(ivFoto)
            } else {
                ivFoto.setImageResource(placeholder)
            }
        }
    }
}