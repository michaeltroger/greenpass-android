import com.github.jk1.license.filter.*
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.com.github.jk1.dependency.license.report)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.io.gitlab.arturbosch.detekt)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.parcelize)
}

android {
    namespace = "com.michaeltroger.gruenerpass"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.michaeltroger.gruenerpass"
        minSdk = 21
        targetSdk = 34
        versionCode = 53
        versionName = "3.4.3"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            signingConfigs {
                create("release") {
                    try {
                        val keystorePropertiesFile = rootProject.file("credentials/keystore.properties")
                        val keystoreProperties = Properties()
                        keystoreProperties.load(FileInputStream(keystorePropertiesFile))

                        keyAlias = keystoreProperties.getProperty("KEY_ALIAS")
                        keyPassword = keystoreProperties.getProperty("KEY_PASSWORD")
                        storeFile = rootProject.file("credentials/${keystoreProperties.getProperty("STORE_FILE")}")
                        storePassword = keystoreProperties.getProperty("STORE_PASSWORD")
                    } catch(ignored: IOException) {
                        println("No signing configuration found, ignoring: $ignored")
                    }
                }
            }
            signingConfig = signingConfigs.getByName("release")
        }
    }

    lint {
        warningsAsErrors = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    kotlinOptions {
        allWarningsAsErrors = true
    }
}

kotlin.jvmToolchain(21)

licenseReport {
    outputDir = "$rootDir/docs/licenses"
    configurations = arrayOf("releaseRuntimeClasspath")
    filters = arrayOf(LicenseBundleNormalizer(), ExcludeTransitiveDependenciesFilter())
}

dependencies {
    debugImplementation(libs.com.squareup.leakcanary.android)

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.room.ktx)
    implementation(libs.com.github.chrisbanes.photoview)
    implementation(libs.com.github.lisawray.groupie)
    implementation(libs.com.github.lisawray.groupie.viewbinding)
    implementation(libs.com.google.android.material)
    implementation(libs.com.google.zxing.core)
    implementation(libs.com.tom.roush.pdfbox.android)

    ksp(libs.androidx.room.compiler)

    testImplementation(libs.androidx.test.ext.junit.ktx)
    testImplementation(libs.app.cash.turbine)
    testImplementation(libs.io.kotest.assertions.core)
    testImplementation(libs.io.mockk)
    testImplementation(libs.org.jetbrains.kotlinx.coroutines.test)
    testImplementation(libs.org.robolectric)

    androidTestImplementation(libs.androidx.test.espresso.contrib)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.uiautomator)

    androidTestUtil(libs.androidx.test.orchestrator)
}
