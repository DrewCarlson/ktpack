package ktpack.manifest

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import ktpack.configuration.DependencyScope
import ktpack.configuration.KotlinTarget

class ManifestTomlTests : FunSpec(
    {

        KotlinTarget.entries.forEach { testTarget ->
            test("dependenciesFor($testTarget)") {
                val manifest = createTestManifestToml()

                val subject = manifest.dependenciesFor(DependencyScope.entries, listOf(testTarget))

                subject shouldBeEqual dependencyForExpects.getValue(testTarget)
            }
        }

        KotlinTarget.entries.shuffled().chunked(2).forEach { targets ->
            test("dependenciesFor(${targets.joinToString()})") {
                val manifest = createTestManifestToml()
                val subject = manifest.dependenciesFor(DependencyScope.entries, targets)

                subject shouldBeEqual dependencyForExpects.filterKeys(targets::contains)
            }
        }

    },
)

private val dependencyForExpects: Map<KotlinTarget, Map<String, TargetDependenciesToml>> = run {
    val deps = createTestManifestToml().dependencies
    KotlinTarget.entries.associateWith { target ->
        when (target) {
            KotlinTarget.JVM -> deps.filterKeys { it == "jvm" }
            KotlinTarget.JS_NODE -> deps.filterKeys { it == "js" }
            KotlinTarget.JS_BROWSER -> deps.filterKeys { it == "js" }
            KotlinTarget.MACOS_ARM64 -> deps.filterKeys { it == "macos" }
            KotlinTarget.MACOS_X64 -> deps.filterKeys { it == "macos" }
            KotlinTarget.MINGW_X64 -> deps.filterKeys { it == "windows" }
            KotlinTarget.LINUX_ARM64 -> deps.filterKeys { it == "linux" }
            KotlinTarget.LINUX_X64 -> deps.filterKeys { it == "linux" }
        }
    }
}

private fun createTestManifestToml(): ManifestToml {
    return ManifestToml(
        module = ModuleToml("name"),
        versions = mapOf(),
        dependencies = mapOf(
            "common" to mapOf("ktor-core" to DependencyToml.Maven(maven = "")),
            "jvm" to mapOf("ktor-cio" to DependencyToml.Maven(maven = "")),
            "macos" to mapOf("ktor-darwin" to DependencyToml.Maven(maven = "")),
            "windows" to mapOf("ktor-winhttp" to DependencyToml.Maven(maven = "")),
            "linux" to mapOf("ktor-curl" to DependencyToml.Maven(maven = "")),
            "js" to mapOf("ktor-js" to DependencyToml.Maven(maven = "")),
        ),
    )
}
