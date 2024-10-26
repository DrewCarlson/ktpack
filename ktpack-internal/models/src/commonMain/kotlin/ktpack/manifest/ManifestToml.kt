package ktpack.manifest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ktpack.configuration.DependencyScope
import ktpack.configuration.KotlinTarget


@Serializable
data class ManifestToml(
    val module: ModuleToml,
    val versions: Map<String, String> = emptyMap(),
    val dependencies: DependenciesToml = emptyMap(),
    val docs: DocsToml = DocsToml(),
) {
    fun resolveDependencyShorthand(): ManifestToml {
        return copy(
            dependencies = dependencies.mapValues { (_, dependenciesMap) ->
                dependenciesMap.mapValues { (key, dependency) ->
                    val versionKey = key.substringBefore('-')
                    versions[versionKey]
                        ?.let(dependency::withVersion)
                        ?: dependency
                }
            },
        )
    }

    fun dependenciesFor(
        scopes: List<DependencyScope>,
        targets: List<KotlinTarget> = emptyList(),
        includeCommon: Boolean = false,
    ): Map<String, TargetDependenciesToml> {
        return dependencies
            .filter { (targetName, _) ->
                if (targets.isEmpty()) {
                    targetName == "common"
                } else {
                    val aliasTargets = KotlinTarget.targetsFromAlias(targetName)
                    (includeCommon && targetName == "common") || targets.any { aliasTargets.contains(it) }
                }
            }
            .filterValues { dependencies ->
                dependencies.any { scopes.contains(it.value.scope) }
            }
    }
}

@Serializable
sealed class OutputToml {

    abstract val name: String?
    abstract val targets: List<KotlinTarget>

    fun copy(newName: String): OutputToml {
        return when (this) {
            is BinCommon.Bin -> copy(name = newName)
            is BinCommon.JvmBin -> copy(name = newName)
            is BinCommon.LinuxBin -> copy(name = newName)
            is BinCommon.MacosBin -> copy(name = newName)
            is BinCommon.WindowsBin -> copy(name = newName)
            is Lib -> copy(name = newName)
        }
    }

    @SerialName("lib")
    @Serializable
    data class Lib(
        override val name: String? = null,
        override val targets: List<KotlinTarget> = KotlinTarget.entries,
    ) : OutputToml()

    @Serializable
    sealed class BinCommon : OutputToml() {
        @SerialName("bin")
        @Serializable
        data class Bin(
            val main: String = "MainKt",
            override val name: String? = null,
            override val targets: List<KotlinTarget> = KotlinTarget.entries,
        ) : BinCommon()

        @SerialName("macos-bin")
        @Serializable
        data class MacosBin(
            val main: String,
            override val name: String? = null,
            override val targets: List<KotlinTarget> = KotlinTarget.ALL_MACOS,
        ) : BinCommon()

        @SerialName("windows-bin")
        @Serializable
        data class WindowsBin(
            val main: String,
            override val name: String? = null,
            override val targets: List<KotlinTarget> = KotlinTarget.ALL_WINDOWS,
        ) : BinCommon()

        @SerialName("linux-bin")
        @Serializable
        data class LinuxBin(
            val main: String,
            override val name: String? = null,
            override val targets: List<KotlinTarget> = KotlinTarget.ALL_LINUX,
        ) : BinCommon()

        @SerialName("jvm-bin")
        @Serializable
        data class JvmBin(
            val main: String,
            override val name: String? = null,
            override val targets: List<KotlinTarget> = KotlinTarget.ALL_JVM,
        ) : BinCommon()
    }
}
