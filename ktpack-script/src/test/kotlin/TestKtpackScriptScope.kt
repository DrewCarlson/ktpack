package ktpack.configuration

import kotlinx.serialization.json.Json

class TestKtpackScriptScope : KtpackScriptScope() {

    private val moduleConfs = mutableListOf<ModuleConf>()

    override fun emitModuleConfiguration(moduleConf: ModuleConf) {
        moduleConfs.add(moduleConf)
    }

    fun getConf(index: Int = 0): ModuleConf {
        return moduleConfs[index]
    }

    fun getConfCount(): Int = moduleConfs.size
}

fun withTestScope(build: TestKtpackScriptScope.() -> Unit) {
    build(TestKtpackScriptScope())
}

val testJson = Json {
    prettyPrint = true
}
