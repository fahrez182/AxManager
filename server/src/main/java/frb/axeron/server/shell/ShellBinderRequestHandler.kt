package frb.axeron.server.shell

import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Parcel
import frb.axeron.api.Axeron
import frb.axeron.server.ServerConstants
import frb.axeron.shared.AxeronApiConstant

object ShellBinderRequestHandler {

    fun handleRequest(context: Context, intent: Intent): Boolean {
        if (intent.action != ServerConstants.REQUEST_BINDER_AXRUNTIME) {
            return false
        }

        val binder = intent.getBundleExtra("data")?.getBinder("binder") ?: return false
        val axeronBinder = Axeron.getBinder()
//        if (axeronBinder == null) {
//            LOGGER.w("Binder not received or AxManager service not running")
//        }

        val data = Parcel.obtain()
        return try {
            data.writeStrongBinder(axeronBinder)
            data.writeLong(AxeronApiConstant.server.VERSION_CODE)
            data.writeString(context.applicationInfo.nativeLibraryDir)
            binder.transact(1, data, null, IBinder.FLAG_ONEWAY)
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        } finally {
            data.recycle()
        }
    }
}
