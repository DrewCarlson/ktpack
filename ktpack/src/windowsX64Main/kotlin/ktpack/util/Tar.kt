package ktpack.util

import kotlinx.coroutines.flow.Flow

actual object Tar : Compressor {
    actual override fun countFiles(archivePath: String): Long = throw NotImplementedError()

    actual override fun extract(archivePath: String, outputDir: String): Flow<String> =
        throw NotImplementedError()
}