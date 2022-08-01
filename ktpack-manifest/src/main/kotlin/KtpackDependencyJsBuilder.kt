package ktpack.configuration

class KtpackDependencyJsBuilder(targets: List<KotlinTarget>) : KtpackDependencyBuilder(targets) {

    fun npm(name: String, version: String) {
        add(DependencyConf.NpmDependency(name, version, false, DependencyScope.IMPLEMENTATION))
    }

    fun npmDev(name: String, version: String) {
        add(DependencyConf.NpmDependency(name, version, true, DependencyScope.TEST))
    }

    fun npmTest(name: String, version: String) {
        add(DependencyConf.NpmDependency(name, version, true, DependencyScope.TEST))
    }
}