package ktpack.util

import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import ktpack.buildDir
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class GitCliTests {

    private lateinit var gitCli: GitCli
    private val testDir = Path(buildDir, "test-temp")

    @BeforeTest
    fun setup() {
        gitCli = GitCli()
        SystemFileSystem.createDirectories(testDir)
    }

    @AfterTest
    fun cleanup() {
        try {
            // TODO: okio delete doesn't work on windows
            Path(testDir.toString()).deleteRecursively()
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

        val gitDir = Path(testDir, ".git")
        assertTrue(SystemFileSystem.exists(gitDir), "Git directory does not exist")
    }
}
