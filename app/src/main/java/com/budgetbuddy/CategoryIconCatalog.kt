package com.budgetbuddy

import android.content.Context
import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import com.google.android.material.color.MaterialColors

/** A curated set keeps UI-only assets and broken placeholders out of category selection. */
object CategoryIconCatalog {
    val selectableIcons = listOf(
        "ic_shopping_basket",
        "ic_car",
        "ic_train",
        "ic_petrol",
        "ic_house",
        "ic_utilities",
        "ic_health_heart",
        "ic_forkin_knife",
        "ic_coffee",
        "ic_play_button",
        "ic_book",
        "ic_dumbell",
        "ic_phone_mobile",
        "ic_clothing",
        "ic_paw",
        "ic_plane",
        "ic_gift",
        "ic_briefcase",
        "ic_box",
        "ic_alcohol",
        "ic_calendar",
        "ic_cash_paper",
        "ic_money_bag",
        "ic_piggy",
        "ic_currency"
    )

    fun drawableId(context: Context, iconName: String?): Int =
        iconName
            ?.takeIf(String::isNotBlank)
            ?.let { context.resources.getIdentifier(it, "drawable", context.packageName) }
            ?.takeIf { it != 0 }
            ?: R.drawable.ic_currency

    fun bind(imageView: ImageView, iconName: String?) {
        imageView.setImageResource(drawableId(imageView.context, iconName))
        ImageViewCompat.setImageTintList(
            imageView,
            ColorStateList.valueOf(
                MaterialColors.getColor(imageView, R.attr.budgetTextColor)
            )
        )
    }
}
