plugins {
    id("greenpass.lib-conventions")
}

android.namespace = "com.michaeltroger.gruenerpass.pdfdecryptor"

dependencies {
    implementation(project(":coroutines"))
    implementation(libs.com.tom.roush.pdfbox.android)

    // The bouncy castle libs are generally only transitive dependencies through pdfbox and only indirectly needed in this app
    // However pdfbox doesn't use up-to-date version and therefore we are explicitly forcing the latest version here
    implementation(libs.org.bouncycastle.bcprov.jdk15to18)
    implementation(libs.org.bouncycastle.bcpkix.jdk15to18)
    implementation(libs.org.bouncycastle.bcutil.jdk15to18)
}
