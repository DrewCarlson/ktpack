package ktpack.compilation.tools.models


import kotlinx.serialization.Serializable

@Serializable
data class SourceSet(
    val displayName: String,
    val sourceLinks: List<SourceLink> = emptyList(),
    val sourceSetID: SourceSetID,
    val analysisPlatform: String? = null,
    val apiVersion: String? = null,
    val classpath: List<String> = emptyList(),
    val dependentSourceSets: List<DependentSourceSet> = emptyList(),
    val documentedVisibilities: List<DocumentedVisibilities> = emptyList(),
    val externalDocumentationLinks: List<ExternalDocumentationLink> = emptyList(),
    val includes: List<String> = emptyList(),
    val jdkVersion: Int? = null,
    val languageVersion: String? = null,
    val noJdkLink: Boolean = true,
    val noStdlibLink: Boolean = true,
    val perPackageOptions: List<PerPackageOption> = emptyList(),
    val reportUndocumented: Boolean = false,
    val samples: List<String> = emptyList(),
    val skipDeprecated: Boolean = false,
    val skipEmptyPackages: Boolean = true,
    val sourceRoots: List<String> = emptyList(),
    val suppressedFiles: List<String> = emptyList()
)
