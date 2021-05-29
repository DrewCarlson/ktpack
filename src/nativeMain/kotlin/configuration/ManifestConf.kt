package ktpack.configuration

import kotlinx.cinterop.*
import tomlc99.*
import kotlin.system.*

data class ManifestConf(
    val module: ModuleConf,
) {
    companion object {
        fun fromToml(content: String): ManifestConf =
            parseToml(content)
    }
}

// TODO: tomlc99 is the best option for toml V1 compliant parsing,
//   that required more focus for build infrastructure resulting
//   in the hastily written mess.  Sorry.
private fun parseToml(content: String): ManifestConf = memScoped {
    val errBuffSize = 200
    val errBuff = allocArray<ByteVar>(errBuffSize)
    val rootTable = checkNotNull(toml_parse(content.cstr, errBuff, errBuffSize)) {
        error("Failed to parse manifest file:\n${errBuff.toKStringFromUtf8()}")
    }
    defer { toml_free(rootTable) }

    val moduleTable = toml_table_in(rootTable, "module")
    if (moduleTable == null) {
        println("Missing [module] definition")
        exitProcess(-1)
    }

    val moduleName = toml_string_in(moduleTable, "name").toKStringFromUtf8()
    val version = toml_string_in(moduleTable, "version").toKStringFromUtf8()
    val description = toml_string_in(moduleTable, "description").toKStringFromUtf8()
    val homepage = toml_string_in(moduleTable, "homepage").toKStringFromUtf8()
    val readme = toml_string_in(moduleTable, "readme").toKStringFromUtf8()
    val repository = toml_string_in(moduleTable, "repository").toKStringFromUtf8()
    val publish = toml_bool_in(moduleTable, "publish").toBoolean()
    val autobin = toml_bool_in(moduleTable, "autobin").toBoolean()
    val authorString = toml_string_in(moduleTable, "authors").toKStringFromUtf8()
    val authorsArray = toml_array_in(moduleTable, "authors")
    val authors = if (authorsArray == null) {
        emptyList()
    } else {
        List(toml_array_nelem(authorsArray)) {
            checkNotNull(toml_string_at(authorsArray, it).toKStringFromUtf8())
        }
    }
    val keywordsArray = toml_array_in(moduleTable, "keywords")
    val keywords = if (keywordsArray == null) {
        null
    } else {
        List(toml_array_nelem(keywordsArray)) {
            checkNotNull(toml_string_at(keywordsArray, it).toKStringFromUtf8())
        }
    }

    return ManifestConf(
        module = ModuleConf(
            name = checkNotNull(moduleName),
            version = checkNotNull(version),
            authors = authorString?.let { listOf(it) } ?: authors,
            description = description,
            keywords = keywords,
            homepage = homepage,
            readme = readme,
            repository = repository,
            publish = publish ?: false,
            autobin = autobin ?: true,
        )
    )
}

private fun CValue<toml_datum_t>.toBoolean(): Boolean? {
    return useContents { if (ok == 1) u.b == 1 else null }
}

// TODO: free string, can posix free be used on windows?
private fun CValue<toml_datum_t>.toKStringFromUtf8(): String? {
    return useContents { u.s?.toKStringFromUtf8() }
}
