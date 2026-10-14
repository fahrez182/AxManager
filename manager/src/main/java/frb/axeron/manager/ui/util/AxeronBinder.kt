package frb.axeron.manager.ui.util

import frb.axeron.api.Axeron
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Suspends until the Axeron binder is received, or [timeoutMillis] passes. On a cold start
 * (e.g. launched from a home-screen shortcut) the binder arrives shortly after the process starts,
 * so callers must not treat "no binder yet" as "server not running" without waiting.
 */
suspend fun awaitAxeronBinder(timeoutMillis: Long = 5000): Boolean {
    if (Axeron.pingBinder()) return true
    return withTimeoutOrNull(timeoutMillis) {
        suspendCancellableCoroutine { continuation ->
            val listener = object : Axeron.OnBinderReceivedListener {
                override fun onBinderReceived() {
                    Axeron.removeBinderReceivedListener(this)
                    if (continuation.isActive) continuation.resume(true)
                }
            }
            continuation.invokeOnCancellation { Axeron.removeBinderReceivedListener(listener) }
            Axeron.addBinderReceivedListenerSticky(listener)
        }
    } ?: false
}
