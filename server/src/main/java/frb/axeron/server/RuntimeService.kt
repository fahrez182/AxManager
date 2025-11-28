package frb.axeron.server

import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import frb.axeron.server.utils.Logger
import frb.axeron.server.utils.ParcelFileDescriptorUtil
import java.io.IOException
import java.util.concurrent.TimeUnit

class RuntimeService(
    private val process: Process,
    token: IBinder?
) : IRuntimeService.Stub() {

    private val logger = Logger("RuntimeServiceImpl")

    private var inFd: ParcelFileDescriptor? = null
    private var outFd: ParcelFileDescriptor? = null
    private var errFd: ParcelFileDescriptor? = null

    init {
        if (token != null) {
            try {
                val deathRecipient = IBinder.DeathRecipient {
                    try {
                        if (alive()) {
                            destroy()
                            logger.i("Destroyed process because the owner is dead")
                        }
                    } catch (e: Throwable) {
                        logger.w(e, "Failed to destroy process after death")
                    }
                }
                token.linkToDeath(deathRecipient, 0)
            } catch (e: Throwable) {
                logger.w(e, "linkToDeath failed")
            }
        }
    }

    override fun getOutputStream(): ParcelFileDescriptor {
        if (outFd == null) {
            outFd = try {
                ParcelFileDescriptorUtil.pipeTo(process.outputStream)
            } catch (e: IOException) {
                throw IllegalStateException("Failed to get output stream", e)
            }
        }
        return outFd!!
    }

    override fun getInputStream(): ParcelFileDescriptor {
        if (inFd == null) {
            inFd = try {
                ParcelFileDescriptorUtil.pipeFrom(process.inputStream)
            } catch (e: IOException) {
                throw IllegalStateException("Failed to get input stream", e)
            }
        }
        return inFd!!
    }

    override fun getErrorStream(): ParcelFileDescriptor {
        if (errFd == null) {
            errFd = try {
                ParcelFileDescriptorUtil.pipeFrom(process.errorStream)
            } catch (e: IOException) {
                throw IllegalStateException("Failed to get error stream", e)
            }
        }
        return errFd!!
    }

    override fun waitFor(): Int {
        return try {
            process.waitFor()
        } catch (e: InterruptedException) {
            throw IllegalStateException("Interrupted while waiting for process", e)
        }
    }

    override fun exitValue(): Int = process.exitValue()

    override fun destroy() {
        tryClose(inFd)
        tryClose(outFd)
        tryClose(errFd)
        process.destroy()
    }

    @Throws(RemoteException::class)
    override fun alive(): Boolean {
        return try {
            process.exitValue()
            false
        } catch (_: IllegalThreadStateException) {
            true
        }
    }

    @Throws(RemoteException::class)
    override fun waitForTimeout(timeout: Long, unitName: String): Boolean {
        val unit = TimeUnit.valueOf(unitName)
        val startTime = System.nanoTime()
        var remaining = unit.toNanos(timeout)

        while (remaining > 0) {
            try {
                process.exitValue()
                return true
            } catch (_: IllegalThreadStateException) {
                try {
                    Thread.sleep(
                        minOf(TimeUnit.NANOSECONDS.toMillis(remaining) + 1, 100)
                    )
                } catch (_: InterruptedException) {
                    throw IllegalStateException("Interrupted while waiting for timeout")
                }
            }
            remaining = unit.toNanos(timeout) - (System.nanoTime() - startTime)
        }
        return false
    }

    private fun tryClose(fd: ParcelFileDescriptor?) {
        try {
            fd?.close()
        } catch (_: IOException) {
        }
    }
}
