package ktpack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.invoke
import kotlinx.coroutines.test.runTest
import ksubprocess.CommunicateResult
import ksubprocess.ProcessException
import ksubprocess.exec
import ktfio.deleteRecursively
import ktpack.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BuildCommandTests {

    @Test
    fun `1 basic`() = runTest { buildSample("1-basic") }

    @Test
    fun `2 multifile`() = runTest { buildSample("2-multifile") }

    @Test
    fun `3 multiple bins`() = runTest { buildSample("3-multiple-bins") }

    @Test
    fun `4 basic lib`() = runTest { buildSample("4-basic-lib") }

    @Test
    fun `5 multifile lib`() = runTest { buildSample("5-multifile-lib") }

    private suspend fun buildSample(name: String) {
        getSample(name, "out").deleteRecursively()
        val result = Dispatchers.Default {
            try {
                exec {
                    workingDirectory = getSamplePath(name)
                    arg(KTPACK.getAbsolutePath())
                    arg("--stacktrace")
                    arg("--debug")
                    arg("build")
                }.also {
                    println(it.errors)
                    println(it.output)
                }
            } catch (e: ProcessException) {
                throw e.cause ?: e
            }
        }

        assertEquals(
            0,
            result.exitCode,
            "Process Failed:\nOutput: ${result.output}\nError: ${result.errors}"
        )
    }
}
