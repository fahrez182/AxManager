package frb.axeron.server

import com.google.gson.annotations.SerializedName

/**
 * ax_permission.json, read and written by Gson through these fields. Kept as the Java class had
 * them: the same names, a no-arg constructor (so a missing field keeps its default), and no
 * other backing fields (Gson would write those too).
 */
class AxeronConfig() {

    @field:SerializedName("version")
    @JvmField
    var version: Int = LATEST_VERSION

    @field:SerializedName("packages")
    @JvmField
    var packages: MutableList<PackageEntry>? = ArrayList()

    constructor(packages: MutableList<PackageEntry>) : this() {
        this.version = LATEST_VERSION
        this.packages = packages
    }

    /**
     * Gson builds it without a constructor (there is no no-arg one), so [packages] is null when
     * the file has none: [AxeronConfigManager] fills it in after loading.
     */
    class PackageEntry(uid: Int, flags: Int) : ConfigPackageEntry() {

        @field:SerializedName("uid")
        @JvmField
        val uid: Int = uid

        @field:SerializedName("flags")
        @JvmField
        var flags: Int = flags

        @field:SerializedName("packages")
        @JvmField
        var packages: MutableList<String>? = ArrayList()

        override val isAllowed: Boolean
            get() = (flags and ConfigManager.FLAG_ALLOWED) != 0

        override val isDenied: Boolean
            get() = (flags and ConfigManager.FLAG_DENIED) != 0
    }

    companion object {
        /**
         * 3: FLAG_DENIED means "denied in the manager" and is enforced (no dialog). Before, it was
         * also written for every app that simply wasn't allowed, so loading an older config clears it.
         */
        const val LATEST_VERSION = 3
    }
}
