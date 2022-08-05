package ktpack.maven

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.*

private const val NAMESPACE = "http://maven.apache.org/POM/4.0.0"

@Serializable
@XmlSerialName("project", namespace = NAMESPACE, prefix = "")
data class MavenProject(
    val groupValue: PomGroup?,
    val artifactValue: PomArtifact?,
    val versionValue: PomVersion?,
    val nameValue: PomName?,
    val descriptionValue: PomDescription?,
    val urlValue: PomUrl?,
    val organization: PomOrganization?,

    // @XmlSerialName("licenses", namespace = NAMESPACE, prefix = "")
    // @XmlChildrenName("license", namespace = NAMESPACE, prefix = "")
    // val licenses: List<PomLicense>?,

    // @XmlSerialName("developers", namespace = NAMESPACE, prefix = "")
    // @XmlChildrenName("developer", namespace = NAMESPACE, prefix = "")
    // val developers: List<PomDeveloper>?,

    @XmlSerialName("dependencies", namespace = NAMESPACE, prefix = "")
    @XmlChildrenName("dependency", namespace = NAMESPACE, prefix = "")
    val dependencies: List<PomDependency> = emptyList(),
) {

    @Serializable
    @XmlSerialName("groupId", namespace = NAMESPACE, prefix = "")
    data class PomGroup(
        @XmlValue(true)
        val value: String
    )

    @Serializable
    @XmlSerialName("artifactId", namespace = NAMESPACE, prefix = "")
    data class PomArtifact(
        @XmlValue(true)
        val value: String
    )

    @Serializable
    @XmlSerialName("version", namespace = NAMESPACE, prefix = "")
    data class PomVersion(
        @XmlValue(true)
        val value: String
    )

    @Serializable
    @XmlSerialName("name", namespace = NAMESPACE, prefix = "")
    data class PomName(
        @XmlValue(true)
        val value: String = ""
    )

    @Serializable
    @XmlSerialName("description", namespace = NAMESPACE, prefix = "")
    data class PomDescription(
        @XmlValue(true)
        val value: String = ""
    )

    @Serializable
    @XmlSerialName("url", namespace = NAMESPACE, prefix = "")
    data class PomUrl(
        @XmlValue(true)
        val value: String = ""
    )

    @Serializable
    @XmlSerialName("dependency", namespace = NAMESPACE, prefix = "")
    data class PomDependency(
        val groupId: PomGroup,
        val artifactId: PomArtifact,
        val version: PomVersion?,
        val scope: PomScope?,
        // val type: String? = null,
    )

    @Serializable
    @XmlSerialName("scope", namespace = NAMESPACE, prefix = "")
    data class PomScope(
        @XmlValue(true)
        val value: String,
    )

    @Serializable
    @XmlSerialName("organization", namespace = NAMESPACE, prefix = "")
    data class PomOrganization(
        @XmlValue(true)
        val name: String?
    )

    @Serializable
    @XmlSerialName("license", namespace = NAMESPACE, prefix = "")
    data class PomLicense(
        val name: String,
        val url: String,
    )

    @Serializable
    @XmlSerialName("developer", namespace = NAMESPACE, prefix = "")
    data class PomDeveloper(
        val name: String,
        val organization: String,
    )
}
