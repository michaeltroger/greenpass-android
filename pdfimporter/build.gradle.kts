plugins {
    id("greenpass.lib-conventions")
}

android.namespace = "com.michaeltroger.gruenerpass.pdfimporter"

dependencies {
    implementation(project(":coroutines"))
    implementation(project(":pdfdecryptor"))
    implementation(project(":pdfrenderer"))
    implementation(project(":logger"))
}