package frb.axeron.manager

import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Process
import frb.axeron.api.core.AxeronSettings
import androidx.core.os.bundleOf
import frb.axeron.api.Axeron
import frb.axeron.ktx.workerHandler
import frb.axeron.provider.AxeronProvider
import frb.axeron.server.util.Logger
import frb.axeron.shared.ShizukuApiConstant.USER_SERVICE_ARG_PGID
import frb.axeron.shared.ShizukuApiConstant.USER_SERVICE_ARG_TOKEN
import moe.shizuku.api.BinderContainer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class AxManagerProvider : AxeronProvider() {

    companion object {
        private const val EXTRA_BINDER = "moe.shizuku.privileged.api.intent.extra.BINDER"
        private const val METHOD_SEND_USER_SERVICE = "sendUserService"

        /**
         * Asked over `adb shell content call` by a phone doing OTG activation on this one, so
         * TCP/IP follows *this* phone's TCP mode setting. Keys are parsed by OtgActivator.
         */
        const val METHOD_GET_TCP_CONFIG = "getTcpConfig"
        const val KEY_TCP_MODE = "tcp_mode"
        const val KEY_TCP_PORT = "tcp_port"

        private val LOGGER = Logger("AxManagerProvider")
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method == METHOD_GET_TCP_CONFIG) return tcpConfig()
        if (extras == null) return null

        return if (method == METHOD_SEND_USER_SERVICE) {
            LOGGER.d("sendUserService")
            try {
                extras.classLoader = BinderContainer::class.java.classLoader

                val token = extras.getString(USER_SERVICE_ARG_TOKEN) ?: return null
                val pgid = extras.getInt(USER_SERVICE_ARG_PGID)
                val binder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    extras.getParcelable(EXTRA_BINDER,  BinderContainer::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    extras.getParcelable(EXTRA_BINDER)
                }?.binder ?: return null
//                val binder = extras.getParcelableCompat(EXTRA_BINDER, BinderContainer::class.java)?.binder ?: return null

                val countDownLatch = CountDownLatch(1)
                var reply: Bundle? = Bundle()

                val listener = object : Axeron.OnBinderReceivedListener {

                    override fun onBinderReceived() {
                        try {
                            Axeron.attachUserService(binder, bundleOf(
                                USER_SERVICE_ARG_TOKEN to token,
                                USER_SERVICE_ARG_PGID to pgid
                            )
                            )
                            reply!!.putParcelable(EXTRA_BINDER,
                                BinderContainer(Axeron.getShizukuService().asBinder())
                            )
                        } catch (e: Throwable) {
                            LOGGER.e(e, "attachUserService $token")
                            reply = null
                        }

                        Axeron.removeBinderReceivedListener(this)

                        countDownLatch.countDown()
                    }
                }

                Axeron.addBinderReceivedListenerSticky(listener, workerHandler)

                return try {
                    countDownLatch.await(5, TimeUnit.SECONDS)
                    reply
                } catch (e: TimeoutException) {
                    LOGGER.e(e, "Binder not received in 5s")
                    null
                }
            } catch (e: Throwable) {
                LOGGER.e(e, "sendUserService")
                null
            }
        } else {
            super.call(method, arg, extras)
        }
    }

    /** Only adb's shell (or root) gets an answer; nothing secret, but no reason to widen it. */
    private fun tcpConfig(): Bundle? {
        val uid = Binder.getCallingUid()
        if (uid != Process.SHELL_UID && uid != Process.ROOT_UID) return null
        return bundleOf(
            KEY_TCP_MODE to AxeronSettings.getTcpMode(),
            KEY_TCP_PORT to AxeronSettings.getTcpPort()
        )
    }
}