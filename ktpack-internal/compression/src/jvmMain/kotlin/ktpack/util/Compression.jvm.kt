@file:OptIn(ExperimentalPathApi::class)

package ktpack.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import net.lingala.zip4j.ZipFile
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.walk


actual object Zip : Compressor {
    actual override fun countFiles(archivePath: String): Long{
        return ZipFile(archivePath).fileHeaders.size.toLong()
    }
    actual override fun extract(archivePath: String, outputDir: String): Flow<String> {
        ZipFile(archivePath).extractAll(outputDir)
        return Path.of(outputDir)
            .walk()
            .map { it.absolutePathString() }
            .asFlow()
    }
}

actual object Tar : Compressor {
    actual override fun countFiles(archivePath: String): Long = error("Not implemented")
    actual override fun extract(archivePath: String, outputDir: String): Flow<String> = error("Not implemented")
}
