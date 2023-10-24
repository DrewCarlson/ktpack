package ktpack.util

import io.ktor.utils.io.errors.*
import ksubprocess.ProcessException
import ksubprocess.Redirect
import ksubprocess.exec
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use


// Splits `name = bob` into 1:name and 2:bob
private val gitconfigRegex = """^\s*([A-Za-z]*)\s?=\s?(.*)$""".toRegex()

class GitCli(
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {

    private val gitPath: String = when (Platform.osFamily) {
        OsFamily.WINDOWS -> "git.exe"
        OsFamily.MACOSX -> "/usr/local/bin/git".toPath().let {
            if (fileSystem.exists(it)) toString() else "/usr/bin/git"
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
        val gitconfig = USER_HOME.toPath() / ".gitconfig"
        if (!fileSystem.exists(gitconfig)) return emptyMap()
        return fileSystem.source(gitconfig)
            .use { source -> source.buffer().use { it.readUtf8() } }
            .lineSequence()
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
