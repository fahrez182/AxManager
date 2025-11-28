package frb.axeron.server.utils

import frb.axeron.api.core.Engine.Companion.application
import java.io.File

object Starter {

    private val starterFile = File(application.applicationInfo.nativeLibraryDir, "libaxeron.so")

    val userCommand: String = starterFile.absolutePath

    val adbCommand = "adb shell $userCommand"

    val internalCommand = "$userCommand --apk=${application.applicationInfo.sourceDir}"
}