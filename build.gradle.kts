import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    alias(libs.plugins.com.android.application) apply false
    alias(libs.plugins.com.android.library) apply false
    alias(libs.plugins.com.github.ben.manes.versions) apply true
    alias(libs.plugins.com.github.jk1.dependency.license.report) apply false
    alias(libs.plugins.com.google.dagger.hilt.android) apply false
    alias(libs.plugins.com.google.devtools.ksp) apply false
    alias(libs.plugins.io.gitlab.arturbosch.detekt) apply false
    alias(libs.plugins.nl.littlerobots.version.catalog.update) apply true
    alias(libs.plugins.org.jetbrains.kotlin.android) apply false
    alias(libs.plugins.org.jetbrains.kotlin.plugin.parcelize) apply false
}

versionCatalogUpdate {
    keep {
        keepUnusedVersions = true // otherwise it would delete Java and SDK variables
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(this.candidate.version) && !isNonStable(this.currentVersion)
    }
}