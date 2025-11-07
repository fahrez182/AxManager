package com.frb.engine.utils

import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object ParcelFileDescriptorUtil {

    /**
     * Mengalirkan data dari [InputStream] ke ParcelFileDescriptor (untuk stdout/stderr)
     */
    @Throws(IOException::class)
    fun pipeFrom(inputStream: InputStream): ParcelFileDescriptor {
        val (readSide, writeSide) = ParcelFileDescriptor.createPipe()
        transferAsync(inputStream, ParcelFileDescriptor.AutoCloseOutputStream(writeSide))
        return readSide
    }

    /**
     * Mengalirkan data dari ParcelFileDescriptor ke [OutputStream] (untuk stdin)
     */
    @Throws(IOException::class)
    fun pipeTo(outputStream: OutputStream): ParcelFileDescriptor {
        val (readSide, writeSide) = ParcelFileDescriptor.createPipe()
        transferAsync(ParcelFileDescriptor.AutoCloseInputStream(readSide), outputStream)
        return writeSide
    }

    /**
     * Coroutine-based transfer (non-blocking, aman di background)
     */
    private fun transferAsync(
        input: InputStream,
        output: OutputStream,
        bufferSize: Int = 8192,
        autoFlush: Boolean = false,
        onError: ((Throwable) -> Unit)? = null,
        onComplete: (() -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val buf = ByteArray(bufferSize)
            try {
                input.use { i ->
                    output.use { o ->
                        var len: Int
                        while (i.read(buf).also { len = it } != -1) {
                            o.write(buf, 0, len)
                            if (autoFlush) o.flush()
                        }
                        o.flush()
                    }
                }
                onComplete?.invoke()
            } catch (e: Throwable) {
                Log.e("ParcelFDUtil", "Transfer error: ${e.message}")
                onError?.invoke(e)
            }
        }
    }
}
