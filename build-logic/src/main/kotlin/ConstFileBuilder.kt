import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.provider.Provider


fun buildConstFile(
    packageName: String,
    className: String,
    body: ConstFileBuilder.() -> Unit
): FileSpec {
    return ConstFileBuilder(packageName, className).apply(body).build()
}

class ConstFileBuilder internal constructor(
    packageName: String,
    className: String
) {

    private val fileSpecBuilder = FileSpec.builder(packageName, className)
    private val typeSpecBuilder = TypeSpec.objectBuilder(className)

    fun add(name: String, value: String) {
        typeSpecBuilder.addProperty(constString(name, value))
    }

    fun add(name: String, value: Provider<String>) {
        typeSpecBuilder.addProperty(constString(name, value.get()))
    }

    private fun constString(name: String, value: String): PropertySpec {
        return PropertySpec.builder(name, String::class)
            .addModifiers(KModifier.CONST)
            .initializer("%S", value)
            .build()
    }

    fun build(): FileSpec {
        return fileSpecBuilder
            .addType(typeSpecBuilder.build())
            .build()
    }
}
