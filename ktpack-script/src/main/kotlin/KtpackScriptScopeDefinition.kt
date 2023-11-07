package ktpack.configuration

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*

@KotlinScript(
    compilationConfiguration = KtpackScriptCompilationConfiguration::class,
)
public abstract class KtpackScriptScopeDefinition(
    @Suppress("unused")
    public val args: Array<String>, // DO NOT REMOVE
) : KtpackScriptScope()

internal class KtpackScriptCompilationConfiguration : ScriptCompilationConfiguration({
    ide {
        acceptedLocations(ScriptAcceptedLocation.Project)
    }
    baseClass(KtpackScriptScopeDefinition::class)
    defaultImports(
        "ktpack.configuration.*",
        "ktpack.configuration.KotlinTarget",
        "ktpack.configuration.KotlinTarget.*",
    )
})
