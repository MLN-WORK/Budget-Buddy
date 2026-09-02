package com.budgetbuddy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/*
 * Start of class
 * Name of class and related classes (parent/child classes): AppearancePreferencesTest
 * Parent class: Any; child classes: none; related classes: AppearancePreferences, JUnit, and the application code under test.
 * What the class does: Verifies the AppearancePreferences behavior and its regression cases.
 * What's important to other classes, if applicable: Its assertions document the behavior production classes must preserve.
 * Code with comments begins below.
 */
class AppearancePreferencesTest {
    @Test
    fun darkPaletteIsExactInverseOfLightPalette() {
        val light = AppearanceDefaults.LIGHT_APP
        val dark = AppearanceDefaults.DARK_APP

        assertEquals(invert(light.main), dark.main)
        assertEquals(invert(light.surface), dark.surface)
        assertEquals(invert(light.input), dark.input)
        assertEquals(invert(light.accent), dark.accent)
        assertEquals(invert(light.onMain), dark.onMain)
        assertEquals(invert(light.onAccent), dark.onAccent)
    }

    @Test
    fun defaultOkayGaugeUsesAClearYellowRatherThanBrown() {
        assertEquals(0xFFB8860B.toInt(), AppearanceDefaults.DEFAULT_GAUGE.okay)
    }

    @Test
    fun appearanceSelectionResolvesTheChosenGaugePalette() {
        val custom = GaugePalette(0xFF112233.toInt(), 0xFF445566.toInt(), 0xFF778899.toInt())
        val selection = AppearanceSelection(
            AppThemeMode.CUSTOM,
            AppearanceDefaults.CUSTOM_ACCENT,
            AppearanceDefaults.CUSTOM_MAIN,
            GaugePaletteMode.CUSTOM,
            custom
        )

        assertEquals(custom, selection.gaugePalette)
        assertEquals(AppearanceDefaults.CUSTOM_ACCENT, selection.appPalette.accent)
    }

    @Test
    fun previewDraftCanBeDiscardedWithoutChangingTheSavedSelection() {
        val saved = AppearanceSelection(
            AppThemeMode.LIGHT,
            AppearanceDefaults.CUSTOM_ACCENT,
            AppearanceDefaults.CUSTOM_MAIN,
            GaugePaletteMode.DEFAULT,
            AppearanceDefaults.DEFAULT_GAUGE
        )
        val preview = saved.copy(themeMode = AppThemeMode.AMOLED)

        AppearancePreviewStore.clear()
        assertEquals(saved, AppearancePreviewStore.begin(saved))
        AppearancePreviewStore.update(preview)
        assertEquals(preview, AppearancePreviewStore.current)
        AppearancePreviewStore.clear()
        assertNull(AppearancePreviewStore.current)
    }

    private fun invert(color: Int): Int =
        (color and 0xFF000000.toInt()) or ((color xor 0x00FFFFFF) and 0x00FFFFFF)
}
// End of class: AppearancePreferencesTest
