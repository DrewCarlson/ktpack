package ktpack.configuration

open class DependencyBuilder(private val targets: List<KotlinTarget>) {
    private val dependencies = mutableListOf<DependencyConf>()

    protected fun add(dep: DependencyConf) {
        dependencies.add(dep)
    }

    fun local(path: String) {
        add(DependencyConf.LocalPathDependency(path, DependencyScope.IMPLEMENTATION))
    }

    fun git(url: String, tag: String? = null, branch: String? = null, version: String? = null) = add(
        DependencyConf.GitDependency(
            gitUrl = url,
            tag = tag,
            branch = branch,
            version = version,
            scope = DependencyScope.IMPLEMENTATION
        )
    )

    fun maven(coordinates: String) {
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

    fun maven(groupId: String, artifactId: String, version: String) {
        add(
            DependencyConf.MavenDependency(
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                scope = DependencyScope.IMPLEMENTATION
            )
        )
    }

    fun localApi(path: String) {
        add(DependencyConf.LocalPathDependency(path, DependencyScope.API))
    }

    fun gitApi(url: String, tag: String? = null, branch: String? = null, version: String? = null) {
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

    fun mavenApi(coordinates: String) {
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

    fun mavenApi(groupId: String, artifactId: String, version: String) {
        add(
            DependencyConf.MavenDependency(
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                scope = DependencyScope.API
            )
        )
    }

    fun localCompile(path: String) {
        add(DependencyConf.LocalPathDependency(path, DependencyScope.COMPILE))
    }

    fun gitCompile(url: String, tag: String? = null, branch: String? = null, version: String? = null) {
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

    fun mavenCompile(coordinates: String) {
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

    fun mavenCompile(groupId: String, artifactId: String, version: String) {
        add(
            DependencyConf.MavenDependency(
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                scope = DependencyScope.COMPILE
            )
        )
    }

    fun localTest(path: String) {
        add(DependencyConf.LocalPathDependency(path, DependencyScope.TEST))
    }

    fun gitTest(url: String, tag: String? = null, branch: String? = null, version: String? = null) {
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

    fun mavenTest(coordinates: String) {
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

    fun mavenTest(groupId: String, artifactId: String, version: String) {
        add(
            DependencyConf.MavenDependency(
                groupId = groupId,
                artifactId = artifactId,
                version = version,
                scope = DependencyScope.TEST
            )
        )
    }

    fun build(): DependencyContainer {
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
