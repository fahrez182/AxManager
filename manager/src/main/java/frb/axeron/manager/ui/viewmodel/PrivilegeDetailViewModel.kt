package frb.axeron.manager.ui.viewmodel

import android.app.AppOpsManager
import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.PowerManager
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import frb.axeron.manager.R
import frb.axeron.manager.data.PackageControl
import frb.axeron.shared.MiuiAppOps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.compatibility.DeviceCompatibility

/**
 * Live state of one app for the Privilege detail screen. Package info is re-read here rather
 * than taken from the list, so a grant made a moment ago shows up.
 */
class PrivilegeDetailViewModel(application: Application) : AndroidViewModel(application) {

    /** How a permission can be changed. Only [RUNTIME] and [DEVELOPMENT] go through `pm grant`. */
    enum class Kind {
        /** Dangerous: the user grants it (camera, location, …). */
        RUNTIME,

        /** Not shown to users, but `pm grant` can give it (READ_LOGS, WRITE_SECURE_SETTINGS, …). */
        DEVELOPMENT,

        /** Granted through an app-op ("special access"): display over apps, all files, …. */
        SPECIAL,

        /** Granted at install. */
        NORMAL,

        /** Only for apps signed like the platform; can't be granted. */
        SIGNATURE,

        /** Not defined on this device (another app's, or a newer Android's). */
        UNKNOWN;

        /** Changeable with `pm grant` / `pm revoke`. */
        val grantable: Boolean get() = this == RUNTIME || this == DEVELOPMENT
    }

    @Immutable
    data class Permission(
        val name: String,
        val label: String,
        /** What it allows, in a sentence; null when neither the system nor we have one. */
        val description: String? = null,
        val kind: Kind,
        /** In effect: for [Kind.SPECIAL] that is the app-op mode, not the grant flag. */
        val granted: Boolean,
        /** App-op behind a [Kind.SPECIAL] permission ("SYSTEM_ALERT_WINDOW"), as `cmd appops` names it. */
        val appOp: String? = null,
        /** Its package and uid-wide modes; null when unread (server off) or never set. */
        val appOpState: PackageControl.AppOpState? = null
    )

    var packageName: String = ""
        private set
    var userId: Int = 0
        private set

    /** Device-level switches every app has, whatever it requests. */
    enum class GeneralItem { BATTERY_UNRESTRICTED, BACKGROUND_ACTIVITY, ENABLED }

    var permissions by mutableStateOf(emptyList<Permission>())
        private set

    /**
     * App-ops the system reports for this app that nothing above stands for: no requested
     * permission, not a General switch. Read from `cmd appops get`, so a ROM's own ops show up
     * whatever they are called; [knownAppOpText] only names the ones it knows.
     */
    var otherAppOps by mutableStateOf(emptyList<Permission>())
        private set

    /** On/off per [GeneralItem]; an item is absent when its state can't be read. */
    var general by mutableStateOf(emptyMap<GeneralItem, Boolean>())
        private set

    var loading by mutableStateOf(true)
        private set

    /** The app was uninstalled (for this user) while the page was open or in the background. */
    var gone by mutableStateOf(false)
        private set

    /** Permission names and [GeneralItem] names with a change in flight. */
    val changing = mutableStateMapOf<String, Boolean>()

    /** Binds to an app once; later calls with the same app keep the loaded state. */
    fun bind(packageName: String, uid: Int) {
        if (this.packageName == packageName) return
        this.packageName = packageName
        userId = PackageControl.userIdOf(uid)
        refresh()
    }

    private var refreshJob: Job? = null

    /**
     * Re-reads everything. Called on bind and whenever the screen resumes, since grants can
     * change outside it (system Settings, the Shizuku app, a shell). A load already running
     * covers the request, so the first open doesn't load twice.
     */
    fun refresh() {
        if (packageName.isEmpty() || refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            load()
            loading = false
        }
    }

    /** Last read app-op state, so a write knows whether a uid-wide mode must be cleared. */
    private var appOps: Map<String, PackageControl.AppOpState> = emptyMap()

    /** Grants or revokes a runtime/development permission; on failure the outcome says why. */
    suspend fun setGranted(permission: Permission, grant: Boolean): PackageControl.Outcome =
        change(permission.name) {
            PackageControl.setPermission(packageName, userId, permission.name, grant)
        }

