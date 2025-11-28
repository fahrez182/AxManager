import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.tools.refine)
}

android {
    namespace = "rikka.shizuku.server"
    compileSdk = 36

    defaultConfig {
        minSdk = 27

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

    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.0")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
    implementation(libs.hidden.compat)
    compileOnly(libs.hidden.stub)
    implementation(libs.androidx.appcompat)
    implementation(libs.refine.runtime)
    implementation(libs.dev.rikka.rikkax.parcelablelist)
    implementation(libs.androidx.core.ktx)

    implementation(project(":aidl"))
}