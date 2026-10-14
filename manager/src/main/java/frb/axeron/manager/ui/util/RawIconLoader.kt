package frb.axeron.manager.ui.util

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.util.LruCache
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlin.math.roundToInt

/**
 * App icon straight from the APK's own resources (ported from AXGC): the overlay-free archive
 * resources are tried first, so launcher/theme overlays and icon-pack shrinking don't apply.
 */
class RawIcon internal constructor(
    private val drawable: Drawable,
    private val density: Float
) {

    private val adaptive = drawable as? AdaptiveIconDrawable

    val isAdaptive: Boolean = adaptive != null

    val foreground: Bitmap? by lazy { adaptive?.foreground?.renderLayer() }

    val background: Bitmap? by lazy { adaptive?.background?.renderLayer() }

    val monochrome: Bitmap? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            adaptive?.monochrome?.renderLayer()
        } else {
            null
        }
    }

    val legacy: Bitmap? by lazy {
        if (adaptive != null) {
            null
        } else {
            (drawable as? BitmapDrawable)?.bitmap ?: drawable.renderLayer()
        }
    }

    fun compose(shape: Path? = null, fullBleed: Boolean = false): Bitmap {
        if (adaptive == null && shape == null) {
            legacy?.let { return it }
        }
        val source = legacy
        val width = source?.width ?: LAYER_PX
        val height = source?.height ?: LAYER_PX
        val output = createBitmap(width, height)
        val canvas = Canvas(output)
        if (shape != null) canvas.clipPath(shape)
        if (adaptive != null) {
            val targetPx = if (fullBleed) LAYER_PX else (LAYER_PX * VIEWPORT_SCALE).roundToInt()
            val offset = (LAYER_PX - targetPx) / 2f
            canvas.translate(offset, offset)
            adaptive.background?.drawLayer(canvas, targetPx)
            adaptive.foreground?.drawLayer(canvas, targetPx)
        } else if (source != null) {
            canvas.drawBitmap(source, 0f, 0f, null)
        }
        return output
    }

    private fun Drawable.renderLayer(): Bitmap {
        val bitmap = createBitmap(LAYER_PX, LAYER_PX)
        drawLayer(Canvas(bitmap), LAYER_PX)
        return bitmap
    }

    private fun Drawable.drawLayer(canvas: Canvas, targetPx: Int) {
        val layerPx = (LAYER_DP * density).roundToInt().coerceAtLeast(1)
        val checkpoint = canvas.save()
        val scale = targetPx / layerPx.toFloat()
        canvas.scale(scale, scale)
        setBounds(0, 0, layerPx, layerPx)
        draw(canvas)
        canvas.restoreToCount(checkpoint)
    }

    companion object {
        const val LAYER_PX = 432
        private const val LAYER_DP = 108
        private const val VIEWPORT_SCALE = 1.5f
    }
}

object RawIconLoader {

    private const val TAG = "RawIconLoader"
    private const val MAX_CACHE_BYTES = 24 * 1024 * 1024
    private const val ROUND_SUFFIX = "#round"
    private const val ROUND_ICON_FIELD = "roundIconRes"
    private const val DENSITY = DisplayMetrics.DENSITY_XXXHIGH

    /** Adaptive icons render full-bleed square; give them one consistent rounded mask. */
    private const val ADAPTIVE_CORNER_FRACTION = 0.25f

    private class Entry(val versionCode: Long, val lastUpdateTime: Long, val bitmap: Bitmap)

