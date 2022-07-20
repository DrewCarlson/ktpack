package ktpack.util

import kotlinx.cinterop.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import libzip.*

private val callback = staticCFunction { filepath: CPointer<ByteVar>?, arg: COpaquePointer? ->
    val pathString = filepath?.toKString().orEmpty()
    val sendPath = arg?.asStableRef<(String) -> Unit>()?.get()
    sendPath?.invoke(pathString)
    0
}

actual object Zip : Compressor {
    actual override fun countFiles(archivePath: String): Long {
        val zip = zip_open(archivePath, 0, 'r'.code.toByte())
        val total = zip_entries_total(zip)
        zip_close(zip)
        return total
    }

    actual override fun extract(archivePath: String, outputDir: String): Flow<String> {
        return channelFlow {
            val channelRef = StableRef.create { filepath: String -> trySend(filepath) }
            val result = zip_extract(archivePath, outputDir, callback, channelRef.asCPointer())
            channelRef.dispose()
            if (result != 0) {
                throw ZipException(result, result.toMessage())
            }
        }
    }

    private fun Int.toMessage() = when (this) {
        ZIP_ENOINIT -> "ZIP_ENOINIT $ZIP_ENOINIT: not initialized"
        ZIP_EINVENTNAME -> "ZIP_EINVENTNAME $ZIP_EINVENTNAME: invalid entry name"
        ZIP_ENOENT -> "ZIP_ENOENT $ZIP_ENOENT: entry not found"
        ZIP_EINVMODE -> "ZIP_EINVMODE $ZIP_EINVMODE: invalid zip mode"
        ZIP_EINVLVL -> "ZIP_EINVLVL $ZIP_EINVLVL: invalid compression level"
        ZIP_ENOSUP64 -> "ZIP_ENOSUP64 $ZIP_ENOSUP64: no zip 64 support"
        ZIP_EMEMSET -> "ZIP_EMEMSET $ZIP_EMEMSET: memset error"
        ZIP_EWRTENT -> "ZIP_EWRTENT $ZIP_EWRTENT: cannot write data to entry"
        ZIP_ETDEFLINIT -> "ZIP_ETDEFLINIT $ZIP_ETDEFLINIT: cannot initialize tdefl compressor"
        ZIP_EINVIDX -> "ZIP_EINVIDX $ZIP_EINVIDX: invalid index"
        ZIP_ENOHDR -> "ZIP_ENOHDR $ZIP_ENOHDR: header not found"
        ZIP_ETDEFLBUF -> "ZIP_ETDEFLBUF $ZIP_ETDEFLBUF: cannot flush tdefl buffer"
        ZIP_ECRTHDR -> "ZIP_ECRTHDR $ZIP_ECRTHDR: cannot create entry header"
        ZIP_EWRTHDR -> "ZIP_EWRTHDR $ZIP_EWRTHDR: cannot write entry header"
        ZIP_EWRTDIR -> "ZIP_EWRTDIR $ZIP_EWRTDIR: cannot write to central dir"
        ZIP_EOPNFILE -> "ZIP_EOPNFILE $ZIP_EOPNFILE: cannot open file"
        ZIP_EINVENTTYPE -> "ZIP_EINVENTTYPE $ZIP_EINVENTTYPE: invalid entry type"
        ZIP_EMEMNOALLOC -> "ZIP_EMEMNOALLOC $ZIP_EMEMNOALLOC: extracting data using no memory allocation"
        ZIP_ENOFILE -> "ZIP_ENOFILE $ZIP_ENOFILE: file not found"
        ZIP_ENOPERM -> "ZIP_ENOPERM $ZIP_ENOPERM: no permission"
        ZIP_EOOMEM -> "ZIP_EOOMEM $ZIP_EOOMEM: out of memory"
        ZIP_EINVZIPNAME -> "ZIP_EINVZIPNAME $ZIP_EINVZIPNAME: invalid zip archive name"
        ZIP_EMKDIR -> "ZIP_EMKDIR $ZIP_EMKDIR: make dir error"
        ZIP_ESYMLINK -> "ZIP_ESYMLINK $ZIP_ESYMLINK: symlink error"
        ZIP_ECLSZIP -> "ZIP_ECLSZIP $ZIP_ECLSZIP: close archive error"
        ZIP_ECAPSIZE -> "ZIP_ECAPSIZE $ZIP_ECAPSIZE: capacity size too small"
        ZIP_EFSEEK -> "ZIP_EFSEEK $ZIP_EFSEEK: fseek error"
        ZIP_EFREAD -> "ZIP_EFREAD $ZIP_EFREAD: fread error"
        ZIP_EFWRITE -> "ZIP_EFWRITE $ZIP_EFWRITE: fwrite error"
        else -> "Unknown zip extract error code $this"
    }
}
