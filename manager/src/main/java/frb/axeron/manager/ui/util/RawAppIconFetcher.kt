package frb.axeron.manager.ui.util

import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options

/**
 * Coil engine for `AsyncImage(model = packageInfo)`: icons come from [RawIconLoader],
 * so every app list in the manager shows the same raw, overlay-free icon as AXGC.
 */
class RawAppIconFetcher(
    private val packageInfo: PackageInfo,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val pm = options.context.packageManager
        val bitmap = RawIconLoader.load(pm, packageInfo) ?: return null
        return DrawableResult(
            drawable = bitmap.toDrawable(options.context.resources),
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<PackageInfo> {
        override fun create(data: PackageInfo, options: Options, imageLoader: ImageLoader): Fetcher =
            RawAppIconFetcher(data, options)
    }
}

/** Memory-cache key that changes when the app is updated, so a new icon is picked up. */
class RawAppIconKeyer : Keyer<PackageInfo> {
    override fun key(data: PackageInfo, options: Options): String =
        "raw-icon:${data.packageName}:${PackageInfoCompat.getLongVersionCode(data)}:${data.lastUpdateTime}"
}
