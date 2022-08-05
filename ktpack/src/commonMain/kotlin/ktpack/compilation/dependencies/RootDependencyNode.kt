package ktpack.compilation.dependencies

import kotlinx.serialization.Serializable
import ktpack.configuration.KotlinTarget
import ktpack.configuration.ModuleConf

@Serializable
data class RootDependencyNode(
    val targets: List<KotlinTarget>,
    val module: ModuleConf,
    val children: List<ChildDependencyNode>,
    val artifacts: List<String> = emptyList(),
) {
    fun printDependencyTree() {
        println("Dependencies for '${module.name}' on ${targets.joinToString().ifBlank { "all targets" }}:")
        children.forEach { child ->
            printChild(child, 1)
        }
    }

    private fun printChild(child: ChildDependencyNode, level: Int) {
        repeat(level) { print("--") }
        println(" $child")
        child.children.forEach { innerChild ->
            printChild(innerChild, level + 1)
        }
    }
}
