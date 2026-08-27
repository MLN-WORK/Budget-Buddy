package com.example.budgetbuddy

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

enum class GaugePaletteMode {
    DEFAULT,
    COLOR_BLIND,
    CUSTOM;

    companion object {
        fun fromStored(value: String?): GaugePaletteMode =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}

data class GaugePalette(
    val good: Int,
    val okay: Int,
    val bad: Int
)

data class AppPalette(
    val main: Int,
    val surface: Int,
    val input: Int,
    val accent: Int,
    val onMain: Int,
    val onAccent: Int
)

object AppearanceDefaults {
    const val CUSTOM_ACCENT = 0xFF46B793.toInt()
    const val CUSTOM_MAIN = 0xFFFCF5E8.toInt()

    val DEFAULT_GAUGE = GaugePalette(
        good = 0xFF2E7D32.toInt(),
        okay = 0xFFF9A825.toInt(),
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
