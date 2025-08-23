import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.tools.refine)
    id("kotlin-parcelize")
}

android {
    namespace = "com.frb.axmanager"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.frb.axmanager"
        minSdk = 27
        targetSdk = 36
        versionCode = 101
        versionName = "1.0.1-QuickShell"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                arguments.add("-DANDROID_STL=none")
            }
        }
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

    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
            version = "3.31.0"
        }
    }

    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes.add("**")
        }
    }


    applicationVariants.all {
        outputs.all {
            val outputImpl = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            outputImpl.outputFileName = "AxManager ${versionName}-${versionCode}-${buildType.name}.apk"
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        prefab = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildToolsVersion = "36.0.0"
    ndkVersion = "29.0.13113456"
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {

    implementation(libs.compose.activity)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.appiconloader.coil)

    implementation(libs.mmrl.ui)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.gson)

    implementation(project(":engine"))
    implementation(libs.sdp.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
    implementation("com.github.Fox2Code.AndroidANSI:library:1.2.0")
    implementation("com.github.Fox2Code.AndroidANSI:library-ktx:1.2.1")
//    implementation("io.github.aghajari:AnnotatedText:1.0.3")

    implementation(libs.boringssl)
    implementation("org.lsposed.libcxx:libcxx:27.0.12077973")
}