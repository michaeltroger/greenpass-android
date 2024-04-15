import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    alias(libs.plugins.com.github.ben.manes.versions) apply true
    alias(libs.plugins.nl.littlerobots.version.catalog.update) apply true
}

versionCatalogUpdate {
    sortByKey = true
    keep {
        keepUnusedVersions = true
        keepUnusedLibraries = true
        keepUnusedPlugins = true
    }
}

private fun isNonStable(version: String): Boolean {
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
