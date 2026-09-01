package com.budgetbuddy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.os.BundleCompat
import kotlin.math.max

/*
 * Start of class
 * Name of class and related classes (parent/child classes): ColorPickerView
 * Parent class: View; child classes: none; related classes: ThemeColorsActivity and AppearanceSelection.
 * What the class does: Draws an HSV colour picker and reports the selected opaque colour.
 * What's important to other classes, if applicable: Related classes depend on this class keeping its inputs validated and its output contract deterministic.
 * Code with comments begins below.
 */
class ColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
    }
    private val saturationValueRect = RectF()
    private val hueRect = RectF()
    private var saturationShader: Shader? = null
    private var valueShader: Shader? = null
    private var hueShader: Shader? = null
    private var hue = 160f
    private var saturation = 0.62f
    private var value = 0.72f

    var onColorChanged: ((Int) -> Unit)? = null

    var selectedColor: Int
        get() = Color.HSVToColor(floatArrayOf(hue, saturation, value))
        set(color) {
            val hsv = FloatArray(3)
            Color.colorToHSV(color, hsv)
            hue = hsv[0]
            saturation = hsv[1]
            value = hsv[2]
            updateShaders()
            invalidate()
        }

    init {
        isClickable = true
        isFocusable = true
        contentDescription = context.getString(R.string.color_picker_description)
        minimumHeight = (190f * resources.displayMetrics.density).toInt()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        val padding = 8f * resources.displayMetrics.density
        val hueHeight = 24f * resources.displayMetrics.density
        val gap = 12f * resources.displayMetrics.density
        saturationValueRect.set(
            padding,
            padding,
            width - padding,
            max(padding, height - padding - hueHeight - gap)
        )
        hueRect.set(
            padding,
            saturationValueRect.bottom + gap,
            width - padding,
            height - padding
        )
        updateShaders()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.shader = saturationShader
        canvas.drawRoundRect(saturationValueRect, 12f, 12f, paint)
        paint.shader = valueShader
        canvas.drawRoundRect(saturationValueRect, 12f, 12f, paint)

        paint.shader = hueShader
        canvas.drawRoundRect(hueRect, hueRect.height() / 2f, hueRect.height() / 2f, paint)
        paint.shader = null

        val svX = saturationValueRect.left + saturation * saturationValueRect.width()
        val svY = saturationValueRect.top + (1f - value) * saturationValueRect.height()
        drawCursor(canvas, svX, svY, 8f * resources.displayMetrics.density)
        val hueX = hueRect.left + (hue / 360f) * hueRect.width()
        drawCursor(canvas, hueX, hueRect.centerY(), 7f * resources.displayMetrics.density)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN && event.action != MotionEvent.ACTION_MOVE &&
            event.action != MotionEvent.ACTION_UP
        ) return super.onTouchEvent(event)

        when {
            event.y >= hueRect.top -> {
                hue = ((event.x - hueRect.left) / hueRect.width()).coerceIn(0f, 1f) * 360f
                updateShaders()
            }
            else -> {
                saturation = ((event.x - saturationValueRect.left) / saturationValueRect.width())
                    .coerceIn(0f, 1f)
                value = (1f - (event.y - saturationValueRect.top) / saturationValueRect.height())
                    .coerceIn(0f, 1f)
            }
        }
        invalidate()
        onColorChanged?.invoke(selectedColor)
        if (event.action == MotionEvent.ACTION_UP) performClick()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onSaveInstanceState(): Parcelable = Bundle().apply {
        putParcelable(STATE_SUPER, super.onSaveInstanceState())
        putInt(STATE_COLOR, selectedColor)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            selectedColor = state.getInt(STATE_COLOR, AppearanceDefaults.CUSTOM_ACCENT)
            super.onRestoreInstanceState(
                BundleCompat.getParcelable(state, STATE_SUPER, Parcelable::class.java)
            )
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private fun drawCursor(canvas: Canvas, x: Float, y: Float, radius: Float) {
        cursorPaint.color = Color.WHITE
        cursorPaint.strokeWidth = 4f * resources.displayMetrics.density
        canvas.drawCircle(x, y, radius, cursorPaint)
        cursorPaint.color = Color.BLACK
        cursorPaint.strokeWidth = 1.5f * resources.displayMetrics.density
        canvas.drawCircle(x, y, radius + 2f * resources.displayMetrics.density, cursorPaint)
    }

    private fun updateShaders() {
        if (width <= 0 || height <= 0) return
        val hueColor = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        saturationShader = LinearGradient(
            saturationValueRect.left,
            0f,
            saturationValueRect.right,
            0f,
            Color.WHITE,
            hueColor,
            Shader.TileMode.CLAMP
        )
        valueShader = LinearGradient(
            0f,
            saturationValueRect.top,
            0f,
            saturationValueRect.bottom,
            Color.TRANSPARENT,
            Color.BLACK,
            Shader.TileMode.CLAMP
        )
        hueShader = LinearGradient(
            hueRect.left,
            0f,
            hueRect.right,
            0f,
            intArrayOf(
                Color.RED,
                Color.YELLOW,
                Color.GREEN,
                Color.CYAN,
                Color.BLUE,
                Color.MAGENTA,
                Color.RED
            ),
            null,
            Shader.TileMode.CLAMP
        )
    }

    companion object {
        private const val STATE_SUPER = "super"
        private const val STATE_COLOR = "color"
    }
}
// End of class: ColorPickerView
