plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "frb.axeron.aidl"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        aidl = true
        buildConfig = false
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":data-shared"))
    implementation(libs.dev.rikka.rikkax.parcelablelist)
}