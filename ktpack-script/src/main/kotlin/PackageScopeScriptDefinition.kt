package ktpack.configuration

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.api.defaultImports

@KotlinScript(
    compilationConfiguration = PackScriptCompilationConfiguration::class,
)
abstract class PackageScopeScriptDefinition(
    @Suppress("unused")
    val args: Array<String> // DO NOT REMOVE
) : PackageScope()

internal object PackScriptCompilationConfiguration : ScriptCompilationConfiguration({
    baseClass(PackageScopeScriptDefinition::class)
    defaultImports(
        "ktpack.configuration.*",
        "ktpack.configuration.KotlinTarget",
        "ktpack.configuration.KotlinTarget.*",
    )
})
