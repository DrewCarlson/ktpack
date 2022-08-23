package ktpack.util

import io.ktor.utils.io.errors.*
import ksubprocess.ProcessException
import ksubprocess.Redirect
import ksubprocess.exec
import ktfio.File

class GitCli {

    suspend fun hasGit(): Boolean {
        val result = try {
            exec {
                args("git", "help")
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
                args("git", "init")
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
