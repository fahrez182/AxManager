package frb.axeron.server.shell

import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Parcel
import frb.axeron.api.Axeron
import frb.axeron.server.ServerConstants

object ShellBinderRequestHandler {

    fun handleRequest(context: Context, intent: Intent): Boolean {
        if (intent.action != ServerConstants.REQUEST_BINDER_AXERISH) {
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
            data.writeString(context.applicationInfo.sourceDir)
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
