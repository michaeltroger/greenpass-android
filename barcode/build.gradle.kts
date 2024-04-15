plugins {
    id("greenpass.lib-conventions")
}

android.namespace = "com.michaeltroger.gruenerpass.barcode"

dependencies {
    implementation(project(":coroutines"))
    implementation(libs.com.github.markusfisch.zxing.cpp)
}
