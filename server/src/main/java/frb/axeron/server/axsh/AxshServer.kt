package frb.axeron.server.axsh

import android.net.LocalSocket
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import android.util.SparseArray
import frb.axeron.shell.Sessions
import frb.axeron.server.IAxeronService
import frb.axeron.server.util.Logger
import java.io.FileDescriptor
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Native `axsh` clients connect to the abstract socket [SOCKET_NAME] wherever SELinux lets them
 * (root mode) and run the shared [Sessions] protocol right here, without the binder round
 * trip a broker needs. Callers are identified by SO_PEERCRED.
 */
class AxshServer(
    private val server: () -> IAxeronService,
    private val authorize: (uid: Int, pid: Int) -> Boolean,
    private val requestPermission: (uid: Int, pid: Int) -> Unit,
) : Sessions.Host {

    companion object {
        private val LOGGER = Logger("AxshServer")

        const val SOCKET_NAME = "axeron"
    }

    override val tag = "AxshServer"

    private val sessions = Sessions(this)

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
     * A channel for a helper running as [uid] (axsh's native daemon), which can't connect to
     * [SOCKET_NAME] under SELinux but can hold a socket we made. Each byte it sends carries one
     * client socket (SCM_RIGHTS) that it accepted; the session is then served here, exactly as
     * if the client had connected to us. Returns the helper's end.
     */
    fun openChannel(uid: Int): ParcelFileDescriptor {
        val pair = ParcelFileDescriptor.createSocketPair()
        synchronized(channels) {
            (channels.get(uid) ?: mutableListOf<ParcelFileDescriptor>().also { channels.put(uid, it) })
                .add(pair[0])
        }
        thread(name = "axsh-channel-$uid", isDaemon = true) { serveChannel(uid, pair[0]) }
        return pair[1]
    }

    /** Our ends of the open channels, by uid. */
    private val channels = SparseArray<MutableList<ParcelFileDescriptor>>()

    /**
     * Ends [uid]'s channels: its daemon sees EOF and exits. shutdown, not close: close doesn't
     * wake the channel thread blocked reading it, which then closes the fd itself.
     */
    fun closeChannels(uid: Int) {
        val open = synchronized(channels) { channels.get(uid)?.toList() }.orEmpty()
        for (own in open) {
            try {
                Os.shutdown(own.fileDescriptor, OsConstants.SHUT_RDWR)
            } catch (_: Exception) {
            }
        }
        if (open.isNotEmpty()) LOGGER.i("closed %d axsh channel(s) of uid=%d", open.size, uid)
    }

    private fun serveChannel(uid: Int, own: ParcelFileDescriptor) {
        LOGGER.i("axsh channel for uid=%d open", uid)
        try {
            val channel = localSocket(own.fileDescriptor)
            val input = channel.inputStream
            while (input.read() >= 0) {
                channel.ancillaryFileDescriptors?.forEach { take(uid, it) }
            }
        } catch (e: Exception) {
            LOGGER.w(e, "axsh channel for uid=%d failed", uid)
        } finally {
            synchronized(channels) {
                channels.get(uid)?.let {
                    it.remove(own)
                    if (it.isEmpty()) channels.remove(uid)
                }
            }
            try {
                own.close()
            } catch (_: IOException) {
            }
            LOGGER.i("axsh channel for uid=%d closed", uid)
        }
    }

    /** One forwarded client: only the channel's own uid, which refuse() then checks as usual. */
    private fun take(uid: Int, fd: FileDescriptor) {
        val client = try {
            localSocket(fd)
        } catch (e: Exception) {
            LOGGER.w(e, "forwarded fd is not usable")
            try {
                Os.close(fd)
            } catch (_: Exception) {
            }
            return
        }
        val peer = try {
            client.peerCredentials
        } catch (_: IOException) {
            null
        }
        if (peer?.uid != uid) {
            LOGGER.w("channel of uid=%d forwarded a client of uid=%s, dropped", uid, peer?.uid.toString())
            client.close()
            return
        }
        thread(name = "axsh-session", isDaemon = true) { sessions.serve(client) }
    }

    // LocalSocket(FileDescriptor) is hidden; the server runs in app_process, without enforcement.
    private fun localSocket(fd: FileDescriptor): LocalSocket =
        LocalSocket::class.java.getDeclaredConstructor(FileDescriptor::class.java)
            .apply { isAccessible = true }
            .newInstance(fd)

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
        return if (awaitPermission(uid, pid)) null else Sessions.DENIED_MESSAGE
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

        val answered = wait.latch.await(Sessions.PERMISSION_WAIT_MS, TimeUnit.MILLISECONDS)
        if (!answered) {
            // Unanswered (dismissed or left open): the next client asks again.
            synchronized(waits) {
                if (waits.get(uid) === wait) waits.remove(uid)
            }
        }
        return answered && wait.allowed
    }
}
