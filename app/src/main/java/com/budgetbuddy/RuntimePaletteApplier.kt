package com.budgetbuddy

import android.content.res.ColorStateList
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ScaleDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors

/*
 * Start of class
 * Name of class and related classes (parent/child classes): RuntimePaletteApplier
 * Parent class: Any; child classes: ResolvedPalette; related classes: AppPalette, ResolvedPalette, BaseActivity, and ThemeColorsActivity.
 * What the class does: Maps inflated view colours to a custom application palette at runtime.
 * What's important to other classes, if applicable: ThemeColorsActivity and BaseActivity depend on this contract so previews are immediate, reversible, and consistent.
 * Code with comments begins below.
 */
object RuntimePaletteApplier {
    /*
     * Start of class
     * Name of class and related classes (parent/child classes): ResolvedPalette
     * Parent class: Any; child classes: none; related classes: RuntimePaletteApplier and AppPalette.
     * What the class does: Stores the source theme colours used during runtime palette mapping.
     * What's important to other classes, if applicable: Consumers rely on its property meanings remaining stable across persistence, calculation, and display code.
     * Code with comments begins below.
     */
    private data class ResolvedPalette(
        val background: Int,
        val surface: Int,
        val input: Int,
        val primary: Int,
        val text: Int,
        val onPrimary: Int
    )
    // End of class: ResolvedPalette

    fun apply(root: View, custom: AppPalette) {
        val resolved = ResolvedPalette(
            background = MaterialColors.getColor(root, R.attr.budgetBackgroundColor),
            surface = MaterialColors.getColor(root, R.attr.budgetSurfaceColor),
            input = MaterialColors.getColor(root, R.attr.budgetInputColor),
            primary = MaterialColors.getColor(root, R.attr.budgetPrimaryColor),
            text = MaterialColors.getColor(root, R.attr.budgetTextColor),
            onPrimary = MaterialColors.getColor(root, R.attr.budgetOnPrimaryColor)
        )
        walk(root) { view -> recolorView(view, resolved, custom) }
    }

    /** Recolours an already-previewed custom hierarchy without reinflating the activity. */
    fun transition(root: View, previous: AppPalette, next: AppPalette) {
        val resolved = ResolvedPalette(
            background = previous.main,
            surface = previous.surface,
            input = previous.input,
            primary = previous.accent,
            text = previous.onMain,
            onPrimary = previous.onAccent
        )
        walk(root) { view -> recolorView(view, resolved, next) }
    }

    fun applyIfCustom(root: View) {
        val localData = LocalDataStore(root.context)
        if (localData.appThemeMode != AppThemeMode.CUSTOM) return
        apply(
            root,
            AppearanceDefaults.customAppPalette(
                main = localData.customMainColor,
                accent = localData.customAccentColor
            )
        )
    }

    private fun walk(view: View, action: (View) -> Unit) {
        action(view)
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) walk(view.getChildAt(index), action)
        }
    }

    private fun recolorView(view: View, old: ResolvedPalette, new: AppPalette) {
        recolorDrawable(view.background, old, new)
        view.backgroundTintList = view.backgroundTintList?.mapped { mapBackground(it, old, new) }

        if (view is TextView) {
            // Preserve enabled, disabled, pressed, and checked colours. Collapsing a
            // ColorStateList to its default colour made custom-theme radio buttons
            // look unchecked even when they were selected.
            view.setTextColor(view.textColors.mapped { mapForeground(it, old, new) })
            TextViewCompat.setCompoundDrawableTintList(
                view,
                TextViewCompat.getCompoundDrawableTintList(view)?.mapped {
                    mapForeground(it, old, new)
                }
            )
        }
        if (view is ImageView) {
            view.imageTintList = view.imageTintList?.mapped { mapForeground(it, old, new) }
        }
        if (view is CompoundButton) {
            view.buttonTintList = view.buttonTintList?.mapped { mapForeground(it, old, new) }
        }
        if (view is ProgressBar) {
            view.progressTintList = view.progressTintList?.mapped { mapBackground(it, old, new) }
            view.progressBackgroundTintList = view.progressBackgroundTintList?.mapped {
                mapBackground(it, old, new)
            }
        }
        if (view is MaterialCardView) {
            view.setCardBackgroundColor(mapBackground(view.cardBackgroundColor.defaultColor, old, new))
        }
    }

    private fun recolorDrawable(drawable: Drawable?, old: ResolvedPalette, new: AppPalette) {
        val mutable = drawable?.mutate() ?: return
        when (mutable) {
            is ColorDrawable -> mutable.color = mapBackground(mutable.color, old, new)
            is GradientDrawable -> mutable.color?.defaultColor?.let {
                mutable.setColor(mapBackground(it, old, new))
            }
            is RippleDrawable -> {
                for (index in 0 until mutable.numberOfLayers) {
                    recolorDrawable(mutable.getDrawable(index), old, new)
                }
            }
            is LayerDrawable -> {
                for (index in 0 until mutable.numberOfLayers) {
                    recolorDrawable(mutable.getDrawable(index), old, new)
                }
            }
            is InsetDrawable -> recolorDrawable(mutable.drawable, old, new)
            is ScaleDrawable -> recolorDrawable(mutable.drawable, old, new)
            is ClipDrawable -> recolorDrawable(mutable.drawable, old, new)
            is StateListDrawable -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                for (index in 0 until mutable.stateCount) {
                    recolorDrawable(mutable.getStateDrawable(index), old, new)
                }
            }
        }
    }

    private fun mapBackground(color: Int, old: ResolvedPalette, new: AppPalette): Int = when {
        same(color, old.background) -> withOriginalAlpha(color, new.main)
        same(color, old.surface) -> withOriginalAlpha(color, new.surface)
        same(color, old.input) || same(color, old.onPrimary) -> withOriginalAlpha(color, new.input)
        same(color, old.primary) -> withOriginalAlpha(color, new.accent)
        same(color, old.text) -> withOriginalAlpha(color, new.onMain)
        else -> color
    }

    private fun mapForeground(color: Int, old: ResolvedPalette, new: AppPalette): Int = when {
        same(color, old.primary) -> withOriginalAlpha(color, new.accent)
        same(color, old.text) -> withOriginalAlpha(color, new.onMain)
        same(color, old.onPrimary) -> withOriginalAlpha(color, new.onAccent)
        same(color, old.background) -> withOriginalAlpha(color, new.main)
        else -> color
    }

    private fun ColorStateList.mapped(map: (Int) -> Int): ColorStateList {
        val states = arrayOf(
            intArrayOf(-android.R.attr.state_enabled, android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_enabled, -android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed),
            intArrayOf(android.R.attr.state_enabled, android.R.attr.state_focused),
            intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_enabled, -android.R.attr.state_checked),
            intArrayOf()
        )
        val colours = IntArray(states.size) { index ->
            map(getColorForState(states[index], defaultColor))
        }
        return ColorStateList(states, colours)
    }

    private fun same(first: Int, second: Int): Boolean =
        (first and 0x00FFFFFF) == (second and 0x00FFFFFF)

    private fun withOriginalAlpha(original: Int, replacement: Int): Int =
        (original and 0xFF000000.toInt()) or (replacement and 0x00FFFFFF)
}
// End of class: RuntimePaletteApplier
