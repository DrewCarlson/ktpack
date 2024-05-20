package ktpack.manifest.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ktpack.manifest.DependencyToml

internal class DependencyTomlSerializer : KSerializer<DependencyToml> {
    override val descriptor: SerialDescriptor = DependencyToml.serializer().descriptor

    private val baseSerializer = DependencyToml.serializer()
    private val mavenSerializer = DependencyToml.Maven.serializer()
    private val npmSerializer = DependencyToml.Npm.serializer()
    private val gitSerializer = DependencyToml.Git.serializer()
    private val localSerializer = DependencyToml.Local.serializer()
    private val serializerList = listOf(
        mavenSerializer,
        npmSerializer,
        gitSerializer,
        localSerializer,
    )

    override fun deserialize(decoder: Decoder): DependencyToml {
        return serializerList.firstNotNullOfOrNull { serializer ->
            serializer.deserialize(decoder)
        } ?: throw SerializationException("Could not deserialize type of '${baseSerializer.descriptor.serialName}'")
    }

    override fun serialize(encoder: Encoder, value: DependencyToml) {
        when (value) {
            is DependencyToml.Git -> gitSerializer.serialize(encoder, value)
            is DependencyToml.Maven -> mavenSerializer.serialize(encoder, value)
            is DependencyToml.Npm -> npmSerializer.serialize(encoder, value)
            is DependencyToml.Local -> localSerializer.serialize(encoder, value)
        }
    }

}
