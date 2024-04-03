import com.github.jk1.license.filter.LicenseBundleNormalizer
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
    compileSdk = libs.versions.sdk.compile.get().toInt()

    defaultConfig {
        applicationId = "com.michaeltroger.gruenerpass"
        minSdk = libs.versions.sdk.min.get().toInt()
        targetSdk = libs.versions.sdk.target.get().toInt()
        versionCode = 55
        versionName = "4.0.0"

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

    @Suppress("UnstableApiUsage")
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

kotlin.jvmToolchain(libs.versions.java.get().toInt())

licenseReport {
    outputDir = "$rootDir/docs/licenses"
    configurations = arrayOf("releaseRuntimeClasspath")
    filters = arrayOf(LicenseBundleNormalizer())
}

dependencies {
    // assure consistent versions
    implementation(platform(libs.org.jetbrains.kotlin.bom))
    implementation(platform(libs.org.jetbrains.kotlinx.coroutines.bom))

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
    implementation(libs.com.github.markusfisch.zxing.cpp)
    implementation(libs.com.google.android.material)
    implementation(libs.com.tom.roush.pdfbox.android)

    // The bouncy castle libs are generally only transitive dependencies through pdfbox and only indirectly needed in this app
    // However pdfbox doesn't use up-to-date version and therefore we are explicitly forcing the latest version here
    implementation(libs.org.bouncycastle.bcprov.jdk15to18)
    implementation(libs.org.bouncycastle.bcpkix.jdk15to18)
    implementation(libs.org.bouncycastle.bcutil.jdk15to18)

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
