package ktpack.util

import kotlinx.coroutines.flow.Flow

interface Compressor {
    fun countFiles(archivePath: String): Long
    fun extract(archivePath: String, outputDir: String): Flow<String>
}

expect object Zip : Compressor {
    override fun countFiles(archivePath: String): Long
    override fun extract(archivePath: String, outputDir: String): Flow<String>
}

class ZipException(val code: Int, override val message: String) : Exception()

expect object Tar : Compressor {
    override fun countFiles(archivePath: String): Long
    override fun extract(archivePath: String, outputDir: String): Flow<String>
}

class TarException(val code: Int, override val message: String) : Exception()