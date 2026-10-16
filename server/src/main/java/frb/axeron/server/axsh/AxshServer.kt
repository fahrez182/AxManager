package frb.axeron.server.axsh

import android.util.SparseArray
import frb.axeron.axsh.AxshSessions
import frb.axeron.server.IAxeronService
import frb.axeron.server.util.Logger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Native `axsh` clients connect to the abstract socket [SOCKET_NAME] wherever SELinux lets them
 * (root mode) and run the shared [AxshSessions] protocol right here, without the binder round
 * trip a broker needs. Callers are identified by SO_PEERCRED.
 */
class AxshServer(
    private val server: () -> IAxeronService,
    private val authorize: (uid: Int, pid: Int) -> Boolean,
    private val requestPermission: (uid: Int, pid: Int) -> Unit,
) : AxshSessions.Host {

    companion object {
        private val LOGGER = Logger("AxshServer")

        const val SOCKET_NAME = "axeron"
    }

    override val tag = "AxshServer"

    private val sessions = AxshSessions(this)

    /** One dialog per uid in flight; every session from that uid waits on the same answer. */
    private class PermissionWait {
        val latch = CountDownLatch(1)

        @Volatile
        var allowed = false
    }

    private val waits = SparseArray<PermissionWait>()

    fun start() {
        if (!sessions.listen(SOCKET_NAME)) LOGGER.w("axsh socket @%s unavailable", SOCKET_NAME)
    }

    fun stop() = sessions.stop()

    /**
     * The permission dialog was answered for [uid]; from dispatchPermissionConfirmationResult.
     * Wakes the sessions waiting on it.
     */
    fun onPermissionResult(uid: Int, allowed: Boolean) {
        val wait = synchronized(waits) {
            waits.get(uid)?.also { waits.remove(uid) }
        } ?: return
        wait.allowed = allowed
        wait.latch.countDown()
    }

    override fun refuse(uid: Int, pid: Int): String? {
        if (authorize(uid, pid)) return null
        return if (awaitPermission(uid, pid)) null else AxshSessions.DENIED_MESSAGE
    }

    // Started from here, the shell runs as us: the calls are local, past the binder permission
    // check, which refuse() already did by the client's uid.
    override fun service(): IAxeronService = server()

    /** Asks once per uid and blocks this session until the answer, or gives up. */
    private fun awaitPermission(uid: Int, pid: Int): Boolean {
        var ask = false
        val wait = synchronized(waits) {
            waits.get(uid) ?: PermissionWait().also {
                waits.put(uid, it)
                ask = true
            }
        }
        if (ask) {
            try {
                requestPermission(uid, pid)
            } catch (e: Throwable) {
                LOGGER.w(e, "permission request for uid=%d failed", uid)
                onPermissionResult(uid, false)
            }
        }

        val answered = wait.latch.await(AxshSessions.PERMISSION_WAIT_MS, TimeUnit.MILLISECONDS)
        if (!answered) {
            // Unanswered (dismissed or left open): the next client asks again.
            synchronized(waits) {
                if (waits.get(uid) === wait) waits.remove(uid)
            }
        }
        return answered && wait.allowed
    }
}
