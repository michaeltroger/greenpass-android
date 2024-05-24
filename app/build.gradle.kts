plugins {
    id("greenpass.app-conventions")
}

android {
    namespace = "com.michaeltroger.gruenerpass"

    defaultConfig {
        applicationId = "com.michaeltroger.gruenerpass"
        versionCode = 57
        versionName = "4.1.1"
    }
}

dependencies {
    implementation(project(":barcode"))
    implementation(project(":coroutines"))
    implementation(project(":logger"))
    implementation(project(":pdfimporter"))
    implementation(project(":pdfrenderer"))

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
    implementation(libs.com.github.lisawray.groupie)
    implementation(libs.com.github.lisawray.groupie.viewbinding)
    implementation(libs.com.google.android.material)
    implementation(libs.io.github.panpf.zoomimage.view)

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
