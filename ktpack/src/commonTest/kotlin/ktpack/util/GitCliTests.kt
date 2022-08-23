package ktpack.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import ktfio.deleteRecursively
import ktfio.nestedFile
import ktpack.buildDir
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GitCliTests {

    private lateinit var gitCli: GitCli
    private val testDir = buildDir.nestedFile("test-temp")

    @BeforeTest
    fun setup() {
        gitCli = GitCli()
        testDir.mkdirs()
    }

    @AfterTest
    fun cleanup() {
        testDir.deleteRecursively()
    }

    @Test
    fun testHasGitCli() = runTest {
        assertTrue(gitCli.hasGit())
    }

    @Test
    fun testInitRepository() = runTest {
        assertTrue(gitCli.initRepository(testDir))

        val gitDir = testDir.nestedFile(".git")
        assertTrue(gitDir.exists(), "Git directory does not exist")
    }
}
