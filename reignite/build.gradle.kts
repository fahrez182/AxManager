plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.rikka.tools.refine)
}

android {
    namespace = "frb.axeron.reignite"

    defaultConfig {
        applicationId = "frb.axeron.reignite"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            multiDexEnabled = false
        }
    }

    applicationVariants.all {
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
                            "reignite-v${versionName}.txt"
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
}

dependencies {
    implementation(libs.rikka.hidden.compat)
    compileOnly(libs.rikka.hidden.stub)
}