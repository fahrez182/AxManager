package frb.axeron.adb

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import frb.axeron.adb.AdbMdns.Companion.STATUS_FAILED
import java.util.concurrent.CountDownLatch

@RequiresApi(Build.VERSION_CODES.R)
class AdbMdnsSync(
    private val context: Context,
    private val serviceType: String
) {
    fun awaitStart(th: Throwable) : AdbMdns.AdbData {
        val latch = CountDownLatch(1)
        var adbData: AdbMdns.AdbData? = null

        AdbMdns(context, serviceType) { data ->
            adbData = data
            latch.countDown()
        }.runCatching {
            start()
        }.onFailure {
            latch.countDown()
            throw th
        }

        latch.await()
        return adbData ?: AdbMdns.AdbData(STATUS_FAILED)
    }
}