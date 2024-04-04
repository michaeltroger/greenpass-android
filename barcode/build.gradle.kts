plugins {
    id(libs.plugins.com.android.library.get().pluginId)
    id(libs.plugins.org.jetbrains.kotlin.android.get().pluginId)
    id(libs.plugins.com.google.devtools.ksp.get().pluginId)
}

android {
    namespace = "com.michaeltroger.gruenerpass.barcode"
    compileSdk = libs.versions.sdk.compile.get().toInt()
    defaultConfig {
        minSdk = libs.versions.sdk.min.get().toInt()
    }
}

kotlin {
    explicitApi()
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    implementation(project(":core"))

    implementation(libs.com.google.dagger.hilt.android)
    implementation(libs.com.github.markusfisch.zxing.cpp)

    ksp(libs.com.google.dagger.hilt.compiler)
}