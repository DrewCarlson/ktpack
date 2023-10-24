package ktpack.util

import io.ktor.utils.io.errors.*
import ksubprocess.ProcessException
import ksubprocess.Redirect
import ksubprocess.exec
import okio.Path.Companion.toPath


// Splits `name = bob` into 1:name and 2:bob
private val gitconfigRegex = """^\s*([A-Za-z]*)\s?=\s?(.*)$""".toRegex()

class GitCli {

    private val gitPath: String = when (Platform.osFamily) {
        OsFamily.WINDOWS -> "git.exe"
        OsFamily.MACOSX -> "/usr/local/bin/git".toPath().run {
            if (exists()) toString() else "/usr/bin/git"
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

    suspend fun initRepository(directory: String): Boolean {
        val result = try {
            exec {
                args(gitPath, "init")
                workingDirectory = directory
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

    fun discoverAuthorDetails(): Map<String, String> {
        val gitconfig = pathFrom(USER_HOME, ".gitconfig")
        if (!gitconfig.exists()) return emptyMap()
        return gitconfig.readUtf8Lines()
            .filter(gitconfigRegex::containsMatchIn)
            .mapNotNull { config ->
                val result = checkNotNull(gitconfigRegex.find(config))
                val (_, keyName, value) = result.groupValues
                when (val keyLowercase = keyName.lowercase()) {
                    "name" -> keyLowercase to value
                    "email" -> keyLowercase to value
                    else -> null
                }
            }
            .take(2)
            .toMap()
    }
}
