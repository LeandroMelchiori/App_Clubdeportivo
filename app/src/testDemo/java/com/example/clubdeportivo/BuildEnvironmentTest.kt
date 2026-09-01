package com.example.clubdeportivo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildEnvironmentTest {
    @Test
    fun demo_usaIdentidadYBanderaDeDemostracion() {
        assertEquals("demo", BuildConfig.FLAVOR)
        assertTrue(BuildConfig.DEMO_MODE)
        assertTrue(BuildConfig.APPLICATION_ID.endsWith(".demo"))
    }
}
