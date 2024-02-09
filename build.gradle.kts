import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0" apply true
    alias(libs.plugins.com.android.application) apply false
    alias(libs.plugins.com.github.ben.manes.versions) apply true
    alias(libs.plugins.com.github.jk1.dependency.license.report) apply false
    alias(libs.plugins.com.google.devtools.ksp) apply false
    alias(libs.plugins.io.gitlab.arturbosch.detekt) apply false
    alias(libs.plugins.nl.littlerobots.version.catalog.update) apply true
    alias(libs.plugins.org.jetbrains.kotlin.android) apply false
    alias(libs.plugins.org.jetbrains.kotlin.plugin.parcelize) apply false
}

versionCatalogUpdate {
    pin {
        libraries.set(listOf(
            libs.com.google.zxing.core, // last version to support API < 24
            libs.com.tom.roush.pdfbox.android // versions >= 2.0.26.0 increase app size by 4MB (issue is in its dependency >= org.bouncycastle:bcprov-jdk15to18:1.72)
        ))
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