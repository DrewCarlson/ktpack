package ktpack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.invoke
import kotlinx.coroutines.test.runTest
import ksubprocess.exec
import ktpack.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BuildCommandTests {

    @Test
    fun `1 basic`() = runTest {
        getSample("1-basic", "out").delete()

        val result = Dispatchers.Default {
            exec {
                workingDirectory = getSamplePath("1-basic")
                arg(KTPACK.getAbsolutePath())
                arg("--stacktrace")
                arg("--debug")
                arg("build")
            }
        }

        assertEquals(0, result.exitCode, "Process failed: ${result.output}")

        getSample("1-basic", "out", "hello_world.$EXE_EXTENSION").delete()
    }

    @Test
    fun `2 multifile`() = runTest {
        getSample("2-multifile", "out").delete()

        val result = Dispatchers.Default {
            exec {
                workingDirectory = getSamplePath("2-multifile")
                arg(KTPACK.getAbsolutePath())
                arg("--stacktrace")
                arg("--debug")
                arg("build")
            }
        }

        assertEquals(0, result.exitCode, "Process failed: ${result.output}")

        getSample("2-multifile", "out", "multifile.$EXE_EXTENSION").delete()
    }

    @Test
    fun `3 multiple bins`() = runTest {
        getSample("3-multiple-bins", "out").delete()
        val result = Dispatchers.Default {
            exec {
                workingDirectory = getSamplePath("3-multiple-bins")
                arg(KTPACK.getAbsolutePath())
                arg("--stacktrace")
                arg("--debug")
                arg("build")
            }
        }

        assertEquals(0, result.exitCode, "Process failed: ${result.output}")

        getSample("3-multiple-bins", "out", "multiplebins.$EXE_EXTENSION").delete()
        getSample("3-multiple-bins", "out", "otherbin.$EXE_EXTENSION").delete()
    }
}
