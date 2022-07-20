package ktpack.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.runBlocking
import ksubprocess.Process

actual object Tar : Compressor {
    actual override fun countFiles(archivePath: String): Long = runBlocking(Dispatchers.Default) {
        try {
            Process {
                workingDirectory = archivePath.substringBeforeLast('/')
                arg("tar")
                arg("-tzf")
                arg(archivePath.substringAfterLast('/'))
            }.stdoutLines.count().toLong()
        } catch (e: Throwable) {
            throw TarException(-1, e.message ?: "failed to read archive")
        }
    }

    actual override fun extract(archivePath: String, outputDir: String): Flow<String> {
        return channelFlow {
            try {

                val process = Process {
                    workingDirectory = archivePath.substringBeforeLast('/')
                    arg("tar")
                    arg("-zxvf")
                    arg(archivePath.substringAfterLast('/'))
                }

                process.stdoutLines.collect { trySend(it) }
            } catch (e: Throwable) {
                throw TarException(-1, e.message ?: "failed to read archive")
            }
        }
    }
}