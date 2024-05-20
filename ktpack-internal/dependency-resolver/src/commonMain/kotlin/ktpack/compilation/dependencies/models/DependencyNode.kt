package ktpack.compilation.dependencies.models

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.VersionFormatException
import kotlinx.serialization.Serializable
import ktpack.manifest.DependencyToml
import ktpack.manifest.ManifestToml

@Serializable
data class DependencyNode(
    val localManifest: ManifestToml?,
    val dependencyConf: DependencyToml,
    val children: List<DependencyNode>,
    val artifacts: List<String>,
    val exclude: Boolean = false,
) {
    override fun toString(): String = when (dependencyConf) {
        is DependencyToml.Local -> "[local] module=${localManifest?.module?.name} path=${dependencyConf.path}"
        is DependencyToml.Git -> "[git] module=${localManifest?.module?.name} url=${dependencyConf.git}"
        is DependencyToml.Maven -> "[maven] ${dependencyConf.toMavenString()}"
        is DependencyToml.Npm -> "[npm] name=${dependencyConf.npm} dev=${dependencyConf.isDev}"
    }
}

fun List<DependencyNode>.dependencyTreeString(): String {
    return buildString {
        this@dependencyTreeString.forEach { node ->
            generateTreeString(node, isTail = false)
        }
    }
}

private fun StringBuilder.generateTreeString(node: DependencyNode, prefix: String = "", isTail: Boolean = true) {
    append(prefix)
    append(if (isTail) "└── " else "├── ")
    append("${node}\n")
    node.children.forEachIndexed { i, child ->
        val childPrefix = prefix + if (isTail) "    " else "│   "
        val isLast = i == node.children.lastIndex
        generateTreeString(child, childPrefix, isLast)
    }
}

/**
 * Resolve all [DependencyNode]s and their children according to
 * the dependency resolution rules, returning a flat list of
 * all required [DependencyNode]s.
 */
// TODO: Optimize this...
fun List<DependencyNode>.resolveAndFlatten(): List<DependencyNode> {
    return resolveVersions()
        .flatten()
        .map { it.copy(children = emptyList()) }
        .distinctBy { it.dependencyConf.key }
}

fun List<DependencyNode>.resolveVersions(): List<DependencyNode> {
    val dependencyMap = mutableMapOf<String, DependencyNode>()
    forEach { collectDependencies(it, dependencyMap) }
    return map { node ->
        node.applyRules { childNode ->
            dependencyMap.getValue(childNode.dependencyConf.key)
        }
    }
}

fun List<DependencyNode>.flatten(): List<DependencyNode> {
    return flatMap { node ->
        listOf(node) + node.children.flatten()
    }
}

private fun DependencyNode.applyRules(override: (node: DependencyNode) -> DependencyNode): DependencyNode {
    return override(this).copy(
        children = children.map { it.applyRules(override) },
    )
}

/*private fun List<ExcludeDependency>.check(dependencyConf: DependencyConf): Boolean {
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
}*/

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
