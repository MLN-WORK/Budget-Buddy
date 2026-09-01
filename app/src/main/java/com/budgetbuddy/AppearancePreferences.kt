package com.budgetbuddy

/*
 * Start of class
 * Name of class and related classes (parent/child classes): AppThemeMode
 * Parent class: Enum; child classes: none; related classes: AppearanceSelection, BaseActivity, and BudgetBuddyApplication.
 * What the class does: Names every supported application theme mode.
 * What's important to other classes, if applicable: Related classes depend on this class keeping its inputs validated and its output contract deterministic.
 * Code with comments begins below.
 */
enum class AppThemeMode {
    LIGHT,
    DARK,
    MATERIAL_YOU,
    AMOLED,
    CUSTOM;

    companion object {
        fun fromStored(value: String?, legacyDark: Boolean, legacyMaterialYou: Boolean): AppThemeMode =
            entries.firstOrNull { it.name == value }
                ?: when {
                    legacyMaterialYou -> MATERIAL_YOU
                    legacyDark -> DARK
                    else -> LIGHT
                }
    }
}
// End of class: AppThemeMode

/*
 * Start of class
 * Name of class and related classes (parent/child classes): GaugePaletteMode
 * Parent class: Enum; child classes: none; related classes: AppearanceSelection, GaugePalette, and LocalDataStore.
 * What the class does: Names the built-in and custom gauge palette choices.
 * What's important to other classes, if applicable: Related classes depend on this class keeping its inputs validated and its output contract deterministic.
 * Code with comments begins below.
 */
enum class GaugePaletteMode {
    DEFAULT,
    COLOR_BLIND,
    CUSTOM;

    companion object {
        fun fromStored(value: String?): GaugePaletteMode =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}
// End of class: GaugePaletteMode

/*
 * Start of class
 * Name of class and related classes (parent/child classes): GaugePalette
 * Parent class: Any; child classes: none; related classes: AppearanceSelection, AnalyticsActivity, MainActivity, and TutorialFlow.
 * What the class does: Carries the good, caution, and bad status colours shared by gauges and analytics.
 * What's important to other classes, if applicable: Consumers rely on its property meanings remaining stable across persistence, calculation, and display code.
 * Code with comments begins below.
 */
data class GaugePalette(
    val good: Int,
    val okay: Int,
    val bad: Int
)
// End of class: GaugePalette

/*
 * Start of class
 * Name of class and related classes (parent/child classes): AppPalette
 * Parent class: Any; child classes: none; related classes: AppearanceSelection, AppearanceDefaults, and RuntimePaletteApplier.
 * What the class does: Carries the resolved background, surface, input, accent, and foreground colours.
 * What's important to other classes, if applicable: Consumers rely on its property meanings remaining stable across persistence, calculation, and display code.
 * Code with comments begins below.
 */
data class AppPalette(
    val main: Int,
    val surface: Int,
    val input: Int,
    val accent: Int,
    val onMain: Int,
    val onAccent: Int
)
// End of class: AppPalette

/*
 * Start of class
 * Name of class and related classes (parent/child classes): AppearanceSelection
 * Parent class: Any; child classes: none; related classes: ThemeColorsActivity, AppearancePreviewStore, and BaseActivity.
 * What the class does: Represents one complete theme and gauge selection, saved or previewed.
 * What's important to other classes, if applicable: Consumers rely on its property meanings remaining stable across persistence, calculation, and display code.
 * Code with comments begins below.
 */
data class AppearanceSelection(
    val themeMode: AppThemeMode,
    val customAccent: Int,
    val customMain: Int,
    val gaugeMode: GaugePaletteMode,
    val customGauge: GaugePalette
) {
    val appPalette: AppPalette
        get() = when (themeMode) {
            AppThemeMode.LIGHT, AppThemeMode.MATERIAL_YOU -> AppearanceDefaults.LIGHT_APP
            AppThemeMode.DARK -> AppearanceDefaults.DARK_APP
            AppThemeMode.AMOLED -> AppearanceDefaults.AMOLED_APP
            AppThemeMode.CUSTOM -> AppearanceDefaults.customAppPalette(customMain, customAccent)
        }

    val gaugePalette: GaugePalette
        get() = when (gaugeMode) {
            GaugePaletteMode.DEFAULT -> AppearanceDefaults.DEFAULT_GAUGE
            GaugePaletteMode.COLOR_BLIND -> AppearanceDefaults.COLOR_BLIND_GAUGE
            GaugePaletteMode.CUSTOM -> customGauge
        }

    companion object {
        fun from(localData: LocalDataStore) = AppearanceSelection(
            themeMode = localData.appThemeMode,
            customAccent = localData.customAccentColor,
            customMain = localData.customMainColor,
            gaugeMode = localData.gaugePaletteMode,
            customGauge = localData.customGaugePalette
        )
    }
}
// End of class: AppearanceSelection

/** Holds only the unsaved appearance draft while the appearance screen is being previewed. */
/*
 * Start of class
 * Name of class and related classes (parent/child classes): AppearancePreviewStore
 * Parent class: Any; child classes: none; related classes: ThemeColorsActivity, BaseActivity, and BudgetBuddyApplication.
 * What the class does: Keeps the appearance screen's unsaved live-preview draft in process memory.
 * What's important to other classes, if applicable: ThemeColorsActivity and BaseActivity depend on this contract so previews are immediate, reversible, and consistent.
 * Code with comments begins below.
 */
object AppearancePreviewStore {
    @Volatile
    var current: AppearanceSelection? = null
        private set

