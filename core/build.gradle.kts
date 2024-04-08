plugins {
    id(libs.plugins.com.android.library.get().pluginId)
    id(libs.plugins.org.jetbrains.kotlin.android.get().pluginId)
    id(libs.plugins.com.google.devtools.ksp.get().pluginId)
}

android {
    namespace = "com.michaeltroger.gruenerpass.core"
    compileSdk = libs.versions.sdk.compile.get().toInt()
    defaultConfig {
        minSdk = libs.versions.sdk.min.get().toInt()
    }
    buildFeatures {
        buildConfig = true
        androidResources = false
    }
}

kotlin {
    explicitApi()
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    api(libs.org.jetbrains.kotlinx.coroutines.android)

    implementation(libs.com.google.dagger.hilt.android)

    ksp(libs.com.google.dagger.hilt.compiler)
}
