package ktpack.manifest

const val MANIFEST_FILENAME = "pack.toml"

interface ManifestLoader {

    fun load(
        filePath: String = MANIFEST_FILENAME
    ): ManifestToml
}
