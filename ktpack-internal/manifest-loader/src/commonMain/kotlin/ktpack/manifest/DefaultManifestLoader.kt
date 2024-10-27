package ktpack.manifest

import kotlinx.io.files.Path
import kotlinx.serialization.decodeFromString
import ktpack.util.exists
import ktpack.util.readString
import ktpack.util.resolve
import ktpack.util.workingDirectory

class DefaultManifestLoader : ManifestLoader {

    override fun load(filePath: String): ManifestToml {
        val path = Path(filePath).let { path ->
            if (path.isAbsolute) {
                path
            } else {
                Path(workingDirectory, path.toString()).resolve()
            }
        }
        check(path.exists()) { "No $MANIFEST_FILENAME file found in '${path.parent}'" }
        return toml.decodeFromString<ManifestToml>(path.readString())
            .resolveDependencyShorthand()
    }
}
