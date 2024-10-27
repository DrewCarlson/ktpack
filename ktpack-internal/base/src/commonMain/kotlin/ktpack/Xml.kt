package ktpack

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.XML


@OptIn(ExperimentalXmlUtilApi::class)
val xml = XML {
    policy = DefaultXmlSerializationPolicy.Builder()
        .apply {
            pedantic = false
            ignoreUnknownChildren()
        }
        .build()
}

// Not all pom files include a namespace, there is no XML configuration
// to work around this.  This provides a safe way to decode POM files for now
// See https://github.com/pdvrieze/xmlutil/issues/170
inline fun <reified T : Any> XML.decodeFromString(
    namespace: String,
    string: String,
): T {
    return decodeFromReader<T>(ManualNamespaceXmlReader(namespace, string))
}

@PublishedApi
@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
internal class ManualNamespaceXmlReader(
    override val namespaceURI: String,
    xmlString: String,
) : XmlReader by xmlStreaming.newReader(xmlString) {
    override fun toString(): String = "ManualNamespaceXmlReader(namespaceURI=\"$namespaceURI\")"

    override val name: QName
        get() = QName(namespaceURI, localName, prefix)
}
