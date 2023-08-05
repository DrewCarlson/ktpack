package ktpack.compilation.dependencies

import ktpack.compilation.dependencies.models.ChildDependencyNode
import ktpack.configuration.KotlinTarget
import ktpack.configuration.ModuleConf

abstract class DependencyResolver {
    protected abstract val module: ModuleConf

    abstract fun canResolve(node: ChildDependencyNode): Boolean

    abstract suspend fun resolve(
        node: ChildDependencyNode,
        releaseMode: Boolean,
        target: KotlinTarget,
        downloadArtifacts: Boolean,
        recurse: Boolean,
    ): ChildDependencyNode
}
