package ktpack.compilation

import ktpack.configuration.KotlinTarget

sealed class ArtifactResult {
    data class Success(
        val artifactPath: String,
        val compilationDuration: Double,
        val outputText: String,
        val target: KotlinTarget,
        val dependencyArtifacts: List<String>,
    ) : ArtifactResult()

    data class ProcessError(
        val exitCode: Int,
        val message: String?,
    ) : ArtifactResult()

    data object NoArtifactFound : ArtifactResult()
    data object NoSourceFiles : ArtifactResult()
}
