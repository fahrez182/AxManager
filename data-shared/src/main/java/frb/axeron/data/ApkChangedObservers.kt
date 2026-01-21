package frb.axeron.shared

import android.os.Build
import android.os.FileObserver
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

fun interface ApkChangedListener {
    fun onApkChanged()
}

private val observers = ConcurrentHashMap<String, ApkChangedObserver>()

object ApkChangedObservers {

    @JvmStatic
    fun start(apkPath: String, mainHandler: Handler, listener: ApkChangedListener) {
        // inotify watchs inode, if the there are still processes holds the file, DELTE_SELF will not be triggered
        // so we need to watch the parent folder

        val path = File(apkPath)
        val observer = observers.getOrPut(path.parent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ApkChangedObserver(mainHandler, path).apply {
                    startWatching()
                }
            } else {
                ApkChangedObserver(mainHandler, path.parent).apply {
                    startWatching()
                }
            }
        }
        observer.addListener(listener)
    }

//    @JvmStatic
//    fun stop(listener: ApkChangedListener) {
//        val pathToRemove = mutableListOf<String>()
//
//        for ((path, observer) in observers) {
//            observer.removeListener(listener)
//
//            if (!observer.hasListeners()) {
//                pathToRemove.add(path)
//            }
//        }
//
//        for (path in pathToRemove) {
//            observers.remove(path)?.stopWatching()
//        }
//    }

    @JvmStatic
    fun stop(listener: ApkChangedListener) {
        observers.entries.removeIf { (_, observer) ->
            observer.removeListener(listener)
            if (!observer.hasListeners()) {
                observer.stopWatching()
                true
            } else false
        }
    }

}

class ApkChangedObserver : FileObserver {

    private val path: File
    private val handler: Handler
    private val listeners: CopyOnWriteArraySet<ApkChangedListener>

    @RequiresApi(Build.VERSION_CODES.Q)
    constructor(mainHandler: Handler, path: File) : super(path, DELETE or DELETE_SELF or MOVE_SELF or CLOSE_WRITE) {
        this.path = path
        this.handler = mainHandler
        this.listeners = CopyOnWriteArraySet<ApkChangedListener>()
    }

    @Suppress("DEPRECATION")
    constructor(mainHandler: Handler, path: String?) : super(path, DELETE or DELETE_SELF or MOVE_SELF or CLOSE_WRITE) {
        if (path == null) throw IllegalArgumentException("path cannot be null")
        this.path = File(path)
        this.handler = mainHandler
        this.listeners = CopyOnWriteArraySet<ApkChangedListener>()
    }



    fun addListener(listener: ApkChangedListener): Boolean {
        return listeners.add(listener)
    }

    fun removeListener(listener: ApkChangedListener): Boolean {
        return listeners.remove(listener)
    }

    fun hasListeners(): Boolean {
        return listeners.isNotEmpty()
    }

    @Volatile
    private var active = true

    override fun onEvent(event: Int, path: String?) {
        if (!active || path == null) return
        if (path == "base.apk") {
            active = false
            handler.post {
                stopWatching()
                listeners.forEach { it.onApkChanged() }
            }
        }
    }

    override fun startWatching() {
        super.startWatching()
        Log.d("AxeronServer", "start watching $path")
    }

    override fun stopWatching() {
        super.stopWatching()
        Log.d("AxeronServer", "stop watching $path")
    }
}

private fun eventToString(event: Int): String {
    val sb = StringBuilder()
    if (event and FileObserver.ACCESS == FileObserver.ACCESS) {
        sb.append("ACCESS").append(" | ")
    }
    if (event and FileObserver.MODIFY == FileObserver.MODIFY) {
        sb.append("MODIFY").append(" | ")
    }
    if (event and FileObserver.ATTRIB == FileObserver.ATTRIB) {
        sb.append("ATTRIB").append(" | ")
    }
    if (event and FileObserver.CLOSE_WRITE == FileObserver.CLOSE_WRITE) {
        sb.append("CLOSE_WRITE").append(" | ")
    }
    if (event and FileObserver.CLOSE_NOWRITE == FileObserver.CLOSE_NOWRITE) {
        sb.append("CLOSE_NOWRITE").append(" | ")
    }
    if (event and FileObserver.OPEN == FileObserver.OPEN) {
        sb.append("OPEN").append(" | ")
    }
    if (event and FileObserver.MOVED_FROM == FileObserver.MOVED_FROM) {
        sb.append("MOVED_FROM").append(" | ")
    }
    if (event and FileObserver.MOVED_TO == FileObserver.MOVED_TO) {
        sb.append("MOVED_TO").append(" | ")
    }
    if (event and FileObserver.CREATE == FileObserver.CREATE) {
        sb.append("CREATE").append(" | ")
    }
    if (event and FileObserver.DELETE == FileObserver.DELETE) {
        sb.append("DELETE").append(" | ")
    }
    if (event and FileObserver.DELETE_SELF == FileObserver.DELETE_SELF) {
        sb.append("DELETE_SELF").append(" | ")
    }
    if (event and FileObserver.MOVE_SELF == FileObserver.MOVE_SELF) {
        sb.append("MOVE_SELF").append(" | ")
    }

    if (event and 0x00008000 == 0x00008000) {
        sb.append("IN_IGNORED").append(" | ")
    }

    if (event and 0x40000000 == 0x40000000) {
        sb.append("IN_ISDIR").append(" | ")
    }

    return if (sb.isNotEmpty()) {
        sb.substring(0, sb.length - 3)
    } else {
        sb.toString()
    }
}
