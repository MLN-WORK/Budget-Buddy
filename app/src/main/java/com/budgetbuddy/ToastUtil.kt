package com.budgetbuddy

import android.app.Activity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.budgetbuddy.R

object ToastUtil {
    fun showCustomToast(activity: Activity, message: String) {
        val inflater = LayoutInflater.from(activity)
        val layout = inflater.inflate(R.layout.custom_toast, activity.findViewById(android.R.id.content), false)
        RuntimePaletteApplier.applyIfCustom(layout)

        val text: TextView = layout.findViewById(R.id.tvToast)
        text.text = message

        val toast = Toast(activity.applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }
}