    /**
     * Sets the app-op [mode] of a special-access permission, for the package or, with [uid],
     * uid-wide. A package write also resets a uid-wide mode that would keep overriding it; a
     * uid write leaves the package mode as it is.
     */
    suspend fun setAppOpMode(permission: Permission, mode: String, uid: Boolean = false): PackageControl.Outcome {
        val op = permission.appOp ?: return PackageControl.Outcome(false, error = "No app-op")
        return change(permission.name) {
            if (uid) PackageControl.setUidAppOp(packageName, userId, op, mode)
            else writeAppOp(op, mode)
        }
    }

    suspend fun setGeneral(item: GeneralItem, on: Boolean): PackageControl.Outcome =
        change(item.name) {
            when (item) {
                GeneralItem.BATTERY_UNRESTRICTED -> PackageControl.setBatteryExempt(packageName, on)
                GeneralItem.BACKGROUND_ACTIVITY ->
                    writeAppOp(OP_RUN_ANY_IN_BACKGROUND, if (on) "allow" else "ignore")
                GeneralItem.ENABLED -> PackageControl.setEnabled(packageName, userId, on)
            }
        }

    private suspend fun writeAppOp(op: String, mode: String) = PackageControl.setAppOpMode(
        packageName, userId, op, mode,
        clearUidOverride = appOps[op]?.uidOverrides == true
    )

    /** Runs [block] with [id] marked busy, then re-reads everything on success. */
    private suspend fun change(id: String, block: suspend () -> PackageControl.Outcome): PackageControl.Outcome {
        changing[id] = true
        val outcome = block()
        if (outcome.ok) load()
        changing.remove(id)
        return outcome
    }

    /** One `cmd appops get` feeds both special access and background activity. */
    private suspend fun load() {
        if (!withContext(Dispatchers.IO) { isInstalled() }) {
            gone = true
            return
        }
        val ops = PackageControl.appOps(packageName, userId)
        appOps = ops.orEmpty()
        val (perms, gen) = withContext(Dispatchers.IO) { readPermissions(ops) to readGeneral(ops) }
        permissions = perms
        general = gen
        otherAppOps = withContext(Dispatchers.IO) { readOtherAppOps(ops) }
    }

    /** Disabled apps still count as installed, so turning one off doesn't read as "gone". */
    private fun isInstalled(): Boolean = runCatching {
        getApplication<Application>().packageManager.getApplicationInfo(packageName, MATCH_DISABLED)
    }.isSuccess

    private fun readGeneral(appOps: Map<String, PackageControl.AppOpState>?): Map<GeneralItem, Boolean> {
        val app = getApplication<Application>()
        return buildMap {
            app.getSystemService(PowerManager::class.java)?.let {
                put(GeneralItem.BATTERY_UNRESTRICTED, it.isIgnoringBatteryOptimizations(packageName))
            }
            // Restricted means "ignore"; allow and default both let it run in the background.
            appOps?.let {
                put(GeneralItem.BACKGROUND_ACTIVITY, it[OP_RUN_ANY_IN_BACKGROUND]?.effective != "ignore")
            }
            runCatching { app.packageManager.getApplicationInfo(packageName, MATCH_DISABLED).enabled }
                .getOrNull()?.let { put(GeneralItem.ENABLED, it) }
        }
    }

    /**
     * @param appOps from the server; null when it is off, and special access then falls
     * back to the grant flag (which an app allowed through Settings usually doesn't have).
     */
    /**
     * A permission's label and description come from the package that *declares* it (for
     * android.permission.* the framework), never from the app requesting it. Some special
     * accesses are declared without either (PACKAGE_USAGE_STATS): system Settings names them
     * from its own strings, which differ per OEM. These are the same names, as a fallback.
     */
    /**
     * OEM op numbers aren't standard: 10008 is Autostart on MIUI and may be anything elsewhere.
     * So MIUI's names only apply on MIUI; on other ROMs the same number shows as it is.
     */
    private val isMiui by lazy { DeviceCompatibility.isMiui() }

