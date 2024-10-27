package ktpack.util

import kotlinx.io.*
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

val Path.nameWithoutExtension: String
    get() = name.substringBeforeLast(".")

fun Path.writeString(
    string: String,
    onError: ((error: IOException) -> Unit)? = null,
) {
    try {
        SystemFileSystem.sink(this).use { sink ->
            sink.buffered().use { it.writeString(string) }
        }
    } catch (e: IOException) {
        onError?.invoke(e) ?: throw e
    }
}

fun Path.appendString(
    string: String,
    onError: (error: IOException) -> Unit,
) {
    try {
        SystemFileSystem.sink(this, append = true).use { sink ->
            sink.buffered().use { it.writeString(string) }
        }
    } catch (e: IOException) {
        onError(e)
    }
}

fun Path.writeBytes(
    bytes: ByteArray,
    onError: (error: IOException) -> Unit,
) {
    try {
        SystemFileSystem.sink(this).use { sink ->
            sink.buffered().use { it.write(bytes) }
        }
    } catch (e: IOException) {
        onError(e)
    }
}

fun Path.sink(): RawSink {
    return SystemFileSystem.sink(this)
}

fun Path.source(): RawSource {
    return SystemFileSystem.source(this)
}

fun Path.readLinesStrict(): Sequence<String> {
    return sequence {
        SystemFileSystem.source(this@readLinesStrict)
            .use { source ->
                source.buffered().use { bufferedSource ->
                    while (!bufferedSource.exhausted()) {
                        yield(bufferedSource.readLineStrict())
                    }
                }
            }
    }
}

fun Path.readString(): String {
    return SystemFileSystem.source(this).use { source ->
        source.buffered().use { it.readString() }
    }
}

fun Path.readByteArray(): ByteArray {
    return SystemFileSystem.source(this).use { source ->
        source.buffered().use { it.readByteArray() }
    }
}

fun Path.delete(): Boolean {
    if (!exists()) return true
    SystemFileSystem.delete(this, mustExist = false)
    return !exists()
}

fun Path.deleteRecursively(): Boolean {
    if (!exists()) return true
    listRecursively()
        .sortedByDescending { it.toString().length }
        .forEach { SystemFileSystem.delete(it, mustExist = false) }
    SystemFileSystem.delete(this, mustExist = false)
    return !exists()
}

fun Path.mkdirs(mustCreate: Boolean = false): Path {
    SystemFileSystem.createDirectories(this, mustCreate = mustCreate)
    return this
}

fun Path.list(): List<Path> {
    return SystemFileSystem.list(this).toList()
}

fun Path.listRecursively(): List<Path> {
    return list().flatMap { child ->
        if (child.isDirectory()) {
            listOf(child) + child.listRecursively()
        } else {
            listOf(child)
        }
    }
}

fun Path.resolve(): Path {
    return SystemFileSystem.resolve(this)
}

fun Path.exists(): Boolean {
    return SystemFileSystem.exists(this)
}

fun Path.isDirectory(): Boolean {
    return SystemFileSystem.metadataOrNull(this)?.isDirectory == true
}

fun Path.isEmpty(): Boolean {
    return (SystemFileSystem.metadataOrNull(this)?.size ?: 0L) == 0L
}

fun Path.isNotEmpty(): Boolean {
    return !isEmpty()
}

fun Path.createNewFile(): Boolean {
    return try {
        SystemFileSystem.sink(this).close()
        exists()
    } catch (_: IOException) {
        false
    }
}

fun Path.renameTo(path: Path) {
    SystemFileSystem.atomicMove(this, path)
}
