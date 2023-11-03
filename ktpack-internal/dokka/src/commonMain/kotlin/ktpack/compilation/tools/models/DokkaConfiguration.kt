package ktpack.compilation.tools.models


import kotlinx.serialization.Serializable

@Serializable
data class DokkaConfiguration(
    val moduleName: String,
    val outputDir: String,
    val moduleVersion: String? = null,
    val externalDocumentationLinks: List<ExternalDocumentationLink> = emptyList(),
    val failOnWarning: Boolean = false,
    val includes: List<String> = emptyList(),
    val offlineMode: Boolean = false,
    val perPackageOptions: List<PerPackageOption> = emptyList(),
    val pluginsClasspath: List<String> = emptyList(),
    val pluginsConfiguration: List<PluginsConfiguration> = emptyList(),
    val sourceLinks: List<SourceLink> = emptyList(),
    val sourceSets: List<SourceSet> = emptyList(),
    val suppressInheritedMembers: Boolean = false,
    val suppressObviousFunctions: Boolean = true,
)
