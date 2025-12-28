plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "frb.axeron.aidl"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":data-shared"))
    implementation(libs.rikka.parcelablelist)
}