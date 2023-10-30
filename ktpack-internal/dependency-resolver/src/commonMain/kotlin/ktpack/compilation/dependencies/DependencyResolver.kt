package ktpack.compilation.dependencies

import ktpack.compilation.dependencies.models.DependencyNode
import ktpack.configuration.KotlinTarget
import ktpack.configuration.ModuleConf

abstract class DependencyResolver {
    protected abstract val module: ModuleConf

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
