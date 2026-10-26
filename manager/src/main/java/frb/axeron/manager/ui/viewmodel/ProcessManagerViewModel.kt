package frb.axeron.manager.ui.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import frb.axeron.api.Axeron
import frb.axeron.manager.data.PackageControl
import frb.axeron.manager.data.ProcessControl
import frb.axeron.manager.data.ProcessControl.Outcome
import frb.axeron.manager.data.ProcessControl.Proc
import frb.axeron.manager.data.ProcessControl.Signal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Data for the process manager: one `ps` snapshot per refresh, with app names put on app uids.
 * Search, filter, sort and the tree are derived from the snapshot, so they cost no process.
 */
class ProcessManagerViewModel(application: Application) : AndroidViewModel(application) {

    /** [info] is what the icon loader (`AsyncImage(model = packageInfo)`) takes. */
    @Immutable
    class App(val info: PackageInfo, val label: String) {
        val packageName: String get() = info.packageName
    }

    /** One per tab: every process falls in exactly one of APPS, SYSTEM and KERNEL. */
    enum class Category { ALL, APPS, SYSTEM, KERNEL, AXMANAGER }

    @Immutable
    class Row(
        val proc: Proc,
        /** The app(s) this process runs (see [appsOf]); empty for daemons and kernel threads. */
        val apps: List<App>,
        /** Not in the previous snapshot. */
        val isNew: Boolean,
        /** Started by the AxManager server, directly or further down. */
        val fromServer: Boolean,
        /** Tree mode: how deep under its shown ancestor; 0 otherwise. */
        val depth: Int = 0,
    ) {
        val pid get() = proc.pid
        val title: String = apps.firstOrNull()?.label ?: proc.name

        /** kthreadd (pid 2) and everything it starts. */
        val isKernel: Boolean get() = proc.pid == 2 || proc.ppid == 2

        fun isIn(category: Category): Boolean = when (category) {
            Category.ALL -> true
            Category.APPS -> apps.isNotEmpty()
            Category.SYSTEM -> apps.isEmpty() && !isKernel
            Category.KERNEL -> isKernel
            Category.AXMANAGER -> fromServer
        }
    }

    enum class SortBy { PID, NAME, RSS, VSZ, CPU, UID }

    /** Every field null or empty = no constraint. */
    @Immutable
    data class Filter(
        val pid: Int? = null,
        val ppid: Int? = null,
        val pgid: Int? = null,
        val uid: Int? = null,
        /** KiB. */
        val minRss: Long? = null,
        /** KiB. */
        val maxRss: Long? = null,
        /** First letter of `ps` S: R, S, D, Z, T, … */
        val states: Set<String> = emptySet(),
    ) {
        val isActive get() = this != Filter()
    }

    var rows by mutableStateOf<List<Row>>(emptyList())
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set

    var query by mutableStateOf("")
    var filter by mutableStateOf(Filter())
    var category by mutableStateOf(Category.ALL)
    var sortBy by mutableStateOf(SortBy.RSS)
    var ascending by mutableStateOf(false)
    var treeMode by mutableStateOf(false)

    /** Seconds between refreshes while [autoRefresh] runs. */
    var interval by mutableStateOf(3)

    private var appsByAppId: Map<Int, List<App>>? = null
    private var previousPids: Set<Int> = emptySet()
    private var autoJob: Job? = null

    val isAutoRefreshing: Boolean get() = autoJob?.isActive == true

    /** [rows] passing the search and filter, before the category tab picks its share. */
    private val matched: List<Row> by derivedStateOf {
        val q = query.trim()
        val f = filter
        rows.filter { it.matches(q) && it.matches(f) }
    }

    val shown: List<Row> by derivedStateOf {
        val c = category
        val inTab = matched.filter { it.isIn(c) }
        if (treeMode) tree(inTab) else inTab.sortedWith(comparator())
    }

    /** What tab [category] would show, for the count on its pill. */
    fun count(category: Category): Int = matched.count { it.isIn(category) }

    fun refresh() {
        if (isLoading) return
        isLoading = true
        viewModelScope.launch {
            load()
            isLoading = false
        }
    }

    /** Refreshes every [interval] seconds until [stopAutoRefresh] or the screen leaves. */
    fun startAutoRefresh() {
        if (isAutoRefreshing) return
        autoJob = viewModelScope.launch {
            while (isActive) {
                load()
                delay(interval * 1000L)
            }
        }
    }

    fun stopAutoRefresh() {
        autoJob?.cancel()
        autoJob = null
    }

    suspend fun signal(pid: Int, signal: Signal): Outcome =
        ProcessControl.signal(pid, signal).also { refresh() }

    suspend fun signalGroup(pgid: Int, signal: Signal): Outcome =
        ProcessControl.signalGroup(pgid, signal).also { refresh() }

    suspend fun forceStop(app: App, uid: Int): PackageControl.Outcome =
        PackageControl.forceStop(app.packageName, PackageControl.userIdOf(uid)).also { refresh() }

    /** Everything the detail sheet shows, each read on its own: one refused read hides nothing else. */
    suspend fun detail(pid: Int): Map<String, Outcome> = coroutineScope {
        val reads = listOf(
            "status" to async { ProcessControl.readProc(pid, "status") },
            "cmdline" to async { ProcessControl.readProc(pid, "cmdline") },
            "environ" to async { ProcessControl.readProc(pid, "environ") },
            "exe" to async { ProcessControl.readLink(pid, "exe") },
            "cwd" to async { ProcessControl.readLink(pid, "cwd") },
            "cgroup" to async { ProcessControl.readProc(pid, "cgroup") },
            "oom_score_adj" to async { ProcessControl.readProc(pid, "oom_score_adj") },
            "limits" to async { ProcessControl.readProc(pid, "limits") },
            "fd" to async { ProcessControl.fds(pid) },
        )
        reads.associate { (name, read) -> name to read.await() }
    }

