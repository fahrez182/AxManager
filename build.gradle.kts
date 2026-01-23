// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.rikka.tools.refine) apply false
    alias(libs.plugins.android.test) apply false
}

apply(from = "api/manifest.gradle.kts")
val gitCommitCount = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
}.standardOutput.asText.get().trim().toInt()
val verCode = findProperty("api_version_code") as Int
val verName = "${findProperty("api_version_name")}.r${gitCommitCount}"
extra["version_code"] = verCode
extra["version_name"] = verName