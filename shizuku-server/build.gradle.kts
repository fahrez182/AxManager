import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.rikka.tools.refine)
}

android {
    namespace = "rikka.shizuku.server"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = false
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

dependencies {

    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.toml4j)
    implementation(libs.rikka.hidden.compat)
    compileOnly(libs.rikka.hidden.stub)
    implementation(libs.rikka.refine.runtime)
    implementation(libs.rikka.parcelablelist)
    implementation(libs.androidx.core.ktx)

    implementation(project(":aidl"))
    implementation(project(":data-shared"))
}