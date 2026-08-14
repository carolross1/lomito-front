// Paquete: com.lomito.seguro.ui.home
package com.lomito.seguro.ui.home

// Importa la dependencia necesaria: LayoutInflater
import android.view.LayoutInflater
// Importa componentes de la interfaz gráfica
import android.view.ViewGroup
// Importa el contexto de Android
import androidx.core.content.ContextCompat
// Importa la dependencia necesaria: DiffUtil
import androidx.recyclerview.widget.DiffUtil
// Importa la dependencia necesaria: ListAdapter
import androidx.recyclerview.widget.ListAdapter
// Importa componentes de la interfaz gráfica
import androidx.recyclerview.widget.RecyclerView
// Importa la librería de carga de imágenes
import com.bumptech.glide.Glide
// Importa la dependencia necesaria: R
import com.lomito.seguro.R
// Importa la dependencia necesaria: Mascota
import com.lomito.seguro.data.model.Mascota
// Importa el sistema de View Binding
import com.lomito.seguro.databinding.ItemMascotaCardBinding
// Importa la dependencia necesaria: toAbsoluteUrl
import com.lomito.seguro.util.toAbsoluteUrl

// Adaptador MascotaCardAdapter: conecta los datos con la vista del RecyclerView
class MascotaCardAdapter(
    // Constante onClick: valor inmutable que no cambia tras su asignación
    private val onClick: (Mascota) -> Unit
) : ListAdapter<Mascota, MascotaCardAdapter.VH>(DiffCallback()) {

    // Accede a un componente de UI a través del View Binding type-safe
    inner class VH(val binding: ItemMascotaCardBinding) : RecyclerView.ViewHolder(binding.root) {
        // Función bind: define la lógica de esta operación
        fun bind(mascota: Mascota) {
            // Actualiza el componente de UI a través del View Binding
            binding.tvNombre.text = mascota.nombre
            // Actualiza el componente de UI a través del View Binding
            binding.tvEspecie.text = "${mascota.especie} • ${mascota.raza}"
            // Actualiza el componente de UI a través del View Binding
            binding.tvEstado.text = when (mascota.estado) {
                "EN_CASA" -> "✅ En casa"
                "PERDIDA" -> "🚨 Perdida"
                "ENCONTRADA" -> "✅ Encontrada"
                else -> mascota.estado
            }
            // Constante estadoColor: valor inmutable que no cambia tras su asignación
            val estadoColor = when (mascota.estado) {
                "PERDIDA" -> R.color.alerta_rojo
                "ENCONTRADA" -> R.color.verde_ok
                else -> R.color.primary
            }
            // Accede a un componente de UI a través del View Binding type-safe
            binding.tvEstado.setTextColor(ContextCompat.getColor(binding.root.context, estadoColor))

            // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
            if (!mascota.fotoUrl.isNullOrEmpty()) {
                // Constante iv: valor inmutable que no cambia tras su asignación
                val iv = binding.ivMascota
                iv.imageTintList = null
                iv.setPadding(0, 0, 0, 0)
                // Accede a un componente de UI a través del View Binding type-safe
                Glide.with(binding.root).load(mascota.fotoUrl.toAbsoluteUrl())
                    .placeholder(R.drawable.ic_pet_placeholder)
                    .circleCrop()
                    .into(iv)
            } else {
                // Constante iv: valor inmutable que no cambia tras su asignación
                val iv = binding.ivMascota
                // Constante pad: valor inmutable que no cambia tras su asignación
                val pad = (8 * binding.root.resources.displayMetrics.density).toInt()
                iv.setPadding(pad, pad, pad, pad)
                // Actualiza el componente de UI a través del View Binding
                iv.imageTintList = ContextCompat.getColorStateList(binding.root.context, R.color.white)
                iv.setImageResource(
                    // Condición: evalúa si se cumplen los requisitos para ejecutar el bloque
                    if (mascota.especie == "GATO") R.drawable.ic_cat else R.drawable.ic_dog
                )
            }
            // Accede a un componente de UI a través del View Binding type-safe
            binding.root.setOnClickListener { onClick(mascota) }
        }
    }

    // Infla el layout y crea el ViewHolder para el RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemMascotaCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    // Vincula los datos del modelo con las vistas del ViewHolder
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    // Declaración de la clase DiffCallback
    class DiffCallback : DiffUtil.ItemCallback<Mascota>() {
        // Sobreescribe la función areItemsTheSame de la clase padre
        override fun areItemsTheSame(a: Mascota, b: Mascota) = a.id == b.id
        // Sobreescribe la función areContentsTheSame de la clase padre
        override fun areContentsTheSame(a: Mascota, b: Mascota) = a == b
    }
}
