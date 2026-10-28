package frb.axeron.manager.receiver

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.util.Log
import frb.axeron.api.Axeron
import frb.axeron.manager.ui.util.Stealth
import frb.axeron.shell.KeepAlive
import frb.axeron.server.ServerConstants
import frb.axeron.shared.AxeronApiConstant
import java.util.concurrent.atomic.AtomicBoolean

object ShellBinderRequestHandler {

    private const val TAG = "ShellBinderRequest"

    /**
     * How long to wait for the server binder when the request woke us up: the server pushes it
     * to a fresh manager process a moment after it starts. Under the requester's own 5 s.
     */
    private const val BINDER_WAIT_MS = 4_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Replies to the requester with the server binder, now or as soon as it arrives.
     * [onDone] runs once either way, on the main thread when the wait was needed.
     * Returns false when the request is not one for us (and [onDone] has already run).
     */
    fun handleRequest(context: Context, intent: Intent, onDone: () -> Unit = {}): Boolean {
        if (intent.action != ServerConstants.REQUEST_BINDER_AXRUNTIME) {
            onDone()
            return false
        }

        val request = intent.getBundleExtra("data")
        val receiver = request?.getBinder("binder")
        // Older requesters send no uid: their keep-alive is off.
        val uid = request?.getInt(KeepAlive.EXTRA_UID, -1) ?: -1
        if (receiver == null) {
            onDone()
            return false
        }

        Axeron.getBinder()?.takeIf { it.pingBinder() }?.let {
            answer(context, receiver, uid)
            onDone()
            return true
        }

        // A fresh process: the request woke us before the server's binder push arrived.
        Log.i(TAG, "binder not here yet, waiting up to ${BINDER_WAIT_MS}ms")
        val done = AtomicBoolean(false)
        val listener = object : Axeron.OnBinderReceivedListener {
            override fun onBinderReceived() {
                if (done.getAndSet(true)) return
                Axeron.removeBinderReceivedListener(this)
                if (Axeron.getBinder() != null) answer(context, receiver, uid)
                onDone()
            }
        }
        Axeron.addBinderReceivedListenerSticky(listener, mainHandler)
        mainHandler.postDelayed({
            if (done.getAndSet(true)) return@postDelayed
            Axeron.removeBinderReceivedListener(listener)
            Log.w(TAG, "no binder from the server within ${BINDER_WAIT_MS}ms")
            onDone()
        }, BINDER_WAIT_MS)
        return true
    }

    /**
     * The requester gets the capability of the uid it names, never the server's own binder: it
     * works for that uid alone (to ask for the permission, until allowed), so naming someone
     * else's uid gets nothing usable. A requester naming no uid (older) gets no answer.
     */
    private fun answer(context: Context, receiver: IBinder, uid: Int) {
        // Stealth: only an app already allowed is answered; to any other, nothing is here.
        if (Stealth.isOn() && Axeron.getFlagsForUid(uid, 1 shl 1 /* FLAG_ALLOWED */) == 0) {
            Log.i(TAG, "stealth: uid $uid isn't allowed, not answering")
            return
        }
        val capability = (if (uid >= 0) Axeron.getClientCapability(uid) else null) ?: run {
            Log.w(TAG, "no capability for uid $uid, not answering")
            return
        }
        reply(context, receiver, capability, keepAlive(uid))
    }

    /** The requester's setting, kept by the server: ask only once its binder is here. */
    private fun keepAlive(uid: Int): Int =
        if (uid < 0) KeepAlive.OFF
        else KeepAlive.fromFlags(Axeron.getFlagsForUid(uid, KeepAlive.MASK))

    private fun reply(context: Context, receiver: IBinder, axeronBinder: IBinder, keepAlive: Int) {
        val data = Parcel.obtain()
        try {
            data.writeStrongBinder(axeronBinder)
            data.writeLong(AxeronApiConstant.server.VERSION_CODE)
            data.writeString(context.applicationInfo.nativeLibraryDir)
            // Appended last, so older readers that stop after the library dir still work.
            data.writeInt(keepAlive)
            receiver.transact(1, data, null, IBinder.FLAG_ONEWAY)
        } catch (e: Throwable) {
            Log.w(TAG, "reply failed", e)
        } finally {
            data.recycle()
        }
    }
}
