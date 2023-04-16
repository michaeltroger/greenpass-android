import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.org.jetbrains.kotlin.android)
}

android {
    namespace = "com.michaeltroger.gruenerpass"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.michaeltroger.gruenerpass"
        minSdk = 21
        targetSdk = 33
        versionCode = 41
        versionName = "2.3.2"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas".toString())
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            signingConfigs {
                create("release") {
                    try {
                        val keystorePropertiesFile = rootProject.file("keystore.properties")
                        val keystoreProperties = Properties()
                        keystoreProperties.load(FileInputStream(keystorePropertiesFile))

                        keyAlias = keystoreProperties.getProperty("KEY_ALIAS")
                        keyPassword = keystoreProperties.getProperty("KEY_PASSWORD")
                        storeFile = rootProject.file(keystoreProperties.getProperty("STORE_FILE"))
                        storePassword = keystoreProperties.getProperty("STORE_PASSWORD")
                    } catch(ignored: IOException) {
                        println(ignored) // We don't have release keys, ignoring
                    }
                }
            }
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.com.github.chrisbanes.photoview)
    implementation(libs.com.github.lisawray.groupie)
    implementation(libs.com.github.lisawray.groupie.viewbinding)
    implementation(libs.com.google.android.material)
    implementation(libs.com.google.zxing.core)
    implementation(libs.com.tom.roush.pdfbox.android)

    ksp(libs.androidx.room.compiler)
}