    private suspend fun load() {
        val result = ProcessControl.list()
        val procs = result.getOrElse {
            error = it.message
            return
        }
        val apps = appsByAppId ?: loadApps().also { appsByAppId = it }
        val serverPid = withContext(Dispatchers.IO) {
            runCatching { Axeron.getAxeronInfo().serverInfo.pid }.getOrDefault(-1)
        }
        val fromServer = descendants(procs, serverPid)
        val firstLoad = previousPids.isEmpty()

        rows = procs.map { proc ->
            Row(
                proc = proc,
                apps = appsOf(proc, apps[proc.uid % PER_USER_RANGE].orEmpty()),
                isNew = !firstLoad && proc.pid !in previousPids,
                fromServer = proc.pid in fromServer,
            )
        }
        previousPids = procs.mapTo(HashSet()) { it.pid }
        error = null
    }

    /** Apps keyed by app id (uid without the user part), so other users' processes match too. */
    private suspend fun loadApps(): Map<Int, List<App>> = withContext(Dispatchers.IO) {
        val pm = getApplication<Application>().packageManager
        val packages = runCatching { Axeron.getPackages(0) }.getOrDefault(emptyList())
        packages.mapNotNull { info -> info.applicationInfo?.let { info to it } }.map { (info, appInfo) ->
            async {
                val label = runCatching { appInfo.loadLabel(pm).toString() }.getOrDefault(appInfo.packageName)
                appInfo.uid % PER_USER_RANGE to App(info, label)
            }
        }.awaitAll().groupBy({ it.first }, { it.second })
    }

    /**
     * The app(s) [proc] belongs to among [candidates], the packages of its uid. An app process is
     * named after its package (`pkg` or `pkg:suffix`), so the name decides. The uid alone only
     * when it can't mislead: an app uid (10000+) with one package. A shared system uid
     * (android.uid.system, …) holds many packages, and native daemons run under it too.
     */
    private fun appsOf(proc: Proc, candidates: List<App>): List<App> {
        val pkg = proc.name.substringBefore(':')
        val named = candidates.filter { it.packageName == pkg }
        if (named.isNotEmpty()) return named
        val appId = proc.uid % PER_USER_RANGE
        return if (appId >= FIRST_APPLICATION_UID && candidates.size == 1) candidates else emptyList()
    }

    /** [root] and everything under it. */
    private fun descendants(procs: List<Proc>, root: Int): Set<Int> {
        if (root <= 0) return emptySet()
        val children = procs.groupBy { it.ppid }
        val found = HashSet<Int>()
        val stack = ArrayDeque(listOf(root))
        while (stack.isNotEmpty()) {
            val pid = stack.removeLast()
            if (found.add(pid)) children[pid]?.forEach { stack.addLast(it.pid) }
        }
        return found
    }

    /**
     * Depth-first under each parent, siblings in sort order. A row whose parent was filtered
     * out starts its own tree.
     */
    private fun tree(matched: List<Row>): List<Row> {
        val pids = matched.mapTo(HashSet()) { it.pid }
        val children = matched.groupBy { it.proc.ppid }
        val order = comparator()
        val out = ArrayList<Row>(matched.size)
        fun visit(row: Row, depth: Int) {
            out += Row(row.proc, row.apps, row.isNew, row.fromServer, depth)
            children[row.pid]?.sortedWith(order)?.forEach {
                if (it.pid != row.pid) visit(it, depth + 1)
            }
        }
        matched.filter { it.proc.ppid !in pids || it.proc.ppid == it.pid }
            .sortedWith(order)
            .forEach { visit(it, 0) }
        return out
    }

    private fun Row.matches(q: String): Boolean =
        q.isEmpty() ||
                proc.pid.toString() == q ||
                proc.name.contains(q, true) ||
                proc.args.contains(q, true) ||
                proc.user.contains(q, true) ||
                proc.label?.contains(q, true) == true ||
                apps.any { it.label.contains(q, true) || it.packageName.contains(q, true) }

    private fun Row.matches(f: Filter): Boolean =
        (f.pid == null || proc.pid == f.pid) &&
                (f.ppid == null || proc.ppid == f.ppid) &&
                (f.pgid == null || proc.pgid == f.pgid) &&
                (f.uid == null || proc.uid == f.uid) &&
                (f.minRss == null || proc.rss >= f.minRss) &&
                (f.maxRss == null || proc.rss <= f.maxRss) &&
                (f.states.isEmpty() || proc.state.take(1) in f.states)

    private fun comparator(): Comparator<Row> {
        val base: Comparator<Row> = when (sortBy) {
            SortBy.PID -> compareBy { it.pid }
            SortBy.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            SortBy.RSS -> compareBy { it.proc.rss }
            SortBy.VSZ -> compareBy { it.proc.vsz }
            SortBy.CPU -> compareBy { it.proc.cpu ?: 0f }
            SortBy.UID -> compareBy { it.proc.uid }
        }
        return (if (ascending) base else base.reversed()).thenBy { it.pid }
    }

    override fun onCleared() {
        stopAutoRefresh()
    }

    private companion object {
        /** Uids of user N are N * 100000 + app id. */
        const val PER_USER_RANGE = 100_000

        /** android.os.Process.FIRST_APPLICATION_UID: app ids below are the system's own. */
        const val FIRST_APPLICATION_UID = 10_000
    }
}
