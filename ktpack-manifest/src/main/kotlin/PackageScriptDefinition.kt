package ktpack.configuration

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ktpack.configuration.KotlinTarget.*
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.api.defaultImports

@KotlinScript(
    compilationConfiguration = PackageScriptCompilationConfiguration::class
)
abstract class PackageScriptDefinition(val args: Array<String>) {

    fun module(name: String, moduleBuilder: KtpackModule.() -> Unit = {}) {
        val moduleConf = KtpackModule(name).apply(moduleBuilder).toConf()
        println("ktpack-module:${Json.encodeToString(moduleConf)}")
    }

    fun KtpackModule.dependencies(vararg target: KotlinTarget, configure: KtpackDependencyBuilder.() -> Unit) {
        dependencies.add(KtpackDependencyBuilder(target.toList()).apply(configure).build())
    }

    fun KtpackModule.dependenciesJs(vararg target: KotlinTarget, configure: KtpackDependencyJsBuilder.() -> Unit) {
        val targetList = if (target.isEmpty()) listOf(JS_NODE, JS_BROWSER) else target.toList()
        dependencies.add(KtpackDependencyJsBuilder(targetList).apply(configure).build())
    }
}

internal object PackageScriptCompilationConfiguration : ScriptCompilationConfiguration({
    baseClass(PackageScriptDefinition::class)
    defaultImports(
        "ktpack.configuration.*",
        "ktpack.configuration.KotlinTarget",
        "ktpack.configuration.KotlinTarget.*",
    )
})
