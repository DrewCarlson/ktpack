package ktpack.compilation.dependencies

import ktpack.compilation.dependencies.models.DependencyNode
import ktpack.configuration.KotlinTarget
import ktpack.manifest.ManifestToml
import ktpack.manifest.ModuleToml

abstract class DependencyResolver {
    protected abstract val manifest: ManifestToml

    val module: ModuleToml
        get() = manifest.module

    abstract fun canResolve(node: DependencyNode): Boolean

    abstract suspend fun resolve(
        node: DependencyNode,
        releaseMode: Boolean,
        target: KotlinTarget,
    ): DependencyNode

    abstract suspend fun resolveArtifacts(
        nodes: List<DependencyNode>,
        releaseMode: Boolean,
        target: KotlinTarget,
    ): List<DependencyNode>
}
