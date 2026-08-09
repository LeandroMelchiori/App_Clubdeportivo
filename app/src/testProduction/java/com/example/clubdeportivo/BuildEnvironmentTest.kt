package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BuildEnvironmentTest {
    @Test
    fun production_usaIdentidadEstableYSinModoDemo() {
        assertEquals("production", BuildConfig.FLAVOR)
        assertFalse(BuildConfig.DEMO_MODE)
        assertEquals("com.example.clubdeportivo", BuildConfig.APPLICATION_ID)
    }
}
