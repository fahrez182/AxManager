package frb.axeron.manager.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Builds the bitmaps for WebUI home-screen shortcuts. They are full adaptive-icon layers
 * (108dp worth of pixels), so the launcher applies its own mask; only the center 72dp shows.
 */
object ShortcutIcons {

    /** 108dp at xxxhdpi, the adaptive icon layer size. */
    const val SIZE = 432

    /** Visible part of an adaptive layer (72dp of 108dp). */
    const val VISIBLE_FRACTION = 72f / 108f

    /** Loads any Coil model (URL, bytes, content Uri) into a software bitmap. */
    suspend fun loadBitmap(context: Context, model: Any): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(model)
            .allowHardware(false)
            .size(1024)
            .build()
        val result = context.imageLoader.execute(request) as? SuccessResult ?: return null
        return result.drawable.toBitmap()
    }

    /** Center-crops [source] to a square layer, e.g. a wide plugin banner or a gallery photo. */
    fun fromImage(source: Bitmap): Bitmap {
        val side = min(source.width, source.height)
        val left = (source.width - side) / 2
        val top = (source.height - side) / 2
        val output = createBitmap(SIZE, SIZE)
        Canvas(output).drawBitmap(
            source,
            Rect(left, top, left + side, top + side),
            Rect(0, 0, SIZE, SIZE),
            Paint(Paint.FILTER_BITMAP_FLAG)
        )
        return output
    }

    /** Solid background with up to two initials, kept inside the visible safe zone. */
    fun fromInitials(name: String, background: Int, foreground: Int): Bitmap {
        val output = createBitmap(SIZE, SIZE)
        val canvas = Canvas(output)
        canvas.drawColor(background)

        val text = initialsOf(name)
        if (text.isNotEmpty()) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = foreground
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = SIZE * if (text.length == 1) 0.34f else 0.28f
            }
            val y = SIZE / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(text, SIZE / 2f, y, paint)
        }
        return output
    }

    /**
     * Recents/task icon from a shortcut layer: just the visible center, rounded like the
     * launcher preview, at a size the task description can carry cheaply.
     */
    fun toTaskIcon(layer: Bitmap, size: Int = 192): Bitmap {
        val visible = (layer.width * VISIBLE_FRACTION).roundToInt()
        val offset = (layer.width - visible) / 2
        val output = createBitmap(size, size)
        val canvas = Canvas(output)
        val radius = size * 0.3f
        canvas.clipPath(Path().apply {
            addRoundRect(0f, 0f, size.toFloat(), size.toFloat(), radius, radius, Path.Direction.CW)
        })
        canvas.drawBitmap(
            layer,
            Rect(offset, offset, offset + visible, offset + visible),
            Rect(0, 0, size, size),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        )
        return output
    }

    /** "My Cool Plugin" -> "MC", "plugin" -> "P". */
    fun initialsOf(name: String): String =
        name.split(' ', '-', '_', '.')
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() } }
            .joinToString("")
            .uppercase()
}
