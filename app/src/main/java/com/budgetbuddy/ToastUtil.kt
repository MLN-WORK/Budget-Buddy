package com.budgetbuddy

import android.app.Activity
import android.view.LayoutInflater
import android.graphics.Color
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors

/*
 * Start of class
 * Name of class and related classes (parent/child classes): ToastUtil
 * Parent class: Any; child classes: none; related classes: activities that report validation or local-operation results.
 * What the class does: Shows consistently styled, short-lived application messages.
 * What's important to other classes, if applicable: Related classes depend on this class keeping its inputs validated and its output contract deterministic.
 * Code with comments begins below.
 */
object ToastUtil {
    fun showCustomToast(activity: Activity, message: String) {
        val inflater = LayoutInflater.from(activity)
        val layout = inflater.inflate(R.layout.custom_toast, activity.findViewById(android.R.id.content), false)
        RuntimePaletteApplier.applyIfCustom(layout)

        val text: TextView = layout.findViewById(R.id.tvToast)
        text.text = message
        val background = MaterialColors.getColor(layout, R.attr.budgetPrimaryColor)
        layout.background = layout.background?.mutate()?.apply { setTint(background) }
        val blackContrast = ColorUtils.calculateContrast(Color.BLACK, background)
        val whiteContrast = ColorUtils.calculateContrast(Color.WHITE, background)
        text.setTextColor(if (blackContrast >= whiteContrast) Color.BLACK else Color.WHITE)

        val toast = Toast(activity.applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }
}
// End of class: ToastUtil
