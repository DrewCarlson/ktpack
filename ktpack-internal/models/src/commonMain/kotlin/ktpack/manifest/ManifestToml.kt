package ktpack.manifest

import kotlinx.serialization.Serializable
import ktpack.configuration.DependencyScope
import ktpack.configuration.KotlinTarget


@Serializable
data class ManifestToml(
    val module: ModuleToml,
    val versions: Map<String, String> = emptyMap(),
    val dependencies: DependenciesToml = emptyMap(),
) {
    fun resolveDependencyShorthand(): ManifestToml {
        return ManifestToml(
            module = module,
            versions = versions,
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
        includeCommon: Boolean = false
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
