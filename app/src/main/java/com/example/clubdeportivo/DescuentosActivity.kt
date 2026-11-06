package com.example.clubdeportivo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DescuentosActivity : AppCompatActivity() {
    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: DescuentoAdapter
    private lateinit var emptyState: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_descuentos)
        SystemBarInsets.applyTo(this)

        dbHelper = DBHelper(this)
        emptyState = findViewById(R.id.tvEstadoDescuentos)
        findViewById<Button>(R.id.btnVolver).setOnClickListener { finish() }

        adapter = DescuentoAdapter(
            currency = dbHelper.obtenerConfiguracionClub().currency,
            onEdit = { discount ->
                startActivity(
                    Intent(this, FormularioDescuentoActivity::class.java)
                        .putExtra(FormularioDescuentoActivity.EXTRA_DISCOUNT_ID, discount.id)
                )
            },
            onDelete = ::confirmDelete
        )
        findViewById<RecyclerView>(R.id.rvDescuentos).apply {
            layoutManager = LinearLayoutManager(this@DescuentosActivity)
            adapter = this@DescuentosActivity.adapter
        }
        findViewById<FloatingActionButton>(R.id.fabNuevoDescuento).setOnClickListener {
            startActivity(Intent(this, FormularioDescuentoActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    private fun refresh() {
        val discounts = dbHelper.obtenerDescuentos()
        adapter.submit(discounts)
        emptyState.visibility = if (discounts.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmDelete(discount: Discount) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_discount_title)
            .setMessage(getString(R.string.delete_discount_message, discount.name))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                if (dbHelper.eliminarDescuento(discount.id)) {
                    Toast.makeText(this, R.string.discount_deleted, Toast.LENGTH_SHORT).show()
                    refresh()
                } else {
                    Toast.makeText(this, R.string.discount_in_use, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}

class DescuentoAdapter(
    private val currency: ClubCurrency,
    private val onEdit: (Discount) -> Unit,
    private val onDelete: (Discount) -> Unit
) : RecyclerView.Adapter<DescuentoAdapter.ViewHolder>() {
    private var discounts: List<Discount> = emptyList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvNombreDescuento)
        val detail: TextView = view.findViewById(R.id.tvDetalleDescuento)
        val edit: Button = view.findViewById(R.id.btnEditar)
        val delete: Button = view.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_descuento, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val discount = discounts[position]
        holder.name.text = discount.name
        holder.detail.text = BenefitText.discountDetail(discount, currency)
        holder.edit.contentDescription = "${holder.edit.text} ${discount.name}"
        holder.delete.contentDescription = "${holder.delete.text} ${discount.name}"
        holder.edit.setOnClickListener { onEdit(discount) }
        holder.delete.setOnClickListener { onDelete(discount) }
    }

    override fun getItemCount(): Int = discounts.size

    @android.annotation.SuppressLint("NotifyDataSetChanged")
    fun submit(newDiscounts: List<Discount>) {
        discounts = newDiscounts
        notifyDataSetChanged()
    }
}
