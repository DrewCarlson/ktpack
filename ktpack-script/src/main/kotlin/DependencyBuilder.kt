package ktpack.configuration

public open class DependencyBuilder(private val targets: List<KotlinTarget>) {
    private val dependencies = mutableListOf<DependencyConf>()
    private val exclude = mutableListOf<ExcludeDependency>()
    private val constraints = mutableListOf<VersionConstraint>()

    protected fun add(dep: DependencyConf) {
        dependencies.add(dep)
    }

    public fun exclude(path: String, version: String? = null) {
        exclude.add(ExcludeDependency(path, version))
    }

    public fun local(path: String) {
        add(DependencyConf.LocalPathDependency(path, DependencyScope.IMPLEMENTATION))
    }

    public fun git(url: String, tag: String? = null, branch: String? = null, version: String? = null) {
        add(
            DependencyConf.GitDependency(
                gitUrl = url,
                tag = tag,
                branch = branch,
                version = version,
                scope = DependencyScope.IMPLEMENTATION,
            ),
        )
    }

    public fun maven(coordinates: String) {
        val (groupId: String, artifactId: String, version: String) = coordinates.extractMavenComponents()
        add(
            DependencyConf.MavenDependency(
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                scope = DependencyScope.IMPLEMENTATION,
            ),
        )
    }

    public fun maven(groupId: String, artifactId: String, version: String) {
        add(
            DependencyConf.MavenDependency(
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                scope = DependencyScope.IMPLEMENTATION,
            ),
        )
    }

    public fun localApi(path: String) {
        add(DependencyConf.LocalPathDependency(path, DependencyScope.API))
    }

    public fun gitApi(url: String, tag: String? = null, branch: String? = null, version: String? = null) {
        add(
            DependencyConf.GitDependency(
                gitUrl = url,
                tag = tag,
                branch = branch,
                version = version,
                scope = DependencyScope.API,
            ),
        )
    }

    public fun mavenApi(coordinates: String) {
        val (groupId: String, artifactId: String, version: String) = coordinates.extractMavenComponents()
        add(
            DependencyConf.MavenDependency(
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                scope = DependencyScope.API,
            ),
        )
    }

    public fun mavenApi(groupId: String, artifactId: String, version: String) {
        add(
            DependencyConf.MavenDependency(
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                scope = DependencyScope.API,
            ),
        )
    }

    public fun localTest(path: String) {
        add(DependencyConf.LocalPathDependency(path, DependencyScope.TEST))
    }

    public fun gitTest(url: String, tag: String? = null, branch: String? = null, version: String? = null) {
        add(
            DependencyConf.GitDependency(
                gitUrl = url,
                tag = tag,
                branch = branch,
                version = version,
                scope = DependencyScope.TEST,
            ),
        )
    }

    public fun mavenTest(coordinates: String) {
        val (groupId: String, artifactId: String, version: String) = coordinates.extractMavenComponents()
        add(
            DependencyConf.MavenDependency(
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                scope = DependencyScope.TEST,
            ),
        )
    }

    public fun mavenTest(groupId: String, artifactId: String, version: String) {
        add(
            DependencyConf.MavenDependency(
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                scope = DependencyScope.TEST,
            ),
        )
    }

    public fun build(): DependencyContainer {
        return DependencyContainer(
            targets = targets,
            dependencies = dependencies,
        )
    }

    private fun String.extractMavenComponents(excludeVersion: Boolean = false): List<String> {
        return split(':').apply {
            if (excludeVersion) {
                check(size == 2) {
                    "Maven coordinates must use the `org.jetbrains.kotlinx:kotlin-stdlib` format.\n$this"
                }
            } else {
                check(size == 3) {
                    "Maven coordinates must use the `org.jetbrains.kotlinx:kotlin-stdlib:1.7.10` format.\n$this"
                }
            }
        }
    }
}
