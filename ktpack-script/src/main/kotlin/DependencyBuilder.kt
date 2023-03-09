package ktpack.configuration

public open class DependencyBuilder(private val targets: List<KotlinTarget>) {
    private val dependencies = mutableListOf<DependencyConf>()

    protected fun add(dep: DependencyConf) {
        dependencies.add(dep)
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
                scope = DependencyScope.IMPLEMENTATION
            )
        )
    }

    public fun maven(coordinates: String) {
        val (groupId: String, artifactId: String, version: String) = coordinates.extractMavenComponents()
        add(
            DependencyConf.MavenDependency(
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                scope = DependencyScope.IMPLEMENTATION
            )
        )
    }

    public fun maven(groupId: String, artifactId: String, version: String) {
        add(
            DependencyConf.MavenDependency(
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                scope = DependencyScope.IMPLEMENTATION
            )
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
                scope = DependencyScope.API
            )
        )
    }

    public fun mavenApi(coordinates: String) {
        val (groupId: String, artifactId: String, version: String) = coordinates.extractMavenComponents()
        add(
            DependencyConf.MavenDependency(
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                scope = DependencyScope.API
            )
        )
    }

    public fun mavenApi(groupId: String, artifactId: String, version: String) {
        add(
            DependencyConf.MavenDependency(
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                scope = DependencyScope.API
            )
        )
    }

    public fun localCompile(path: String) {
        add(DependencyConf.LocalPathDependency(path, DependencyScope.COMPILE))
    }

    public fun gitCompile(url: String, tag: String? = null, branch: String? = null, version: String? = null) {
        add(
            DependencyConf.GitDependency(
                gitUrl = url,
                tag = tag,
                branch = branch,
                version = version,
                scope = DependencyScope.COMPILE
            )
        )
    }

    public fun mavenCompile(coordinates: String) {
        val (groupId: String, artifactId: String, version: String) = coordinates.extractMavenComponents()
        add(
            DependencyConf.MavenDependency(
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                scope = DependencyScope.COMPILE
            )
        )
    }

    public fun mavenCompile(groupId: String, artifactId: String, version: String) {
        add(
            DependencyConf.MavenDependency(
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                scope = DependencyScope.COMPILE
            )
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
                scope = DependencyScope.TEST
            )
        )
    }

    public fun mavenTest(coordinates: String) {
        val (groupId: String, artifactId: String, version: String) = coordinates.extractMavenComponents()
        add(
            DependencyConf.MavenDependency(
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                scope = DependencyScope.TEST
            )
        )
    }

    public fun mavenTest(groupId: String, artifactId: String, version: String) {
        add(
            DependencyConf.MavenDependency(
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                scope = DependencyScope.TEST
            )
        )
    }

    public fun build(): DependencyContainer {
        return DependencyContainer(
            targets = targets,
            dependencies = dependencies
        )
    }

    private fun String.extractMavenComponents(): List<String> {
        return try {
            split(':').apply { check(size == 3) }
        } catch (e: Throwable) {
            error("Maven coordinates must use the `org.jetbrains.kotlinx:kotlin-stdlib:1.7.10` format.\n$this")
        }
    }
}
