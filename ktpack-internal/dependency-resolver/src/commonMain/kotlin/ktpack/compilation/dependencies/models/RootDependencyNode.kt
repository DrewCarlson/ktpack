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
) {
    fun printDependencyTree(): String {
        return buildString {
            appendLine("Dependencies for '${module.name}' on ${targets.joinToString().ifBlank { "all targets" }}:")
            children
                .sortedBy { it.children.size }
                .forEach { child ->
                    printChild(child, 0)
                }
        }
    }

    private fun StringBuilder.printChild(child: ChildDependencyNode, level: Int) {
        if (level > 0) append("|")
        repeat(level) {
            if (level > 1 && level == it + 1) append("|")
            append("   ")
        }
        append("+-- ")
        appendLine(child)

        child.children.forEach { innerChild ->
            printChild(innerChild, level + 1)
        }
    }

    fun filterChildVersions(): List<ChildDependencyNode> {
        val dependencyMap = mutableMapOf<String, ChildDependencyNode>()
        children.forEach {
            collectDependencies(it, dependencyMap)
        }
        return dependencyMap.values.toList()
    }

    private fun collectDependencies(
        node: ChildDependencyNode,
        dependencyMap: MutableMap<String, ChildDependencyNode>,
    ) {
        val key = node.dependencyConf.key
        val existingNode = dependencyMap[key]
        if (existingNode == null) {
            dependencyMap[key] = node
        } else {
            val checkVersion = node.dependencyConf.version?.run(Version::parse)
            val existingVersion = existingNode.dependencyConf.version?.run(Version::parse)
            if (checkVersion == null || existingVersion == null) {
                // TODO: handle other non-maven cases
                println("Ignored node $checkVersion > $existingVersion: $node")
                return
            } else if (checkVersion > existingVersion) {
                dependencyMap.remove(key)
                dependencyMap[key] = node
            } else {
                return
            }
        }
        node.children.forEach {
            collectDependencies(it, dependencyMap)
        }
    }
}
