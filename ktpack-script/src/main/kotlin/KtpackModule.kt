package ktpack.configuration

class KtpackModule(
    var name: String,
    var version: String = "0.0.1",
    var kotlinVersion: String? = null,
    val authors: MutableList<String> = mutableListOf(),
    var description: String? = null,
    var readme: String? = null,
    var homepage: String? = null,
    var repository: String? = null,
    var license: String? = null,
    var publish: Boolean = false,
    var autobin: Boolean = true,
    val targets: MutableList<KotlinTarget> = mutableListOf(),
    val dependencies: MutableList<DependencyContainer> = mutableListOf(),
) {
    fun targets(vararg targets: KotlinTarget) {
        this.targets.addAll(targets)
    }

    fun authors(vararg authors: String) {
        this.authors.addAll(authors)
    }

    internal fun toConf(): ModuleConf = ModuleConf(
        name = name,
        version = version,
        authors = authors.toList(),
        description = description,
        readme = readme,
        homepage = homepage,
        repository = repository,
        license = license,
        publish = publish,
        autobin = autobin,
        targets = targets.toList(),
        kotlinVersion = kotlinVersion,
        dependencies = dependencies.toList(),
    )
}