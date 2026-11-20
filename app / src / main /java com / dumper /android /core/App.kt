package frb.axeron.manager.core

import android.app.Application
import android.content.Context
import frb.axeron.manager.BuildConfig
import com.topjohnwu.superuser.Shell

class App : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER)
        )
    }

}
