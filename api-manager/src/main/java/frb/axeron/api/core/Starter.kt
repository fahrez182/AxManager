package frb.axeron.api.core

import java.io.File

object Starter {

    private val starterFile =
        File(Engine.application.applicationInfo.nativeLibraryDir, "libaxeron.so")

    val userCommand: String = starterFile.absolutePath

    val adbCommand = "adb shell $userCommand"

    val internalCommand = "$userCommand --apk=${Engine.application.applicationInfo.sourceDir}"
}