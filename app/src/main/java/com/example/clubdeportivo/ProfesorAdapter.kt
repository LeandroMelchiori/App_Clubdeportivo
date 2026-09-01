package com.example.clubdeportivo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class ProfesorAdapter(
    private var list: List<DBHelper.Profesor>,
    private val onEdit: (DBHelper.Profesor) -> Unit,
    private val onDelete: (DBHelper.Profesor) -> Unit
) : RecyclerView.Adapter<ProfesorAdapter.ProfesorViewHolder>() {

    class ProfesorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreProfesor)
        val tvDni: TextView = view.findViewById(R.id.tvDniProfesor)
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloProfesor)
        val tvEstado: TextView = view.findViewById(R.id.tvEstadoProfesor)
        val btnEditar: Button = view.findViewById(R.id.btnEditar)
        val btnEliminar: Button = view.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfesorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profesor, parent, false)
        return ProfesorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfesorViewHolder, position: Int) {
        val item = list[position]
        val context = holder.itemView.context
        holder.tvNombre.text = context.getString(R.string.professor_display_name, item.apellido, item.nombre)
        holder.tvDni.text = context.getString(R.string.professor_dni, item.dni)
        holder.tvTitulo.text = context.getString(R.string.professor_title, item.titulo ?: "-")

        if (item.activo) {
            holder.tvEstado.setText(R.string.professor_status_active)
            holder.tvEstado.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.green_user))
            holder.btnEliminar.isEnabled = true
            holder.btnEliminar.alpha = 1.0f
        } else {
            holder.tvEstado.setText(R.string.professor_status_inactive)
            holder.tvEstado.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.red_user))
            holder.btnEliminar.isEnabled = false
            holder.btnEliminar.alpha = 0.5f
        }

        holder.btnEditar.setOnClickListener { onEdit(item) }
        holder.btnEliminar.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<DBHelper.Profesor>) {
        val previous = list
        val updates = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = previous.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPosition: Int, newPosition: Int) =
                previous[oldPosition].dni == newList[newPosition].dni

            override fun areContentsTheSame(oldPosition: Int, newPosition: Int) =
                previous[oldPosition] == newList[newPosition]
        })
        list = newList
        updates.dispatchUpdatesTo(this)
    }
}
