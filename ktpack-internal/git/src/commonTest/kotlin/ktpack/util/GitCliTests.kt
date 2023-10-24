package ktpack.util

import kotlinx.coroutines.test.runTest
import ktpack.buildDir
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
        testDir.mkdirs()
    }

    @AfterTest
    fun cleanup() {
        try {
            testDir.deleteRecursively()
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
        assertTrue(gitDir.exists(), "Git directory does not exist")
    }
}