    // FIXME: the MIUI names are a static table (MiuiAppOps). OEMs number their own ops however
    // they like and a code can mean something else on another vendor's ROM, or change between
    // ROM versions, so a name here can mislead. It is only used on MIUI for that reason; a
    // dynamic source (the ROM's own permission app, or a community mapping) should replace it.
    private fun knownAppOpText(appOp: String): Pair<Int, Int>? =
        if (appOp.all(Char::isDigit) && !isMiui) null
        else knownAppOpTextOnThisRom(appOp)

    /**
     * What the ROM itself calls an op it keys by number: the hidden AppOpsManager.opToName,
     * which knows a ROM's own ops when the ROM registered a name. Falls back to how
     * `cmd appops` prints it.
     */
    private fun romOpName(op: String): String {
        if (!op.all(Char::isDigit)) return op
        val code = op.toIntOrNull() ?: return op
        val name = runCatching {
            AppOpsManager::class.java.getMethod("opToName", Int::class.javaPrimitiveType)
                .invoke(null, code) as? String
        }.getOrNull()
        return name?.takeIf { it.isNotBlank() } ?: "MIUIOP($op)"
    }

    private fun knownAppOpTextOnThisRom(appOp: String): Pair<Int, Int>? = when (appOp) {
        "SYSTEM_ALERT_WINDOW" -> R.string.special_access_overlay to R.string.special_access_overlay_desc
        "WRITE_SETTINGS" -> R.string.special_access_write_settings to R.string.special_access_write_settings_desc
        "GET_USAGE_STATS" -> R.string.special_access_usage to R.string.special_access_usage_desc
        "MANAGE_EXTERNAL_STORAGE" -> R.string.special_access_all_files to R.string.special_access_all_files_desc
        "REQUEST_INSTALL_PACKAGES" -> R.string.special_access_install to R.string.special_access_install_desc
        "SCHEDULE_EXACT_ALARM" -> R.string.special_access_alarms to R.string.special_access_alarms_desc
        "MANAGE_MEDIA" -> R.string.special_access_media to R.string.special_access_media_desc
        "USE_FULL_SCREEN_INTENT" -> R.string.special_access_full_screen to R.string.special_access_full_screen_desc
        // MIUI/HyperOS ops come keyed by number (see PackageControl.parseAppOps).
        "${MiuiAppOps.AUTO_START}" -> R.string.miui_op_autostart to R.string.miui_op_autostart_desc
        "${MiuiAppOps.INSTALL_SHORTCUT}" -> R.string.miui_op_shortcut to R.string.miui_op_shortcut_desc
        "${MiuiAppOps.BACKGROUND_START_ACTIVITY}" ->
            R.string.miui_op_background_start to R.string.miui_op_background_start_desc
        else -> null
    }

