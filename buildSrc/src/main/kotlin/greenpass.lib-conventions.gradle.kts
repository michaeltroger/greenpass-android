plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    compileSdk = Versions.COMPILE_SDK

    buildFeatures {
        androidResources = false
        buildConfig = false
    }
    defaultConfig {
        minSdk = Versions.MIN_SDK
    }
    kotlinOptions {
        allWarningsAsErrors = true
    }
    lint {
        warningsAsErrors = true
    }
}

kotlin {
    explicitApi()
    jvmToolchain(Versions.JAVA)
}

dependencies {
    implementation(libs.libHiltAndroid)
    ksp(libs.libHiltCompiler)
}