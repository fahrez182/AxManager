plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.rikka.tools.refine)
    id("kotlin-parcelize")
}

android {
    namespace = "frb.axeron.server"

    defaultConfig {
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
//            version = "3.31.0"
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


//    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)

    implementation(libs.libcxx)

    implementation(project(":aidl"))
    implementation(project(":shared"))
    implementation(project(":api"))
    implementation(project(":provider"))
    implementation(project(":rish"))
    implementation(project(":server-shared"))
    compileOnly(project(":server:stub"))
}