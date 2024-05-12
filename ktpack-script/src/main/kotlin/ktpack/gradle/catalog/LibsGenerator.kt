package ktpack.gradle.catalog

import com.squareup.kotlinpoet.*
import ktpack.configuration.DependencyConfProvider

internal object LibsGenerator {

    fun generate(catalog: VersionCatalogToml): FileSpec {
        val generatedCatalogClass = ClassName("", "GeneratedCatalog")
        val generatedVersionsClass = ClassName("", "GeneratedVersions")
        val versionProperties = catalog.versions.map { (key, version) ->
            PropertySpec.builder(key, String::class)
                .initializer("%S", version.value)
                .build()
        }
        val versionsType = TypeSpec.classBuilder(generatedVersionsClass)
            .addProperties(versionProperties)
            .build()

        val classLibs = mutableMapOf<String, TypeSpec>()
        val topLevelLibs = mutableListOf<PropertySpec>()

        fun getParent(key: String): TypeSpec {
            return classLibs.getOrPut(key) {
                val lastKeyPart = key.substringAfterLast('-')
                val className = ClassName("", "GeneratedLib${lastKeyPart.replaceFirstChar(Char::uppercaseChar)}")
                val parentKey = key.substringBeforeLast('-')
                val parent = if (parentKey == key) null else getParent(parentKey)

                parent?.toBuilder()
                    ?.addProperty(
                        PropertySpec.builder(lastKeyPart, className)
                            .initializer("%T()", className)
                            .build(),
                    )
                    ?.build()
                    ?.also { classLibs[parentKey] = it }

                TypeSpec.classBuilder(className).build()
            }
        }
        catalog.libraries.forEach { (key, value) ->
            val keyParts = key.split('-')

            if (catalog.libraries.any { (nk, _) -> nk.startsWith("$key-", true) }) {
                val parentClass = getParent(key)
                classLibs[key] = parentClass.toBuilder()
                    .addSuperinterface(DependencyConfProvider::class)
                    .addFunction(
                        FunSpec.builder("getDependencyConf")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(String::class)
                            .addCode("return %S", "${value.module}:${value.version.value}")
                            .build()
                    )
                    .build()
                return@forEach
            }

            if (keyParts.size == 1) {
                topLevelLibs.add(
                    PropertySpec.builder(key, String::class)
                        .initializer("%S", "${value.module}:${value.version.value}")
                        .build(),
                )
            } else {
                val parentKey = keyParts.dropLast(1).joinToString("-")
                val parentClass = getParent(parentKey)
                classLibs[parentKey] = parentClass.toBuilder()
                    .addProperty(
                        PropertySpec.builder(keyParts.last(), String::class)
                            .initializer("%S", "${value.module}:${value.version.value}")
                            .build(),
                    )
                    .build()
            }
        }

        val catalogType = TypeSpec.classBuilder(generatedCatalogClass)
            .addProperty(
                PropertySpec.builder("versions", generatedVersionsClass)
                    .initializer("%T()", generatedVersionsClass)
                    .build(),
            )
            .addProperties(topLevelLibs)
            .apply {
                classLibs.forEach { (key, typeSpec) ->
                    if (key.none { it == '-' }) {
                        val className = ClassName("", typeSpec.name!!)
                        addProperty(
                            PropertySpec.builder(key, className)
                                .initializer("%T()", className)
                                .build()
                        )
                    }
                }
            }
            .build()

        return FileSpec.builder("", "libs-catalog.kt")
            .addTypes(classLibs.values)
            .addType(versionsType)
            .addType(catalogType)
            .addProperty(
                PropertySpec.builder("libs", generatedCatalogClass)
                    .initializer("%T()", generatedCatalogClass)
                    .build(),
            )
            .build()
    }
}
