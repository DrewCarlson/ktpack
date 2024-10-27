package ktpack.maven

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.*

const val POM_NAMESPACE = "http://maven.apache.org/POM/4.0.0"

@Serializable
@XmlSerialName("project", namespace = POM_NAMESPACE)
data class MavenProject(
    val groupValue: PomGroup?,
    val artifactValue: PomArtifact?,
    val versionValue: PomVersion?,
    val nameValue: PomName?,
    val descriptionValue: PomDescription?,
    val urlValue: PomUrl?,
    val organization: PomOrganization?,

    // @XmlSerialName("licenses", namespace = NAMESPACE)
    // @XmlChildrenName("license", namespace = NAMESPACE)
    // val licenses: List<PomLicense>?,

    // @XmlSerialName("developers", namespace = NAMESPACE)
    // @XmlChildrenName("developer", namespace = NAMESPACE)
    // val developers: List<PomDeveloper>?,

    @XmlSerialName("dependencies", namespace = POM_NAMESPACE)
    @XmlChildrenName("dependency", namespace = POM_NAMESPACE)
    val dependencies: List<PomDependency> = emptyList(),
) {

    @Serializable
    @XmlSerialName("groupId", namespace = POM_NAMESPACE)
    data class PomGroup(
        @XmlValue(true)
        val value: String,
    )

    @Serializable
    @XmlSerialName("artifactId", namespace = POM_NAMESPACE)
    data class PomArtifact(
        @XmlValue(true)
        val value: String,
    )

    @Serializable
    @XmlSerialName("version", namespace = POM_NAMESPACE)
    data class PomVersion(
        @XmlValue(true)
        val value: String,
    )

    @Serializable
    @XmlSerialName("name", namespace = POM_NAMESPACE)
    data class PomName(
        @XmlValue(true)
        val value: String = "",
    )

    @Serializable
    @XmlSerialName("description", namespace = POM_NAMESPACE)
    data class PomDescription(
        @XmlValue(true)
        val value: String = "",
    )

    @Serializable
    @XmlSerialName("url", namespace = POM_NAMESPACE)
    data class PomUrl(
        @XmlValue(true)
        val value: String = "",
    )

    @Serializable
    @XmlSerialName("dependency", namespace = POM_NAMESPACE)
    data class PomDependency(
        val groupId: PomGroup,
        val artifactId: PomArtifact,
        val version: PomVersion?,
        val scope: PomScope?,
        // val type: String? = null,
    )

    @Serializable
    @XmlSerialName("scope", namespace = POM_NAMESPACE)
    data class PomScope(
        @XmlValue(true)
        val value: String,
    )

    @Serializable
    @XmlSerialName("organization", namespace = POM_NAMESPACE)
    data class PomOrganization(
        val name: String?,
        val url: String?,
    )

    @Serializable
    @XmlSerialName("license", namespace = POM_NAMESPACE)
    data class PomLicense(
        val name: String,
        val url: String,
    )

    @Serializable
    @XmlSerialName("developer", namespace = POM_NAMESPACE)
    data class PomDeveloper(
        val name: String,
        val organization: String,
    )
}
