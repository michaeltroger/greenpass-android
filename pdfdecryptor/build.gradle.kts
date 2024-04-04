plugins {
    id(libs.plugins.com.android.library.get().pluginId)
    id(libs.plugins.org.jetbrains.kotlin.android.get().pluginId)
    id(libs.plugins.com.google.devtools.ksp.get().pluginId)
}

android {
    namespace = "com.michaeltroger.gruenerpass.pdfdecryptor"
    compileSdk = libs.versions.sdk.compile.get().toInt()
}

kotlin {
    explicitApi()
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    implementation(project(":core"))

    implementation(libs.com.google.dagger.hilt.android)
    implementation(libs.com.tom.roush.pdfbox.android)

    // The bouncy castle libs are generally only transitive dependencies through pdfbox and only indirectly needed in this app
    // However pdfbox doesn't use up-to-date version and therefore we are explicitly forcing the latest version here
    implementation(libs.org.bouncycastle.bcprov.jdk15to18)
    implementation(libs.org.bouncycastle.bcpkix.jdk15to18)
    implementation(libs.org.bouncycastle.bcutil.jdk15to18)

    ksp(libs.com.google.dagger.hilt.compiler)
}