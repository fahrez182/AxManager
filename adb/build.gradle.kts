import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "frb.axeron.adb"
    compileSdk {
        version = release(36)
    }


    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=none"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("/src/main/cpp/CMakeLists.txt")
            version = "3.31.0"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        prefab = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildToolsVersion = "36.0.0"
    ndkVersion = "29.0.13113456"
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(project(":api-manager"))
    implementation(libs.dev.rikka.rikkax.core.ktx)
    implementation(libs.bcpkix.jdk18on)
    implementation(libs.androidx.annotation.jvm)
    implementation(libs.androidx.lifecycle.livedata.core.ktx)

    implementation(libs.boringssl)
    implementation(libs.org.lsposed.libcxx)

    implementation(libs.hidden.compat)
    compileOnly(libs.hidden.stub)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}