    private val cache = object : LruCache<String, Entry>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Entry): Int = value.bitmap.byteCount
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = HashMap<String, Deferred<Bitmap?>>()

    suspend fun load(
        pm: PackageManager,
        packageInfo: PackageInfo,
        preferRound: Boolean = false
    ): Bitmap? {
        val key = cacheKey(packageInfo.packageName, preferRound)
        val deferred = synchronized(inFlight) {
            inFlight.getOrPut(key) {
                scope.async {
                    try {
                        resolve(pm, packageInfo, preferRound)
                    } finally {
                        synchronized(inFlight) { inFlight.remove(key) }
                    }
                }
            }
        }
        return deferred.await()
    }

    fun invalidate(packageName: String? = null) {
        if (packageName == null) {
            cache.evictAll()
        } else {
            cache.remove(cacheKey(packageName, false))
            cache.remove(cacheKey(packageName, true))
        }
    }

    private fun cacheKey(packageName: String, preferRound: Boolean): String =
        if (preferRound) packageName + ROUND_SUFFIX else packageName

    private fun resolve(pm: PackageManager, info: PackageInfo, preferRound: Boolean): Bitmap? {
        val packageName = info.packageName
        val appInfo = info.applicationInfo ?: return null
        val key = cacheKey(packageName, preferRound)
        val versionCode = PackageInfoCompat.getLongVersionCode(info)
        val lastUpdateTime = info.lastUpdateTime

        cache.get(key)?.let { entry ->
            if (entry.versionCode == versionCode && entry.lastUpdateTime == lastUpdateTime) {
                return entry.bitmap
            }
        }

        val bitmap = try {
            val raw = RawIcon(
                loadDrawable(pm, appInfo, preferRound),
                Resources.getSystem().displayMetrics.density
            )
            if (raw.isAdaptive) raw.compose(shape = roundedMask()) else raw.compose()
        } catch (e: Throwable) {
            Log.w(TAG, "render failed for $packageName", e)
            return null
        }
        cache.put(key, Entry(versionCode, lastUpdateTime, bitmap))
        return bitmap
    }

    private fun roundedMask(): Path {
        val size = RawIcon.LAYER_PX.toFloat()
        val radius = size * ADAPTIVE_CORNER_FRACTION
        return Path().apply {
            addRoundRect(0f, 0f, size, size, radius, radius, Path.Direction.CW)
        }
    }

    private fun loadDrawable(
        pm: PackageManager,
        appInfo: ApplicationInfo,
        preferRound: Boolean
    ): Drawable {
        val packageName = appInfo.packageName
        val iconId = resolveIconId(pm, appInfo, preferRound)
        if (iconId == 0) {
            Log.d(TAG, "[default] $packageName has no icon resource")
            return defaultIcon()
        }

        overlayFreeResources(pm, appInfo)?.let { resources ->
            loadFrom(resources, iconId, "overlay-free", packageName)?.let { return it }
        }

        installedResources(pm, appInfo)?.let { resources ->
            loadFrom(resources, iconId, "installed", packageName)?.let {
                Log.w(TAG, "[installed] $packageName icon may carry overlays")
                return it
            }
        }

        try {
            val drawable = pm.getApplicationIcon(appInfo)
            Log.w(TAG, "[system] $packageName icon comes from PackageManager")
            return drawable
        } catch (e: Throwable) {
            Log.w(TAG, "[system] $packageName failed", e)
        }

        Log.w(TAG, "[default] $packageName fell through every layer")
        return defaultIcon()
    }

    private fun resolveIconId(
        pm: PackageManager,
        appInfo: ApplicationInfo,
        preferRound: Boolean
    ): Int {
        if (preferRound) {
            val round = roundIconRes(appInfo)
            if (round != 0) return round
        }
        val launcherIcon = try {
            pm.getLaunchIntentForPackage(appInfo.packageName)?.component
                ?.let { pm.getActivityInfo(it, 0).icon }
                ?: 0
        } catch (_: Exception) {
            0
        }
        return if (launcherIcon != 0) launcherIcon else appInfo.icon
    }

    private fun roundIconRes(appInfo: ApplicationInfo): Int =
        try {
            ApplicationInfo::class.java.getField(ROUND_ICON_FIELD).getInt(appInfo)
        } catch (_: Throwable) {
            0
        }

    private fun overlayFreeResources(pm: PackageManager, appInfo: ApplicationInfo): Resources? {
        val sourceDir = appInfo.sourceDir ?: return null
        return try {
            val archive = pm.getPackageArchiveInfo(sourceDir, 0)?.applicationInfo ?: return null
            archive.sourceDir = sourceDir
            archive.publicSourceDir = appInfo.publicSourceDir ?: sourceDir
            archive.splitSourceDirs = appInfo.splitSourceDirs
            archive.splitPublicSourceDirs = appInfo.splitPublicSourceDirs
            pm.getResourcesForApplication(archive)
        } catch (e: Throwable) {
            Log.w(TAG, "[overlay-free] ${appInfo.packageName} resources unavailable", e)
            null
        }
    }

    private fun installedResources(pm: PackageManager, appInfo: ApplicationInfo): Resources? =
        try {
            pm.getResourcesForApplication(appInfo)
        } catch (e: Throwable) {
            Log.w(TAG, "[installed] ${appInfo.packageName} resources unavailable", e)
            null
        }

    private fun loadFrom(
        resources: Resources,
        iconId: Int,
        layer: String,
        packageName: String
    ): Drawable? =
        try {
            resources.loadIcon(iconId)
        } catch (e: Throwable) {
            Log.w(TAG, "[$layer] $packageName icon 0x${iconId.toString(16)} failed", e)
            null
        }

    private fun defaultIcon(): Drawable =
        Resources.getSystem().loadIcon(android.R.drawable.sym_def_app_icon)

    private fun Resources.loadIcon(iconId: Int): Drawable {
        val native = getDrawable(iconId, null)
        if (native is AdaptiveIconDrawable) return native
        return getDrawableForDensity(iconId, DENSITY, null) ?: native
    }
}
