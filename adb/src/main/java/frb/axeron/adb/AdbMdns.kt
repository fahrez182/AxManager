package frb.axeron.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.Observer
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket

/**
 * Finds this device's own adbd service of [serviceType] over mDNS and reports its port to
 * [observer] (-1 once that service is lost).
 *
 * All state lives on the main thread: [start]/[stop] may be called from any thread and
 * NsdManager calls its listeners on its own thread, so both are posted to [handler] first.
 */
class AdbMdns(
    context: Context, private val serviceType: String,
    private val observer: Observer<Int>
) {

    private enum class Discovery { IDLE, STARTING, ACTIVE, STOPPING }

    private val nsdManager: NsdManager = context.getSystemService(NsdManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val timers = Any()
    private val listener = DiscoveryListener(this)

    private var discovery = Discovery.IDLE
    private var running = false
    private var serviceName: String? = null
    private var accepted = false
    private var restartScheduled = false
    private var attempts = 0
    var indefinite: Boolean = false

    // Before Android 14 NsdService runs one resolve per client at a time and fails any other
    // with FAILURE_ALREADY_ACTIVE, so found services wait here and are resolved one by one.
    private val pending = ArrayDeque<NsdServiceInfo>()
    private var resolving: NsdServiceInfo? = null
    private var resolveFailures = 0

    // Bumped on every stop/restart, so the result of a resolve started before is dropped.
    private var generation = 0

    fun start() = onMain {
        if (running) return@onMain
        running = true
        attempts = 0
        // STARTING/ACTIVE already discover; STOPPING discovers again in onDiscoveryStopped.
        if (discovery == Discovery.IDLE) discover()
    }

    fun stop() = onMain {
        if (!running) return@onMain
        running = false
        serviceName = null
        accepted = false
        reset()
        // STARTING is stopped in onDiscoveryStarted, once the listener is registered.
        if (discovery == Discovery.ACTIVE) stopDiscovery()
    }

    private fun reset() {
        handler.removeCallbacksAndMessages(timers)
        restartScheduled = false
        pending.clear()
        resolving = null
        resolveFailures = 0
        generation++
    }

    private fun discover() {
        discovery = Discovery.STARTING
        runCatching {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
        }.onFailure {
            Log.w(TAG, "discoverServices: $serviceType", it)
            discovery = Discovery.IDLE
            retryDiscovery()
        }
    }

    private fun stopDiscovery() {
        discovery = Discovery.STOPPING
        runCatching {
            nsdManager.stopServiceDiscovery(listener)
        }.onFailure {
            Log.w(TAG, "stopServiceDiscovery: $serviceType", it)
            discovery = Discovery.IDLE
            if (running) discover()
        }
    }

    private fun retryDiscovery() {
        if (!running) return
        later(1000L) {
            if (running && discovery == Discovery.IDLE) discover()
        }
    }

    /**
     * Rediscovers, so NsdManager reports the services it already found once more. It used to
     * restart a fixed 100 ms after stopping, which on a slow device came before
     * onDiscoveryStopped and then never restarted at all; now onDiscoveryStopped restarts it.
     */
    private fun scheduleRestart() {
        if (!running || accepted || restartScheduled) return
        if (!indefinite && attempts >= 5) {
            Log.w(TAG, "$serviceType: no usable service after $attempts restarts")
            return
        }
        attempts++
        restartScheduled = true
        later(if (indefinite) 2000L else attempts * 1000L) {
            restartScheduled = false
            if (!running || accepted) return@later
            reset()
            when (discovery) {
                Discovery.ACTIVE -> stopDiscovery()
                Discovery.IDLE -> discover()
                else -> {}
            }
        }
    }

    private fun onDiscoveryStarted() {
        discovery = Discovery.ACTIVE
        if (!running) stopDiscovery()
    }

    private fun onStartDiscoveryFailed() {
        discovery = Discovery.IDLE
        retryDiscovery()
    }

    private fun onDiscoveryStopped() {
        discovery = Discovery.IDLE
        if (running) discover()
    }

    private fun onStopDiscoveryFailed() {
        // NsdManager drops the listener on a failed stop as well, so it is free to use again.
        discovery = Discovery.IDLE
        if (running) discover()
    }

    private fun onServiceFound(info: NsdServiceInfo) {
        if (!running) return
        val queued = resolving?.serviceName == info.serviceName ||
                pending.any { it.serviceName == info.serviceName }
        if (!queued) pending.addLast(info)
        resolveNext()
    }

    private fun onServiceLost(info: NsdServiceInfo) {
        if (!running) return
        pending.removeAll { it.serviceName == info.serviceName }
        if (info.serviceName == serviceName) {
            serviceName = null
            accepted = false
            observer.onChanged(-1)
        }
    }

    private fun resolveNext() {
        if (!running || resolving != null) return
        val info = pending.removeFirstOrNull() ?: return
        resolving = info
        val gen = generation
        runCatching {
            @Suppress("DEPRECATION")
            nsdManager.resolveService(info, ResolveListener(this, gen))
        }.onFailure {
            Log.w(TAG, "resolveService: ${info.serviceName}", it)
            onResolveFailed(gen, info, -1)
        }
    }

    private fun onResolveFailed(gen: Int, info: NsdServiceInfo, errorCode: Int) {
        if (gen != generation) return
        resolving = null
        Log.v(TAG, "onResolveFailed: ${info.serviceName}, $errorCode")

        // It used to be dropped here, so a service whose resolve collided with another one
        // was never reported. Retried with backoff, then given up for a rediscovery.
        if (++resolveFailures <= 5) {
            pending.addFirst(info)
            later(resolveFailures * 500L) { resolveNext() }
        } else {
            resolveFailures = 0
            scheduleRestart()
            resolveNext()
        }
    }

    private fun onServiceResolved(gen: Int, info: NsdServiceInfo) {
        if (gen != generation) return
        resolving = null
        resolveFailures = 0
        if (!running) return

        if (isOwnHost(info) && isPortListening(info.port)) {
            serviceName = info.serviceName
            accepted = true
            observer.onChanged(info.port)
        } else {
            Log.v(TAG, "onServiceResolved: ${info.serviceName}:${info.port} isn't this device's adbd")
            scheduleRestart()
        }
        resolveNext()
    }

    /**
     * Whether the service is advertised by this device. Compares the addresses themselves, not
     * their text, which differs in the IPv6 scope suffix (`%wlan0` or `%30`). Android 14 may
     * leave [NsdServiceInfo.getHost] null and lists every address in hostAddresses instead.
     */
    @Suppress("DEPRECATION")
    private fun isOwnHost(info: NsdServiceInfo): Boolean {
        val addresses: List<InetAddress> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) info.hostAddresses
            else listOfNotNull(info.host)

        val local = runCatching {
            NetworkInterface.getNetworkInterfaces()?.asSequence()
                ?.flatMap { it.inetAddresses.asSequence() }
                ?.toSet()
        }.getOrNull().orEmpty()

        return addresses.any { it in local }
    }

    /** Whether something (adbd) already listens on [port]: then it can't be bound here. */
    private fun isPortListening(port: Int) = try {
        ServerSocket().use {
            it.bind(InetSocketAddress("127.0.0.1", port), 1)
            false
        }
    } catch (e: IOException) {
        true
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == handler.looper) block() else handler.post(block)
    }

    private fun later(delayMs: Long, block: () -> Unit) {
        handler.postAtTime(block, timers, SystemClock.uptimeMillis() + delayMs)
    }

    internal class DiscoveryListener(private val adbMdns: AdbMdns) : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) {
            Log.v(TAG, "onDiscoveryStarted: $serviceType")

            adbMdns.onMain { adbMdns.onDiscoveryStarted() }
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.v(TAG, "onStartDiscoveryFailed: $serviceType, $errorCode")

            adbMdns.onMain { adbMdns.onStartDiscoveryFailed() }
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.v(TAG, "onDiscoveryStopped: $serviceType")

            adbMdns.onMain { adbMdns.onDiscoveryStopped() }
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.v(TAG, "onStopDiscoveryFailed: $serviceType, $errorCode")

            adbMdns.onMain { adbMdns.onStopDiscoveryFailed() }
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            Log.v(TAG, "onServiceFound: ${serviceInfo.serviceName}")

            adbMdns.onMain { adbMdns.onServiceFound(serviceInfo) }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            Log.v(TAG, "onServiceLost: ${serviceInfo.serviceName}")

            adbMdns.onMain { adbMdns.onServiceLost(serviceInfo) }
        }
    }

    internal class ResolveListener(
        private val adbMdns: AdbMdns,
        private val generation: Int
    ) : NsdManager.ResolveListener {
        override fun onResolveFailed(nsdServiceInfo: NsdServiceInfo, errorCode: Int) {
            adbMdns.onMain { adbMdns.onResolveFailed(generation, nsdServiceInfo, errorCode) }
        }

        override fun onServiceResolved(nsdServiceInfo: NsdServiceInfo) {
            adbMdns.onMain { adbMdns.onServiceResolved(generation, nsdServiceInfo) }
        }
    }

    companion object {
        const val TLS_CONNECT = "_adb-tls-connect._tcp"
        const val TLS_PAIRING = "_adb-tls-pairing._tcp"
        const val TAG = "AdbMdns"
    }
}
