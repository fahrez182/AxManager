package frb.axeron.adb

import android.os.Build
import androidx.annotation.RequiresApi
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.R)
class AdbClientSync(
    private val host: String, private val port: Int, private val key: AdbKey
) {

    fun awaitConnect(command: String, th: Throwable) {
        val latch = CountDownLatch(1)
        var isSuccess = false

        Executors.newSingleThreadExecutor().execute {
            AdbClient(host, port, key).runCatching {
                connect()
                shellCommand(command, null)
                close()
                isSuccess = true
                latch.countDown()
            }.onFailure {
                latch.countDown()
            }
        }

        latch.await()
        if (!isSuccess) {
            throw th
        }
    }
}