    fun begin(saved: AppearanceSelection): AppearanceSelection = current ?: saved.also { current = it }

    fun update(selection: AppearanceSelection) {
        current = selection
    }

    fun clear() {
        current = null
    }
}
// End of class: AppearancePreviewStore

/*
 * Start of class
 * Name of class and related classes (parent/child classes): AppearanceDefaults
 * Parent class: Any; child classes: none; related classes: AppPalette, GaugePalette, BaseActivity, and LocalDataStore.
 * What the class does: Defines built-in palettes and derives readable custom palettes.
 * What's important to other classes, if applicable: ThemeColorsActivity and BaseActivity depend on this contract so previews are immediate, reversible, and consistent.
 * Code with comments begins below.
 */
object AppearanceDefaults {
    const val CUSTOM_ACCENT = 0xFF46B793.toInt()
    const val CUSTOM_MAIN = 0xFFFCF5E8.toInt()

    val LIGHT_APP = AppPalette(
        main = 0xFFFCF5E8.toInt(),
        surface = 0xFFEADFCB.toInt(),
        input = 0xFFFFFFFF.toInt(),
        accent = 0xFF46B793.toInt(),
        onMain = 0xFF000000.toInt(),
        onAccent = 0xFFFFFFFF.toInt()
    )
    val DARK_APP = AppPalette(
        main = 0xFF030A17.toInt(),
        surface = 0xFF152034.toInt(),
        input = 0xFF000000.toInt(),
        accent = 0xFFB9486C.toInt(),
        onMain = 0xFFFFFFFF.toInt(),
        onAccent = 0xFF000000.toInt()
    )
    val AMOLED_APP = AppPalette(
        main = 0xFF000000.toInt(),
        surface = 0xFF080808.toInt(),
        input = 0xFF151515.toInt(),
        accent = 0xFF52D6AD.toInt(),
        onMain = 0xFFFFFFFF.toInt(),
        onAccent = 0xFF000000.toInt()
    )

    val DEFAULT_GAUGE = GaugePalette(
        good = 0xFF2E7D32.toInt(),
        okay = 0xFFFFD600.toInt(),
        bad = 0xFFC62828.toInt()
    )
    val COLOR_BLIND_GAUGE = GaugePalette(
        good = 0xFF0072B2.toInt(),
        okay = 0xFFE69F00.toInt(),
        bad = 0xFFCC79A7.toInt()
    )

    fun customAppPalette(main: Int, accent: Int): AppPalette {
        val dark = perceivedLuminance(main) < 0.45
        val contrastMain = if (dark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        val contrastAccent = if (perceivedLuminance(accent) < 0.45) {
            0xFFFFFFFF.toInt()
        } else {
            0xFF000000.toInt()
        }
        return AppPalette(
            main = opaque(main),
            surface = blend(main, if (dark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt(), 0.09f),
            input = blend(main, 0xFFFFFFFF.toInt(), if (dark) 0.14f else 0.45f),
            accent = opaque(accent),
            onMain = contrastMain,
            onAccent = contrastAccent
        )
    }

    fun perceivedLuminance(color: Int): Double {
        val red = (color ushr 16) and 0xFF
        val green = (color ushr 8) and 0xFF
        val blue = color and 0xFF
        return (0.2126 * red + 0.7152 * green + 0.0722 * blue) / 255.0
    }

    private fun opaque(color: Int): Int = color or 0xFF000000.toInt()

    private fun blend(first: Int, second: Int, ratio: Float): Int {
        fun channel(shift: Int): Int {
            val a = (first ushr shift) and 0xFF
            val b = (second ushr shift) and 0xFF
            return (a + (b - a) * ratio).toInt().coerceIn(0, 255)
        }
        return 0xFF000000.toInt() or
            (channel(16) shl 16) or
            (channel(8) shl 8) or
            channel(0)
    }
}
// End of class: AppearanceDefaults
