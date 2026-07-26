import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import java.io.FileInputStream
import java.util.Properties

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.rikka.tools.refine) apply false
    alias(libs.plugins.android.test) apply false
}

apply(from = "api/manifest.gradle.kts")
val gitCommitCount = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
}.standardOutput.asText.get().trim().toInt()
val verCode = findProperty("api_version_code") as Int
val verName = "${findProperty("api_version_name")}.r${gitCommitCount}"

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

subprojects {
    plugins.withId("com.android.application") {
        extensions.configure<ApplicationExtension> {
            compileSdk = 36
            buildToolsVersion = "36.0.0"
            ndkVersion = "29.0.14206865"
            signingConfigs {
                create("frb-project") {
                    storeFile =
                        localProperties.getProperty("signing.storeFile")?.let { file(it) }
                    storePassword = localProperties.getProperty("signing.storePassword")
                    keyAlias = localProperties.getProperty("signing.keyAlias")
                    keyPassword = localProperties.getProperty("signing.keyPassword")
                }
            }
            defaultConfig {
                minSdk = 26
                targetSdk = 36
                versionCode = verCode
                versionName = verName
            }
            packaging {
                jniLibs {
                    useLegacyPackaging = true
//                    excludes += "**/libapplovin-native-crash-reporter.so"
                }
                dex {
                    useLegacyPackaging = true
                }
                resources {
                    excludes += "**"
                }
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
    }
    plugins.withId("com.android.library") {
        extensions.configure<LibraryExtension> {
            compileSdk = 36
            buildToolsVersion = "36.0.0"
            ndkVersion = "29.0.14206865"
            defaultConfig {
                minSdk = 26
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
    }
    plugins.withId("org.jetbrains.kotlin.android") {
        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }
    }
}

