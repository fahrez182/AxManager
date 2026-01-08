import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "frb.axeron.reignite"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "frb.axeron.reignite"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

android.applicationVariants.all {
    outputs.all {
        val outDir = File(rootDir, "out")
        val mappingPath = File(outDir, "mapping").absolutePath
        val dexPath = "${rootProject.project(":manager").projectDir}/src/main/assets/scripts"


        assembleProvider.get().doLast {
            // copy mapping.txt kalau minify aktif
            if (buildType.isMinifyEnabled) {
                copy {
                    from(mappingFileProvider.get())
                    into(mappingPath)
                    rename {
                        "cmd-v${versionName}.txt"
                    }
                }
            }

            // extract classes*.dex dari APK
            copy {
                val dexFile = zipTree(file(outputFile))
                    .matching { include("classes*.dex") }
                    .singleFile

                from(dexFile)
                into(dexPath)
                rename {
                    "ax_reignite.dex"
                }
            }
        }
    }
}


dependencies {
    implementation(libs.rikka.hidden.compat)
    compileOnly(libs.rikka.hidden.stub)
}