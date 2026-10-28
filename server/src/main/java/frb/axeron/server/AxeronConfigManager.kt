package frb.axeron.server

import android.content.pm.PackageManager
import android.os.Build
import android.system.Os
import android.util.AtomicFile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import frb.axeron.ktx.workerHandler
import frb.axeron.server.ServerConstants.PERMISSION
import frb.axeron.server.util.UserHandleCompat
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.PathHelper
import rikka.hidden.compat.PackageManagerApis
import rikka.hidden.compat.UserManagerApis
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader

class AxeronConfigManager : ConfigManager() {

    private val config: AxeronConfig = load()
    private val mWriteRunner = Runnable { write(config) }

    /** Uids whose entry was checked against the app on them, until the uid's processes are gone. */
    private val verified = HashSet<Int>()

    init {
        var changed = false

        if (config.packages == null) {
            config.packages = ArrayList()
            changed = true
        }
        val entries = config.packages!!

        if (config.version < 3) {
            // Older configs stamped FLAG_DENIED on every app that wasn't allowed; nobody chose it.
            for (entry in entries) {
                entry.flags = entry.flags and FLAG_DENIED.inv()
            }
            LOGGER.i("config v%d -> v%d: cleared implicit denials", config.version, AxeronConfig.LATEST_VERSION)
            config.version = AxeronConfig.LATEST_VERSION
            changed = true
        }

        for (entry in ArrayList(entries)) {
            val entryPackages = entry.packages ?: ArrayList<String>().also { entry.packages = it }

            val packages = PackageManagerApis.getPackagesForUidNoThrow(entry.uid)
            if (packages.isEmpty()) {
                LOGGER.i("remove config for uid %d since it has gone", entry.uid)
                entries.remove(entry)
                changed = true
                continue
            }

            var packagesChanged = true

            for (packageName in entryPackages) {
                if (packages.contains(packageName)) {
                    packagesChanged = false
                    break
                }
            }

            val rawSize = entryPackages.size
            val s = LinkedHashSet(entryPackages)
            entryPackages.clear()
            entryPackages.addAll(s)
            val shrunkSize = entryPackages.size
            if (shrunkSize < rawSize) {
                LOGGER.w("entry.packages has duplicate! Shrunk. (%d -> %d)", rawSize, shrunkSize)
            }

            if (packagesChanged) {
                LOGGER.i("remove config for uid %d since the packages for it changed", entry.uid)
                entries.remove(entry)
                changed = true
            }
        }

        for (userId in UserManagerApis.getUserIdsNoThrow()) {
            for (pi in PackageManagerApis.getInstalledPackagesNoThrow(PackageManager.GET_PERMISSIONS.toLong(), userId)) {
                if (pi == null ||
                    pi.applicationInfo == null ||
                    pi.requestedPermissions == null ||
                    !pi.requestedPermissions!!.contains(PERMISSION)
                ) {
                    continue
                }

                val uid = pi.applicationInfo!!.uid
                val pkg = pi.packageName
                val entry = findLocked(uid)
                var allowed = false
                var denied = false

                if (entry != null && entry.packages?.contains(pkg) == true) {
                    allowed = entry.isAllowed
                    denied = entry.isDenied
                }

                val packages = arrayListOf(pkg)

                // Neither allowed nor denied is "ask": 0, not FLAG_DENIED.
                val flags = if (allowed) FLAG_ALLOWED else if (denied) FLAG_DENIED else 0
                updateLocked(uid, packages, MASK_PERMISSION, flags)
                changed = true
            }
        }

        if (changed) {
            scheduleWriteLocked()
        }
    }

