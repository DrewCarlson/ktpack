package ktpack.util

import kotlinx.coroutines.test.runTest
import ktfio.File
import ktfio.deleteRecursively
import ktpack.buildDir
import okio.FileSystem
import okio.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class GitCliTests {

    private lateinit var gitCli: GitCli
    private val testDir = buildDir / "test-temp"

    @BeforeTest
    fun setup() {
        gitCli = GitCli()
        SystemFs.createDirectories(testDir)
    }

    @AfterTest
    fun cleanup() {
        try {
            // TODO: okio delete doesn't work on windows
            File(testDir.toString()).deleteRecursively()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Test
    fun testHasGitCli() = runTest {
        assertTrue(gitCli.hasGit())
    }

    @Test
    fun testInitRepository() = runTest {
        assertTrue(gitCli.initRepository(testDir.toString()))

        val gitDir = testDir / ".git"
        assertTrue(SystemFs.exists(gitDir), "Git directory does not exist")
    }
}
