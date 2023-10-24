package ktpack.util

import ktfio.File
import ktfio.deleteRecursively
import okio.*
import okio.Path.Companion.DIRECTORY_SEPARATOR
import okio.Path.Companion.toPath

fun pathFrom(vararg parts: String): Path {
    return buildString(parts.sumOf { it.length }) {
        parts.forEachIndexed { index, part ->
            if (index > 0) {
                append(DIRECTORY_SEPARATOR)
            }
            append(part)
        }
    }.toPath()
}

val Path.nameWithoutExtension: String
    get() = name.substringBeforeLast(".")

fun Path.writeUtf8(
    string: String,
    onError: (error: IOException) -> Unit,
) {
    try {
        FileSystem.SYSTEM.sink(this).use { sink ->
            sink.buffer().use { it.writeUtf8(string) }
        }
    } catch (e: IOException) {
        onError(e)
    }
}

fun Path.readUtf8Lines(): Sequence<String> {
    return sequence {
        FileSystem.SYSTEM.source(this@readUtf8Lines)
            .use { source ->
                source.buffer().use { bufferedSource ->
                    while (!bufferedSource.exhausted()) {
                        yield(bufferedSource.readUtf8LineStrict())
                    }
                }
            }
    }
}

fun Path.readUtf8(): String {
    return FileSystem.SYSTEM.source(this).use { source ->
        source.buffer().use { it.readUtf8() }
    }
}

fun Path.readByteArray(): ByteArray {
    return FileSystem.SYSTEM.source(this).use { source ->
        source.buffer().use { it.readByteArray() }
    }
}

fun Path.delete(): Boolean {
    File(toString()).delete()
    // TODO: Doesn't work on windows FileSystem.SYSTEM.delete(this)
    return !exists()
}

fun Path.deleteRecursively(): Boolean {
    File(toString()).deleteRecursively()
    // TODO: Doesn't work on windows FileSystem.SYSTEM.deleteRecursively(this)
    return !exists()
}

fun Path.mkdirs(mustCreate: Boolean = false): Path {
    FileSystem.SYSTEM.createDirectories(this, mustCreate = mustCreate)
    return this
}

fun Path.list(): List<Path> {
    return FileSystem.SYSTEM.listOrNull(this) ?: emptyList()
}

fun Path.listRecursively(): Sequence<Path> {
    return FileSystem.SYSTEM.listRecursively(this)
}

fun Path.exists(): Boolean {
    return FileSystem.SYSTEM.exists(this)
}

fun Path.isDirectory(): Boolean {
    return FileSystem.SYSTEM.metadataOrNull(this)?.isDirectory == true
}

fun Path.isEmpty(): Boolean {
    return (FileSystem.SYSTEM.listOrNull(this)?.size ?: 0) == 0
}

fun Path.isNotEmpty(): Boolean {
    return (FileSystem.SYSTEM.listOrNull(this)?.size ?: 0) > 0
}

fun Path.createNewFile(): Boolean {
    return try {
        FileSystem.SYSTEM.openReadWrite(this).close()
        exists()
    } catch (_: IOException) {
        false
    }
}

fun Path.renameTo(path: Path): Boolean {
    return try {
        FileSystem.SYSTEM.atomicMove(this, path)
        true
    } catch (_: Exception) {
        false
    }
}
