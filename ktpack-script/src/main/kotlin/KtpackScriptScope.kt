package ktpack.configuration

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

open class KtpackScriptScope {

    private val jsTargets = KotlinTarget.values().filter(KotlinTarget::isJs)
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun module(name: String, build: KtpackModule.() -> Unit = {}) {
        emitModuleConfiguration(KtpackModule(name).apply(build).toConf())
    }

    fun KtpackModule.dependencies(vararg target: KotlinTarget, configure: DependencyBuilder.() -> Unit) {
        dependencies.add(DependencyBuilder(target.toList()).apply(configure).build())
    }

    fun KtpackModule.dependenciesJs(vararg target: KotlinTarget, configure: DependencyJsBuilder.() -> Unit) {
        val targetList = if (target.isEmpty()) jsTargets else target.toList()
        dependencies.add(DependencyJsBuilder(targetList).apply(configure).build())
    }

    open fun emitModuleConfiguration(moduleConf: ModuleConf) {
        println("ktpack-module:${json.encodeToString(moduleConf)}")
    }
}
