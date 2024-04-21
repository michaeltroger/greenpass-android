plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.plugin.com.android.library)
    implementation(libs.plugin.com.android.application)
    implementation(libs.plugin.org.jetbrains.kotlin.android)
    implementation(libs.plugin.com.google.dagger.hilt.android)
    implementation(libs.plugin.com.google.devtools.ksp)
    implementation(libs.plugin.com.github.jk1.dependency.license.report)
    implementation(libs.plugin.io.gitlab.arturbosch.detekt)
    implementation(libs.plugin.androidx.navigation.safe.args)
}
