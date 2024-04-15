import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal val VersionCatalog.libHiltAndroid: Provider<MinimalExternalModuleDependency>
    get() = findLibraryOrThrow("com-google-dagger-hilt-android")

internal val VersionCatalog.libHiltCompiler: Provider<MinimalExternalModuleDependency>
    get() = findLibraryOrThrow("com-google-dagger-hilt-compiler")

internal val VersionCatalog.libLeakCanary: Provider<MinimalExternalModuleDependency>
    get() = findLibraryOrThrow("com-squareup-leakcanary-android")

internal val VersionCatalog.libKotlinBom: Provider<MinimalExternalModuleDependency>
    get() = findLibraryOrThrow("org-jetbrains-kotlin-bom")

internal val VersionCatalog.libRoomCompiler: Provider<MinimalExternalModuleDependency>
    get() = findLibraryOrThrow("androidx-room-compiler")

internal val VersionCatalog.libRoomKtx: Provider<MinimalExternalModuleDependency>
    get() = findLibraryOrThrow("androidx-room-ktx")

private fun VersionCatalog.findLibraryOrThrow(name: String) =
    findLibrary(name)
        .orElseThrow { Exception("$name not found in version catalog") }
