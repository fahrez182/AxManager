plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "frb.axeron.reignite"
}

dependencies {
    // DdmHandleAppName is hidden; app_process has it at runtime.
    compileOnly(libs.rikka.hidden.stub)
}

// ---- libaxreignite.so: the igniter's own dex ----
//
// The igniter runs in app_process (`CLASSPATH=<lib dir>/libaxreignite.so app_process /
// frb.axeron.reignite.Igniter`), from the plugin service and from the server's death watchdog.
// Its dex is R8-shrunk to what Igniter.main reaches and shipped as lib<abi>/libaxreignite.so,
// so it is in the manager's native lib dir with nothing to extract. ART goes by the dex magic,
// not the name. Same as axsh's libaxshboot.so.

val igniterAbis = listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
val igniterLibDir = layout.buildDirectory.dir("generated/igniterLib")
val igniterDexDir = layout.buildDirectory.dir("intermediates/igniterDex")

android.sourceSets.getByName("main").jniLibs.srcDir(igniterLibDir)

fun classesJars(configuration: String) = configurations.getByName(configuration).incoming
    .artifactView {
        attributes { attribute(Attribute.of("artifactType", String::class.java), "android-classes-jar") }
        lenient(true)
    }.files

val igniterDex = tasks.register<JavaExec>("igniterDex") {
    val ownJar = tasks.named("bundleLibRuntimeToJarRelease")
    val runtimeJars = classesJars("releaseRuntimeClasspath")
    // Compile-only (hidden-api stubs): referenced, provided by the framework at runtime.
    val libraryJars = classesJars("releaseCompileClasspath").minus(runtimeJars)
    val bootClasspath = androidComponents.sdkComponents.bootClasspath
    val rules = file("proguard-rules.pro")
    val outDir = igniterDexDir

    inputs.files(ownJar, runtimeJars, libraryJars, rules)
    outputs.dir(outDir)

    classpath = files(
        android.sdkDirectory.resolve("build-tools/${android.buildToolsVersion}/lib/d8.jar")
    )
    mainClass.set("com.android.tools.r8.R8")
    argumentProviders.add(CommandLineArgumentProvider {
        outDir.get().asFile.mkdirs()
        buildList {
            addAll(listOf("--release", "--min-api", "26", "--output", outDir.get().asFile.path))
            addAll(listOf("--pg-conf", rules.path))
            bootClasspath.get().forEach { addAll(listOf("--lib", it.asFile.path)) }
            libraryJars.forEach { addAll(listOf("--lib", it.path)) }
            ownJar.get().outputs.files.filter { it.name.endsWith(".jar") }.forEach { add(it.path) }
            runtimeJars.forEach { add(it.path) }
        }
    })
}

val igniterLib = tasks.register<Sync>("igniterLib") {
    into(igniterLibDir)
    igniterAbis.forEach { abi ->
        from(igniterDex.map { it.outputs.files.singleFile.resolve("classes.dex") }) {
            into(abi)
            rename { "libaxreignite.so" }
        }
    }
}

tasks.matching { it.name.matches(Regex("merge\\w*JniLibFolders")) }.configureEach {
    dependsOn(igniterLib)
}
