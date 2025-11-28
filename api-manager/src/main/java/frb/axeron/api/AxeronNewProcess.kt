package frb.axeron.api

import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.os.RemoteException
import android.util.ArraySet
import android.util.Log
import frb.axeron.server.IRuntimeService
import java.io.InputStream
import java.io.OutputStream
import java.util.Collections

class AxeronNewProcess(
    private var runtimeService: IRuntimeService?
) : Process(), Parcelable {

    companion object {
        private const val TAG = "AxeronNewProcess"

        // Cache untuk mempertahankan referensi binder agar tidak dikoleksi GC
        private val cache: MutableSet<AxeronNewProcess> =
            Collections.synchronizedSet(ArraySet())

        @JvmField
        val CREATOR = object : Parcelable.Creator<AxeronNewProcess> {
            override fun createFromParcel(parcel: Parcel): AxeronNewProcess {
                val binder = parcel.readStrongBinder()
                return AxeronNewProcess(IRuntimeService.Stub.asInterface(binder))
            }

            override fun newArray(size: Int): Array<AxeronNewProcess?> =
                arrayOfNulls(size)
        }
    }

    // gunakan lazy agar inisialisasi dilakukan hanya saat dibutuhkan
    private val outStream: OutputStream by lazy {
        try {
            ParcelFileDescriptor.AutoCloseOutputStream(
                runtimeService?.outputStream
                    ?: throw IllegalStateException("Runtime service is null")
            )
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        }
    }

    private val inStream: InputStream by lazy {
        try {
            ParcelFileDescriptor.AutoCloseInputStream(
                runtimeService?.inputStream
                    ?: throw IllegalStateException("Runtime service is null")
            )
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        }
    }

    private val errStream: InputStream by lazy {
        try {
            ParcelFileDescriptor.AutoCloseInputStream(
                runtimeService?.errorStream
                    ?: throw IllegalStateException("Runtime service is null")
            )
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        }
    }

    init {
        runtimeService?.let { service ->
            try {
                service.asBinder().linkToDeath({
                    runtimeService = null
                    Log.v(TAG, "AxeronNewProcess is dead")
                    cache.remove(this@AxeronNewProcess)
                }, 0)
            } catch (e: RemoteException) {
                Log.e(TAG, "linkToDeath failed", e)
            }

            // simpan referensi binder di cache agar tidak GC
            cache.add(this)
        }
    }

    override fun getOutputStream(): OutputStream = outStream
    override fun getInputStream(): InputStream = inStream
    override fun getErrorStream(): InputStream = errStream

    override fun waitFor(): Int = try {
        runtimeService?.waitFor() ?: -1
    } catch (e: RemoteException) {
        throw RuntimeException(e)
    }

    override fun exitValue(): Int = try {
        runtimeService?.exitValue() ?: -1
    } catch (e: RemoteException) {
        throw RuntimeException(e)
    }

    override fun destroy() {
        try {
            runtimeService?.destroy()
        } catch (e: RemoteException) {
            Log.w(TAG, "destroy failed", e)
        } finally {
            runtimeService = null
            cache.remove(this)
        }
    }

    // Parcelable section
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeStrongBinder(runtimeService?.asBinder())
    }
}
