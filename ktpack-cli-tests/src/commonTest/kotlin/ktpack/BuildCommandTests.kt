package ktpack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.invoke
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import ksubprocess.ProcessException
import ksubprocess.exec
import ktfio.deleteRecursively
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BuildCommandTests {

    @Test
    fun `1 basic`() = buildSample("1-basic")

    @Test
    fun `2 multifile`() = buildSample("2-multifile")

    @Test
    fun `3 multiple bins`() = buildSample("3-multiple-bins")

    @Test
    fun `4 basic lib`() = buildSample("4-basic-lib")

    @Test
    fun `5 multifile lib`() = buildSample("5-multifile-lib")

    @Test
    fun `6 dependencies`() = buildSample("6-dependencies")

    private fun buildSample(name: String): TestResult = runTest(dispatchTimeoutMs = 180_000L) {
        getSample(name, "out").deleteRecursively()
        val workingDir = getSamplePath(name)
        val result = Dispatchers.Default {
            try {
                exec {
                    workingDirectory = workingDir
                    arg(KTPACK_BIN)
                    arg("--stacktrace")
                    arg("--debug")
                    arg("build")
                }
            } catch (e: ProcessException) {
                throw e.cause ?: e
            }
        }

        assertEquals(
            0,
            result.exitCode,
            buildString {
                appendLine("Process Failed:")
                append("Ktpack Bin: ")
                appendLine(KTPACK_BIN)
                append("Working Dir: ")
                appendLine(workingDir)
                append("Output: ")
                appendLine(result.output)
                append("Error: ")
                appendLine(result.errors)
            }
        )
    }
}
