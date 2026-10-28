package frb.axeron.manager.ui.webui

import android.annotation.SuppressLint
import android.content.Context
import android.os.RemoteException
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import frb.axeron.api.Axeron
import frb.axeron.shared.PathHelper
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream

class AxPathHandler @SuppressLint("RestrictedApi") constructor(
    directory: File,
    private val mInsetsSupplier: InsetsSupplier
) : AxWebLoader.PathHandler {

    private val mDirectory: File

    init {
        try {
            mDirectory = File(getCanonicalDirPath(directory))
            if (!isAllowedInternalStorageDir()) {
                throw IllegalArgumentException(
                    "The given directory \"" + directory +
                            "\" doesn't exist under an allowed app internal storage directory"
                )
            }
        } catch (e: IOException) {
            throw IllegalArgumentException(
                "Failed to resolve the canonical path for the given directory: " + directory.path, e
            )
        }
    }

    //UTIL

    @SuppressLint("RestrictedApi")
    override fun handle(context: Context, view: WebView, request: WebResourceRequest): WebResourceResponse? {
        val path = request.url.path ?: return WebResourceResponse(null, null, null)
        Log.d(TAG, "Raw url: " + request.url)
        Log.d(TAG, "Raw path: $path")

        if ("/internal/insets.css" == path) {
            val css = mInsetsSupplier.get().css
            return WebResourceResponse(
                "text/css",
                "utf-8",
                ByteArrayInputStream(css.toByteArray(Charsets.UTF_8))
            )
        }
        if ("/internal/colors.css" == path) {
            val css = MonetColorsProvider.getColorsCss()
            Log.d(TAG, "Raw css: $css")
            return WebResourceResponse(
                "text/css",
                "utf-8",
                ByteArrayInputStream(css.toByteArray(Charsets.UTF_8))
            )
        }
        try {
            val file = getCanonicalFileIfChild(mDirectory, path)
            if (file != null) {
                Log.d(TAG, "Requested path: " + getCanonicalDirPath(file))
                val fileService = Axeron.newFileService() ?: throw IOException("Server isn't running")
                val stream = handleSvgzStream(
                    file.path,
                    fileService.setFileInputStream(getCanonicalDirPath(file))
                )
                val mimeType = guessMimeType(path)
                Log.d(TAG, "Requested path is: " + getCanonicalDirPath(file))
                return WebResourceResponse(mimeType, null, stream).apply {
                    responseHeaders = mapOf("Content-Security-Policy" to PLUGIN_CSP)
                }
            } else {
                Log.e(
                    TAG, String.format(
                        "The requested file: %s is outside the mounted directory: %s", path,
                        mDirectory
                    )
                )
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error opening the requested path: $path")
        } catch (e: RemoteException) {
            Log.e(TAG, "Error opening the requested path: $path")
        }
        return WebResourceResponse(null, null, null)
    }

    @Throws(IOException::class)
    private fun isAllowedInternalStorageDir(): Boolean {
        val dir = getCanonicalDirPath(mDirectory)

        for (forbiddenPath in FORBIDDEN_DATA_DIRS) {
            if (dir.startsWith(forbiddenPath) &&
                !dir.startsWith(ALLOWED_DATA_DIRS) &&
                !dir.startsWith(ALLOWED_DATA_DIRS_ROOT)
            ) {
                return false
            }
        }
        return true
    }

    fun interface InsetsSupplier {
        fun get(): Insets
    }

    fun interface OnInsetsRequestedListener {
        fun onInsetsRequested(enable: Boolean)
    }

    companion object {

        private const val TAG = "AxPathHandler"

        const val DEFAULT_MIME_TYPE = "text/plain"

        /**
         * The bridges (Axeron, ksu) reach every frame in the WebView and can't tell which one
         * called, and a form's POST skips shouldOverrideUrlLoading. So a plugin's page may frame
         * and post to the plugin's own pages only; scripts, styles and data load from anywhere.
         */
        private const val PLUGIN_CSP =
            "frame-src ax: https://mui.kernelsu.org blob: data:; form-action ax: https://mui.kernelsu.org"
        private val FORBIDDEN_DATA_DIRS = arrayOf("/data/data", "/data/system")

        private val ALLOWED_DATA_DIRS = PathHelper.getWorkingPath(false, null).absolutePath
        private val ALLOWED_DATA_DIRS_ROOT = PathHelper.getWorkingPath(true, null).absolutePath

        @JvmStatic
        @Throws(IOException::class)
        fun getCanonicalDirPath(file: File): String {
            var canonicalPath = file.canonicalPath
            if (!canonicalPath.endsWith("/")) canonicalPath += "/"
            return canonicalPath
        }

        @JvmStatic
        @Throws(IOException::class)
        fun getCanonicalFileIfChild(parent: File, child: String): File? {
            val parentCanonicalPath = getCanonicalDirPath(parent)
            val childCanonicalPath = File(parent, child).canonicalPath
            if (childCanonicalPath.startsWith(parentCanonicalPath)) {
                return File(childCanonicalPath)
            }
            return null
        }

        @Throws(IOException::class)
        private fun handleSvgzStream(path: String, stream: InputStream): InputStream {
            return if (path.endsWith(".svgz")) GZIPInputStream(stream) else stream
        }

        @JvmStatic
        fun guessMimeType(filePath: String): String =
            MimeUtil.getMimeFromFileName(filePath) ?: DEFAULT_MIME_TYPE
    }
}
