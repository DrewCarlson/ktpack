package ktpack.configuration

public class KtpackModule(
    public var name: String,
    public var version: String = "0.0.1",
    public var kotlinVersion: String? = null,
    public val authors: MutableList<String> = mutableListOf(),
    public var description: String? = null,
    public var readme: String? = null,
    public var homepage: String? = null,
    public var repository: String? = null,
    public var license: String? = null,
    public var publish: Boolean = false,
    public var autobin: Boolean = true,
    public val targets: MutableList<KotlinTarget> = mutableListOf(),
    public val dependencies: MutableList<DependencyContainer> = mutableListOf(),
) {
    public fun targets(vararg targets: KotlinTarget) {
        this.targets.addAll(targets)
    }

    public fun authors(vararg authors: String) {
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
