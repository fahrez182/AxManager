import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.tools.refine)
    id("kotlin-parcelize")
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "frb.axeron.manager"
    compileSdk = 36

    defaultConfig {
        applicationId = "frb.axeron.manager"
        minSdk = 27
        targetSdk = 36
        versionCode = 13_113
        versionName = "1.3.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    applicationVariants.all {
        outputs.all {
            val outputImpl = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            outputImpl.outputFileName = "AxManager_v${versionName}_${versionCode}-${buildType.name}.apk"
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
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

    implementation(libs.compose.activity)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.destinations.core)
    implementation(libs.androidx.profileinstaller)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    "baselineProfile"(project(":baselineprofile"))
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    ksp(libs.compose.destinations.ksp)

    implementation(libs.coil.compose)
    implementation(libs.appiconloader.coil)

    implementation(libs.androidx.foundation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.dev.rikka.rikkax.parcelablelist)
    implementation(libs.hidden.compat)
    compileOnly(libs.hidden.stub)

    implementation(libs.gson)
    implementation(libs.markdown)
    implementation(project(":server"))

    implementation(project(":aidl"))
    implementation(project(":adb"))
    implementation(project(":data-shared"))
    implementation(project(":api-manager"))
    implementation(project(":shizuku-server"))
    implementation(libs.sdp.android)
    implementation(libs.material)
    implementation(libs.mmrl.ui)
    implementation("dev.rikka.rikkax.compatibility:compatibility:2.0.0")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
    implementation("com.github.Fox2Code.AndroidANSI:library:1.2.0")
    implementation("com.github.Fox2Code.AndroidANSI:library-ktx:1.2.1")

//    implementation("dev.chrisbanes.haze:haze:1.7.1")
//    implementation("dev.chrisbanes.haze:haze-materials:1.7.1")
}