    /**
     * What [readOtherAppOps] leaves out: ops behind the app's requested permissions (shown with
     * those), the General switch, and the MIUI ops [miuiPermissions] lists anyway.
     */
    private fun coveredAppOps(): Set<String> {
        val pm = getApplication<Application>().packageManager
        val requested = runCatching {
            pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS or MATCH_DISABLED).requestedPermissions
        }.getOrNull().orEmpty()
        return buildSet {
            requested.forEach { name ->
                AppOpsManager.permissionToOp(name)?.removePrefix("android:")?.uppercase()?.let(::add)
            }
            permissions.mapNotNullTo(this) { it.appOp }
            add(OP_RUN_ANY_IN_BACKGROUND)
        }
    }

    private fun readOtherAppOps(appOps: Map<String, PackageControl.AppOpState>?): List<Permission> {
        if (appOps.isNullOrEmpty()) return emptyList()
        val app = getApplication<Application>()
        val covered = coveredAppOps()
        return appOps.filterKeys { it !in covered }.map { (op, state) ->
            val known = knownAppOpText(op)
            val shown = romOpName(op)
            Permission(
                name = shown,
                label = known?.let { app.getString(it.first) } ?: shown,
                description = app.getString(known?.second ?: R.string.app_op_unknown_desc),
                kind = Kind.SPECIAL,
                granted = state.effective == "allow" || state.effective == "foreground",
                appOp = op,
                appOpState = state
            )
        }.sortedBy { it.label.lowercase() }
    }

    private fun readPermissions(appOps: Map<String, PackageControl.AppOpState>?): List<Permission> {
        val app = getApplication<Application>()
        val pm = app.packageManager
        val info: PackageInfo = runCatching {
            pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS or MATCH_DISABLED)
        }.getOrNull() ?: return emptyList()
        // No requested permissions still leaves the MIUI ops, which every app has.
        val names = info.requestedPermissions ?: emptyArray()
        val flags = info.requestedPermissionsFlags

        return names.mapIndexed { i, name ->
            val granted = flags != null &&
                    (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            val defined = runCatching { pm.getPermissionInfo(name, 0) }.getOrNull()
            val kind = defined?.kind() ?: Kind.UNKNOWN
            // "android:system_alert_window" → "SYSTEM_ALERT_WINDOW", the name cmd appops prints.
            val appOp = if (kind != Kind.SPECIAL) null
            else AppOpsManager.permissionToOp(name)?.removePrefix("android:")?.uppercase()
            val state = appOp?.let { appOps?.get(it) }
            // Special access goes by the names Settings shows; the system label comes after.
            val known = appOp?.let { knownAppOpText(it) }
            Permission(
                name = name,
                label = known?.let { app.getString(it.first) }
                    ?: defined?.loadLabel(pm)?.toString()?.takeIf { it != name }
                        ?.replaceFirstChar { it.uppercaseChar() }
                    ?: name.substringAfterLast('.'),
                description = defined?.loadDescription(pm)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                    ?: known?.let { app.getString(it.second) },
                kind = kind,
                granted = when (state?.effective) {
                    "allow", "foreground" -> true
                    "ignore", "deny", "errored" -> false
                    else -> granted // "default" or unset defers to the grant flag
                },
                appOp = appOp,
                appOpState = state
            )
        }.distinctBy { it.name }
            .plus(miuiPermissions(appOps))
            .sortedWith(compareBy({ it.kind.ordinal }, { it.label.lowercase() }))
    }

    /**
     * The MIUI/HyperOS ops that matter even before anything set them (an unset op isn't in
     * `cmd appops get`, so [readOtherAppOps] can't find it): always listed on those ROMs.
     * Unset reads as not allowed, which is how MIUI treats it.
     */
    private fun miuiPermissions(appOps: Map<String, PackageControl.AppOpState>?): List<Permission> {
        if (!isMiui) return emptyList()
        val app = getApplication<Application>()
        return listOf(
            MiuiAppOps.AUTO_START,
            MiuiAppOps.INSTALL_SHORTCUT,
            MiuiAppOps.BACKGROUND_START_ACTIVITY,
        ).map { code ->
            val op = code.toString()
            val state = appOps?.get(op)
            val text = knownAppOpText(op)
            val romName = romOpName(op)
            Permission(
                name = romName,
                label = text?.let { app.getString(it.first) } ?: romName,
                description = app.getString(text?.second ?: R.string.app_op_unknown_desc),
                kind = Kind.SPECIAL,
                granted = state?.effective == "allow" || state?.effective == "foreground",
                appOp = op,
                appOpState = state
            )
        }
    }

    private companion object {
        const val OP_RUN_ANY_IN_BACKGROUND = "RUN_ANY_IN_BACKGROUND"


        /** Keeps a disabled app readable instead of looking uninstalled. */
        const val MATCH_DISABLED = PackageManager.MATCH_DISABLED_COMPONENTS or
                PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
    }

    @Suppress("DEPRECATION") // protectionLevel: `protection`/`protectionFlags` need API 28
    private fun PermissionInfo.kind(): Kind {
        val base = protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
        val extra = protectionLevel and PermissionInfo.PROTECTION_MASK_FLAGS
        return when {
            base == PermissionInfo.PROTECTION_DANGEROUS -> Kind.RUNTIME
            // App-op before development: SYSTEM_ALERT_WINDOW and PACKAGE_USAGE_STATS carry both
            // flags, and what decides them in practice is the app-op, not the grant flag.
            extra and PermissionInfo.PROTECTION_FLAG_APPOP != 0 &&
                    AppOpsManager.permissionToOp(name) != null -> Kind.SPECIAL
            extra and PermissionInfo.PROTECTION_FLAG_DEVELOPMENT != 0 -> Kind.DEVELOPMENT
            base == PermissionInfo.PROTECTION_NORMAL -> Kind.NORMAL
            else -> Kind.SIGNATURE
        }
    }
}
