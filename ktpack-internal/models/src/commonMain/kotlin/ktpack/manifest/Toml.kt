package ktpack.manifest

import kotlinx.serialization.modules.SerializersModule
import ktpack.manifest.serializer.DependencyTomlSerializer
import net.peanuuutz.tomlkt.Toml
import net.peanuuutz.tomlkt.TomlIndentation

val toml = Toml {
    indentation = TomlIndentation.Space2
    explicitNulls = false
    serializersModule = SerializersModule {
        polymorphicDefaultSerializer(DependencyToml::class) { DependencyTomlSerializer() }
        polymorphicDefaultDeserializer(DependencyToml::class) { DependencyTomlSerializer() }
    }
}
