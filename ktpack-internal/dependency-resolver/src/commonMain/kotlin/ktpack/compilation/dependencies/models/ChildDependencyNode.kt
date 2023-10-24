package ktpack.compilation.dependencies.models

import kotlinx.serialization.Serializable
import ktpack.configuration.DependencyConf
import ktpack.configuration.ModuleConf

@Serializable
data class ChildDependencyNode(
    val localModule: ModuleConf?,
    val dependencyConf: DependencyConf,
    val children: List<ChildDependencyNode>,
    val artifacts: List<String> = emptyList(),
) {
    override fun toString(): String = when (dependencyConf) {
        is DependencyConf.LocalPathDependency -> "local: module=${localModule?.name} path=${dependencyConf.path}"
        is DependencyConf.GitDependency -> "git: module=${localModule?.name} url=${dependencyConf.gitUrl}"
        is DependencyConf.MavenDependency -> "maven: ${dependencyConf.toMavenString()}"
        is DependencyConf.NpmDependency -> "npm: name=${dependencyConf.name} dev=${dependencyConf.isDev}"
    }
}
