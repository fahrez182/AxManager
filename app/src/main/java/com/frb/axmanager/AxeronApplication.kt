package com.frb.axmanager

import android.content.Context
import android.os.Build
import coil.Coil
import coil.ImageLoader
import com.frb.engine.core.AxeronSettings
import com.frb.engine.core.Engine
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.File
import java.util.Locale

lateinit var axApp: AxeronApplication
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

    lateinit var okhttpClient: OkHttpClient

    private fun init(context: Context) {
        AxeronSettings.initialize(context)
    }

    override fun onCreate() {
        super.onCreate()
        axApp = this
        init(axApp)

        val context = this
        val iconSize = resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
        Coil.setImageLoader(
            ImageLoader.Builder(context)
                .components {
                    add(AppIconKeyer())
                    add(AppIconFetcher.Factory(iconSize, false, context))
                }
                .build()
        )

        okhttpClient =
            OkHttpClient.Builder().cache(Cache(File(cacheDir, "okhttp"), 10 * 1024 * 1024))
                .addInterceptor { block ->
                    block.proceed(
                        block.request().newBuilder()
                            .header("User-Agent", "AxManager/${BuildConfig.VERSION_CODE}")
                            .header("Accept-Language", Locale.getDefault().toLanguageTag()).build()
                    )
                }.build()
    }
}