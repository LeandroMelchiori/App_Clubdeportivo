package com.example.clubdeportivo

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

// Con targetSdk 35+ Android 15 dibuja la app detras de las barras del sistema.
// Las pantallas sin BottomNavigationView suman ese espacio al padding de su raiz.
object SystemBarInsets {
    private val bars = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()

    fun applyTo(activity: Activity) {
        applyTo(rootOf(activity))
    }

    fun applyTo(root: View) {
        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val bottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(bars)
            view.updatePadding(
                left = left + bars.left,
                top = top + bars.top,
                right = right + bars.right,
                bottom = bottom + bars.bottom
            )
            insets
        }
    }

    // Las pantallas con BottomNavigationView solo reservan la barra de estado: la barra
    // inferior ya suma el inset de navegacion. Va en la raiz y no en el ScrollView porque
    // al desplazarse hasta una vista el ScrollView no descuenta su propio padding.
    fun applyTopTo(activity: Activity) {
        val root = rootOf(activity)
        val top = root.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            view.updatePadding(top = top + insets.getInsets(bars).top)
            insets
        }
    }

    private fun rootOf(activity: Activity): View =
        activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
}
