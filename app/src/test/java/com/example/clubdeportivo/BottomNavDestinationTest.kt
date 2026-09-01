package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BottomNavDestinationTest {
    @Test
    fun keyFor_mapeaItemsPrincipales() {
        assertEquals(BottomNavDestination.HOME, BottomNavDestination.keyFor(R.id.nav_home))
        assertEquals(BottomNavDestination.ACTIVIDADES, BottomNavDestination.keyFor(R.id.nav_activity))
        assertEquals(BottomNavDestination.CONFIGURACION, BottomNavDestination.keyFor(R.id.nav_settings))
        assertEquals(BottomNavDestination.LISTADOS, BottomNavDestination.keyFor(R.id.nav_listas))
        assertEquals(BottomNavDestination.PAGOS, BottomNavDestination.keyFor(R.id.nav_pagos))
    }

    @Test
    fun keyFor_devuelveNullParaItemDesconocido() {
        assertNull(BottomNavDestination.keyFor(-1))
    }
}
