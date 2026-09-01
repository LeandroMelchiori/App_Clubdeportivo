package com.example.clubdeportivo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class CatalogoActividadAdapter(
    private val currency: ClubCurrency,
    private var list: List<DBHelper.CatalogoActividad>,
    private val onEdit: (DBHelper.CatalogoActividad) -> Unit,
    private val onDelete: (DBHelper.CatalogoActividad) -> Unit
) : RecyclerView.Adapter<CatalogoActividadAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreActividad)
        val tvPrecio: TextView = view.findViewById(R.id.tvPrecioActividad)
        val btnEditar: Button = view.findViewById(R.id.btnEditar)
        val btnEliminar: Button = view.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_catalogo_actividad, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvNombre.text = item.nombre
        holder.tvPrecio.text = holder.itemView.context.getString(
            R.string.activity_base_price,
            MoneyFormatter.format(item.precio, currency)
        )

        holder.btnEditar.setOnClickListener { onEdit(item) }
        holder.btnEliminar.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<DBHelper.CatalogoActividad>) {
        val previous = list
        val updates = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = previous.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPosition: Int, newPosition: Int) =
                previous[oldPosition].id == newList[newPosition].id

            override fun areContentsTheSame(oldPosition: Int, newPosition: Int) =
                previous[oldPosition] == newList[newPosition]
        })
        list = newList
        updates.dispatchUpdatesTo(this)
    }
}
