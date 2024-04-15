plugins {
    id("greenpass.lib-conventions")
}

android.namespace = "com.michaeltroger.gruenerpass.coroutines"

dependencies {
    implementation(platform(libs.org.jetbrains.kotlinx.coroutines.bom))
    implementation(libs.org.jetbrains.kotlinx.coroutines.android)
}
