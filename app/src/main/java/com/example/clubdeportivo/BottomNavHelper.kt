package com.example.clubdeportivo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

object BottomNavHelper {
    fun setup(activity: AppCompatActivity, usuario: String, selectedItemId: Int) {
        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottomNav)
        bottom.selectedItemId = selectedItemId
        bottom.setOnItemSelectedListener { item ->
            if (item.itemId == selectedItemId) {
                true
            } else {
                val target = targetFor(item.itemId)
                if (target == null) {
                    true
                } else {
                    activity.startActivity(
                        Intent(activity, target).putExtra(SessionExtras.USUARIO, usuario)
                    )
                    true
                }
            }
        }
    }

    private fun targetFor(itemId: Int): Class<*>? = when (BottomNavDestination.keyFor(itemId)) {
        BottomNavDestination.HOME -> InicioActivity::class.java
        BottomNavDestination.ACTIVIDADES -> ActividadesActivity::class.java
        BottomNavDestination.CONFIGURACION -> ConfiguracionActivity::class.java
        BottomNavDestination.LISTADOS -> ListadosActivity::class.java
        BottomNavDestination.PAGOS -> ResumenMensualActivity::class.java
        else -> null
    }
}
