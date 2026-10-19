/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package frb.axeron.manager.ui.webui

import java.net.URLConnection
import java.util.Locale

internal object MimeUtil {

    fun getMimeFromFileName(fileName: String?): String? {
        if (fileName == null) {
            return null
        }

        // Copying the logic and mapping that Chromium follows.
        // First we check against the OS (this is a limited list by default)
        // but app developers can extend this.
        // We then check against a list of hardcoded mime types above if the
        // OS didn't provide a result.
        return URLConnection.guessContentTypeFromName(fileName) ?: guessHardcodedMime(fileName)
    }

    // We should keep this map in sync with the lists under
    // //net/base/mime_util.cc in Chromium.
    // A bunch of the mime types don't really apply to Android land
    // like word docs so feel free to filter out where necessary.
    private fun guessHardcodedMime(fileName: String): String? {
        val finalFullStop = fileName.lastIndexOf('.')
        if (finalFullStop == -1) {
            return null
        }

        val extension = fileName.substring(finalFullStop + 1).lowercase(Locale.getDefault())

        return when (extension) {
            "webm" -> "video/webm"
            "mpeg", "mpg" -> "video/mpeg"
            "mp3" -> "audio/mpeg"
            "wasm" -> "application/wasm"
            "xhtml", "xht", "xhtm" -> "application/xhtml+xml"
            "flac" -> "audio/flac"
            "ogg", "oga", "opus" -> "audio/ogg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/x-m4a"
            "gif" -> "image/gif"
            "jpeg", "jpg", "jfif", "pjpeg", "pjp" -> "image/jpeg"
            "png" -> "image/png"
            "apng" -> "image/apng"
            "svg", "svgz" -> "image/svg+xml"
            "webp" -> "image/webp"
            "mht", "mhtml" -> "multipart/related"
            "css" -> "text/css"
            "html", "htm", "shtml", "shtm", "ehtml" -> "text/html"
            "js", "mjs" -> "application/javascript"
            "xml" -> "text/xml"
            "mp4", "m4v" -> "video/mp4"
            "ogv", "ogm" -> "video/ogg"
            "ico" -> "image/x-icon"
            "woff" -> "application/font-woff"
            "gz", "tgz" -> "application/gzip"
            "json" -> "application/json"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "bmp" -> "image/bmp"
            "tiff", "tif" -> "image/tiff"
            else -> null
        }
    }
}
