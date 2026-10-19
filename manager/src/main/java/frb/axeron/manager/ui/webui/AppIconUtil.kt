package frb.axeron.manager.ui.webui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import frb.axeron.api.core.Engine
import java.lang.ref.WeakReference


/**
 * By FahrezONE
 */
object AppIconUtil {

    private val iconCache = HashMap<String, WeakReference<Bitmap>>()
    private val resultListeners = HashMap<String, Result>()

    @JvmStatic
    fun loadAppIconSync(packageName: String, sizePx: Int): Bitmap? {
        getFromCache(packageName)?.let { return it }

        return try {
            loadIcon(packageName, sizePx)
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    fun loadAppIcon(packageName: String, sizePx: Int, result: Result) {

        val cached = getFromCache(packageName)
        if (cached != null) {
            result.onIconReady(cached)
            return
        }

        //masukan kedalam waiting
        if (resultListeners.containsKey(packageName)) return
        resultListeners[packageName] = result

        Thread {
            val icon: Bitmap? = try {
                loadIcon(packageName, sizePx)
            } catch (ignored: Exception) {
                null
            }

            val resultListener = resultListeners[packageName]

            if (resultListener != null) {
                Handler(Looper.getMainLooper()).post {
                    resultListener.onIconReady(icon)
                }
                resultListeners.remove(packageName)
            }
        }.start()
    }

    private fun loadIcon(packageName: String, sizePx: Int): Bitmap {
        val pm = Engine.application.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        val drawable = pm.getApplicationIcon(appInfo)
        val raw = drawableToBitmap(drawable, sizePx)
        val icon = Bitmap.createScaledBitmap(raw, sizePx, sizePx, true)
        putToCache(icon, packageName)
        return icon
    }

    private fun getFromCache(packageName: String): Bitmap? = iconCache[packageName]?.get()

    private fun putToCache(bmp: Bitmap?, packageName: String) {
        if (bmp != null) {
            iconCache[packageName] = WeakReference(bmp)
        }
    }

    private fun drawableToBitmap(drawable: Drawable, size: Int): Bitmap {
        if (drawable is BitmapDrawable) return drawable.bitmap
        val bmp = Bitmap.createBitmap(
            if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else size,
            if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else size,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }

    fun interface Result {
        fun onIconReady(icon: Bitmap?)
    }
}
