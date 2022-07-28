package ktpack.configuration

import Target

class KtpackDependencyJsBuilder(targets: List<Target>) : KtpackDependencyBuilder(targets) {

    fun npm(name: String, version: String) {
        add(KtpackDependency.NpmDependency(name, version, false, DependencyScope.IMPLEMENTATION))
    }

    fun npmDev(name: String, version: String) {
        add(KtpackDependency.NpmDependency(name, version, true, DependencyScope.TEST))
    }

    fun npmTest(name: String, version: String) {
        add(KtpackDependency.NpmDependency(name, version, false, DependencyScope.TEST))
    }

    fun npmTestDev(name: String, version: String) {
        add(KtpackDependency.NpmDependency(name, version, true, DependencyScope.TEST))
    }
}