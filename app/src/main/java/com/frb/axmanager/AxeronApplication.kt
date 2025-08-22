package com.frb.axmanager

import android.content.Context
import android.os.Build
import com.frb.engine.AxeronData
import com.frb.engine.AxeronSettings
import com.frb.engine.Engine
import org.lsposed.hiddenapibypass.HiddenApiBypass

class AxeronApplication : Engine() {
    companion object {

        init {
//            logd("ShizukuApplication", "init")
//
//            Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_REDIRECT_STDERR))
            if (Build.VERSION.SDK_INT >= 28) {
                HiddenApiBypass.setHiddenApiExemptions("")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                System.loadLibrary("adb")
            }
        }
    }

    private fun init(context: Context) {
        AxeronSettings.initialize(context)
    }

    override fun onCreate() {
        super.onCreate()
        init(this)
        AxeronData.init()
    }
}