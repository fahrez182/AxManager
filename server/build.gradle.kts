import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.tools.refine)
    id("kotlin-parcelize")
}

android {
    namespace = "frb.axeron.server"
    compileSdk = 36

    defaultConfig {
        minSdk = 27

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

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "**"
        }
    }

    buildFeatures {
        buildConfig = true
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

    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.toml4j)

    implementation(libs.dev.rikka.rikkax.parcelablelist)
    implementation(libs.hidden.compat)
    compileOnly(libs.hidden.stub)

    implementation(libs.androidx.appcompat)
    implementation(libs.refine.runtime)
    implementation(libs.androidx.core.ktx)

    implementation(libs.org.lsposed.libcxx)

    implementation(project(":aidl"))
    implementation(project(":data-shared"))
    implementation(project(":api-manager"))
    implementation(project(":shizuku-server"))
}