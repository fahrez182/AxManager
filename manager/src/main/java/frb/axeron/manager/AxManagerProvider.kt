package frb.axeron.manager

import android.os.Bundle
import androidx.core.os.bundleOf
import frb.axeron.api.core.AxeronProvider
import moe.shizuku.api.BinderContainer
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants
import rikka.shizuku.ktx.workerHandler
import rikka.shizuku.server.util.Logger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import frb.axeron.manager.utils.getParcelableCompat

class AxManagerProvider : AxeronProvider() {

    companion object {
        private const val EXTRA_BINDER = "moe.shizuku.privileged.api.intent.extra.BINDER"
        private const val METHOD_SEND_USER_SERVICE = "sendUserService"
        private val LOGGER = Logger("AxManagerProvider")
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (extras == null) return null

        return if (method == METHOD_SEND_USER_SERVICE) {
            LOGGER.d("sendUserService")
            try {
                extras.classLoader = BinderContainer::class.java.classLoader

                val token = extras.getString(ShizukuApiConstants.USER_SERVICE_ARG_TOKEN) ?: return null
                val pid = extras.getInt(ShizukuApiConstants.USER_SERVICE_ARG_PID)
                val binder = extras.getParcelableCompat(EXTRA_BINDER, BinderContainer::class.java)?.binder ?: return null

                val countDownLatch = CountDownLatch(1)
                var reply: Bundle? = Bundle()

                val listener = object : Shizuku.OnBinderReceivedListener {

                    override fun onBinderReceived() {
                        try {
                            Shizuku.attachUserService(binder, bundleOf(
                                ShizukuApiConstants.USER_SERVICE_ARG_TOKEN to token,
                                ShizukuApiConstants.USER_SERVICE_ARG_PID to pid
                            )
                            )
                            reply!!.putParcelable(EXTRA_BINDER,
                                BinderContainer(Shizuku.getBinder())
                            )
                        } catch (e: Throwable) {
                            LOGGER.e(e, "attachUserService $token")
                            reply = null
                        }

                        Shizuku.removeBinderReceivedListener(this)

                        countDownLatch.countDown()
                    }
                }

                Shizuku.addBinderReceivedListenerSticky(listener, workerHandler)

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
}