    private fun scheduleWriteLocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (workerHandler.hasCallbacks(mWriteRunner)) {
                return
            }
        } else {
            workerHandler.removeCallbacks(mWriteRunner)
        }
        workerHandler.postDelayed(mWriteRunner, WRITE_DELAY)
    }

    private fun findLocked(uid: Int): AxeronConfig.PackageEntry? {
        for (entry in config.packages!!) {
            if (uid == entry.uid) {
                return entry
            }
        }
        return null
    }

    /** The signing certificate of the app on [uid] now (shared-uid packages share one). */
    private fun signatureOf(uid: Int, packages: List<String>): String? =
        packages.firstOrNull()?.let { AxeronService.certDigest(it, UserHandleCompat.getUserId(uid)) }

    /**
     * [uid]'s entry, if it still belongs to the app it was made for: one of its packages is on the
     * uid and signed the same. An uninstalled app leaves its entry behind and its uid can go to
     * another app, which would inherit the grant; such an entry is dropped. Checked once per uid,
     * as this is on every gated call; [invalidate] has it checked again.
     */
    private fun findVerifiedLocked(uid: Int): AxeronConfig.PackageEntry? {
        val entry = findLocked(uid) ?: return null
        if (uid in verified) return entry
        val packages = PackageManagerApis.getPackagesForUidNoThrow(uid)
        val signature = signatureOf(uid, packages)
        // A look-up that failed (or nobody on the uid yet) proves nothing: refuse this call, keep
        // the entry, check again on the next.
        if (signature == null) return null
        val stored = entry.signature
        if (entry.packages.orEmpty().none { it in packages } || (stored != null && stored != signature)) {
            LOGGER.w("remove config for uid %d since it isn't the app it was made for", uid)
            removeLocked(uid)
            return null
        }
        if (stored == null) {
            // Made before signatures were kept: trusted so far, so bound to the app there now.
            entry.signature = signature
            scheduleWriteLocked()
        }
        verified.add(uid)
        return entry
    }

    override fun find(uid: Int): AxeronConfig.PackageEntry? {
        synchronized(this) {
            return findVerifiedLocked(uid)
        }
    }

    /** The uids allowed now, as stored; each is checked against its app when it is next found. */
    fun allowedUids(): List<Int> =
        synchronized(this) { config.packages!!.filter { it.isAllowed }.map { it.uid } }

    /** [uid]'s processes are gone: its entry is checked again before it is used next. */
    fun invalidate(uid: Int) {
        synchronized(this) {
            verified.remove(uid)
        }
    }

    fun findOrUpdate(uid: Int): AxeronConfig.PackageEntry? {
        synchronized(this) {
            // A dropped entry is made again for the app on the uid now, not shown as the old one.
            var entry = findVerifiedLocked(uid) ?: findLocked(uid)
            if (entry == null) {
                // A first request: "ask", not denied.
                entry = AxeronConfig.PackageEntry(uid, 0)
                val packages = PackageManagerApis.getPackagesForUidNoThrow(uid)
                entry.packages!!.addAll(packages)
                entry.signature = signatureOf(uid, packages)
                config.packages!!.add(entry)
            }
            return entry
        }
    }

    private fun updateLocked(uid: Int, packages: List<String?>?, mask: Int, values: Int) {
        var entry = findLocked(uid)
        if (entry == null) {
            entry = AxeronConfig.PackageEntry(uid, mask and values)
            entry.signature = signatureOf(uid, PackageManagerApis.getPackagesForUidNoThrow(uid))
            config.packages!!.add(entry)
        } else {
            val newValue = (entry.flags and mask.inv()) or (mask and values)
            if (newValue == entry.flags) {
                return
            }
            entry.flags = newValue
        }
        if (packages != null) {
            val entryPackages = entry.packages ?: ArrayList<String>().also { entry.packages = it }
            for (packageName in packages) {
                if (packageName == null || entryPackages.contains(packageName)) {
                    continue
                }
                entryPackages.add(packageName)
            }
        }
        scheduleWriteLocked()
    }

    override fun update(uid: Int, packages: List<String?>?, mask: Int, values: Int) {
        synchronized(this) {
            updateLocked(uid, packages, mask, values)
        }
    }

    private fun removeLocked(uid: Int) {
        val entry = findLocked(uid) ?: return
        config.packages!!.remove(entry)
        verified.remove(uid)
        scheduleWriteLocked()
    }

    override fun remove(uid: Int) {
        synchronized(this) {
            removeLocked(uid)
        }
    }

    companion object {

        private val GSON_IN: Gson = GsonBuilder()
            .create()
        private val GSON_OUT: Gson = GsonBuilder()
            .setVersion(AxeronConfig.LATEST_VERSION.toDouble())
            .create()

        private const val WRITE_DELAY = 10 * 1000L

        private val FILE = PathHelper.getWorkingPath(Os.getuid() == 0, AxeronApiConstant.folder.PARENT + "ax_permission.json")
        private val ATOMIC_FILE = AtomicFile(FILE)

        @JvmStatic
        fun load(): AxeronConfig {
            val stream = try {
                ATOMIC_FILE.openRead()
            } catch (e: FileNotFoundException) {
                LOGGER.i("no existing config file " + ATOMIC_FILE.baseFile + "; starting empty")
                return AxeronConfig()
            }

            var config: AxeronConfig? = null
            try {
                config = GSON_IN.fromJson(InputStreamReader(stream), AxeronConfig::class.java)
            } catch (tr: Throwable) {
                LOGGER.w(tr, "load config")
            } finally {
                try {
                    stream.close()
                } catch (e: IOException) {
                    LOGGER.w("failed to close: $e")
                }
            }
            return config ?: AxeronConfig()
        }

        @JvmStatic
        fun write(config: AxeronConfig) {
            synchronized(ATOMIC_FILE) {
                val stream = try {
                    ATOMIC_FILE.startWrite()
                } catch (e: IOException) {
                    LOGGER.w("failed to write state: $e")
                    return
                }

                try {
                    val json = GSON_OUT.toJson(config)
                    stream.write(json.toByteArray())

                    ATOMIC_FILE.finishWrite(stream)
                    LOGGER.v("config saved")
                } catch (tr: Throwable) {
                    LOGGER.w(tr, "can't save %s, restoring backup.", ATOMIC_FILE.baseFile)
                    ATOMIC_FILE.failWrite(stream)
                }
            }
        }
    }
}
