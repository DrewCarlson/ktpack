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
    fun `1 basic`() = runTest {
        val result = buildSample("1-basic")

        assertEquals(0, result.exitCode)
    }

    @Test
    fun `2 multifile`() = runTest {
        val result = buildSample("2-multifile")

        assertEquals(0, result.exitCode)
    }

    @Test
    fun `3 multiple bins`() = runTest {
        val result = buildSample("3-multiple-bins")

        assertEquals(0, result.exitCode)
    }

    @Test
    fun `4 basic lib`() = runTest {
        val result = buildSample("4-basic-lib")

        assertEquals(0, result.exitCode)
    }

    @Test
    fun `5 multifile lib`() = runTest {
        val result = buildSample("5-multifile-lib")

        assertEquals(0, result.exitCode)
    }

    private suspend fun buildSample(name: String): CommunicateResult {
        getSample(name, "out").deleteRecursively()
        return Dispatchers.Default {
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
    }
}
