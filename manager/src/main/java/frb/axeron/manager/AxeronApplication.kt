package frb.axeron.manager

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import coil.Coil
import coil.ImageLoader
import com.topjohnwu.superuser.Shell
import frb.axeron.Axerish
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.core.Engine
import frb.axeron.manager.ui.util.RawAppIconFetcher
import frb.axeron.manager.ui.util.RawAppIconKeyer
import frb.axeron.manager.ui.util.createShellBuilder
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.File
import java.util.Locale

class AxeronApplication : Engine() {
    companion object {
        lateinit var axeronApp: AxeronApplication

        init {
//            logd("ShizukuApplication", "init")
            Axerish.initialize(BuildConfig.APPLICATION_ID)
            Shell.setDefaultBuilder(createShellBuilder())
            Shell.enableLegacyStderrRedirection = true
            Shell.enableVerboseLogging = BuildConfig.DEBUG

            if (Build.VERSION.SDK_INT >= 28) {
                HiddenApiBypass.setHiddenApiExemptions("")
            }
        }
    }

    lateinit var okhttpClient: OkHttpClient

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        axeronApp = this
        AxeronSettings.initialize(axeronApp)
    }

    @SuppressLint("ResourceType")
    override fun onCreate() {
        super.onCreate()

        val context = this
        // App icons load raw from each APK's resources (same engine as AXGC).
        Coil.setImageLoader(
            ImageLoader.Builder(context)
                .components {
                    add(RawAppIconKeyer())
                    add(RawAppIconFetcher.Factory())
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