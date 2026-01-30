package frb.axeron.server.shell

import android.app.Application
import android.content.Intent
import android.os.IBinder
import android.os.Parcel
import frb.axeron.server.ServerConstants
import rikka.shizuku.Shizuku
import rikka.shizuku.server.Service.LOGGER

object ShellBinderRequestHandler {

    fun handleRequest(application: Application, intent: Intent): Boolean {
        if (intent.action != ServerConstants.REQUEST_BINDER_AXERISH) {
            return false
        }

        val binder = intent.getBundleExtra("data")?.getBinder("binder") ?: return false
        val shizukuBinder = Shizuku.getBinder()
        if (shizukuBinder == null) {
            LOGGER.w("Binder not received or AxManager service not running")
        }

        val data = Parcel.obtain()
        return try {
            data.writeStrongBinder(shizukuBinder)
            data.writeString(application.applicationInfo.sourceDir)
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
