package ktpack.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import ksubprocess.Process

actual object Tar : Compressor {
    actual override fun countFiles(archivePath: String): Long = runBlocking(Dispatchers.Default) {
        try {
            Process {
                arg("/usr/bin/tar")
                arg("-tzf")
                arg(archivePath)
            }.stdoutLines.count().toLong()
        } catch (e: Throwable) {
            throw TarException(-1, e.message ?: "failed to read archive")
        }
    }

    actual override fun extract(archivePath: String, outputDir: String): Flow<String> {
        return channelFlow {
            try {
                Process {
                    arg("/usr/bin/tar")
                    arg("-xvf")
                    arg(archivePath)
                    args("-C", outputDir.substringBeforeLast('/'))
                }.stdoutLines.collect { trySend(it) }
            } catch (e: Throwable) {
                throw TarException(-1, e.message ?: "failed to read archive")
            }
        }
    }
}
