import java.io.FileInputStream
import java.util.Properties

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "AxManager"
include(":manager")
include(":server")
include(":adb")
include(":server:stub")
include(":reignite")

var root = "api"

val propFile = file("local.properties")
val props = Properties()

if (propFile.canRead()) {
    props.load(FileInputStream(propFile))

    if (props["api.useLocal"]?.equals("true") ?: false) {
        root = props["api.dir"] as String
    }
}

include(":aidl")
project(":aidl").projectDir = file("$root${File.separator}aidl")

include(":api")
project(":api").projectDir = file("$root${File.separator}api")

include(":provider")
project(":provider").projectDir = file("$root${File.separator}provider")

include(":shared")
project(":shared").projectDir = file("$root${File.separator}shared")

include(":server-shared")
project(":server-shared").projectDir = file("$root${File.separator}server-shared")

include(":rish")
project(":rish").projectDir = file("$root${File.separator}rish")

include(":shell")
project(":shell").projectDir = file("$root${File.separator}shell")

include(":runtime")
project(":runtime").projectDir = file("$root${File.separator}runtime")

include(":axerish")
project(":axerish").projectDir = file("$root${File.separator}axerish")
