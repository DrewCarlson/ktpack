package ktpack.util

import io.ktor.utils.io.errors.*
import ksubprocess.ProcessException
import ksubprocess.Redirect
import ksubprocess.exec
import ktfio.File

class GitCli {

    private val gitPath: String = when (Platform.osFamily) {
        OsFamily.WINDOWS -> "git.exe"
        OsFamily.MACOSX -> File("/usr/local/bin/git").run {
            if (exists()) getAbsolutePath() else "/usr/bin/git"
        }
        OsFamily.LINUX -> "/usr/bin/git"
        else -> error("Unsupported host os")
    }

    suspend fun hasGit(): Boolean {
        val result = try {
            exec {
                args(gitPath, "help")
                stdin = Redirect.Null
                stderr = Redirect.Null
                stdout = Redirect.Null
            }
        } catch (e: ProcessException) {
            return false
        } catch (e: IOException) {
            return false
        }

        return result.exitCode == 0
    }

    suspend fun initRepository(directory: File): Boolean {
        val result = try {
            exec {
                args(gitPath, "init")
                workingDirectory = directory.getAbsolutePath()
                stdin = Redirect.Null
                stderr = Redirect.Null
                stdout = Redirect.Null
            }
        } catch (e: ProcessException) {
            return false
        } catch (e: IOException) {
            return false
        }

        return result.exitCode == 0
    }
}
