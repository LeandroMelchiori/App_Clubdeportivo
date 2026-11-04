package com.example.clubdeportivo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ImportacionClienteAdapter :
    ListAdapter<ClientCsvImport.Row, ImportacionClienteAdapter.RowViewHolder>(DIFF) {

    class RowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFila: TextView = view.findViewById(R.id.tvFila)
        val tvEstado: TextView = view.findViewById(R.id.tvEstadoFila)
        val tvNombre: TextView = view.findViewById(R.id.tvNombreFila)
        val tvDetalle: TextView = view.findViewById(R.id.tvDetalleFila)
        val tvErrores: TextView = view.findViewById(R.id.tvErroresFila)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_importacion_cliente, parent, false)
        return RowViewHolder(view)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val row = getItem(position)
        holder.tvFila.text = ClientCsvImportText.rowLabel(row.lineNumber)
        holder.tvNombre.text = row.displayName
        holder.tvDetalle.text = ClientCsvImportText.rowDetail(row.dni, row.email)
        val context = holder.itemView.context
        if (row.isValid) {
            holder.tvEstado.setText(R.string.import_row_ready)
            holder.tvEstado.backgroundTintList = ContextCompat.getColorStateList(context, R.color.green_user)
            holder.tvErrores.visibility = View.GONE
        } else {
            holder.tvEstado.setText(R.string.import_row_has_errors)
            holder.tvEstado.backgroundTintList = ContextCompat.getColorStateList(context, R.color.error_on_dark)
            holder.tvErrores.text = ClientCsvImportText.rowErrors(row.errors)
            holder.tvErrores.visibility = View.VISIBLE
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<ClientCsvImport.Row>() {
            override fun areItemsTheSame(oldItem: ClientCsvImport.Row, newItem: ClientCsvImport.Row) =
                oldItem.lineNumber == newItem.lineNumber

            override fun areContentsTheSame(oldItem: ClientCsvImport.Row, newItem: ClientCsvImport.Row) =
                oldItem == newItem
        }
    }
}
