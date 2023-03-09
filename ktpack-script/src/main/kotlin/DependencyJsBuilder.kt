package ktpack.configuration

public class DependencyJsBuilder(targets: List<KotlinTarget>) : DependencyBuilder(targets) {

    public fun npm(name: String, version: String) {
        add(DependencyConf.NpmDependency(name, version, false, DependencyScope.IMPLEMENTATION))
    }

    public fun npmDev(name: String, version: String) {
        add(DependencyConf.NpmDependency(name, version, true, DependencyScope.TEST))
    }

    public fun npmTest(name: String, version: String) {
        add(DependencyConf.NpmDependency(name, version, true, DependencyScope.TEST))
    }
}
