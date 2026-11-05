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
    fun applyTo(activity: Activity) {
        applyTo(activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0))
    }

    fun applyTo(root: View) {
        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val bottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(
                left = left + bars.left,
                top = top + bars.top,
                right = right + bars.right,
                bottom = bottom + bars.bottom
            )
            insets
        }
    }
}
