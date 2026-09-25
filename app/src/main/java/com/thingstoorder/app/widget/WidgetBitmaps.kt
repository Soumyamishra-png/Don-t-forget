package com.thingstoorder.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import com.thingstoorder.app.R

data class SizedBitmap(val bitmap: Bitmap, val widthDp: Float, val heightDp: Float)

// Widgets (RemoteViews) cannot load custom fonts, so Bricolage text is drawn to a bitmap.
object WidgetBitmaps {
    fun text(context: Context, text: String, sp: Float, color: Int): SizedBitmap {
        val dm = context.resources.displayMetrics
        val paint = textPaint(context, sp, color)
        val fm = paint.fontMetrics
        val width = Math.ceil(paint.measureText(text).toDouble()).toInt() + 2
        val height = Math.ceil((fm.descent - fm.ascent).toDouble()).toInt() + 2
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawText(text, 1f, 1f - fm.ascent, paint)
        return SizedBitmap(bitmap, width / dm.density, height / dm.density)
    }

    fun badge(context: Context, count: Int): SizedBitmap {
        val dm = context.resources.displayMetrics
        val paint = textPaint(context, 13f, 0xFFFFFFFF.toInt())
        val label = count.toString()
        val textWidth = paint.measureText(label)
        val height = (22 * dm.density).toInt()
        val width = maxOf((30 * dm.density).toInt(), (textWidth + 18 * dm.density).toInt())
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF5E1A.toInt() }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), height / 2f, height / 2f, fill)
        val fm = paint.fontMetrics
        canvas.drawText(label, (width - textWidth) / 2f, height / 2f - (fm.ascent + fm.descent) / 2f, paint)
        return SizedBitmap(bitmap, width / dm.density, height / dm.density)
    }

    private fun textPaint(context: Context, sp: Float, argb: Int): Paint {
        val dm = context.resources.displayMetrics
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = argb
            typeface = ResourcesCompat.getFont(context, R.font.bricolage_extrabold)
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, dm)
        }
    }
}
