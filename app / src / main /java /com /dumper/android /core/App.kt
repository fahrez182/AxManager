package com.dumper.android.core

import android.app.Application
import android.content.Context
import com.dumper.android.BuildConfig
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
