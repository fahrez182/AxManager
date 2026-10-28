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
import java.security.SecureRandom
import kotlin.concurrent.thread

/**
 * Native `axsh` clients connect to the abstract socket [socketName] wherever SELinux lets them
 * (root mode) and run the shared [Sessions] protocol right here, without the binder round
 * trip a broker needs. Callers are identified by SO_PEERCRED.
 *
 * The name is new on every start and only told to allowed apps (getAxshSocketName), so an app
 * that isn't can't find the socket, let alone knock on it.
 */
class AxshServer(
    private val server: () -> IAxeronService,
    private val authorize: (uid: Int, pid: Int) -> Boolean,
) : Sessions.Host {

    companion object {
        private val LOGGER = Logger("AxshServer")
    }

    val socketName: String = "axeron." +
        ByteArray(8).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }

    override val tag = "AxshServer"

    private val sessions = Sessions(this)

    fun start() {
        if (!sessions.listen(socketName)) LOGGER.w("axsh socket unavailable")
    }

    fun stop() = sessions.stop()

    /**
     * A channel for a helper running as [uid] (axsh's native daemon), which can't connect to
     * [socketName] under SELinux but can hold a socket we made. Each byte it sends carries one
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

    // Only an allowed app is told this socket's name or given a channel, so a uid here that
    // isn't allowed holds one from before it lost that. It is not answered at all: its client
    // falls back to the way an app not allowed takes, the manager's relay, which is the one
    // place that asks (or, in stealth, stays silent).
    override fun ignore(uid: Int, pid: Int): Boolean = !authorize(uid, pid)

    // Not reached for a uid ignore() let through, short of a revoke between the two.
    override fun refuse(uid: Int, pid: Int): String? =
        if (authorize(uid, pid)) null else Sessions.DENIED_MESSAGE

    // Started from here, the shell runs as us: the calls are local, past the binder permission
    // check, which ignore() already did by the client's uid.
    override fun service(): IAxeronService = server()
}
