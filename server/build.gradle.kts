import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.rikka.tools.refine)
    id("kotlin-parcelize")
}

android {
    namespace = "frb.axeron.server"
    compileSdk = 36

    defaultConfig {
        minSdk = 21

        consumerProguardFiles("consumer-rules.pro")
        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=none"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = project.file("src/main/cpp/CMakeLists.txt")
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
    ndkVersion = "29.0.14206865"
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

    implementation(libs.rikka.parcelablelist)
    annotationProcessor(libs.rikka.refine.annotation.processor)
    implementation(libs.rikka.refine.runtime)
    implementation(libs.rikka.refine.annotation)
    implementation(libs.rikka.hidden.compat)
    compileOnly(libs.rikka.hidden.stub)
    compileOnly(project(":server:stub"))

//    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)

    implementation(libs.libcxx)

    implementation(project(":aidl"))
    implementation(project(":data-shared"))
    implementation(project(":api-manager"))
    implementation(project(":shizuku-server"))
}