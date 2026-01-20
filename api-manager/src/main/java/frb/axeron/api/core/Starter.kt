package frb.axeron.api.core

import java.io.File

object Starter {

    const val KEY_PAIR = "axeron_adb_key_pair"

    private val starterFile =
        File(Engine.application.applicationInfo.nativeLibraryDir, "libaxeron.so")

    val userCommand: String = starterFile.absolutePath

    val adbCommand = "adb shell $userCommand"

    val internalCommand = "$userCommand --apk=${Engine.application.applicationInfo.sourceDir}"

    fun internalAdbCommand(keyPair: String) = "$userCommand --apk=${Engine.application.applicationInfo.sourceDir}; settings put global $KEY_PAIR $keyPair"
}