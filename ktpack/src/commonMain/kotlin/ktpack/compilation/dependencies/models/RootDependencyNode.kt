package ktpack.compilation.dependencies.models

import io.github.z4kn4fein.semver.Version
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
            printChild(child, 0)
        }
    }

    private fun printChild(child: ChildDependencyNode, level: Int) {
        val prefix = buildString {
            if (level > 0) append("|")
            repeat(level) {
                if (level > 1 && level == it + 1) append("|")
                append("   ")
            }
            append("+-- ")
            append(child)
        }
        println(prefix)
        child.children.forEach { innerChild ->
            printChild(innerChild, level + 1)
        }
    }

    fun filterChildVersions(): List<ChildDependencyNode> {
        val dependencyMap = mutableMapOf<String, ChildDependencyNode>()
        val allDependencies = mutableListOf<ChildDependencyNode>()
        children.forEach {
            collectDependencies(it, allDependencies, dependencyMap)
        }
        return dependencyMap.values.toList()
    }

    private fun collectDependencies(
        node: ChildDependencyNode,
        allDependencies: MutableList<ChildDependencyNode>,
        dependencyMap: MutableMap<String, ChildDependencyNode>,
    ) {
        val key = node.dependencyConf.key
        val existingNode = dependencyMap[key]
        if (existingNode == null) {
            dependencyMap[key] = node
            allDependencies.add(node)
        } else {
            val version = node.dependencyConf.version?.run(Version::parse)
            val existingVersion = existingNode.dependencyConf.version?.run(Version::parse)
            if (version == null || existingVersion == null) {
                // TODO: handle other non-maven cases
            } else {
                if (version > existingVersion) {
                    dependencyMap[key] = node
                    allDependencies.remove(existingNode)
                    allDependencies.add(node)
                }
            }
        }
        node.children.forEach {
            collectDependencies(it, allDependencies, dependencyMap)
        }
    }
}
