package ktpack.compilation.dependencies.models

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.VersionFormatException
import kotlinx.serialization.Serializable
import ktpack.configuration.DependencyConf
import ktpack.configuration.ExcludeDependency
import ktpack.configuration.ModuleConf

@Serializable
data class DependencyNode(
    val localModule: ModuleConf?,
    val dependencyConf: DependencyConf,
    val children: List<DependencyNode>,
    val artifacts: List<String>,
    val exclude: Boolean = false,
) {
    override fun toString(): String = when (dependencyConf) {
        is DependencyConf.LocalPathDependency -> "[local] module=${localModule?.name} path=${dependencyConf.path}"
        is DependencyConf.GitDependency -> "[git] module=${localModule?.name} url=${dependencyConf.gitUrl}"
        is DependencyConf.MavenDependency -> "[maven] ${dependencyConf.toMavenString()}"
        is DependencyConf.NpmDependency -> "[npm] name=${dependencyConf.name} dev=${dependencyConf.isDev}"
    }
}

fun List<DependencyNode>.printDependencyTree(): String {
    return buildString {
        this@printDependencyTree.forEach { node ->
            append(generateTree(node, isTail = false))
        }
    }
}

private fun generateTree(node: DependencyNode, prefix: String = "", isTail: Boolean = true): String {
    return buildString {
        append(prefix)
        append(if (isTail) "└── " else "├── ")
        append("${node}\n")

        node.children.forEachIndexed { i, child ->
            val isLast = i == node.children.lastIndex
            val childPrefix = prefix + if (isTail) "    " else "│   "
            append(generateTree(child, childPrefix, isLast))
        }
    }
}

/**
 * Turn an exhaustive tree of dependencies into a flat list of required
 * dependencies based on the version constraints.
 */
// TODO: Optimize this...
fun List<DependencyNode>.shakeAndFlattenDependencies(): List<DependencyNode> {
    val dependencyMap = mutableMapOf<String, DependencyNode>()
    forEach { collectDependencies(it, dependencyMap) }

    return flatMap { node ->
        node.flatten { childNode ->
            dependencyMap.getValue(childNode.dependencyConf.key)
        }
    }.distinctBy { it.dependencyConf.key }
}

internal fun DependencyNode.flatten(override: (node: DependencyNode) -> DependencyNode): List<DependencyNode> {
    return children.flatMap { child -> override(child).flatten(override) } + override(this)
}

private fun List<ExcludeDependency>.check(dependencyConf: DependencyConf): Boolean {
    val dependency = dependencyConf
    if (dependency !is DependencyConf.MavenDependency) {
        println("Excludes are implemented only for Maven dependencies.")
        return false
    }
    val mavenString = dependency.toMavenString()
    val mavenStringNoVersion = "${dependency.groupId}:${dependency.artifactId}"
    return any { exclude ->
        if (exclude.version == null) {
            exclude.path == mavenString
        } else {
            "${exclude.path}:${exclude.version}" == mavenStringNoVersion
        }
    }
}

private fun collectDependencies(
    node: DependencyNode,
    dependencyMap: MutableMap<String, DependencyNode>,
) {
    val key = node.dependencyConf.key
    val existingNode = dependencyMap[key]
    if (existingNode == null) {
        dependencyMap[key] = node
    } else {
        val checkVersion = try {
            node.dependencyConf.version?.let { Version.parse(it, strict = false) }
        } catch (e: VersionFormatException) {
            println("Failed to parse version for ${node.dependencyConf}")
            return
        }
        val existingVersion = try {
            existingNode.dependencyConf.version?.let { Version.parse(it, strict = false) }
        } catch (e: VersionFormatException) {
            println("Failed to parse version for ${existingNode.dependencyConf}")
            return
        }
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
