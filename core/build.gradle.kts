plugins {
    id(libs.plugins.com.android.library.get().pluginId)
    id(libs.plugins.org.jetbrains.kotlin.android.get().pluginId)
    id(libs.plugins.com.google.devtools.ksp.get().pluginId)
}

android {
    namespace = "com.michaeltroger.gruenerpass.core"
    compileSdk = libs.versions.sdk.compile.get().toInt()
}

kotlin {
    explicitApi()
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    implementation(libs.com.google.dagger.hilt.android)

    ksp(libs.com.google.dagger.hilt.compiler)
}