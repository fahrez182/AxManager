import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.rikka.tools.refine)
    id("kotlin-parcelize")
}

val splitAbis = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }

android {
    namespace = "frb.axeron.manager"

    defaultConfig {
        applicationId = "frb.axeron.manager"
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("frb-project")
        }
        release {
            signingConfig = signingConfigs.getByName("frb-project")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Release: an APK per ABI plus a universal one. Only the ABIs the server ships its binaries
    // for (server/src/main/jniLibs). Off for other builds, so Studio's run installs one APK.
    // Every APK keeps the same versionCode: the server's update check goes by it.
    splits {
        abi {
            isEnable = splitAbis
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    applicationVariants.all {
        val ts = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyMMddHHmm"))
        outputs.all {
            val outputImpl = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val abi = outputImpl.getFilter(com.android.build.OutputFile.ABI)
                ?: if (splitAbis) "universal" else null
            val abiSuffix = abi?.let { "-$it" } ?: ""
            outputImpl.outputFileName =
                "AxManager_v${versionName}_${versionCode}-${buildType.name}${abiSuffix}_$ts.apk"

            val outDir = File(rootDir, "out")
            val mappingPath = File(outDir, "mapping").absolutePath

            assembleProvider.get().doLast {
                // copy mapping.txt kalau minify aktif
                if (buildType.isMinifyEnabled) {
                    copy {
                        from(mappingFileProvider.get())
                        into(mappingPath)
                        rename {
                            "manager-v${versionName}.txt"
                        }
                    }
                }
            }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {

    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))
    testImplementation(libs.junit)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.ksp)
    implementation(libs.colorpicker.compose)

    implementation(libs.compose.coil)

    implementation(libs.androidx.webkit)
    implementation(libs.androidx.core.ktx)

    implementation(libs.rikka.compatibility)
    implementation(libs.rikka.parcelablelist)
    implementation(libs.rikka.hidden.compat)
    compileOnly(libs.rikka.hidden.stub)

    implementation(libs.topjohnwu.libsu.core)
    implementation(libs.topjohnwu.libsu.io)

    implementation(libs.gson)
    implementation(libs.markdown)
    implementation(project(":server"))

    implementation(project(":aidl"))
    implementation(project(":api"))
    implementation(project(":adb"))
    implementation(project(":shared"))
    implementation(project(":provider"))
    implementation(project(":server-shared"))
    // libaxsh.so (default shell, and the binary other apps exec) plus its broker/bootstrap.
    implementation(project(":axsh"))
    // libaxreignite.so: the plugin igniter, run by the plugin service and the server's watchdog.
    implementation(project(":reignite"))
    implementation(libs.sdp.android)
    implementation(libs.material)
    implementation(libs.mmrl.ui)
    implementation(libs.hiddenapibypass)
    implementation(libs.ansi.library)
    implementation(libs.ansi.library.ktx)

    implementation(libs.sheet.compose.dialogs.core)
    implementation(libs.sheet.compose.dialogs.list)
    implementation(libs.sheet.compose.dialogs.input)
    implementation(libs.capsule)
}