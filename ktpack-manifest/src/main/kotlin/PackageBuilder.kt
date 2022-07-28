import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ktpack.configuration.KtpackModule

fun module(name: String, moduleBuilder: KtpackModule.() -> Unit = {}) {
    val build = KtpackModule(name).apply(moduleBuilder)
    println("ktpack-module:${Json.encodeToString(build)}")
}


