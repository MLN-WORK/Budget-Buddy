package com.example.budgetbuddy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearancePreferencesTest {
    @Test
    fun `legacy appearance preferences migrate to one theme mode`() {
        assertEquals(AppThemeMode.MATERIAL_YOU, AppThemeMode.fromStored(null, true, true))
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromStored(null, true, false))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromStored(null, false, false))
        assertEquals(AppThemeMode.AMOLED, AppThemeMode.fromStored("AMOLED", false, false))
    }

    @Test
    fun `default gauge follows green yellow red semantics`() {
        assertEquals(0xFF2E7D32.toInt(), AppearanceDefaults.DEFAULT_GAUGE.good)
        assertEquals(0xFFF9A825.toInt(), AppearanceDefaults.DEFAULT_GAUGE.okay)
        assertEquals(0xFFC62828.toInt(), AppearanceDefaults.DEFAULT_GAUGE.bad)
    }

    @Test
    fun `custom palette preserves exact chosen main and accent colors`() {
        val palette = AppearanceDefaults.customAppPalette(
            main = 0xFF102030.toInt(),
            accent = 0xFFE0C050.toInt()
        )

        assertEquals(0xFF102030.toInt(), palette.main)
        assertEquals(0xFFE0C050.toInt(), palette.accent)
        assertEquals(0xFFFFFFFF.toInt(), palette.onMain)
        assertEquals(0xFF000000.toInt(), palette.onAccent)
        assertTrue(AppearanceDefaults.perceivedLuminance(palette.surface) > 0.0)
    }
}
