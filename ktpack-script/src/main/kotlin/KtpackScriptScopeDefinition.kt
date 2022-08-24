package ktpack.configuration

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.api.defaultImports

@KotlinScript(
    compilationConfiguration = KtpackScriptCompilationConfiguration::class,
)
abstract class KtpackScriptScopeDefinition(
    @Suppress("unused")
    val args: Array<String> // DO NOT REMOVE
) : KtpackScriptScope()

internal object KtpackScriptCompilationConfiguration : ScriptCompilationConfiguration({
    baseClass(KtpackScriptScopeDefinition::class)
    defaultImports(
        "ktpack.configuration.*",
        "ktpack.configuration.KotlinTarget",
        "ktpack.configuration.KotlinTarget.*",
    )
})
