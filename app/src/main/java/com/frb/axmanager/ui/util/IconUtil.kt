package com.frb.axmanager.ui.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap

fun Drawable.toBitmapSafely(size: Int = 96): Bitmap {
    return when (this) {
        is BitmapDrawable -> this.bitmap
        is AdaptiveIconDrawable -> {
            // Bungkus AdaptiveIconDrawable ke Bitmap
            val bitmap = createBitmap(size, size)
            val canvas = Canvas(bitmap)
            this.setBounds(0, 0, canvas.width, canvas.height)
            this.draw(canvas)
            bitmap
        }
        else -> {
            // Fallback generic
            val bitmap = createBitmap(size, size)
            val canvas = Canvas(bitmap)
            this.setBounds(0, 0, canvas.width, canvas.height)
            this.draw(canvas)
            bitmap
        }
    }
}
