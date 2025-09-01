import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.tools.refine)
}

android {
    namespace = "com.frb.engine"
    compileSdk = 36

    defaultConfig {
        minSdk = 27

//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {

    implementation("com.github.topjohnwu.libsu:nio:6.0.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.0")
    implementation("dev.rikka.rikkax.core:core-ktx:1.4.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.80")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation(libs.hidden.compat)
    implementation(libs.androidx.appcompat)
    compileOnly(libs.hidden.stub)
    implementation(libs.refine.runtime)

    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}