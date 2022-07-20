package ktpack.commands.jdkversions

import kotlinx.cinterop.toKString
import ktfio.File
import ktfio.filePathSeparator
import platform.posix.getenv

data class InstallationDetails(
    val distribution: JdkDistribution,
    val version: String,
    val intellijManifest: String?,
    val path: String,
    val isActive: Boolean,
) {
    val isIntellijInstall: Boolean = !intellijManifest.isNullOrBlank()
}

object JdkInstalls {

    /**
     * Check if the [jdksRoot] contains any JDK install we recognize.
     */
    fun hasAnyJdks(jdksRoot: File): Boolean {
        return jdksRoot.listFiles().any { file ->
            val fileName = file.getName() // Jdk folders use the `zulu-18.0.1` format
            val distribution = try {
                JdkDistribution.valueOf(fileName.substringBefore('-').replaceFirstChar(Char::uppercase))
            } catch (e: IllegalArgumentException) {
                null
            }

            distribution != null && fileName.substringAfter('-').split('.').size == 3
        }
    }

    /**
     * Find the [InstallationDetails] for the installation that best
     * matches the JDK [version] in [jdksRoot], and optionally matches
     * the [distribution] if provided.
     */
    fun findJdk(jdksRoot: File, version: String, distribution: JdkDistribution? = null): InstallationDetails? {
        return discover(jdksRoot).firstOrNull { install ->
            install.version.startsWith(version) && (distribution == null || install.distribution == distribution)
        }
    }

    /**
     * Find all [InstallationDetails] for JDK installs in [jdksRoot].
     */
    fun discover(jdksRoot: File): List<InstallationDetails> {
        val pathEnv = getenv("PATH")?.toKString().orEmpty()
        return jdksRoot.listFiles().mapNotNull { file ->
            if (file.isDirectory() && file.list().isNotEmpty()) {
                createInstallationDetails(file, pathEnv)
            } else null
        }
    }

    private fun createInstallationDetails(file: File, pathEnv: String): InstallationDetails? {
        val fileName = file.getName() // Jdk folders use the `zulu-18.0.1` format
        val distribution = try {
            JdkDistribution.valueOf(fileName.substringBefore('-').replaceFirstChar(Char::uppercase))
        } catch (e: IllegalArgumentException) {
            return null
        }
        val intellijManifestFilename = "${file.getParent()}$filePathSeparator.$fileName.intellij"
        return InstallationDetails(
            distribution = distribution,
            version = fileName.substringAfter('-'),
            intellijManifest = File(intellijManifestFilename).run {
                if (exists()) getAbsolutePath() else null
            },
            path = file.getAbsolutePath(),
            isActive = pathEnv.contains(fileName),
        )
    }
}
